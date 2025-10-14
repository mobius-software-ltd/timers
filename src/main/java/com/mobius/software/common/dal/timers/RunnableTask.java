package com.mobius.software.common.dal.timers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RunnableTask implements Task {
	private static Logger logger = LogManager.getLogger(Worker.class);

	private final Runnable runnable;
	private final String id;
	private final String taskName;
	
	public RunnableTask(Runnable runnable, String id, String taskName) {
		this.runnable = runnable;
		this.id = id;
		this.taskName = taskName;
	}
	
	@Override
	public void execute() {
		if(logger.isDebugEnabled())
			logger.debug("Executing local runnable {}", runnable);

		this.runnable.run();		
	}

	@Override
	public long getStartTime() {
		return System.currentTimeMillis();
	}
	
	public String getId() {
		return this.id;
	}

	public String getTaskName() {
		return this.taskName;
	}

	@Override
	public String printTaskDetails() {
		return "Task details. Task name: " + taskName + ", id: " + id;
	}
}
