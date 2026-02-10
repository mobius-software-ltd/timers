package com.mobius.software.common.dal.timers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
	private static final Logger logger = LogManager.getLogger(Worker.class);
	private static final WorkerPoolManager poolManager = WorkerPoolManager.getInstance();

	private final int id = WorkerPoolManager.getNextPoolID();
	private final String name;

	private CountableQueue<Task> queue;
	private PeriodicQueuedTasks<Timer> periodicQueue;

	private ScheduledExecutorService timersExecutor;
	private ScheduledExecutorService healthCheckExecutor;
	private ExecutorService workersExecutors;

	private long healthCheckInterval = 1000L;
	private long maxHealthCheckExecutionTime = 1000L;
	private long taskPoolInterval;
	private List<Worker> workers;

	private AtomicLong totalStoredTasks = new AtomicLong();
	private AtomicLong totalPendingTasks = new AtomicLong();
	private AtomicLong totalStoredTimerTasks = new AtomicLong();
	private AtomicLong totalPendingTimersTasks = new AtomicLong();

	public WorkerPool(String name)
	{
		this(name, 100L);
	}

	public WorkerPool(String name, long taskPoolInterval)
	{
		this.name = name;
		this.taskPoolInterval = taskPoolInterval;

		queue = new CountableQueue<Task>(totalStoredTasks, totalPendingTasks);
		periodicQueue = new PeriodicQueuedTasks<Timer>(taskPoolInterval, this, totalStoredTimerTasks, totalPendingTimersTasks);
		logger.info("Starting worker pool " + name + " with interval " + taskPoolInterval);
	}

	public void start(int workersNumber)
	{
		start(workersNumber, Long.MAX_VALUE);
	}
	
	public void start(int workersNumber, long alertThreshold)
	{
		if (timersExecutor != null)
		{
			logger.warn("The worker pool  " + name + " is already started, can not start it second time!!!!");
			return;
		}

		timersExecutor = Executors.newScheduledThreadPool(1);
		timersExecutor.scheduleWithFixedDelay(new TimersRunner(name, periodicQueue), 0, taskPoolInterval, TimeUnit.MILLISECONDS);

		workersExecutors = Executors.newFixedThreadPool(workersNumber);

		workers = new ArrayList<Worker>();
		for (int i = 0; i < workersNumber; i++)
		{
			workers.add(new Worker(name, queue, new CountableQueue<Task>(totalStoredTasks, totalPendingTasks), true, taskPoolInterval, i));
			workersExecutors.execute(workers.get(i));
		}

		healthCheckExecutor = Executors.newSingleThreadScheduledExecutor();
		healthCheckExecutor.scheduleWithFixedDelay(new HealthCheckTimer(workers, maxHealthCheckExecutionTime, alertThreshold), healthCheckInterval, healthCheckInterval, TimeUnit.MILLISECONDS);

		poolManager.notifyPoolStarted(this);
	}

	public void stop()
	{
		if (timersExecutor == null)
		{
			logger.warn("The worker pool " + name + " is already stopped or not started, can not stop it second time!!!!");
			return;
		}

		workersExecutors.shutdown();
		workersExecutors = null;

		timersExecutor.shutdown();
		timersExecutor = null;

		healthCheckExecutor.shutdown();
		healthCheckExecutor = null;

		for (Worker worker : workers)
			worker.stop();

		workers = null;
		poolManager.notifyPoolStopped(this);
	}

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public void addTaskFirst(RunnableTask task)
	{
		CountableQueue<Task> queue = this.getQueue(task.getId());
		if (queue != null)
			queue.offerFirst(task);
	}

	public void addTaskLast(RunnableTask task)
	{
		CountableQueue<Task> queue = this.getQueue(task.getId());
		if (queue != null)
			queue.offerLast(task);
	}

	public void addTimer(RunnableTimer timer)
	{
		int queueIndex = this.findQueueIndex(timer.getId());

		timer.setQueueIndex(queueIndex);
		periodicQueue.store(timer.getRealTimestamp(), timer);
	}

	private CountableQueue<Task> getQueue(String id)
	{
		int index = this.findQueueIndex(id);
		return this.getLocalQueue(index);
	}

	public int findQueueIndex(String id)
	{
		return Math.abs(id.hashCode()) % workers.size();
	}

	public CountableQueue<Task> getQueue()
	{
		return queue;
	}

	public CountableQueue<Task> getLocalQueue(int index)
	{
		if (workers == null || index >= workers.size())
			return null;

		return workers.get(index).getLocalQueue();
	}

	public PeriodicQueuedTasks<Timer> getPeriodicQueue()
	{
		return periodicQueue;
	}

	public Long getQueueSize()
	{
		return totalPendingTasks.get();
	}

	public List<Integer> getLocalQueuesSize()
	{
		List<Integer> result = new ArrayList<>();
		if (workers == null)
			return result;

		for (Worker worker : workers)
			result.add(worker.getLocalQueue().size());

		return result;
	}

	public Long getScheduledSize()
	{
		return totalPendingTimersTasks.get();
	}

	public Long getStoredTasks()
	{
		return totalStoredTasks.get();
	}

	public Long getStoredScheduledTasks()
	{
		return totalStoredTimerTasks.get();
	}
}
