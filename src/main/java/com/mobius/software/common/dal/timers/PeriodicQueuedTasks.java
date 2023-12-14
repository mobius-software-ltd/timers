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
	
	public PeriodicQueuedTasks(long period, WorkerPool workerPool)
	{
		this.workerPool = workerPool;
		this.period = period;
		
		Long originalTime = System.currentTimeMillis();
		originalTime = (originalTime - originalTime % period - period);
		this.previousRun.set(originalTime);
	}

	public long getPeriod()
	{
		return period;
	}

	public long getPreviousRun()
	{
		return previousRun.get();
	}

	public void store(long timestamp, T task)
	{
		Long periodTime = timestamp - timestamp % period;
		ConcurrentLinkedQueue<T> queue;
		Long previousRunTime = previousRun.get();
		if(logger.isDebugEnabled())
			logger.debug("storing task {} with timestamp {} in period Time {}", task, timestamp, periodTime);
			
		if (previousRunTime >= periodTime || (timestamp<System.currentTimeMillis() + period)) {
			if(logger.isDebugEnabled())
				logger.debug("storing task {} in passAway queue and removing periodTime {} as previous Run Time {} is higher", task, periodTime, previousRunTime);

			passAwayQueue.offer(task);	
		}
		else
		{
			queue = queues.get(periodTime);
			if (queue == null)
			{
				queue = new ConcurrentLinkedQueue<T>();
				ConcurrentLinkedQueue<T> oldQueue = queues.putIfAbsent(
						periodTime, queue);
				if (oldQueue != null)
					queue = oldQueue;
				if(logger.isDebugEnabled())
					logger.debug("task {} creating in new queue {} for period {}", task, queue.hashCode(), periodTime);
			}
			
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

	public void executePreviousPool(long timestamp)
	{
		Long originalTime = (timestamp - timestamp % period - period);
		Long periodTime = new Long(originalTime);

		ConcurrentLinkedQueue<T> queue = null;
		T current;
		
		// if(logger.isDebugEnabled())
		// 	logger.debug("periodTime {} , originalTime {}, period {}", periodTime, originalTime, period);								

		do
		{
			if(previousRun.addAndGet(period) > System.currentTimeMillis()) {					
				previousRun.addAndGet(0 - period);
				return;
			}
			
			queue = queues.remove(periodTime);
			if (queue != null)
			{
				while ((current = queue.poll()) != null)
				{
					if (current.getRealTimestamp() < (periodTime + period))
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
								logger.debug("previousTimeRun {}, periodTime {} , originalTime {}, period {}, timestamp {}", previousRun.get(), periodTime, originalTime, period, timestamp);									
							}
							
							CountableQueue<Task> countableQueue = workerPool.getLocalQueue(current.getQueueIndex());
							
							if(logger.isDebugEnabled())
								logger.debug("Adding periodic task {} from queue to workerpool local queue {} for execution at task real timestamp {}", current, countableQueue.hashCode(), current.getRealTimestamp());

							countableQueue.offerFirst(current);							
							
						}
						else
						{
							if(logger.isDebugEnabled())
								logger.debug("Adding periodic task {} from queue to workerpool queue for execution at task real timestamp {}", current, current.getRealTimestamp());
							
							workerPool.getQueue().offerFirst(current);
						}
					}
				}
			}
		} 
		while (periodTime.longValue() <= originalTime.longValue());

		while ((current = passAwayQueue.poll()) != null)
		{
			//we are in pass away queue anway , lets execute everything that should be executed even in current cycle
			if (current.getRealTimestamp() < (System.currentTimeMillis() + period))
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
						logger.debug("previousTimeRun {}, periodTime {} , originalTime {}, period {}, timestamp {}", previousRun.get(), periodTime, originalTime, period, timestamp);									
					}
					
					CountableQueue<Task> countableQueue = workerPool.getLocalQueue(current.getQueueIndex());
					
					if(logger.isDebugEnabled())
						logger.debug("Adding periodic task {} from passaway queue to workerpool local queue {} for execution at task real timestamp {}", current, countableQueue.hashCode(), current.getRealTimestamp());
					
					countableQueue.offerFirst(current);	
				}
				else
				{
					if(logger.isDebugEnabled())
						logger.debug("Adding periodic task {} from passaway queue to workerpool queue for execution at task real timestamp {}", current, current.getRealTimestamp());
					
					workerPool.getQueue().offerFirst(current);
				}
			}
			else
				logger.warn("Ignoring task in pass away queue since it was scheduled in the future current time {} , real time of task {}", System.currentTimeMillis(), current.getRealTimestamp());
		}
	}

	public ConcurrentHashMap<Long, ConcurrentLinkedQueue<T>> getQueues()
	{
		return queues;
	}
}