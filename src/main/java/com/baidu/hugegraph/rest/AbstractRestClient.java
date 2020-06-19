/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.rest;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.uri.UriComponent;

import com.baidu.hugegraph.util.ExecutorUtil;
import com.google.common.collect.ImmutableMap;

public abstract class AbstractRestClient implements RestClient {

    // Time unit: hours
    private static final long TTL = 24L;
    // Time unit: seconds
    private static final long IDLE_TIME = 40L;
    // Time unit: seconds
    private static final long CHECK_PERIOD = IDLE_TIME / 2;

    private final Client client;
    private final WebTarget target;

    private PoolingHttpClientConnectionManager pool;
    private ScheduledExecutorService cleanExecutor;

    public AbstractRestClient(String url, int timeout) {
        this(url, new ConfigBuilder().config(timeout).build());
    }

    public AbstractRestClient(String url, String user, String password,
                              int timeout) {
        this(url, new ConfigBuilder().config(timeout)
                                     .config(user, password)
                                     .build());
    }

    public AbstractRestClient(String url, int timeout, int maxTotal,
                              int maxPerRoute) {
        this(url, new ConfigBuilder().config(timeout)
                                     .config(maxTotal, maxPerRoute)
                                     .build());
    }

    public AbstractRestClient(String url, String user, String password,
                              int timeout, int maxTotal, int maxPerRoute) {
        this(url, new ConfigBuilder().config(timeout)
                                     .config(user, password)
                                     .config(maxTotal, maxPerRoute)
                                     .build());
    }

    public AbstractRestClient(String url, String user, String password,
                              int timeout, int maxTotal, int maxPerRoute,
                              String protocol, String trustStoreFile,
                              String trustStorePassword) {
        this(url, new ConfigBuilder().config(timeout)
                                     .config(user, password)
                                     .config(maxTotal, maxPerRoute)
                                     .config(protocol, trustStoreFile,
                                             trustStorePassword)
                                     .build());
    }

    public AbstractRestClient(String url, ClientConfig config) {
        Client client = null;
        Object protocol = config.getProperty("protocol");
        if (protocol != null && protocol.equals("https")) {
            client = wrapTrustConfig(url, config);
        } else {
            client = ClientBuilder.newClient(config);
        }
        this.client = client;
        this.client.register(GZipEncoder.class);
        this.target = this.client.target(url);
        this.pool = (PoolingHttpClientConnectionManager) config.getProperty(
                    ApacheClientProperties.CONNECTION_MANAGER);
        if (this.pool != null) {
            this.cleanExecutor = ExecutorUtil.newScheduledThreadPool(
                                              "conn-clean-worker-%d");
            this.cleanExecutor.scheduleWithFixedDelay(() -> {
                PoolStats stats = this.pool.getTotalStats();
                int using = stats.getLeased() + stats.getPending();
                if (using > 0) {
                    // Do clean only when all connections are idle
                    return;
                }
                this.pool.closeIdleConnections(IDLE_TIME, TimeUnit.SECONDS);
                this.pool.closeExpiredConnections();
            }, CHECK_PERIOD, CHECK_PERIOD, TimeUnit.SECONDS);
        }
    }

    protected abstract void checkStatus(Response response,
                                        Response.Status... statuses);

    protected Response request(Callable<Response> method) {
        try {
            return method.call();
        } catch (Exception e) {
            throw new ClientException("Failed to do request", e);
        }
    }

    @Override
    public RestResult post(String path, Object object) {
        return this.post(path, object, null, null);
    }

    @Override
    public RestResult post(String path, Object object,
                           MultivaluedMap<String, Object> headers) {
        return this.post(path, object, headers, null);
    }

    @Override
    public RestResult post(String path, Object object,
                           Map<String, Object> params) {
        return this.post(path, object, null, params);
    }

    @Override
    public RestResult post(String path, Object object,
                           MultivaluedMap<String, Object> headers,
                           Map<String, Object> params) {
        Pair<Builder, Entity<?>> pair = this.buildRequest(path, null, object,
                                                          headers, params);
        Response response = this.request(() -> {
            // pair.getLeft() is builder, pair.getRight() is entity (http body)
            return pair.getLeft().post(pair.getRight());
        });
        // If check status failed, throw client exception.
        checkStatus(response, Response.Status.CREATED,
                    Response.Status.OK, Response.Status.ACCEPTED);
        return new RestResult(response);
    }

    @Override
    public RestResult put(String path, String id, Object object) {
        return this.put(path, id, object, ImmutableMap.of());
    }

    @Override
    public RestResult put(String path, String id, Object object,
                          MultivaluedMap<String, Object> headers) {
        return this.put(path, id, object, headers, null);
    }

    @Override
    public RestResult put(String path, String id, Object object,
                          Map<String, Object> params) {
        return this.put(path, id, object, null, params);
    }

    @Override
    public RestResult put(String path, String id, Object object,
                          MultivaluedMap<String, Object> headers,
                          Map<String, Object> params) {
        Pair<Builder, Entity<?>> pair = this.buildRequest(path, id, object,
                                                          headers, params);
        Response response = this.request(() -> {
            // pair.getLeft() is builder, pair.getRight() is entity (http body)
            return pair.getLeft().put(pair.getRight());
        });
        // If check status failed, throw client exception.
        checkStatus(response, Response.Status.OK, Response.Status.ACCEPTED);
        return new RestResult(response);
    }

    @Override
    public RestResult get(String path) {
        Response response = this.request(() -> {
            return this.target.path(path).request().get();
        });
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    @Override
    public RestResult get(String path, Map<String, Object> params) {
        Ref<WebTarget> target = Refs.of(this.target);
        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (value instanceof Collection) {
                for (Object i : (Collection<?>) value) {
                    target.set(target.get().queryParam(key, i));
                }
            } else {
                target.set(target.get().queryParam(key, value));
            }
        }
        Response response = this.request(() -> {
            return target.get().path(path).request().get();
        });
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    @Override
    public RestResult get(String path, String id) {
        Response response = this.request(() -> {
            return this.target.path(path).path(encode(id)).request().get();
        });
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    @Override
    public RestResult delete(String path, Map<String, Object> params) {
        Ref<WebTarget> target = Refs.of(this.target);
        for (String key : params.keySet()) {
            target.set(target.get().queryParam(key, params.get(key)));
        }
        Response response = this.request(() -> {
            return target.get().path(path).request().delete();
        });
        checkStatus(response, Response.Status.NO_CONTENT,
                    Response.Status.ACCEPTED);
        return new RestResult(response);
    }

    @Override
    public RestResult delete(String path, String id) {
        Response response = this.request(() -> {
            return this.target.path(path).path(encode(id)).request().delete();
        });
        checkStatus(response, Response.Status.NO_CONTENT,
                    Response.Status.ACCEPTED);
        return new RestResult(response);
    }

    @Override
    public void close() {
        if (this.pool != null) {
            this.pool.close();
            this.cleanExecutor.shutdownNow();
        }
        this.client.close();
    }

    private Pair<Builder, Entity<?>> buildRequest(
                                     String path, String id, Object object,
                                     MultivaluedMap<String, Object> headers,
                                     Map<String, Object> params) {
        WebTarget target = this.target;
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                target = target.queryParam(param.getKey(), param.getValue());
            }
        }

        Builder builder = id == null ? target.path(path).request() :
                          target.path(path).path(encode(id)).request();

        String encoding = null;
        if (headers != null && !headers.isEmpty()) {
            // Add headers
            builder = builder.headers(headers);
            encoding = (String) headers.getFirst("Content-Encoding");
        }

        /*
         * We should specify the encoding of the entity object manually,
         * because Entity.json() method will reset "content encoding =
         * null" that has been set up by headers before.
         */
        Entity<?> entity;
        if (encoding == null) {
            entity = Entity.json(object);
        } else {
            Variant variant = new Variant(MediaType.APPLICATION_JSON_TYPE,
                                          (String) null, encoding);
            entity = Entity.entity(object, variant);
        }
        return Pair.of(builder, entity);
    }

    private static Client wrapTrustConfig(String url, ClientConfig config) {

        SslConfigurator sslConfig = SslConfigurator.newInstance();
        String trustStoreFile = config.getProperty("trustStoreFile").toString();
        String trustStorePassword = config.getProperty("trustStorePassword")
                                          .toString();
        sslConfig.trustStoreFile(trustStoreFile)
                 .trustStorePassword(trustStorePassword);
        sslConfig.securityProtocol("SSL");
        SSLContext context = sslConfig.createSSLContext();
        TrustManager[] trustAllManager = NoCheckTrustManager.create();
        try {
            context.init(null, trustAllManager, new SecureRandom());
        } catch (KeyManagementException e) {
            throw new ClientException("Failed to init security management", e);
        }
        return ClientBuilder.newBuilder()
                            .hostnameVerifier(new HostNameVerifier(url))
                            .sslContext(context)
                            .build();
    }

    public static String encode(String raw) {
        return UriComponent.encode(raw, UriComponent.Type.PATH_SEGMENT);
    }

    public static class HostNameVerifier implements HostnameVerifier {

        private final String url;

        public HostNameVerifier(String url) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            url = URI.create(url).getHost();
            this.url = url;
        }

        @Override
        public boolean verify(String hostname, SSLSession session) {
            if (!this.url.isEmpty() && this.url.endsWith(hostname)) {
                return true;
            } else {
                HostnameVerifier verifier = HttpsURLConnection.getDefaultHostnameVerifier();
                return verifier.verify(hostname, session);
            }
        }
    }

    private static class NoCheckTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                                       throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                                       throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public static TrustManager[] create() {
            return new TrustManager[]{new NoCheckTrustManager()};
        }
    }

    private static class ConfigBuilder {

        private final ClientConfig config;

        public ConfigBuilder() {
            this.config = new ClientConfig();
        }

        public ConfigBuilder config(int timeout) {
            this.config.property(ClientProperties.CONNECT_TIMEOUT, timeout);
            this.config.property(ClientProperties.READ_TIMEOUT, timeout);
            return this;
        }

        public ConfigBuilder config(String username, String password) {
            /*
             * NOTE: don't use non-preemptive mode
             * In non-preemptive mode the authentication information is added
             * only when server refuses the request with 401 status code and
             * then the request is repeated.
             * Non-preemptive has negative impact on the performance. The
             * advantage is it doesn't send credentials when they are not needed
             * https://jersey.github.io/documentation/latest/client.html#d0e5461
             */
            this.config.register(HttpAuthenticationFeature.basic(username,
                                                                 password));
            return this;
        }

        public ConfigBuilder config(int maxTotal, int maxPerRoute) {
            /*
             * Using httpclient with connection pooling, and configuring the
             * jersey connector, reference:
             * http://www.theotherian.com/2013/08/jersey-client-2.0-httpclient-timeouts-max-connections.html
             * https://stackoverflow.com/questions/43228051/memory-issue-with-jax-rs-using-jersey/46175943#46175943
             *
             * But the jersey that has been released in the maven central
             * repository seems to have a bug.
             * https://github.com/jersey/jersey/pull/3752
             */
            PoolingHttpClientConnectionManager pool;
            pool = new PoolingHttpClientConnectionManager(TTL, TimeUnit.HOURS);
            pool.setMaxTotal(maxTotal);
            pool.setDefaultMaxPerRoute(maxPerRoute);
            this.config.property(ApacheClientProperties.CONNECTION_MANAGER,
                                 pool);
            this.config.connectorProvider(new ApacheConnectorProvider());
            return this;
        }

        public ConfigBuilder config(String protocol, String trustStoreFile,
                                    String trustStorePassword) {
            this.config.property("protocol", protocol);
            this.config.property("trustStoreFile", trustStoreFile);
            this.config.property("trustStorePassword", trustStorePassword);
            return this;
        }

        public ClientConfig build() {
            return this.config;
        }
    }
}
