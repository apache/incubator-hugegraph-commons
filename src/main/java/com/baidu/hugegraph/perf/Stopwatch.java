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

import com.baidu.hugegraph.perf.PerfUtil.FastMap;

public final class Stopwatch implements Cloneable {

    private static final String MULTI_THREAD_ACCESS_ERROR =
                         "There may be multi-threaded access, ensure " +
                         "not call PerfUtil.profileSingleThread(true) when " +
                         "multithreading.";

    private long lastStartTime = -1L;

    private long times = 0L;
    private long totalCost = 0L;
    private long minCost = 0L;
    private long maxCost = 0L;
    private long totalSelfWasted = 0L;
    private long totalChildrenWasted = -1L;

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

//    public static StringSlice id(StringSlice parent, String name) {
//        if (parent == null || parent.length() == 0) {
//            return new StringSlice(name);
//        }
////        String sss= parent+'/'+name;
////        return sss;
//        int len = parent.length() + 1 + name.length();
//        StringBuilder sb = new StringBuilder(len);
//        sb.append(parent).append('/').append(name);
//        return sb;
//    }

    public String name() {
        return this.name;
    }

    public Path parent() {
        return this.parent;
    }

    public void startTime(long time, long wastedTime) {
        assert this.lastStartTime == -1L : MULTI_THREAD_ACCESS_ERROR;

        this.lastStartTime = time;
        this.times++;
        this.totalSelfWasted += wastedTime;
    }

    public void endTime(long time, long wastedTime) {
        assert time >= this.lastStartTime && this.lastStartTime != -1L :
               MULTI_THREAD_ACCESS_ERROR;

        long cost = time - this.lastStartTime;
        this.totalCost += cost;
        this.lastStartTime = -1L;
        this.totalSelfWasted += wastedTime;
        this.updateMinMax(cost);
    }

    protected void updateMinMax(long cost) {
        if (this.minCost > cost || this.minCost == 0L) {
            this.minCost = cost;
        }
        if (this.maxCost < cost) {
            this.maxCost = cost;
        }
    }

    protected void totalCost(long totalCost) {
        this.totalCost = totalCost;
    }

    protected void totalChildrenWasted(long totalChildrenWasted) {
        this.totalChildrenWasted = totalChildrenWasted;
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

    public long totalWasted() {
        if (this.totalChildrenWasted >= 0L) {
            return this.totalSelfWasted + this.totalChildrenWasted;
        }
        return this.totalSelfWasted;
    }

    public long totalSelfWasted() {
        return this.totalSelfWasted;
    }

    public long totalChildrenWasted() {
        return this.totalChildrenWasted;
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
        return String.format("{parent:%s,name:%s,times:%s," +
                             "totalCost:%s, minCost:%s, maxCost:%s," +
                             "totalSelfWasted:%s,totalChildrenWasted:%s}",
                             this.parent, this.name, this.times,
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
