package com.mobius.software.common.dal.timers;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegularTask implements Timer {
    private static Logger logger = LogManager.getLogger(RegularTask.class);

   private long startTime;

    private AtomicLong timestamp;
    private AtomicBoolean executed;

    public RegularTask(Long delay) 
    {
    	this.executed = new AtomicBoolean(false);
    	this.startTime = System.currentTimeMillis();
        this.timestamp = new AtomicLong(System.currentTimeMillis() + delay);        
    }

    @Override
    public void execute() 
    {
        logger.debug("Executing local task of type " + this.getClass().getCanonicalName());
        if (timestamp.get() < Long.MAX_VALUE) 
        {
            executed.set(true);
            timestamp.set(Long.MAX_VALUE);           
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
        return timestamp.get();
    }

    public void reschedule(Long delay)
    {
    	this.timestamp = new AtomicLong(System.currentTimeMillis() + delay);
    }
    
    @Override
    public void stop() 
    {
        timestamp.set(Long.MAX_VALUE);     
    }

    @Override
    public Integer getQueueIndex() 
    {
        return 0;        
    }
    
    public Boolean isExecuted() 
    {
        return executed.get();
    }
}