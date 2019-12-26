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

package com.baidu.hugegraph.unit.iterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import com.baidu.hugegraph.iterator.ToListIterator;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.unit.BaseUnitTest;
import com.baidu.hugegraph.unit.iterator.ExtendableIteratorTest.CloseableItor;
import com.google.common.collect.ImmutableList;

@SuppressWarnings("resource")
public class ToListIteratorTest extends BaseUnitTest {

    private static final List<Integer> DATA1 = ImmutableList.of(1);
    private static final List<Integer> DATA2 = ImmutableList.of(2, 3);
    private static final List<Integer> DATA3 = ImmutableList.of(4, 5, 6);

    @Test
    public void testHasNext() {
        Iterator<Integer> origin = DATA1.iterator();
        Assert.assertTrue(origin.hasNext());

        Iterator<Integer> results = new ToListIterator<>(origin);
        Assert.assertTrue(results.hasNext());
        Assert.assertTrue(results.hasNext());
        Assert.assertFalse(origin.hasNext());
    }

    @Test
    public void testNext() {
        Iterator<Integer> results = new ToListIterator<>(DATA1.iterator());
        Assert.assertEquals(1, (int) results.next());
        Assert.assertThrows(NoSuchElementException.class, () -> {
            results.next();
        });
    }

    @Test
    public void testNextWithMultiTimes() {
        Iterator<Integer> results = new ToListIterator<>(DATA2.iterator());
        Assert.assertEquals(2, (int) results.next());
        Assert.assertEquals(3, (int) results.next());
        Assert.assertThrows(NoSuchElementException.class, () -> {
            results.next();
        });
    }

    @Test
    public void testHasNextAndNext() {
        Iterator<Integer> results = new ToListIterator<>(DATA1.iterator());
        Assert.assertTrue(results.hasNext());
        Assert.assertTrue(results.hasNext());
        Assert.assertEquals(1, (int) results.next());
        Assert.assertFalse(results.hasNext());
        Assert.assertFalse(results.hasNext());
        Assert.assertThrows(NoSuchElementException.class, () -> {
            results.next();
        });
    }

    @Test
    public void testRemove() {
        List<Integer> list3 = new ArrayList<>(DATA3);
        Iterator<Integer> results = new ToListIterator<>(list3.iterator());

        Assert.assertEquals(ImmutableList.of(4, 5, 6), list3);

        Assert.assertEquals(4, (int) results.next());
        Assert.assertEquals(5, (int) results.next());
        results.remove();
        Assert.assertEquals(6, (int) results.next());

        Assert.assertEquals(ImmutableList.of(4, 5, 6), list3);
    }

    @Test
    public void testRemoveWithoutResult() {
        Iterator<Integer> empty = Collections.emptyIterator();
        Iterator<Integer> results = new ToListIterator<>(empty);
        Assert.assertThrows(IllegalStateException.class, () -> {
            results.remove();
        });

        List<Integer> list0 = new ArrayList<>();
        Iterator<Integer> results2 = new ToListIterator<>(list0.iterator());
        Assert.assertThrows(IllegalStateException.class, () -> {
            results2.remove();
        });

        List<Integer> list1 = new ArrayList<>(DATA1);
        Iterator<Integer> results3 = new ToListIterator<>(list1.iterator());
        results3.next();
        Assert.assertThrows(NoSuchElementException.class, () -> {
            results3.next();
        });
        results3.remove(); // OK
        Assert.assertEquals(ImmutableList.of(1), list1);
    }

    @Test
    public void testClose() throws Exception {
        CloseableItor<Integer> c1 = new CloseableItor<>(DATA1.iterator());

        ToListIterator<Integer> results = new ToListIterator<>(c1);

        Assert.assertFalse(c1.closed());

        results.close();

        Assert.assertTrue(c1.closed());
    }
}
