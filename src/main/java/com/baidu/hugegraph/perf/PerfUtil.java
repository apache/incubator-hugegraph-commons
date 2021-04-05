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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.baidu.hugegraph.func.TriFunction;
import com.baidu.hugegraph.perf.Stopwatch.Path;
import com.baidu.hugegraph.testutil.Assert.ThrowableConsumer;
import com.baidu.hugegraph.util.Log;
import com.baidu.hugegraph.util.ReflectionUtil;
import com.google.common.reflect.ClassPath.ClassInfo;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public final class PerfUtil {

    private static final Logger LOG = Log.logger(PerfUtil.class);
    private static final int DEFAUL_CAPATICY = 1024;

    private static final ThreadLocal<PerfUtil> INSTANCE = new ThreadLocal<>();
    private static PerfUtil SINGLE_INSTANCE = null;

    private static LocalTimer LOCAL_TIMER = null;

    private final Map<Path, Stopwatch> stopwatches;
    private final LocalStack<Stopwatch> callStack;
    private final Stopwatch root;

    private PerfUtil() {
        this.stopwatches = new HashMap<>(DEFAUL_CAPATICY);
        this.callStack = new LocalStack<>(DEFAUL_CAPATICY);
        this.root = new Stopwatch("", Path.EMPTY);
    }

    public static PerfUtil instance() {
        if (SINGLE_INSTANCE != null) {
            // Return the only one instance for single thread, for performance
            return SINGLE_INSTANCE;
        }

        PerfUtil p = INSTANCE.get();
        if (p == null) {
            p = new PerfUtil();
            INSTANCE.set(p);
        }
        return p;
    }

    public static void profileSingleThread(boolean yes) {
        SINGLE_INSTANCE = yes ? PerfUtil.instance() : null;
    }

    public static void useLocalTimer(boolean yes) {
        if (yes) {
            if (LOCAL_TIMER != null) {
                return;
            }
            LOCAL_TIMER = new LocalTimer();
            LOCAL_TIMER.startTimeUpdateLoop();
        } else {
            if (LOCAL_TIMER == null) {
                return;
            }
            try {
                LOCAL_TIMER.stop();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                LOCAL_TIMER = null;
            }
        }
    }

    private static long now() {
        if (LOCAL_TIMER != null) {
            return LOCAL_TIMER.now();
        }
        // System.nanoTime() cost about 40 ns each call
        return System.nanoTime();
    }

    public boolean start(String name) {
        long start = now();

        Stopwatch parent = this.callStack.empty() ?
                           this.root : this.callStack.peek();

        // Get watch by name from local tree
        Stopwatch watch = parent.child(name);
        if (watch == null) {
            watch = new Stopwatch(name, parent);
            assert !this.stopwatches.containsKey(watch.id()) : watch;
            this.stopwatches.put(watch.id(), watch);
        }
        this.callStack.push(watch);

        long end = now();
        long wastedTime = end - start;

        watch.startTime(start, wastedTime);

        return true; // just for assert
    }

    public boolean start2(String name) {
        long start = now(); // cost 70 ns with System.nanoTime()

        Path parent = this.callStack.empty() ?
                      Path.EMPTY : this.callStack.peek().id();
        Path id = Stopwatch.id(parent, name); // cost 130
        // Get watch by id from global map
        Stopwatch watch = this.stopwatches.get(id); // cost 170
        if (watch == null) {
            watch = new Stopwatch(name, parent);
            this.stopwatches.put(watch.id(), watch); // cost 180
        }
        this.callStack.push(watch); // cost 190

        long end = now();
        long wastedTime = end - start;

        watch.startTime(start, wastedTime);

        return true; // just for assert
    }

    public boolean end(String name) {
        long start = now();

        Stopwatch watch = this.callStack.pop();
        assert watch.id().endsWith(name) : watch;

        if (watch == null) {
            throw new IllegalArgumentException("Invalid watch name: " + name);
        }

        long end = now();
        long wastedTime = end - start;

        watch.endTime(end, wastedTime);

        return true;
    }

    public void clear() {
        String error = "Can't be cleared when the call has not ended yet";
        com.baidu.hugegraph.util.E.checkState(this.callStack.empty(), error);

        this.stopwatches.clear();
        this.root.clear();
    }

    public void profilePackage(String... packages) throws Throwable {
        Set<String> loadedClasses = new HashSet<>();

        ThrowableConsumer<String> profileClassIfPresent = (cls) -> {
            if (!loadedClasses.contains(cls)) {
                // Profile super class
                String pkg = ReflectionUtil.packageName(cls);
                for (String s : ReflectionUtil.superClasses(cls)) {
                    if (!loadedClasses.contains(s) && s.startsWith(pkg)) {
                        profileClass(s);
                        loadedClasses.add(s);
                    }
                }
                // Profile self class
                profileClass(cls);
                loadedClasses.add(cls);
            }
        };

        Iterator<ClassInfo> classes = ReflectionUtil.classes(packages);
        while (classes.hasNext()) {
            String cls = classes.next().getName();
            // Profile self class
            profileClassIfPresent.accept(cls);
            // Profile nested class
            for (String s : ReflectionUtil.nestedClasses(cls)) {
                profileClassIfPresent.accept(s);
            }
        }
    }

    public void profileClass(String... classes) throws Throwable {
        ClassPool classPool = ClassPool.getDefault();

        for (String cls : classes) {
            CtClass ctClass = classPool.get(cls);
            List<CtMethod> methods = ReflectionUtil.getMethodsAnnotatedWith(
                                     ctClass, Watched.class, false);
            for (CtMethod method : methods) {
                profile(method);
            }

            // Load class and make it effective
            if (!methods.isEmpty()) {
                ctClass.toClass();
            }
        }
    }

    private void profile(CtMethod ctMethod)
                         throws CannotCompileException, ClassNotFoundException {
        final String START =
                "com.baidu.hugegraph.perf.PerfUtil.instance().start(\"%s\");";
        final String END =
                "com.baidu.hugegraph.perf.PerfUtil.instance().end(\"%s\");";

        Watched annotation = (Watched) ctMethod.getAnnotation(Watched.class);

        String name = annotation.value();
        if (name.isEmpty()) {
            name = ctMethod.getName();
        }
        if (!annotation.prefix().isEmpty()) {
            name = annotation.prefix() + "." + name;
        }

        ctMethod.insertBefore(String.format(START, name));
        // Insert as a finally-statement
        ctMethod.insertAfter(String.format(END, name), true);

        LOG.debug("Profiled for: '{}' [{}]", name, ctMethod.getLongName());
    }

    @Override
    public String toString() {
        return this.stopwatches.toString();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder(8 + this.stopwatches.size() * 96);
        sb.append('{');
        for (Map.Entry<Path, Stopwatch> w : this.stopwatches.entrySet()) {
            sb.append('"');
            sb.append(w.getKey());
            sb.append('"');

            sb.append(':');

            sb.append(w.getValue().toJson());

            sb.append(',');
        }
        if (!this.stopwatches.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append('}');
        return sb.toString();
    }

    // TODO: move toECharts() method out of this class
    public String toECharts() {
        TriFunction<Integer, Integer, List<Stopwatch>, String> formatLevel = (
                    totalDepth, depth, items) -> {
            float factor = 100.0f / (totalDepth + 1);
            float showFactor = 1 + (totalDepth - depth) / (float) depth;

            float radiusFrom = depth * factor;
            float radiusTo = depth * factor + factor;
            if (depth == 1) {
                radiusFrom = 0;
            }

            StringBuilder sb = new StringBuilder(8 + items.size() * 128);
            sb.append('{');
            sb.append("name: 'Total Cost',");
            sb.append("type: 'pie',");
            sb.append(String.format("radius: ['%s%%', '%s%%'],",
                                    radiusFrom, radiusTo));
            sb.append(String.format(
                      "label: {normal: {position: 'inner', formatter:" +
                      "function(params) {" +
                      "  if (params.percent > %s) return params.data.name;" +
                      "  else return '';" +
                      "}}},", showFactor));
            sb.append("data: [");

            items.sort((i, j) -> i.id().compareTo(j.id()));
            for (Stopwatch w : items) {
                sb.append('{');

                sb.append("id:'");
                sb.append(w.id());
                sb.append("',");

                sb.append("name:'");
                sb.append(w.name());
                sb.append("',");

                sb.append("value:");
                sb.append(w.totalCost()); // w.totalCost() - w.totalWasted() ?
                sb.append(',');

                sb.append("cost:");
                sb.append(w.totalCost() / 1000000.0);
                sb.append(',');

                sb.append("minCost:");
                sb.append(w.minCost());
                sb.append(',');

                sb.append("maxCost:");
                sb.append(w.maxCost());
                sb.append(',');

                sb.append("wasted:");
                sb.append(w.totalWasted() / 1000000.0);
                sb.append(',');

                sb.append("selfWasted:");
                sb.append(w.totalSelfWasted() / 1000000.0);
                sb.append(',');

                sb.append("times:");
                sb.append(w.times());

                sb.append('}');
                sb.append(',');
            }
            if (!items.isEmpty()) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("]}");
            return sb.toString();
        };

        BiConsumer<List<Stopwatch>, List<Stopwatch>> fillWasted =
                                    (itemsOfLn, itemsOfLnParent) -> {
            for (Stopwatch parent : itemsOfLnParent) {
                Stream<Stopwatch> children = itemsOfLn.stream().filter(c -> {
                    return c.parent().equals(parent.id());
                });
                // Fill wasted cost
                long sumWasted = children.mapToLong(c -> c.totalWasted()).sum();
                parent.totalChildrenWasted(sumWasted);
            }
        };

        BiConsumer<List<Stopwatch>, List<Stopwatch>> fillOther =
                                    (itemsOfLn, itemsOfLnParent) -> {
            for (Stopwatch parent : itemsOfLnParent) {
                Stream<Stopwatch> children = itemsOfLn.stream().filter(c -> {
                    return c.parent().equals(parent.id());
                });
                // Fill other cost
                long sumCost = children.mapToLong(c -> c.totalCost()).sum();
                long otherCost = parent.totalCost() - sumCost;
                if (otherCost > 0L) {
                    Stopwatch other = new Stopwatch("~", parent.id());
                    other.totalCost(otherCost);
                    itemsOfLn.add(other);
                }
            }
        };

        Map<Path, Stopwatch> items = this.stopwatches;
        Map<Integer, List<Stopwatch>> levelItems = new HashMap<>();
        int maxDepth = 0;
        for (Map.Entry<Path, Stopwatch> e : items.entrySet()) {
            int depth = e.getKey().toString().split("/").length;
            List<Stopwatch> levelItem = levelItems.get(depth);
            if (levelItem == null) {
                levelItem = new LinkedList<>();
                levelItems.putIfAbsent(depth, levelItem);
            }
            levelItem.add(e.getValue().copy());
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }

        // Fill wasted cost from the outermost to innermost
        for (int i = maxDepth; i > 0; i--) {
            assert levelItems.containsKey(i) : i;
            List<Stopwatch> itemsOfI = levelItems.get(i);
            List<Stopwatch> itemsOfParent = levelItems.get(i - 1);
            if (itemsOfParent != null) {
                // Fill wasted cost
                fillWasted.accept(itemsOfI, itemsOfParent);
            }
        }

        StringBuilder sb = new StringBuilder(8 + items.size() * 128);
        // Output results header
        sb.append("{");
        sb.append("tooltip: {trigger: 'item', " +
            "formatter: function(params) {" +
            "  return params.data.name + ' ' + params.percent + '% <br/>'" +
            "    + 'cost: ' + params.data.cost + ' (ms) <br/>'" +
            "    + 'min cost: ' + params.data.minCost + ' (ns) <br/>'" +
            "    + 'max cost: ' + params.data.maxCost + ' (ns) <br/>'" +
            "    + 'wasted: ' + params.data.wasted + ' (ms) <br/>'" +
            "    + 'self wasted: ' + params.data.selfWasted + ' (ms) <br/>'" +
            "    + 'times: ' + params.data.times + '<br/>'" +
            "    + 'path: ' + params.data.id + '<br/>';" +
            "}");
        sb.append("},");
        sb.append("series: [");
        // Output results data
        for (int i = 1; i <= maxDepth; i++) {
            assert levelItems.containsKey(i) : i;
            List<Stopwatch> itemsOfI = levelItems.get(i);
            List<Stopwatch> itemsOfParent = levelItems.get(i - 1);
            if (itemsOfParent != null) {
                // Fill other cost for non-root level, ignore root level (i=1)
                fillOther.accept(itemsOfI, itemsOfParent);
            }
            // Output items of level I
            sb.append(formatLevel.apply(maxDepth, i, itemsOfI));
            sb.append(',');
        }
        if (!items.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]}");

        return sb.toString();
    }

    public static final class LocalTimer {

        private volatile long time = 0L;
        private volatile boolean running = false;
        private Thread thread = null;

        public long now() {
            return this.time;
        }

        public void startTimeUpdateLoop() {
            this.running = true;
            this.thread = new Thread(() -> {
                while (this.running) {
                    this.time = System.nanoTime();
                }
            }, "LocalTimer");
            this.thread.setDaemon(true);
            this.thread.start();
        }

        public void stop() throws InterruptedException {
            this.running = false;
            if (this.thread != null) {
                this.thread.join();
            }
        }
    }

    public static final class LocalStack<E> {

        private final Object[] elementData;
        private int elementCount;

        public LocalStack(int capacity) {
            this.elementData = new Object[capacity];
            this.elementCount = 0;
        }

        int size() {
            return this.elementCount;
        }

        boolean empty() {
            return this.elementCount == 0;
        }

        public void push(E elem) {
            this.elementData[this.elementCount++] = elem;
        }

        public E pop() {
            if (this.elementCount == 0) {
                throw new EmptyStackException();
            }
            this.elementCount--;
            @SuppressWarnings("unchecked")
            E elem = (E) this.elementData[this.elementCount];
            this.elementData[this.elementCount] = null;
            return elem;
        }

        public E peek() {
            if (this.elementCount == 0) {
                throw new EmptyStackException();
            }
            @SuppressWarnings("unchecked")
            E elem = (E) this.elementData[this.elementCount - 1];
            return elem;
        }
    }

    public static final class FastMap<K, V> {

        private final Map<K, V> hashMap;

        private K key1;
        private K key2;
        private K key3;

        private V val1;
        private V val2;
        private V val3;

        public FastMap() {
            this.hashMap = new HashMap<>();
        }

        public int size() {
            return this.hashMap.size();
        }

        public boolean containsKey(Object key) {
            return this.hashMap.containsKey(key);
        }

        public V get(Object key) {
            if (key == this.key1) {
                return this.val1;
            } else if (key == this.key2) {
                return this.val2;
            } else if (key == this.key3) {
                return this.val3;
            }

            return this.hashMap.get(key);
        }

        public V put(K key, V value) {
            if (this.key1 == null) {
                this.key1 = key;
                this.val1 = value;
            } else if (this.key2 == null) {
                this.key2 = key;
                this.val2 = value;
            } else if (this.key3 == null) {
                this.key3 = key;
                this.val3 = value;
            }

            return this.hashMap.put(key, value);
        }

        public V remove(Object key) {
            if (key == this.key1) {
                this.key1 = null;
                this.val1 = null;
            } else if (key == this.key2) {
                this.key2 = null;
                this.val2 = null;
            } else if (key == this.key3) {
                this.key3 = null;
                this.val3 = null;
            }

            return this.hashMap.remove(key);
        }

        public void clear() {
            this.key1 = null;
            this.key2 = null;
            this.key3 = null;

            this.val1 = null;
            this.val2 = null;
            this.val3 = null;

            this.hashMap.clear();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
    public static @interface Watched {
        public String value() default "";
        public String prefix() default "";
    }
}
