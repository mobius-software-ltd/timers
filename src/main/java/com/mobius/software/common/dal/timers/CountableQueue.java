package com.mobius.software.common.dal.timers;
/* 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version. This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org
 * Copyright 2015-2023, Mobius Software LTD
 */
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CountableQueue<T extends Task>
{
	private LinkedBlockingDeque<T> queue = new LinkedBlockingDeque<T>();
	private AtomicInteger counter = new AtomicInteger(0);

	public void offerLast(T element)
	{
		counter.incrementAndGet();
		queue.offerLast(element);
	}

	public void offerFirst(T element)
	{
		counter.incrementAndGet();
		queue.offerFirst(element);
	}

	public T take() throws InterruptedException
	{
		T element = queue.take();
		if (element != null)
			counter.decrementAndGet();
		return element;
	}

	public T poll(long timeout, TimeUnit unit) throws InterruptedException
	{
		T element = queue.poll(timeout, unit);
		if (element != null)
			counter.decrementAndGet();
		return element;
	}

	public boolean thresholdReached(int threshold,int timeThreshold)
	{
		T task = queue.peek();
		if (task != null && task.getStartTime()<System.currentTimeMillis()-timeThreshold)
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
		counter.set(0);
	}
}
