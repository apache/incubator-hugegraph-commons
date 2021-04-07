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

import org.junit.Test;

import com.baidu.hugegraph.perf.Stopwatch;
import com.baidu.hugegraph.perf.Stopwatch.Path;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.unit.BaseUnitTest;

public class StopwatchTest extends BaseUnitTest {

    @Test
    public void testStopwatchChild() {
        Stopwatch watch1 = new Stopwatch("w1", Path.EMPTY);

        Stopwatch watch2 = new Stopwatch("w2", watch1);
        Stopwatch watch3 = new Stopwatch("w3", watch1);
        Stopwatch watch4 = new Stopwatch("w4", watch1);
        Stopwatch watch5 = new Stopwatch("w5", watch1);

        Assert.assertEquals(watch2, watch1.child("w2"));
        Assert.assertEquals(watch3, watch1.child("w3"));
        Assert.assertEquals(watch4, watch1.child("w4"));
        Assert.assertEquals(watch5, watch1.child("w5"));

        Assert.assertEquals(watch2, watch1.child("w2", null));
        Assert.assertEquals(watch3, watch1.child("w3", null));
        Assert.assertEquals(watch4, watch1.child("w4", null));
        Assert.assertEquals(watch5, watch1.child("w5", null));

        Assert.assertEquals(null, watch1.child("w2"));
        Assert.assertEquals(null, watch1.child("w3"));
        Assert.assertEquals(null, watch1.child("w4"));
        Assert.assertEquals(null, watch1.child("w5"));
    }
}
