package com.mobius.software.common.dal.timers;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/* 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version. This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org
 * Copyright 2015-2023, Mobius Software LTD
 */
public class Worker  implements Runnable
{
	private static Logger logger = LogManager.getLogger(Worker.class);

	private static final long TASK_POLL_INTERVAL = 100L;
	
	private CountableQueue<Task> queue;
	private boolean isRunning;
	
	public Worker(CountableQueue<Task> queue, boolean isRunning)
	{
		this.queue = queue;
		this.isRunning = isRunning;
	}

	@Override
	public void run()
	{
		try
		{
			while (isRunning)
			{
				Task task = this.queue.poll(TASK_POLL_INTERVAL, TimeUnit.MILLISECONDS);
				if (task != null)
				{
					task.execute();
				}
			}
		}
		catch (InterruptedException e)
		{			
		}
		catch (Exception e)
		{
			logger.error("WORKER THREAD CAUGHT UNEXPECTED EXCEPTION!!! " + e.getClass().getSimpleName() + "," + e.getMessage(), e);			
		}
	}

	public void stop()
	{
		this.isRunning = false;
	}
}
