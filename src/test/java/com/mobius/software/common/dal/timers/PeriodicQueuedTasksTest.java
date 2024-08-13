package com.mobius.software.common.dal.timers;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PeriodicQueuedTasksTest 
{
	private static Logger logger = LogManager.getLogger(PeriodicQueuedTasksTest.class);

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
    	
    	RegularTask task = new RegularTask(500L);
        tasks.store(task.getRealTimestamp(), task);
        try
        {
        	Thread.sleep(600L);
        }
        catch(InterruptedException ex)
        {
        	
        }
        
        assertTrue(task.isExecuted());
    	logger.info("One time task test end");
    }
    
    @Test
    public void testCancelTask()
    {
    	logger.info("Cancel task test begin");
    	
    	RegularTask task = new RegularTask(500L);
        tasks.store(task.getRealTimestamp(), task);
        task.stop();
        try
        {
        	Thread.sleep(600L);
        }
        catch(InterruptedException ex)
        {
        	
        }
        
        assertFalse(task.isExecuted());
    	logger.info("Cancel task test end");
    }
    
    @Test
    public void testRescheduleTask()
    {
    	logger.info("Reschedule task test begin");
    	
    	RegularTask task = new RegularTask(500L);
        tasks.store(task.getRealTimestamp(), task);
        task.reschedule(1000L);
        tasks.store(task.getRealTimestamp(), task);
        try
        {
        	Thread.sleep(600L);
        }
        catch(InterruptedException ex)
        {
        	
        }
        
        assertFalse(task.isExecuted());
        
        try
        {
        	Thread.sleep(1000L);
        }
        catch(InterruptedException ex)
        {
        	
        }
        
        assertTrue(task.isExecuted());
        
    	logger.info("Reschedule task test end");
    }
    
    @Test
    public void testPassAwayTask()
    {
    	logger.info("Pass away task test begin");
    	
    	RegularTask task = new RegularTask(500L);
        tasks.store(task.getRealTimestamp(), task);
        try
        {
        	Thread.sleep(600L);
        }
        catch(InterruptedException ex)
        {
        	
        }
        
        assertTrue(task.isExecuted());
    	logger.info("Pass away task test end");
    } 
    
    @Test
    public void testRepeatedTask()
    {
    	logger.info("Repeated task test begin");
    	
    	RepeatedTask task = new RepeatedTask(tasks, 500L);
    	tasks.store(task.getRealTimestamp(), task);
        task.reschedule(1000L);
        task.reschedule(1500L);
        task.reschedule(2000L);
        task.reschedule(2500L);
        task.reschedule(3000L);
        
    	try
        {
        	Thread.sleep(600L);
        }
        catch(InterruptedException ex)
        {
        	
        }
        
        assertEquals(task.getExecutedCount(),new Integer(1));
        
        try
        {
        	Thread.sleep(1000L);
        }
        catch(InterruptedException ex)
        {
        	
        }
        
        assertEquals(task.getExecutedCount(),new Integer(3));
        
        try
        {
        	Thread.sleep(1000L);
        }
        catch(InterruptedException ex)
        {
        	
        }
        
        assertEquals(task.getExecutedCount(),new Integer(5));
        try
        {
        	Thread.sleep(1000L);
        }
        catch(InterruptedException ex)
        {
        	
        }
        
        assertEquals(task.getExecutedCount(),new Integer(6));
        try
        {
        	Thread.sleep(1000L);
        }
        catch(InterruptedException ex)
        {
        	
        }
        
        assertEquals(task.getExecutedCount(),new Integer(6));
        
    	logger.info("Repeated task task test end");
    }       
}