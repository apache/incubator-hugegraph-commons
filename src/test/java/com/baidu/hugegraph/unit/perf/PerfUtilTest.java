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
import com.baidu.hugegraph.unit.perf.testclass.TestClass1;
import com.baidu.hugegraph.unit.perf.testclass.TestClass2;
import com.baidu.hugegraph.unit.perf.testclass2.TestClass4Package;
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
        assertContains(json, "foo.name", "foo");
        assertContains(json, "foo.parent", "");
        assertContains(json, "foo.total_cost");
        assertContains(json, "foo.min_cost");
        assertContains(json, "foo.max_cost");
        assertContains(json, "foo.total_self_wasted");
        assertContains(json, "foo.total_children_wasted", -1);
        assertContains(json, "foo.total_children_times", -1);

        assertContains(json, "foo/bar.times", 1);
        assertContains(json, "foo/bar.name", "bar");
        assertContains(json, "foo/bar.parent", "foo");
        assertContains(json, "foo/bar.total_cost");
        assertContains(json, "foo/bar.min_cost");
        assertContains(json, "foo/bar.max_cost");
        assertContains(json, "foo/bar.total_self_wasted");
        assertContains(json, "foo/bar.total_children_wasted", -1);
        assertContains(json, "foo/bar.total_children_times", -1);

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

        PerfUtil.profileSingleThread(false);

        obj.foo();

        perf.toString();
        perf.toECharts();
        json = perf.toJson();

        assertContains(json, "foo.times", 3);
        assertContains(json, "foo/bar.times", 3);
    }

    @Test
    public void testPerfUtilWithProfilePackage() throws Throwable {
        perf.profilePackage("com.baidu.hugegraph.unit.perf.testclass2");

        TestClass4Package.Foo obj = new TestClass4Package.Foo();
        obj.foo();

        perf.toString();
        perf.toECharts();
        String json = perf.toJson();

        assertContains(json, "foo.times", 1);
        assertContains(json, "foo/bar.times", 1);

        TestClass4Package test = new TestClass4Package();
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

        perf.toString();
        perf.toECharts();
        String json = perf.toJson();
        assertContains(json, "func.times", 1);
        assertContains(json, "func1.times", 1);
        assertContains(json, "func3.times", 3);
    }

    @Test
    public void testPerfUtilWithProfileManually() throws Throwable {
        perf.profileClass(prefix + "TestClass$ManuallyProfile");

        TestClass.ManuallyProfile obj = new TestClass.ManuallyProfile();

        obj.foo();

        perf.toString();
        perf.toECharts();
        String json = perf.toJson();

        assertContains(json, "manu-foo.times", 1);
        assertContains(json, "manu-foo/manu-bar.times", 1);
        assertContains(json, "manu-foo/manu-bar2.times", 1);

        obj.foo();
        obj.bar();

        perf.toString();
        perf.toECharts();
        json = perf.toJson();

        assertContains(json, "manu-foo.times", 2);
        assertContains(json, "manu-foo/manu-bar.times", 2);
        assertContains(json, "manu-bar.times", 1);

        obj.foo2();
        obj.bar2();

        perf.toString();
        perf.toECharts();
        json = perf.toJson();

        assertContains(json, "manu-foo2.times", 1);
        assertContains(json, "manu-foo2/manu-bar.times.times", 1);
        assertContains(json, "manu-foo2/manu-bar2.times.times", 1);
        assertContains(json, "manu-foo.times", 2);
        assertContains(json, "manu-foo/manu-bar.times", 2);
        assertContains(json, "manu-bar.times", 1);
        assertContains(json, "manu-bar2.times", 1);
    }

    @Test
    public void testPerfUtilPerf() throws Throwable {
        perf.profileClass(prefix + "TestClass1");
        perf.profileClass(prefix + "TestClass1$Foo");

        PerfUtil.profileSingleThread(true);
        PerfUtil.useLocalTimer(true);

        int times = 10000000;
        TestClass1 test = new TestClass1();
        test.test(times);
        test.testNew();

        perf.toString();
        perf.toECharts();
        String json = perf.toJson();

        assertContains(json, "testNew.times", 1);
        assertContains(json, "test/testNew.times", times);
        assertContains(json, "test/testNewAndCall.times", times);
        assertContains(json, "test/testCall.times", times);
        assertContains(json, "test/testCallFooThenSum.times", times);

        assertContains(json, "test/testNewAndCall/sum.times", times);
        assertContains(json, "test/testCall/sum.times", times);
        assertContains(json, "test/testCallFooThenSum/foo.times", times);
        assertContains(json, "test/testCallFooThenSum/foo/sum.times", times);

        // Test call multi-times and Reset false
        PerfUtil.profileSingleThread(true);
        PerfUtil.profileSingleThread(true);
        PerfUtil.profileSingleThread(false);
        PerfUtil.profileSingleThread(false);
        PerfUtil.useLocalTimer(true);
        PerfUtil.useLocalTimer(true);
        PerfUtil.useLocalTimer(false);
        PerfUtil.useLocalTimer(false);

        test.testNew();
        json = perf.toJson();
        assertContains(json, "testNew.times", 2);
//        System.out.println(Stopwatch.wastedStart0+", "+Stopwatch.wastedEnd0);
    }

    @Test
    public void testPerfUtilPerf4LightStopwatch() throws Throwable {
        perf.profileClass(prefix + "TestClass2");
        perf.profileClass(prefix + "TestClass2$Foo");

        PerfUtil.profileSingleThread(true);
        PerfUtil.useLightStopwatch(true);
        PerfUtil.useLocalTimer(true);

        int times = 10000000;
        TestClass2 test = new TestClass2();
        test.test(times);
        test.testNew();

        perf.toString();
        perf.toECharts();
        String json = perf.toJson();

        assertContains(json, "testNew.times", 1);
        assertContains(json, "test/testNew.times", times);
        assertContains(json, "test/testNewAndCall.times", times);
        assertContains(json, "test/testCall.times", times);
        assertContains(json, "test/testCallFooThenSum.times", times);

        assertContains(json, "test/testNewAndCall/sum.times", times);
        assertContains(json, "test/testCall/sum.times", times);
        assertContains(json, "test/testCallFooThenSum/foo.times", times);
        assertContains(json, "test/testCallFooThenSum/foo/sum.times", times);

        // Test call multi-times and Reset false
        PerfUtil.profileSingleThread(true);
        PerfUtil.profileSingleThread(true);
        PerfUtil.profileSingleThread(false);
        PerfUtil.profileSingleThread(false);
        PerfUtil.useLocalTimer(true);
        PerfUtil.useLocalTimer(true);
        PerfUtil.useLocalTimer(false);
        PerfUtil.useLocalTimer(false);
        PerfUtil.useLightStopwatch(false);
        PerfUtil.useLightStopwatch(false);

        test.testNew();
        json = perf.toJson();
        assertContains(json, "testNew.times", 2);
    }

    private static void assertContains(String json, String key)
                                       throws Exception {
        Assert.assertNotNull("Not exist key " + key, actualValue(json, key));
    }

    private static void assertContains(String json, String key, Object value)
                                       throws Exception {
        Assert.assertEquals(value, actualValue(json, key));
    }

    private static Object actualValue(String json, String key)
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
        return actual;
    }
}
