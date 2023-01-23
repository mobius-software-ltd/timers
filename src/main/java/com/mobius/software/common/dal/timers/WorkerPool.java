package com.mobius.software.common.dal.timers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/* 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version. This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org
 * Copyright 2015-2023, Mobius Software LTD
 */

public class WorkerPool 
{
	private CountableQueue<Task> queue;
	private PeriodicQueuedTasks<Timer> periodicQueue;
	
	private ScheduledExecutorService timersExecutor;
	private ExecutorService workersExecutors;
	
	public WorkerPool()
	{
		queue=new CountableQueue<Task>();
		periodicQueue=new PeriodicQueuedTasks<Timer>(100, queue);		
	}

	public void start(int workersNumber)
	{
		timersExecutor = Executors.newScheduledThreadPool(1);
		timersExecutor.scheduleAtFixedRate(new TimersRunner(periodicQueue), 0, 100, TimeUnit.MILLISECONDS);
		
		workersExecutors = Executors.newFixedThreadPool(workersNumber);
		
		List<Worker> workers=new ArrayList<Worker>();
		for(int i=0;i<workersNumber;i++)
		{
			workers.add(new Worker(queue, true));
			workersExecutors.execute(workers.get(i));
		}
	}
	
	public void stop()
	{
		workersExecutors.shutdown();
		workersExecutors =  null;
		
		timersExecutor.shutdown();
		timersExecutor = null;
	}
	
	public CountableQueue<Task> getQueue() 
	{
		return queue;
	}

	public PeriodicQueuedTasks<Timer> getPeriodicQueue() 
	{
		return periodicQueue;
	}		
}
