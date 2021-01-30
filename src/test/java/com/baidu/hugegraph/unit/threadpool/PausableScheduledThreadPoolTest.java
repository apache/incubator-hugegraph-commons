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

package com.baidu.hugegraph.unit.threadpool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.baidu.hugegraph.threadpool.PausableScheduledThreadPool;
import com.baidu.hugegraph.util.ExecutorUtil;

public class PausableScheduledThreadPoolTest {

    @Test
    public void testscheduleWithFixedDelay() throws InterruptedException {
        PausableScheduledThreadPool executor =
                ExecutorUtil.newPausableScheduledThreadPool("test");
        AtomicInteger counter = new AtomicInteger(0);
        executor.scheduleWithFixedDelay(() -> {
            System.out.println("counter: " + counter.incrementAndGet());
        }, 2, 2, TimeUnit.SECONDS);

        Thread.sleep(4500);
        Assert.assertEquals(2, counter.get());

        // pause
        executor.pauseSchedule();
        Thread.sleep(2000);
        Assert.assertEquals(2, counter.get());

        // resume
        executor.resumeSchedule();
        Thread.sleep(2000);
        Assert.assertEquals(3, counter.get());

        // pause again
        executor.pauseSchedule();

        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
    }

    @Test
    public void testscheduleWithFixedRate() throws InterruptedException {
        PausableScheduledThreadPool executor =
                ExecutorUtil.newPausableScheduledThreadPool(2, "test");
        AtomicInteger counter = new AtomicInteger(0);
        executor.scheduleAtFixedRate(() -> {
            System.out.println("counter: " + counter.incrementAndGet());
        }, 2, 2, TimeUnit.SECONDS);
        Thread.sleep(4500);
        Assert.assertEquals(2, counter.get());

        // pause
        executor.pauseSchedule();
        Thread.sleep(2000);
        Assert.assertEquals(2, counter.get());

        // resume
        executor.resumeSchedule();
        Thread.sleep(2000);
        Assert.assertEquals(4, counter.get());

        // pause again
        executor.pauseSchedule();

        executor.shutdownNow();
        executor.awaitTermination(3, TimeUnit.SECONDS);
    }
}
