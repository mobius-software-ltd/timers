package com.mobius.software.common.dal.timers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerPoolManager
{
	private static WorkerPoolManager instance;
	private static AtomicInteger counter = new AtomicInteger(0);

	private Set<WorkerPool> workerPools = new HashSet<>();

	public static WorkerPoolManager getInstance()
	{
		if (instance == null)
			instance = new WorkerPoolManager();

		return instance;
	}

	public static int getNextPoolID()
	{
		return counter.incrementAndGet();
	}

	public void notifyPoolStarted(WorkerPool workerPool)
	{
		workerPools.add(workerPool);
	}

	public void notifyPoolStopped(WorkerPool workerPool)
	{
		workerPools.remove(workerPool);
	}

	public List<WorkerPool> getActivePools()
	{
		return new ArrayList<>(workerPools);
	}
}
