package com.mobius.software.common.dal.timers;

import java.util.List;

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
public class HealthCheckTimer implements Runnable
{
	private static Logger logger = LogManager.getLogger(HealthCheckTimer.class);
	private long maxExecutionTime;
	private List<Worker> workers;

	public HealthCheckTimer(List<Worker> workers, long maxExecutionTime)
	{
		this.workers = workers;
		this.maxExecutionTime = maxExecutionTime;
	}

	@Override
	public void run()
	{
		if (workers != null && workers.size() > 0)
		{
			for (Worker worker : workers)
			{
				Task lastTask = worker.getLastTask();
				Long expirationTime = System.currentTimeMillis() - maxExecutionTime;

				if (lastTask != null && expirationTime > lastTask.getStartTime())
				{
					logger.error("Task was not executed within 10 seconds. Task details: " + lastTask.printTaskDetails());
				}
			}
		}
	}
}