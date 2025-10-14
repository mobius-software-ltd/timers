package com.mobius.software.common.dal.timers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HealthCheckTest 
{
	private static Logger logger = LogManager.getLogger(HealthCheckTest.class);

	private PeriodicQueuedTasks<Timer> tasks;
    private WorkerPool workerPool;

    @Before
    public void setUp() 
    {
    	Configurator.initialize(new DefaultConfiguration());
		
        workerPool = new WorkerPool(10);        
        workerPool.start(1);
        tasks = workerPool.getPeriodicQueue();
    }

    @After
    public void tearDown() 
    {
        workerPool.stop();
        workerPool = null;
    }
    
    @Test
    public void testOneTimeTask()
    {
    	logger.info("One time task test begin");
    	
    	TimeoutedTask task = new TimeoutedTask(500L, "timeouted Task");
        tasks.store(task.getRealTimestamp(), task);
        try
        {
        	Thread.sleep(21000L);
        }
        catch(InterruptedException ex)
        {
        	
        }
    }
}