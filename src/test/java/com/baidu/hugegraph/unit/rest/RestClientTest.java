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

package com.baidu.hugegraph.unit.rest;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;
import org.junit.Test;
import org.mockito.Mockito;

import com.baidu.hugegraph.rest.RestClient;
import com.baidu.hugegraph.rest.RestResult;
import com.baidu.hugegraph.testutil.Assert;
import com.google.common.collect.ImmutableMap;

public class RestClientTest {

    private static RestClient restClient = new RestClientImpl("/test", 3000);

    private static class RestClientImpl extends RestClient {

        public RestClientImpl(String url, int timeout) {
            super(url, timeout);
        }

        @Override
        protected Response request(Callable<Response> method) {
            Response response = Mockito.mock(Response.class);
            Mockito.when(response.getStatus()).thenReturn(200);
            Mockito.when(response.getHeaders())
                   .thenReturn(ImmutableMultivaluedMap.empty());
            Mockito.when(response.readEntity(String.class))
                   .thenReturn("");
            return response;
        }

        @Override
        protected void checkStatus(Response response,
                                   Response.Status... statuses) {
            // pass
        }
    }

    @Test
    public void testPost() {
        RestResult restResult = restClient.post("path", new Object());
        Assert.assertEquals(200, restResult.status());
    }

    @Test
    public void testPostWithHeader() {
        MultivaluedMap<String, Object> header = ImmutableMultivaluedMap.empty();
        RestResult restResult = restClient.post("path", new Object(), header);
        Assert.assertEquals(200, restResult.status());
    }

    @Test
    public void testPostWithHeaderAndParams() {
        MultivaluedMap<String, Object> header = ImmutableMultivaluedMap.empty();
        Map<String, Object> params = ImmutableMap.of("param1", "value1");
        RestResult restResult = restClient.post("path", new Object(), header,
                                                params);
        Assert.assertEquals(200, restResult.status());
    }

    @Test
    public void testPut() {
        RestResult restResult = restClient.put("path", "id1", new Object());
        Assert.assertEquals(200, restResult.status());
    }

    @Test
    public void testPutWithParams() {
        Map<String, Object> params = ImmutableMap.of("param1", "value1");
        RestResult restResult = restClient.put("path", "id1", new Object(),
                                               params);
        Assert.assertEquals(200, restResult.status());
    }

    @Test
    public void testGet() {
        RestResult restResult = restClient.get("path");
        Assert.assertEquals(200, restResult.status());
    }

    @Test
    public void testGetWithId() {
        RestResult restResult = restClient.get("path", "id1");
        Assert.assertEquals(200, restResult.status());
    }

    @Test
    public void testGetWithParams() {
        Map<String, Object> params = ImmutableMap.of("param1", "value1");
        RestResult restResult = restClient.get("path", params);
        Assert.assertEquals(200, restResult.status());
    }

    @Test
    public void testDeleteWithId() {
        RestResult restResult = restClient.delete("path", "id1");
        Assert.assertEquals(200, restResult.status());
    }

    @Test
    public void testDeleteWithParams() {
        Map<String, Object> params = ImmutableMap.of("param1", "value1");
        RestResult restResult = restClient.delete("path", params);
        Assert.assertEquals(200, restResult.status());
    }
}
