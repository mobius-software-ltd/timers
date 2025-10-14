package com.mobius.software.common.dal.timers;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RepeatedTask implements Timer {
    private static Logger logger = LogManager.getLogger(RepeatedTask.class);

    private long startTime;

    private ConcurrentLinkedQueue<Long> timestamp=new ConcurrentLinkedQueue<Long>();    
    private AtomicInteger executedTimes;
    private PeriodicQueuedTasks<Timer> periodicQueue;
    
    public RepeatedTask(PeriodicQueuedTasks<Timer> periodicQueue, Long delay) 
    {
    	this.periodicQueue = periodicQueue;
    	this.executedTimes = new AtomicInteger(0);
    	this.startTime = System.currentTimeMillis();
        this.timestamp.offer(System.currentTimeMillis() + delay);        
    }

    @Override
    public void execute() 
    {
        logger.debug("Executing local task of type " + this.getClass().getCanonicalName());
        Long currTimestamp = this.timestamp.poll();
        if (currTimestamp < Long.MAX_VALUE) 
        {
        	executedTimes.incrementAndGet();
        	Long nextTime = getRealTimestamp();
        	if (nextTime < Long.MAX_VALUE) 
        		periodicQueue.store(nextTime, this);
        }
    }

    @Override
    public long getStartTime() 
    {
        return startTime;
    }

    @Override
    public Long getRealTimestamp() 
    {
    	Long result = timestamp.peek();
    	if(result == null)
    		return Long.MAX_VALUE;
    	
    	return result;
    }

    public void reschedule(Long delay)
    {
    	this.timestamp.offer(System.currentTimeMillis() + delay);
    }
    
    @Override
    public void stop() 
    {
    	this.timestamp.clear();
        timestamp.offer(Long.MAX_VALUE);     
    }

    @Override
    public Integer getQueueIndex() 
    {
        return 0;        
    }
    
    public Integer getExecutedCount() 
    {
        return executedTimes.get();
    }

	@Override
	public String printTaskDetails()
	{
		return "Task name: RepeatedTask";
	}
}