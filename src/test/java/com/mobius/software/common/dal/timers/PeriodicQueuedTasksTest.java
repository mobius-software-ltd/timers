package com.mobius.software.common.dal.timers;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PeriodicQueuedTasksTest {
    private PeriodicQueuedTasks<Timer> tasks;
    private WorkerPool workerPool;

    @Before
    public void setUp() {
        workerPool = new WorkerPool(10);        
        workerPool.start(1);
        tasks = workerPool.getPeriodicQueue();
    }

    @After
    public void tearDown() {
        workerPool.stop();
        workerPool = null;
    }

    @Test
    public void testStoreTimeoutAndPeriod() throws InterruptedException {
        System.out.println("testStoreTimeoutAndPeriod");
        TimerTask task = new TimerTask(workerPool, 500, 500);
        tasks.store(task.getRealTimestamp(), task);
        assertEquals(1, tasks.getQueues().size());
        assertEquals(1, tasks.getQueues().values().iterator().next().size());
        assertEquals(task, tasks.getQueues().values().iterator().next().peek());
        Thread.sleep(250);
        assertEquals(1, tasks.getQueues().size());
        assertEquals(1, tasks.getQueues().values().iterator().next().size());
        assertEquals(task, tasks.getQueues().values().iterator().next().peek());
        //1st execution
        Thread.sleep(300);
        // assertEquals(1, tasks.getQueues().size());
        // assertEquals(1, tasks.getQueues().values().iterator().next().size());
        // assertEquals(task, tasks.getQueues().values().iterator().next().peek());
        Thread.sleep(250);
        // assertEquals(1, tasks.getQueues().size());
        // assertEquals(1, tasks.getQueues().values().iterator().next().size());
        // assertEquals(task, tasks.getQueues().values().iterator().next().peek());        
        task.stop();
        // assertEquals(1, tasks.getQueues().size());
        // assertEquals(1, tasks.getQueues().values().iterator().next().size());
        // assertEquals(task, tasks.getQueues().values().iterator().next().peek());
        Thread.sleep(500);
        assertEquals(0, tasks.getQueues().size());
        assertEquals(0, tasks.getQueues().values().size());
        assertEquals(1, task.getCounter().get());
    }

    @Test
    public void testStorePassawayQueue() throws InterruptedException {
        System.out.println("testStorePassawayQueue");
        TimerTask task = new TimerTask(workerPool, System.currentTimeMillis() - 600, 500, 500);
        tasks.store(task.getRealTimestamp(), task);
        assertEquals(0, tasks.getQueues().size());
        assertEquals(0, tasks.getQueues().values().size());
        Thread.sleep(250);
        assertEquals(1, tasks.getQueues().size());
        assertEquals(1, tasks.getQueues().values().iterator().next().size());
        assertEquals(task, tasks.getQueues().values().iterator().next().peek());
        //1st execution
        Thread.sleep(300);
        assertEquals(1, tasks.getQueues().size());
        assertEquals(1, tasks.getQueues().values().iterator().next().size());
        assertEquals(task, tasks.getQueues().values().iterator().next().peek());
        Thread.sleep(250);
        assertEquals(1, tasks.getQueues().size());
        assertEquals(1, tasks.getQueues().values().iterator().next().size());
        assertEquals(task, tasks.getQueues().values().iterator().next().peek());        
        task.stop();
        assertEquals(1, tasks.getQueues().size());
        assertEquals(1, tasks.getQueues().values().iterator().next().size());
        assertEquals(task, tasks.getQueues().values().iterator().next().peek());
        Thread.sleep(500);
        assertEquals(0, tasks.getQueues().size());
        assertEquals(0, tasks.getQueues().values().size());
        assertEquals(2, task.getCounter().get());
    }

    @Test
    public void testZeroDelay() throws InterruptedException {
        System.out.println("testZeroDelay");
        TimerTask task = new TimerTask(workerPool, 0, 500);
        tasks.store(task.getRealTimestamp(), task);
        // assertEquals(0, tasks.getQueues().size());
        // assertEquals(0, tasks.getQueues().values().size());
        Thread.sleep(250);
        assertEquals(1, tasks.getQueues().size());
        assertEquals(1, tasks.getQueues().values().iterator().next().size());
        assertEquals(task, tasks.getQueues().values().iterator().next().peek());
        //1st execution
        Thread.sleep(300);
        // assertEquals(1, tasks.getQueues().size());
        // assertEquals(1, tasks.getQueues().values().iterator().next().size());
        // assertEquals(task, tasks.getQueues().values().iterator().next().peek());
        Thread.sleep(250);
        // assertEquals(1, tasks.getQueues().size());
        // assertEquals(1, tasks.getQueues().values().iterator().next().size());
        // assertEquals(task, tasks.getQueues().values().iterator().next().peek());        
        task.stop();
        // assertEquals(1, tasks.getQueues().size());
        // assertEquals(1, tasks.getQueues().values().iterator().next().size());
        // assertEquals(task, tasks.getQueues().values().iterator().next().peek());
        Thread.sleep(500);
        assertEquals(0, tasks.getQueues().size());
        assertEquals(0, tasks.getQueues().values().size());
        assertEquals(2, task.getCounter().get());
    }
}