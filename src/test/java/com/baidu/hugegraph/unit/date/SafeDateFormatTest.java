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

package com.baidu.hugegraph.unit.date;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.baidu.hugegraph.date.SafeDateFormat;
import com.baidu.hugegraph.testutil.Assert;

public class SafeDateFormatTest {

    @Test
    public void testSafeDateFormatInConcurrency() throws Exception {
        DateFormat format = new SafeDateFormat("yyyy-MM-dd");
        final CountDownLatch latch = new CountDownLatch(1);
        Date date = format.parse("2019-01-01");
        List<Exception> exceptions = new ArrayList<>();

        int threadCount = 10;
        List<Thread> threads = new ArrayList<>(threadCount);
        for (int t = 0; t < threadCount; t++) {
            Thread thread = new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                for (int i = 0; i < 10; i++){
                    try {
                        Assert.assertEquals(date, format.parse("2019-01-01"));
                        Thread.sleep(100);
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
            });
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        latch.countDown();

        for (Thread thread : threads) {
            thread.join();
        }

        Assert.assertTrue(exceptions.isEmpty());
    }
}
