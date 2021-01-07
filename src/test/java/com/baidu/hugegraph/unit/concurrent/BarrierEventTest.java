package com.baidu.hugegraph.unit.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.baidu.hugegraph.concurrent.BarrierEvent;
import com.baidu.hugegraph.testutil.Assert;

public class BarrierEventTest {

    @Test(timeout = 5000)
    public void testAWait() throws InterruptedException {
        BarrierEvent barrierEvent = new BarrierEvent();
        AtomicInteger result = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);
        Thread awaitThread = new Thread(() -> {
            try {
                barrierEvent.await();
                result.incrementAndGet();
            } catch (InterruptedException e) {
                // Do nothing.
            } finally {
                latch.countDown();
            }
        });
        awaitThread.start();
        Thread signalThread = new Thread(() -> {
            barrierEvent.signalAll();
            latch.countDown();
        });
        signalThread.start();
        latch.await();
        Assert.assertEquals(1, result.get());
    }

    @Test
    public void testAWaitWithTimeout() throws InterruptedException {
        BarrierEvent barrierEvent = new BarrierEvent();
        boolean signaled = barrierEvent.await(1L);
        Assert.assertFalse(signaled);
    }

    @Test
    public void testReset() throws InterruptedException {
        BarrierEvent barrierEvent = new BarrierEvent();
        boolean signaled = barrierEvent.await(1L);
        Assert.assertFalse(signaled);
        barrierEvent.signal();
        signaled = barrierEvent.await(1L);
        Assert.assertTrue(signaled);
        barrierEvent.reset();
        signaled = barrierEvent.await(1L);
        Assert.assertFalse(signaled);
    }

    @Test
    public void testSignal() throws InterruptedException {
        BarrierEvent barrierEvent = new BarrierEvent();
        boolean signaled = barrierEvent.await(1L);
        Assert.assertFalse(signaled);
        barrierEvent.signal();
        signaled = barrierEvent.await(1L);
        Assert.assertTrue(signaled);
    }

    @Test
    public void testSignalAll() throws InterruptedException {
        BarrierEvent barrierEvent = new BarrierEvent();
        boolean signaled = barrierEvent.await(1L);
        Assert.assertFalse(signaled);
        barrierEvent.signalAll();
        signaled = barrierEvent.await(1L);
        Assert.assertTrue(signaled);
    }
}
