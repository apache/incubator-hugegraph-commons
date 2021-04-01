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

package com.baidu.hugegraph.unit.perf.testclass;

import com.baidu.hugegraph.perf.PerfUtil.Watched;

public class TestClass {

    @Watched
    public void test() {
        new Foo().bar();
    }

    public static class Foo {

        @Watched
        public void foo() {
            this.bar();
        }

        @Watched
        public void bar() {}
    }

    public static class Bar {

        @Watched
        public void foo() {
            this.bar();
        }

        @Watched
        public void bar() {}
    }

    public static class Base {

        @Watched
        public void func() {}
    }

    public static class Sub extends Base {

        @Watched
        public void func1() {}

        public void func2() {}

        @Watched
        public void func3() {}
    }
}
