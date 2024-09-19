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
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CountableQueue<T extends Task>
{
	private LinkedBlockingDeque<T> queue = new LinkedBlockingDeque<T>();
	private AtomicInteger counter = new AtomicInteger(0);
	private AtomicLong globalPendingCounter;
	private AtomicLong globalStoredCounter;
	
	public CountableQueue(AtomicLong globalStoredCounter,AtomicLong globalPendingCounter)
	{
		this.globalStoredCounter=globalStoredCounter;
		this.globalPendingCounter=globalPendingCounter;
	}
	
	public void offerLast(T element)
	{
		counter.incrementAndGet();
		globalPendingCounter.incrementAndGet();
		globalStoredCounter.incrementAndGet();
		queue.offerLast(element);
	}

	public void offerFirst(T element)
	{
		counter.incrementAndGet();
		globalPendingCounter.incrementAndGet();
		globalStoredCounter.incrementAndGet();
		queue.offerFirst(element);
	}

	public T take() throws InterruptedException
	{
		T element = queue.take();
		if (element != null)
		{
			counter.decrementAndGet();
			globalPendingCounter.decrementAndGet();
		}
		return element;
	}

	public T poll(long timeout, TimeUnit unit) throws InterruptedException
	{
		T element = queue.poll(timeout, unit);
		if (element != null)
		{
			counter.decrementAndGet();
			globalPendingCounter.decrementAndGet();
		}
		return element;
	}
	
	public T poll()
	{
		T element = queue.poll();
		if (element != null)
		{
			counter.decrementAndGet();
			globalPendingCounter.decrementAndGet();
		}
		return element;
	}

	public boolean thresholdReached(int threshold,int timeThreshold)
	{
		T task = queue.peek();
		if (task != null && task.getStartTime()<(System.currentTimeMillis()-timeThreshold))
			return counter.get() >= threshold;
			
		return false;		
	}

	public int getCounter()
	{
		return counter.get();
	}

	public int size()
	{
		return queue.size();
	}

	public void clear()
	{
		queue.clear();
		globalPendingCounter.addAndGet(0-counter.get());
		counter.set(0);
	}
}
