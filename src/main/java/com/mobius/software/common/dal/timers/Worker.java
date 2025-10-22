package com.mobius.software.common.dal.timers;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * Mobius Software LTD
 * Copyright 2019 - 2023, Mobius Software LTD and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
public class Worker  implements Runnable
{
	private static Logger logger = LogManager.getLogger(Worker.class);

	private CountableQueue<Task> queue;
	private CountableQueue<Task> localQueue;
	private Task lastTask;
	
	private boolean isRunning;
	private Long taskPoolInterval;
	private String workerPoolName;
	private Integer workerIndex;
	
	public Worker(String workerPoolName, CountableQueue<Task> queue, CountableQueue<Task> localQueue, boolean isRunning, Long taskPollInterval, Integer workerIndex)
	{
		this.queue = queue;
		this.localQueue = localQueue;
		this.isRunning = isRunning;
		this.taskPoolInterval = taskPollInterval;		
		this.workerIndex = workerIndex;
		this.workerPoolName = workerPoolName;
	}
	
	@Override
	public void run()
	{
		Thread.currentThread().setName(this.workerPoolName + "-thread-" + (workerIndex+1));
		while (isRunning)
		{
			try
			{
				Task task = this.localQueue.poll();
				lastTask = task;
				if (task != null)
				{
					if(logger.isDebugEnabled())
						logger.debug("Executing local task {}", task);

					try
					{
						task.execute();
					}					
					catch (Exception e)
					{
						logger.error("WORKER THREAD CAUGHT UNEXPECTED EXCEPTION!!! " + e.getClass().getSimpleName() + "," + e.getMessage(), e);			
					}
					
					if(logger.isDebugEnabled())
						logger.debug("Done executing local task {}", task);
				}
				
				if(task==null)
					task = this.queue.poll(this.taskPoolInterval, TimeUnit.MILLISECONDS);
				else
					task = this.queue.poll();
				
				if (task != null)
				{
					if(logger.isDebugEnabled())
						logger.debug("Executing task {}", task);

					try
					{
						task.execute();				
					}
					catch (Exception e)
					{
						logger.error("WORKER THREAD CAUGHT UNEXPECTED EXCEPTION!!! " + e.getClass().getSimpleName() + "," + e.getMessage(), e);			
					}
					
					if(logger.isDebugEnabled())
						logger.debug("Done executing task {}", task);					
				}
				else if(logger.isTraceEnabled())
					logger.trace("No tasks found for queue , retrying");
			}
			catch (InterruptedException e)
			{
				//lets try again
			}
		}			
	}

	public void stop()
	{
		this.isRunning = false;
	}
	
	public CountableQueue<Task> getLocalQueue()
	{
		return localQueue;
	}

	public Task getLastTask() {
		return lastTask;
	}
}