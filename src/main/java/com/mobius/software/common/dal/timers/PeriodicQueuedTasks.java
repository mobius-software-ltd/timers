package com.mobius.software.common.dal.timers;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PeriodicQueuedTasks<T extends Timer>
{
	private static Logger logger = LogManager.getLogger(PeriodicQueuedTasks.class);

	private ConcurrentHashMap<Long, ConcurrentLinkedQueue<T>> queues = new ConcurrentHashMap<Long, ConcurrentLinkedQueue<T>>();
	private WorkerPool workerPool;
	private ConcurrentLinkedQueue<T> passAwayQueue = new ConcurrentLinkedQueue<T>();

	private long period;
	private AtomicLong previousRun = new AtomicLong(0);	
	private AtomicLong totalStoredTasks;
	private AtomicLong totalPendingTasks;
	
	public PeriodicQueuedTasks(long period, WorkerPool workerPool, AtomicLong totalStoredTasks, AtomicLong totalPendingTasks)
	{
		this.workerPool = workerPool;
		this.period = period;
		
		Long originalTime = System.currentTimeMillis();
		originalTime = (originalTime - originalTime % period - period);
		this.previousRun.set(originalTime);
		this.totalStoredTasks=totalStoredTasks;
		this.totalPendingTasks=totalPendingTasks;
	}

	public long getPeriod()
	{
		return period;
	}

	public long getPreviousRun()
	{
		return previousRun.get();
	}

	/**
	 * Store the task in the queue for the period of the timestamp
	 * or if the task is scheduled in a period past the previous run, store it in the pass away queue
	 * @param timestamp
	 * @param task
	 */
	public void store(long timestamp, T task)
	{
		Long periodTime = timestamp - timestamp % period;
		ConcurrentLinkedQueue<T> queue;
		Long previousRunTime = previousRun.get();
		if(logger.isDebugEnabled())
			logger.debug("storing task {} with timestamp {} in period Time {}", task, timestamp, periodTime);
			
		totalStoredTasks.incrementAndGet();
		if(timestamp<=System.currentTimeMillis())
		{
			if(task.getQueueIndex()!=null)
			{
				if(logger.isDebugEnabled()) {
					logger.debug("Adding periodic task {} immediately because its in past " +
						" to workerpool local queue {} for execution at task " +
						" real timestamp {}", 
						task, 
						task.getQueueIndex(), 
						task.getRealTimestamp());								
				}
				
				CountableQueue<Task> countableQueue = workerPool.getLocalQueue(task.getQueueIndex());
				countableQueue.offerFirst(task);	
			}
			else
			{
				if(logger.isDebugEnabled())
					logger.debug("Adding periodic task {} immediately because its in past to workerpool queue for execution at task real timestamp {}", task, task.getRealTimestamp());
				
				workerPool.getQueue().offerFirst(task);
			}
		}
		else if (previousRunTime >= periodTime || (timestamp<System.currentTimeMillis() + period)) {
			if(logger.isDebugEnabled())
				logger.debug("storing task {} in passAway queue as previous Run Time {} is higher", task, periodTime, previousRunTime);

			totalPendingTasks.incrementAndGet();
			passAwayQueue.offer(task);	
		}
		else
		{
			queue = queues.get(periodTime);
			if (queue == null)
			{
				queue = new ConcurrentLinkedQueue<T>();
				ConcurrentLinkedQueue<T> oldQueue = queues.putIfAbsent(periodTime, queue);
				if (oldQueue != null)
					queue = oldQueue;
				if(logger.isDebugEnabled())
					logger.debug("task {} creating in new queue {} for period {}", task, queue.hashCode(), periodTime);
			}
			
			totalPendingTasks.incrementAndGet();
			previousRunTime = previousRun.get();
			if (previousRunTime >= periodTime)
			{
				if(logger.isDebugEnabled())
					logger.debug("storing task {} in passAway queue and removing periodTime {} as previous Run Time {} is higher", task, periodTime, previousRunTime);

				passAwayQueue.offer(task);
				queues.remove(periodTime);				
			} else {
				if(logger.isDebugEnabled())
					logger.debug("storing task {} in queue {} for period {}", task, queue.hashCode(), periodTime);
					
				queue.offer(task);
			}
		}
	}

	/**
	 * 1. Execute the previous pool of tasks from 
	 * the current timestamp modulo the period
	 * If period is 10ms and timestamp is 1702569195786 
	 * then we process the pool of tasks from 1702569195780
	 * 
	 * We first execute all tasks from the pass away queue 
	 * to catch up on tasks that may have been scheduled 
	 * in a time period we couldn't process
	 * 
	 * A) due to high load we may not have executed this loop for more then 1 cycle. Therefore 
	 * B) we first check the previous run. if it never happened we assume that it happened one period ago 
	 * C) othewise we are running in loop till the period is <=originalTime+period ( because its less or equal ). 
	 * each time we take one period and execuite it all its tasks
	 *  
	 * @param timestamp
	 */
	public void executePreviousPool()
	{
		long timestamp = System.currentTimeMillis();
		// original period time for the current timestamp
		Long originalTime = (timestamp - timestamp % period - period);
		
		// 1. execute pass away queue
		executePassAwayQueue(originalTime, timestamp);
		// 2. execute previous time periods
		executePreviousRunTilCurrentPeriod(originalTime, timestamp);
	}	

	/**
	 * Execute all tasks from the previous time periods up to the current period
	 * @param periodTime
	 * @param originalTime
	 * @param timestamp
	 */
	private void executePreviousRunTilCurrentPeriod(Long originalTime, long timestamp) {
		ConcurrentLinkedQueue<T> queue = null;		
		
		T current;
		while (previousRun.get() <= originalTime.longValue())
		{					
			if(logger.isTraceEnabled())
					logger.trace("previousRunNewTime: previousRunNewTime {}, originalTime {}, period {}, current Timestamp {}", previousRun.get(), originalTime, period, timestamp);								
			
			// get the queue of tasks for the period
			queue = queues.remove(previousRun.get());
			if (queue != null)
			{		
				while ((current = queue.poll()) != null)
				{
					totalPendingTasks.decrementAndGet();
					if (current.getRealTimestamp() < (originalTime + period))
					{
						if(current.getQueueIndex()!=null)
						{
							if(logger.isDebugEnabled()) {
								logger.debug("Adding periodic task {} from queue " +
									" to workerpool local queue {} for execution at task " +
									" real timestamp {}", 
									current, 
									current.getQueueIndex(), 
									current.getRealTimestamp());								
								logger.debug("previousTimeRun {}, originalTime {}, period {}, timestamp {}", previousRun.get(), originalTime, period, timestamp);									
							}
							
							CountableQueue<Task> countableQueue = workerPool.getLocalQueue(current.getQueueIndex());
							
							if(logger.isDebugEnabled())
								logger.debug("Adding periodic task {} from queue to workerpool local queue {} for execution at task real timestamp {}", current, countableQueue.hashCode(), current.getRealTimestamp());
	
							if (current.getRealTimestamp() < (originalTime + period))
								countableQueue.offerLast(current);							
							
						}
						else
						{
							if(logger.isDebugEnabled())
								logger.debug("Adding periodic task {} from queue to workerpool queue for execution at task real timestamp {}", current, current.getRealTimestamp());
							
							if (current.getRealTimestamp() < (originalTime + period))
								workerPool.getQueue().offerLast(current);
						}
					}
					else
						logger.debug("Ignoring task in pass away queue since it was scheduled in the future current time {} , real time of task {}", timestamp, current.getRealTimestamp());
				}
			}

			// increase previous run time by one period
			previousRun.addAndGet(period);
			
			if(logger.isTraceEnabled())
					logger.trace("previousRunNewTime: previousRunNewTime {}, originalTime {}, period {}, current Timestamp {}", previousRun.get(), originalTime, period, timestamp);											
		}	
	}

	/**
	 * Execute all tasks from the pass away queue up to the current period
	 * @param periodTime
	 * @param originalTime
	 * @param timestamp
	 */
	private void executePassAwayQueue(Long originalTime, long timestamp) {		
		T current;
		while ((current = passAwayQueue.poll()) != null)
		{	
			totalPendingTasks.decrementAndGet();
			//we are in pass away queue anway , lets execute everything that should be executed even in current cycle
			long taskTimeStamp = current.getRealTimestamp();
			if(logger.isDebugEnabled())
				logger.debug("taskTimeStamp {}, periodTime {}, period {}, current Timestamp {}", taskTimeStamp, originalTime, period, timestamp);								

			if (taskTimeStamp < (originalTime + period))
			{
				if(current.getQueueIndex()!=null)
				{
					if(logger.isDebugEnabled()) {
						logger.debug("Adding periodic task {} from passaway queue " +
							" to workerpool local queue {} for execution at task " +
							" real timestamp {}", 
							current, 
							current.getQueueIndex(), 
							current.getRealTimestamp());								
						logger.debug("previousTimeRun {} , originalTime {}, period {}, timestamp {}", previousRun.get(), originalTime, period, timestamp);									
					}
					
					CountableQueue<Task> countableQueue = workerPool.getLocalQueue(current.getQueueIndex());
					
					if(logger.isDebugEnabled())
						logger.debug("Adding periodic task {} from passaway queue to workerpool local queue {} for execution at task real timestamp {}", current, countableQueue.hashCode(), taskTimeStamp);
					
					if (current.getRealTimestamp() < (originalTime + period))
						countableQueue.offerFirst(current);	
				}
				else
				{
					if(logger.isDebugEnabled())
						logger.debug("Adding periodic task {} from passaway queue to workerpool queue for execution at task real timestamp {}", current, taskTimeStamp);
					
					if (current.getRealTimestamp() < (originalTime + period))
						workerPool.getQueue().offerFirst(current);
				}
			}
			else
			 	logger.debug("Ignoring task in pass away queue since it was scheduled in the future current time {} , real time of task {}", timestamp, taskTimeStamp);
		}
	}

	public ConcurrentHashMap<Long, ConcurrentLinkedQueue<T>> getQueues()
	{
		return queues;
	}
}