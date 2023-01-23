package com.mobius.software.common.dal.timers;
/* 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version. This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org
 * Copyright 2015-2023, Mobius Software LTD
 */
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class PeriodicQueuedTasks<T extends Timer>
{
	private ConcurrentHashMap<Long, ConcurrentLinkedQueue<T>> queues = new ConcurrentHashMap<Long, ConcurrentLinkedQueue<T>>();
	private CountableQueue<Task> mainQueue;
	private ConcurrentLinkedQueue<T> passAwayQueue = new ConcurrentLinkedQueue<T>();

	private long period;
	private AtomicLong previousRun = new AtomicLong(0);

	public PeriodicQueuedTasks(long period, CountableQueue<Task> mainQueue)
	{
		this.mainQueue = mainQueue;
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
		if (previousRun.get() >= periodTime)
			passAwayQueue.offer(task);
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
			}

			if (previousRun.get() >= periodTime)
			{
				passAwayQueue.offer(task);
				queues.remove(periodTime);
			} else
				queue.offer(task);
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
						mainQueue.offerFirst(current);
				}
			}
		} while (periodTime.longValue() < originalTime.longValue());

		while ((current = passAwayQueue.poll()) != null)
		{
			if (current.getRealTimestamp() < (periodTime + period))
				mainQueue.offerFirst(current);
		}
	}

	public ConcurrentHashMap<Long, ConcurrentLinkedQueue<T>> getQueues()
	{
		return queues;
	}
}