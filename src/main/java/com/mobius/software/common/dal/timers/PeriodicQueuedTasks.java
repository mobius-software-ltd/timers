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
		logger.debug("storing task {} with timestamp {} in period Time {}", task, timestamp, periodTime);
		if (previousRunTime >= periodTime) {
			logger.debug("storing task {} in passAway queue as previous Run Time {} is higher", task, previousRunTime);
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
				logger.debug("task {} creating in new queue {} for period {}", task, queue.hashCode(), periodTime);
			}

			if (previousRun.get() >= periodTime)
			{
				logger.debug("storing task {} in passAway queue and removing periodTime as previous Run Time {} is higher", task, previousRunTime);
				passAwayQueue.offer(task);
				queues.remove(periodTime);				
			} else {
				logger.debug("storing task {} in queue {} for period {}", task, queue.hashCode(), periodTime);
				queue.offer(task);
			}
		}
	}

	public void executePreviousPool(long timestamp)
	{
		Long originalTime = (timestamp - timestamp % period - period);
		Long periodTime = originalTime;

		ConcurrentLinkedQueue<T> queue = null;
		T current;

		do
		{
			if (!previousRun.compareAndSet(0, periodTime))
				periodTime = previousRun.addAndGet(period);

			queue = queues.remove(periodTime);
			if (queue != null)
			{
				while ((current = queue.poll()) != null)
				{
					if (current.getRealTimestamp() < (periodTime + period))
					{
						if(current.getQueueIndex()!=null)
						{
							if(logger.isDebugEnabled())
								logger.debug("Adding periodic task to local queue {} for execution of type {}", current.getQueueIndex(), current.getClass().getCanonicalName());								
							
							CountableQueue<Task> countableQueue = workerPool.getLocalQueue(current.getQueueIndex());
							
							if(logger.isDebugEnabled())
								logger.debug("Adding periodic task to local queue {} for execution of type {}", countableQueue.hashCode(), current.getClass().getCanonicalName());

							countableQueue.offerFirst(current);							
							
						}
						else
						{
							if(logger.isDebugEnabled())
								logger.debug("Adding periodic task to queue for execution of type {}", current.getClass().getCanonicalName());
							
							workerPool.getQueue().offerFirst(current);
						}
					}
				}
			}
		} 
		while (periodTime.longValue() < originalTime.longValue());

		while ((current = passAwayQueue.poll()) != null)
		{
			if (current.getRealTimestamp() < (periodTime + period))
			{
				if(current.getQueueIndex()!=null)
				{
					if(logger.isDebugEnabled())
						logger.debug("Adding periodic task to local queue {} for execution of type {}", current.getQueueIndex(), current.getClass().getCanonicalName());								
					
					CountableQueue<Task> countableQueue = workerPool.getLocalQueue(current.getQueueIndex());
					
					if(logger.isDebugEnabled())
						logger.debug("Adding periodic task to local queue {} for execution of type {}", countableQueue.hashCode(), current.getClass().getCanonicalName());
					
					countableQueue.offerFirst(current);	
				}
				else
				{
					if(logger.isDebugEnabled())
						logger.debug("Adding periodic task to queue for execution of type {}", current.getClass().getCanonicalName());
					
					workerPool.getQueue().offerFirst(current);
				}
			}
		}
	}

	public ConcurrentHashMap<Long, ConcurrentLinkedQueue<T>> getQueues()
	{
		return queues;
	}
}