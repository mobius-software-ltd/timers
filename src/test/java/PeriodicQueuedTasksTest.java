import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mobius.software.common.dal.timers.PeriodicQueuedTasks;
import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.common.dal.timers.WorkerPool;

public class PeriodicQueuedTasksTest {
    private PeriodicQueuedTasks<Timer> tasks;
    private WorkerPool workerPool;

    @Before
    public void setUp() {
        workerPool = new WorkerPool(10, Boolean.TRUE);        
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
        Timer task = new TimerTask(500, 500);
        tasks.store(task.getRealTimestamp(), task);
        assertEquals(1, tasks.getQueues().size());
        assertEquals(1, tasks.getQueues().get(1000L).size());
        assertEquals(task, tasks.getQueues().get(1000L).peek());
        Thread.sleep(300);
        assertEquals(1, tasks.getQueues().size());
        assertEquals(1, tasks.getQueues().get(1000L).size());
        assertEquals(task, tasks.getQueues().get(1000L).peek());
        Thread.sleep(300);
        assertEquals(0, tasks.getQueues().size());
        assertEquals(0, tasks.getQueues().get(1000L).size());
        assertNull(tasks.getQueues().get(1000L).peek());
    }

    // @Test
    // public void testExecutePreviousPool() {
    //     Timer task1 = new Timer();
    //     Timer task2 = new Timer();
    //     tasks.store(1000, task1);
    //     tasks.store(2000, task2);
    //     tasks.executePreviousPool(3000);
    //     assertEquals(1, workerPool.getQueue().size());
    //     assertEquals(task1, workerPool.getQueue().peek());
    // }
}