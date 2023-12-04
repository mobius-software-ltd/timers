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
	
	private boolean isRunning;
	private Long taskPoolInterval;
	
	public Worker(CountableQueue<Task> queue, CountableQueue<Task> localQueue, boolean isRunning, Long taskPollInterval)
	{
		this.queue = queue;
		this.localQueue = localQueue;
		this.isRunning = isRunning;
		this.taskPoolInterval = taskPollInterval;
	}
	
	@Override
	public void run()
	{
		while (isRunning)
		{
			try
			{
				Task task = this.localQueue.poll();
				if (task != null)
				{
					try
					{
						task.execute();
					}					
					catch (Exception e)
					{
						logger.error("WORKER THREAD CAUGHT UNEXPECTED EXCEPTION!!! " + e.getClass().getSimpleName() + "," + e.getMessage(), e);			
					}
				}
				
				if(task==null)
					task = this.queue.poll(this.taskPoolInterval, TimeUnit.MILLISECONDS);
				else
					task = this.queue.poll();
				
				if (task != null)
				{
					try
					{
						task.execute();				
					}
					catch (Exception e)
					{
						logger.error("WORKER THREAD CAUGHT UNEXPECTED EXCEPTION!!! " + e.getClass().getSimpleName() + "," + e.getMessage(), e);			
					}
				}
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
}