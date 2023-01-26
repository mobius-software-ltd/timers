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
