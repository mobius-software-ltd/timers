package com.mobius.software.common.dal.timers;

public class RunnableTask implements Task {
	private final Runnable runnable;
	private final String id;
	
	public RunnableTask(Runnable runnable, String id) {
		this.runnable = runnable;
		this.id = id;
	}
	
	@Override
	public void execute() {
		this.runnable.run();		
	}

	@Override
	public long getStartTime() {
		return System.currentTimeMillis();
	}
	
	public String getId() {
		return this.id;
	}
}
