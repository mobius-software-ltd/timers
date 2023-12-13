package com.mobius.software.common.dal.timers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

public class WorkerPool 
{
	private static Logger logger = LogManager.getLogger(Worker.class);		

	private CountableQueue<Task> queue;
	private PeriodicQueuedTasks<Timer> periodicQueue;
	
	private ScheduledExecutorService timersExecutor;
	private ExecutorService workersExecutors;
	
	private long taskPoolInterval = 100L;	
	private List<Worker> workers;
	
	public WorkerPool()
	{
		queue=new CountableQueue<Task>();
		periodicQueue=new PeriodicQueuedTasks<Timer>(taskPoolInterval, this);
		
		logger.info("Starting workerpool with interval " + taskPoolInterval);
	}
	
	public WorkerPool(long taskPoolInterval)
	{
		this.taskPoolInterval = taskPoolInterval;
		queue=new CountableQueue<Task>();
		periodicQueue=new PeriodicQueuedTasks<Timer>(taskPoolInterval, this);		
		
		logger.info("Starting workerpool with interval " + taskPoolInterval);
	}	

	public void start(int workersNumber)
	{
		if(timersExecutor == null) {
			logger.warn("The worker pool is already started, can not start it second time!!!!");
			return;
		}
		
		timersExecutor = Executors.newScheduledThreadPool(1);
		timersExecutor.scheduleAtFixedRate(new TimersRunner(periodicQueue), 0, taskPoolInterval, TimeUnit.MILLISECONDS);
		
		workersExecutors = Executors.newFixedThreadPool(workersNumber);
		
		workers = new ArrayList<Worker>();
		for(int i=0;i<workersNumber;i++)
		{
			workers.add(new Worker(queue, new CountableQueue<Task>(), true, taskPoolInterval));
			workersExecutors.execute(workers.get(i));
		}
	}
	
	public void stop()
	{
		if(timersExecutor==null) {
			logger.warn("The worker pool is already stopped or not started, can not stop it second time!!!!");
			return;
		}
		
		workersExecutors.shutdown();
		workersExecutors =  null;
		
		timersExecutor.shutdown();
		timersExecutor = null;
		
		workers = null;
	}
	
	public CountableQueue<Task> getQueue() 
	{
		return queue;
	}

	public CountableQueue<Task> getLocalQueue(int index) 
	{
		// logger.debug("workers " + workers + " workers size " + workers.size() + " index " + index);
		if(workers == null || index>=workers.size())
			return null;
		
		return workers.get(index).getLocalQueue();
	}
	
	public PeriodicQueuedTasks<Timer> getPeriodicQueue() 
	{
		return periodicQueue;
	}		
}
