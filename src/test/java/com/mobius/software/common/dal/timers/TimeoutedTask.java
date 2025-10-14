package com.mobius.software.common.dal.timers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimeoutedTask implements Timer
{
	private static Logger logger = LogManager.getLogger(TimeoutedTask.class);

	private long startTime;

	private AtomicLong timestamp;
	private AtomicBoolean executed;
	private String taskName;
	
	public TimeoutedTask(Long delay, String name)
	{
		this.executed = new AtomicBoolean(false);
		this.startTime = System.currentTimeMillis();
		this.timestamp = new AtomicLong(System.currentTimeMillis() + delay);
		this.taskName = name;
	}

	@Override
	public void execute()
	{
		logger.debug("Executing local task of type " + this.getClass().getCanonicalName());
		if (timestamp.get() < Long.MAX_VALUE)
		{
			try
			{
				Thread.sleep(60000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

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

	public String getTaskName()
	{
		return taskName;
	}

	@Override
	public String printTaskDetails()
	{
		return "Task name: " + taskName;
	}
}