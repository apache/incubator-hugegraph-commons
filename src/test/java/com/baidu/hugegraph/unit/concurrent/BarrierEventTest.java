package com.baidu.hugegraph.unit.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

    @Test(timeout = 5000)
    public void testSignalMultiThread() throws InterruptedException {
        BarrierEvent barrierEvent = new BarrierEvent();
        AtomicInteger result = new AtomicInteger(0);
        int waitThreadNum = 10;
        ExecutorService executorService =
                        Executors.newFixedThreadPool(waitThreadNum);
        CountDownLatch waitLatch = new CountDownLatch(waitThreadNum);
        CountDownLatch signalLatch = new CountDownLatch(1);
        for (int i = 0; i < waitThreadNum; i++) {
            executorService.submit(() -> {
                try {
                    waitLatch.countDown();
                    barrierEvent.await();
                    result.incrementAndGet();
                } catch (InterruptedException e) {
                    // Do nothing
                }
            });
        }

        Thread signalThread = new Thread(() -> {
            barrierEvent.signal();
            signalLatch.countDown();
        });
        signalThread.start();
        waitLatch.await();
        signalLatch.await();
        TimeUnit.MICROSECONDS.sleep(100);
        executorService.shutdownNow();

        Assert.assertEquals(1, result.get());
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

    @Test(timeout = 5000)
    public void testSignalAllMultiThread() throws InterruptedException {
        BarrierEvent barrierEvent = new BarrierEvent();
        AtomicInteger result = new AtomicInteger(0);
        int waitThreadNum = 10;
        ExecutorService executorService =
                        Executors.newFixedThreadPool(waitThreadNum);
        CountDownLatch latch = new CountDownLatch(waitThreadNum + 1);
        for (int i = 0; i < waitThreadNum; i++) {
            executorService.submit(() -> {
                try {
                    barrierEvent.await();
                    result.incrementAndGet();
                    latch.countDown();
                } catch (InterruptedException e) {
                    // Do nothing
                }
            });
        }
        Thread signalThread = new Thread(() -> {
            barrierEvent.signalAll();
            latch.countDown();
        });
        signalThread.start();
        latch.await();
        executorService.shutdownNow();

        Assert.assertEquals(waitThreadNum, result.get());
    }
}
