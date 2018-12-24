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

package com.baidu.hugegraph.unit.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.util.CollectionUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class CollectionUtilsTest {

    @Test
    public void testAllUnique() {
        List<Integer> list = ImmutableList.of();
        Assert.assertTrue(CollectionUtils.allUnique(list));

        list = ImmutableList.of(1, 2, 3, 2, 3);
        Assert.assertFalse(CollectionUtils.allUnique(list));

        list = ImmutableList.of(1, 2, 3, 4, 5);
        Assert.assertTrue(CollectionUtils.allUnique(list));

        list = ImmutableList.of(1, 1, 1, 1, 1);
        Assert.assertFalse(CollectionUtils.allUnique(list));
    }

    @Test
    public void testSubSet() {
        Set<Integer> originSet = ImmutableSet.of(1, 2, 3, 4, 5);

        Set<Integer> subSet = CollectionUtils.subSet(originSet, 1, 1);
        Assert.assertEquals(ImmutableSet.of(), subSet);

        subSet = CollectionUtils.subSet(originSet, 2, 4);
        Assert.assertEquals(ImmutableSet.of(3, 4), subSet);

        subSet = CollectionUtils.subSet(originSet, 2, 5);
        Assert.assertEquals(ImmutableSet.of(3, 4, 5), subSet);

        subSet = CollectionUtils.subSet(originSet, 0, 5);
        Assert.assertEquals(ImmutableSet.of(1, 2, 3, 4, 5), subSet);
    }

    @Test
    public void testUnion() {
        List<Integer> first = new ArrayList<>();
        first.add(1);
        first.add(2);

        Set<Integer> second = new HashSet<>();
        second.add(1);
        second.add(3);

        Set<Integer> results = CollectionUtils.union(first, second);
        Assert.assertEquals(3, results.size());
    }

    @Test
    public void testIntersectWithoutModifying() {
        List<Integer> first = new ArrayList<>();
        first.add(1);
        first.add(2);
        first.add(3);

        List<Integer> second = new ArrayList<>();

        second.add(4);
        second.add(5);
        Collection<Integer> results = CollectionUtils.intersect(first, second);
        Assert.assertEquals(0, results.size());
        Assert.assertEquals(3, first.size());

        second.add(3);
        results = CollectionUtils.intersect(first, second);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(3, first.size());

        second.add(1);
        second.add(2);
        results = CollectionUtils.intersect(first, second);
        Assert.assertEquals(3, results.size());
        Assert.assertEquals(3, first.size());
    }

    @Test
    public void testIntersectWithModifying() {
        Set<Integer> first = new HashSet<>();
        first.add(1);
        first.add(2);
        first.add(3);

        Set<Integer> second = new HashSet<>();
        second.add(1);
        second.add(2);
        second.add(3);

        Collection<Integer> results = CollectionUtils.intersectWithModify(
                                                     first, second);
        Assert.assertEquals(3, results.size());
        Assert.assertEquals(3, first.size());

        // The second set has "1", "2"
        second.remove(3);
        results = CollectionUtils.intersectWithModify(first, second);
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(2, first.size());

        // The second set is empty
        second.remove(1);
        second.remove(2);
        results = CollectionUtils.intersectWithModify(first, second);
        Assert.assertEquals(0, results.size());
        Assert.assertEquals(0, first.size());
    }

    @Test
    public void testHasIntersectionBetweenListAndSet() {
        List<Integer> first = new ArrayList<>();
        first.add(1);
        first.add(2);
        first.add(3);

        Set<Integer> second = new HashSet<>();
        Assert.assertFalse(CollectionUtils.hasIntersection(first, second));

        second.add(4);
        Assert.assertFalse(CollectionUtils.hasIntersection(first, second));

        second.add(1);
        Assert.assertTrue(CollectionUtils.hasIntersection(first, second));
    }

    @Test
    public void testHasIntersectionBetweenSetAndSet() {
        Set<Integer> first = new HashSet<>();
        first.add(1);
        first.add(2);
        first.add(3);

        Set<Integer> second = new HashSet<>();
        Assert.assertFalse(CollectionUtils.hasIntersection(first, second));

        second.add(4);
        Assert.assertFalse(CollectionUtils.hasIntersection(first, second));

        second.add(1);
        Assert.assertTrue(CollectionUtils.hasIntersection(first, second));
    }

    @Test
    public void testMapSortByIntegerValue() {
        Map<String, Integer> unordered = new HashMap<>();
        unordered.put("A", 4);
        unordered.put("B", 2);
        unordered.put("C", 5);
        unordered.put("D", 1);
        unordered.put("E", 3);

        Map<String, Integer> incrOrdered = CollectionUtils.sortByValue(unordered,
                                                                      true);
        Assert.assertEquals(ImmutableList.of(1, 2, 3, 4, 5),
                            ImmutableList.copyOf(incrOrdered.values()));

        Map<String, Integer> decrOrdered = CollectionUtils.sortByValue(unordered,
                                                                      false);
        Assert.assertEquals(ImmutableList.of(5, 4, 3, 2, 1),
                            ImmutableList.copyOf(decrOrdered.values()));
    }

    @Test
    public void testMapSortByStringValue() {
        Map<Integer, String> unordered = new HashMap<>();
        unordered.put(1, "D");
        unordered.put(2, "B");
        unordered.put(3, "E");
        unordered.put(4, "A");
        unordered.put(5, "C");

        Map<Integer, String> incrOrdered = CollectionUtils.sortByValue(unordered,
                                                                      true);
        Assert.assertEquals(ImmutableList.of("A", "B", "C", "D", "E"),
                            ImmutableList.copyOf(incrOrdered.values()));

        Map<Integer, String> decrOrdered = CollectionUtils.sortByValue(unordered,
                                                                      false);
        Assert.assertEquals(ImmutableList.of("E", "D", "C", "B", "A"),
                            ImmutableList.copyOf(decrOrdered.values()));
    }
}
