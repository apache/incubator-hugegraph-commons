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

package com.baidu.hugegraph.concurrent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

public class KeyLock2<K> {

    private final ConcurrentMap<K, Semaphore> map = new ConcurrentHashMap<>();
    private final ThreadLocal<Map<K, LockInfo>> local =
                  ThreadLocal.withInitial(HashMap::new);

    public void lock(K key) {
        if (key == null) {
            return;
        }
        LockInfo info = this.local.get().get(key);
        if (info == null) {
            Semaphore current = new Semaphore(1);
            current.acquireUninterruptibly();
            Semaphore previous = this.map.put(key, current);
            if (previous != null) {
                previous.acquireUninterruptibly();
            }
            this.local.get().put(key, new LockInfo(current));
        } else {
            info.lockCount++;
        }
    }

    public void unlock(K key) {
        if (key == null) {
            return;
        }
        LockInfo info = this.local.get().get(key);
        if (info != null && --info.lockCount == 0) {
            info.current.release();
            this.map.remove(key, info.current);
            this.local.get().remove(key);
        }
    }

    public void lockAll(K... keys) {
        if (keys == null) {
            return;
        }
        List<K> list = Arrays.asList(keys);
        list.sort(Comparator.comparingInt(Object::hashCode));
        for (K key : list) {
            this.lock(key);
        }
    }

    public void unlockAll(K... keys) {
        if (keys == null) {
            return;
        }
        for (K key : keys) {
            this.unlock(key);
        }
    }

    private static class LockInfo {

        private final Semaphore current;
        private int lockCount;

        private LockInfo(Semaphore current) {
            this.current = current;
            this.lockCount = 1;
        }
    }
}
