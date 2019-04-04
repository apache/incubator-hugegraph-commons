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

import static org.glassfish.jersey.apache.connector.ApacheClientProperties.CONNECTION_MANAGER;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.apache.http.config.SocketConfig;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.uri.UriComponent;

import com.google.common.collect.ImmutableMap;

public abstract class RestClient {

    private final ClientConfig config;
    private final Client client;
    private final WebTarget target;

    public RestClient(String url, int timeout) {
        this(url, new ConfigBuilder().config(timeout).build());
    }

    public RestClient(String url, int timeout, int maxTotal, int maxPerRoute) {
        this(url, new ConfigBuilder().config(timeout)
                                     .config(timeout, maxTotal, maxPerRoute)
                                     .build());
    }

    public RestClient(String url, String user, String password, int timeout) {
        this(url, new ConfigBuilder().config(timeout)
                                     .config(user, password)
                                     .build());
    }

    public RestClient(String url, String user, String password, int timeout,
                      int maxTotal, int maxPerRoute) {
        this(url, new ConfigBuilder().config(timeout)
                                     .config(user, password)
                                     .config(timeout, maxTotal, maxPerRoute)
                                     .build());
    }
    
    public RestClient(String url, ClientConfig config) {
        this.config = config;
        this.client = ClientBuilder.newClient(this.config);
        this.client.register(GZipEncoder.class);
        this.target = this.client.target(url);
    }

    private Response request(Callable<Response> method) {
        try {
            return method.call();
        } catch (Exception e) {
            throw new ClientException("Failed to do request", e);
        }
    }

    public RestResult post(String path, Object object) {
        return this.post(path, object, null);
    }

    public RestResult post(String path, Object object,
                           MultivaluedMap<String, Object> headers) {
        return this.post(path, object, headers, null);
    }

    public RestResult post(String path, Object object,
                           MultivaluedMap<String, Object> headers,
                           Map<String, Object> params) {
        WebTarget target = this.target;
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                target = target.queryParam(param.getKey(), param.getValue());
            }
        }

        Ref<Invocation.Builder> builder = Refs.of(target.path(path).request());

        String encoding = null;
        if (headers != null && !headers.isEmpty()) {
            // Add headers
            builder.set(builder.get().headers(headers));
            encoding = (String) headers.getFirst("Content-Encoding");
        }

        /*
         * We should specify the encoding of the entity object manually,
         * because Entity.json() method will reset "content encoding =
         * null" that has been set up by headers before.
         */
        Ref<Entity<?>> entity = Refs.of(null);
        if (encoding == null) {
            entity.set(Entity.json(object));
        } else {
            Variant variant = new Variant(MediaType.APPLICATION_JSON_TYPE,
                                          (String) null, encoding);
            entity.set(Entity.entity(object, variant));
        }

        Response response = this.request(() -> {
            return builder.get().post(entity.get());
        });
        // If check status failed, throw client exception.
        checkStatus(response, Response.Status.CREATED,
                    Response.Status.OK, Response.Status.ACCEPTED);
        return new RestResult(response);
    }

    public RestResult put(String path, String id, Object object) {
        return this.put(path, id, object, ImmutableMap.of());
    }

    public RestResult put(String path, String id, Object object,
                          Map<String, Object> params) {
        Ref<WebTarget> target = Refs.of(this.target);
        if (params != null && !params.isEmpty()) {
            for (String key : params.keySet()) {
                target.set(target.get().queryParam(key, params.get(key)));
            }
        }

        Response response = this.request(() -> {
            return target.get().path(path).path(encode(id)).request()
                         .put(Entity.json(object));
        });
        // If check status failed, throw client exception.
        checkStatus(response, Response.Status.OK, Response.Status.ACCEPTED);
        return new RestResult(response);
    }

    public RestResult get(String path) {
        Response response = this.request(() -> {
            return this.target.path(path).request().get();
        });
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

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

    public RestResult get(String path, String id) {
        Response response = this.request(() -> {
            return this.target.path(path).path(encode(id)).request().get();
        });
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

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

    public RestResult delete(String path, String id) {
        Response response = this.request(() -> {
            return this.target.path(path).path(encode(id)).request().delete();
        });
        checkStatus(response, Response.Status.NO_CONTENT,
                    Response.Status.ACCEPTED);
        return new RestResult(response);
    }

    public void close() {
        this.client.close();
    }

    private static String encode(String raw) {
        return UriComponent.encode(raw, UriComponent.Type.PATH_SEGMENT);
    }

    protected abstract void checkStatus(Response response,
                                        Response.Status... statuses);

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

        public ConfigBuilder config(int timeout, int maxTotal,
                                    int maxPerRoute) {
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
            PoolingHttpClientConnectionManager pcm =
                    new PoolingHttpClientConnectionManager();
            pcm.setDefaultSocketConfig(SocketConfig.custom()
                                                   .setSoTimeout(timeout)
                                                   .build());
            pcm.setMaxTotal(maxTotal);
            pcm.setDefaultMaxPerRoute(maxPerRoute);
            this.config.property(CONNECTION_MANAGER, pcm);
            this.config.connectorProvider(new ApacheConnectorProvider());
            return this;
        }

        public ClientConfig build() {
            return this.config;
        }
    }
}
