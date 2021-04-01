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

package com.baidu.hugegraph.unit.perf;

import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.baidu.hugegraph.perf.PerfUtil;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.unit.BaseUnitTest;
import com.baidu.hugegraph.unit.perf.testclass.TestClass;
import com.baidu.hugegraph.unit.perf.testclass2.TestClass2;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PerfUtilTest extends BaseUnitTest {

    private static final String prefix =
                                "com.baidu.hugegraph.unit.perf.testclass.";
    private static final PerfUtil perf = PerfUtil.instance();

    @After
    public void teardown() {
        perf.clear();
    }

    @Test
    public void testPerfUtil() throws Throwable {
        perf.profileClass(prefix + "TestClass$Foo");

        TestClass.Foo obj = new TestClass.Foo();
        obj.foo();

        perf.toString();
        perf.toECharts();
        String json = perf.toJson();

        assertContains(json, "foo.times", 1);
        assertContains(json, "foo/bar.times", 1);

        TestClass test = new TestClass();
        test.test();
        json = perf.toJson();
        assertContains(json, "bar.times", 1);
        assertContains(json, "foo.times", 1);
        assertContains(json, "foo/bar.times", 1);

        perf.clear();

        obj.foo();
        obj.foo();

        perf.toString();
        perf.toECharts();
        json = perf.toJson();

        assertContains(json, "foo.times", 2);
        assertContains(json, "foo/bar.times", 2);
    }

    @Test
    public void testPerfUtilWithSingleThread() throws Throwable {
        perf.profileClass(prefix + "TestClass$Bar");
        PerfUtil.profileSingleThread(true);

        TestClass.Bar obj = new TestClass.Bar();
        obj.foo();
        perf.toString();
        perf.toECharts();
        String json = perf.toJson();

        assertContains(json, "foo.times", 1);
        assertContains(json, "foo/bar.times", 1);

        perf.clear();

        obj.foo();
        obj.foo();

        perf.toString();
        perf.toECharts();
        json = perf.toJson();

        assertContains(json, "foo.times", 2);
        assertContains(json, "foo/bar.times", 2);
    }

    @Test
    public void testPerfUtilWithProfilePackage() throws Throwable {
        perf.profilePackage("com.baidu.hugegraph.unit.perf.testclass2");

        TestClass2.Foo obj = new TestClass2.Foo();
        obj.foo();

        perf.toString();
        perf.toECharts();
        String json = perf.toJson();

        assertContains(json, "foo.times", 1);
        assertContains(json, "foo/bar.times", 1);

        TestClass2 test = new TestClass2();
        test.test();
        json = perf.toJson();
        assertContains(json, "test.times", 1);
        assertContains(json, "test/bar.times", 1);
        assertContains(json, "foo.times", 1);
        assertContains(json, "foo/bar.times", 1);

        perf.clear();

        obj.foo();
        obj.foo();

        perf.toString();
        perf.toECharts();
        json = perf.toJson();

        assertContains(json, "foo.times", 2);
        assertContains(json, "foo/bar.times", 2);
    }

    @Test
    public void testPerfUtilWithProfileParentClass() throws Throwable {
        perf.profileClass(prefix + "TestClass$Base");
        perf.profileClass(prefix + "TestClass$Sub");

        TestClass.Sub obj = new TestClass.Sub();
        obj.func();
        obj.func1();
        obj.func2();
        obj.func3();
        obj.func3();
        obj.func3();

        String json = perf.toJson();
        assertContains(json, "func.times", 1);
        assertContains(json, "func1.times", 1);
        assertContains(json, "func3.times", 3);
    }

    private static void assertContains(String json, String key, Object value)
                                       throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> map = mapper.readValue(json, Map.class);
        String[] keys = key.split("\\.");
        Object actual = null;
        for (String k : keys) {
            actual = map.get(k);
            if (actual instanceof Map) {
                map = (Map<?, ?>) actual;
            }
        }
        Assert.assertEquals(value, actual);
    }
}
