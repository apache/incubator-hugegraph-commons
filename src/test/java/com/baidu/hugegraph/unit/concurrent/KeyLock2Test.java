package com.baidu.hugegraph.unit.concurrent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.baidu.hugegraph.concurrent.KeyLock2;
import com.baidu.hugegraph.unit.BaseUnitTest;

public class KeyLock2Test extends BaseUnitTest {

    private static final int THREADS_NUM = 8;

    @Test
    public void testKeyLock2() {
        KeyLock2<Integer> lock = new KeyLock2<>();
        lock.lockAll(1, 2, 3);
        lock.unlockAll(1, 2, 3);
    }

    @Test
    public void testKeyLock2WithMultiThreads() {
        KeyLock2 lock = new KeyLock2();
        Set<String> names = new HashSet<>(THREADS_NUM);
        List<Integer> keys = new ArrayList<>(5);
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            keys.add(random.nextInt(THREADS_NUM));
        }

        Assert.assertEquals(0, names.size());

        runWithThreads(THREADS_NUM, () -> {
            lock.lockAll(keys.toArray());
            names.add(Thread.currentThread().getName());
            lock.unlockAll(keys.toArray());
        });

        Assert.assertEquals(THREADS_NUM, names.size());
    }

    @Test
    public void testKeyLock2WithMultiThreadsWithRandomKey() {
        KeyLock2 lock = new KeyLock2();
        Set<String> names = new HashSet<>(THREADS_NUM);

        Assert.assertEquals(0, names.size());

        runWithThreads(THREADS_NUM, () -> {
            List<Integer> keys = new ArrayList<>(5);
            Random random = new Random();
            for (int i = 0; i < 5; i++) {
                keys.add(random.nextInt(THREADS_NUM));
            }
            lock.lockAll(keys.toArray());
            names.add(Thread.currentThread().getName());
            lock.unlockAll(keys.toArray());
        });

        Assert.assertEquals(THREADS_NUM, names.size());
    }
}
