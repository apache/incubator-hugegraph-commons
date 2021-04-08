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

package com.baidu.hugegraph.perf;

import java.util.function.BiFunction;

import org.slf4j.Logger;

import com.baidu.hugegraph.perf.PerfUtil.FastMap;
import com.baidu.hugegraph.perf.PerfUtil.LocalStack;
import com.baidu.hugegraph.testutil.Whitebox;
import com.baidu.hugegraph.util.Log;

public final class Stopwatch implements Cloneable {

    private static final Logger LOG = Log.logger(Stopwatch.class);

    private static final String MULTI_THREAD_ACCESS_ERROR =
                         "There may be multi-threaded access, ensure " +
                         "not call PerfUtil.profileSingleThread(true) when " +
                         "multithreading.";

    private long lastStartTime = -1L;

    private long times = 0L;
    private long totalCost = 0L;
    private long minCost = Long.MAX_VALUE;
    private long maxCost = 0L;
    private long totalSelfWasted = 0L;
    private long totalChildrenWasted = -1L;
    private long totalChildrenTimes = -1L;

    private final String name;
    private final Path parent;
    private final Path id;
    private final FastMap<String, Stopwatch> children;

    public Stopwatch(String name, Stopwatch parent) {
        this(name, parent.id());
        parent.child(name, this);
    }

    public Stopwatch(String name, Path parent) {
        this.name = name;
        this.parent = parent;
        this.id = Stopwatch.id(parent, name);
        this.children = new FastMap<>();
    }

    public Path id() {
        return this.id;
    }

    public static Path id(Path parent, String name) {
        if (parent == Path.EMPTY && name.isEmpty()) {
            return Path.EMPTY;
        }
        return new Path(parent, name);
    }

    public String name() {
        return this.name;
    }

    public Path parent() {
        return this.parent;
    }

    private static long eachStartWastedLost = 0L;
    private static long eachEndWastedLost = 0L;

    protected static void initEachWastedLost() {
        int times = 100000000;

        LocalStack<Stopwatch> callStack = Whitebox.getInternalState(
                                          PerfUtil.instance(), "callStack");

        long baseStart = PerfUtil.now();
        for (int i = 0; i < times; i++) {
            PerfUtil.instance();
        }
        long baseCost = PerfUtil.now() - baseStart;

        BiFunction<String, Runnable, Long> testEachCost = (name, test) -> {
            long start = PerfUtil.now();
            test.run();
            long end = PerfUtil.now();
            long cost = end - start - baseCost;
            assert cost > 0;
            long eachCost = cost / times;

            LOG.info("Wasted time test: cost={}ms, base_cost={}ms, {}={}ns",
                     cost/1000000.0, baseCost/1000000.0, name, eachCost);
            return eachCost;
        };

        String startName = "each_start_cost";
        eachStartWastedLost = testEachCost.apply(startName, () -> {
            for (int i = 0; i < times; i++) {
                // Test call start()
                Stopwatch watch = PerfUtil.instance().start(startName);
                // Mock end()
                watch.lastStartTime = -1L;
                callStack.pop();
            }
        });

        String endName = "each_end_cost";
        eachEndWastedLost = testEachCost.apply(endName, () -> {
            Stopwatch watch = PerfUtil.instance().start(endName);
            PerfUtil.instance().end(endName);
            for (int i = 0; i < times; i++) {
                // Mock start()
                callStack.push(watch);
                watch.lastStartTime = 0L;
                // Test call start()
                PerfUtil.instance().end(endName);
                watch.totalCost = 0L;
            }
        });
    }

    public void startTime(long startTime) {
        assert this.lastStartTime == -1L : MULTI_THREAD_ACCESS_ERROR;

        this.times++;
        this.lastStartTime = startTime;

        long endTime = PerfUtil.now();
        long wastedTime = endTime - startTime;
        if (wastedTime <= 0L) {
//            wastedStart0++;
            wastedTime += eachStartWastedLost;
        }

        this.totalSelfWasted += wastedTime;
    }

//    public static int wastedEnd0=0,wastedStart0=0;
    public void endTime(long startTime) {
        assert startTime >= this.lastStartTime && this.lastStartTime != -1L :
               MULTI_THREAD_ACCESS_ERROR;

        long endTime = PerfUtil.now();
        // The following code cost about 3ns~4ns
        long wastedTime = endTime - startTime;
        if (wastedTime <= 0L) {
//            wastedEnd0++;
            wastedTime += eachEndWastedLost;
        }

        long cost = endTime - this.lastStartTime;

        if (this.minCost > cost) {
            this.minCost = cost;
        }
        if (this.maxCost < cost) {
            this.maxCost = cost;
        }

        this.totalCost += cost;
        this.totalSelfWasted += wastedTime;
        this.lastStartTime = -1L;
    }

    protected void totalCost(long totalCost) {
        this.totalCost = totalCost;
    }

    protected void totalChildrenWasted(long totalChildrenWasted) {
        this.totalChildrenWasted = totalChildrenWasted;
    }

    protected void totalChildrenTimes(long totalChildrenTimes) {
        this.totalChildrenTimes = totalChildrenTimes;
    }

    public long times() {
        return this.times;
    }

    public long totalCost() {
        return this.totalCost;
    }

    public long minCost() {
        return this.minCost;
    }

    public long maxCost() {
        return this.maxCost;
    }

    public long totalTimes() {
        if (this.totalChildrenTimes > 0L) {
            return this.times + this.totalChildrenTimes;
        }
        return this.times;
    }

    public long totalWasted() {
        if (this.totalChildrenWasted > 0L) {
            return this.totalSelfWasted + this.totalChildrenWasted;
        }
        return this.totalSelfWasted;
    }

    public long totalSelfWasted() {
        return this.totalSelfWasted;
    }

    public Stopwatch copy() {
        try {
            return (Stopwatch) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public Stopwatch child(String name) {
        return this.children.get(name);
    }

    public Stopwatch child(String name, Stopwatch watch) {
        if (watch == null) {
            return this.children.remove(name);
        }
        return this.children.put(name, watch);
    }

    public void clear() {
        this.children.clear();
    }

    @Override
    public String toString() {
        return String.format("{parent:%s,name:%s," +
                             "times:%s,totalChildrenTimes:%s" +
                             "totalCost:%s, minCost:%s, maxCost:%s," +
                             "totalSelfWasted:%s,totalChildrenWasted:%s}",
                             this.parent, this.name,
                             this.times, this.totalChildrenTimes,
                             this.totalCost, this.minCost, this.maxCost,
                             this.totalSelfWasted, this.totalChildrenWasted);
    }

    public String toJson() {
        int len = 200 + this.name.length() + this.parent.length();
        StringBuilder sb = new StringBuilder(len);
        sb.append("{");
        sb.append("\"parent\":\"").append(this.parent).append("\"");
        sb.append(",\"name\":\"").append(this.name).append("\"");
        sb.append(",\"times\":").append(this.times);
        sb.append(",\"total_children_times\":").append(this.totalChildrenTimes);
        sb.append(",\"total_cost\":").append(this.totalCost);
        sb.append(",\"min_cost\":").append(this.minCost);
        sb.append(",\"max_cost\":").append(this.maxCost);
        sb.append(",\"total_self_wasted\":").append(this.totalSelfWasted);
        sb.append(",\"total_children_wasted\":").append(
                                                 this.totalChildrenWasted);
        sb.append("}");
        return sb.toString();
    }

    public static final class Path implements Comparable<Path> {

        public static final Path EMPTY = new Path("");

        private final String path;

        public Path(String self) {
            this.path = self;
        }

        public Path(Path parent, String name) {
            if (parent == EMPTY) {
                this.path = name;
            } else {
                int len = parent.length() + 1 + name.length();
                StringBuilder sb = new StringBuilder(len);
                sb.append(parent.path).append('/').append(name);

                this.path = sb.toString();
            }
        }

        public int length() {
            return this.path.length();
        }

        @Override
        public int hashCode() {
            return this.path.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this.hashCode() != obj.hashCode()) {
                return false;
            }
            if (!(obj instanceof Path)) {
                return false;
            }
            Path other = (Path) obj;
            return this.path.equals(other.path);
        }

        @Override
        public int compareTo(Path other) {
            return this.path.compareTo(other.path);
        }

        @Override
        public String toString() {
            return this.path;
        }

        public boolean endsWith(String name) {
            return this.path.endsWith(name);
        }
    }
}
