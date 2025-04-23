package com.mobius.software.common.dal.timers;

public class RunnableTimer implements Timer {
	private final Runnable runnable;
	private final long executionTime;
	private final String id;
	
	protected long startTime = System.currentTimeMillis();
	private Integer queueIndex = null;
	
	public RunnableTimer(Runnable runnable, Long executionTime, String id) {
		this.runnable = runnable;
		this.executionTime = executionTime;
		this.id = id;
	}
	
	@Override
	public void execute() {
		if (this.startTime != Long.MAX_VALUE) {
			this.runnable.run();
		} 
	}

	@Override
	public long getStartTime() {
		return this.startTime;
	}

	@Override
	public Long getRealTimestamp() {
		return this.executionTime;
	}

	@Override
	public void stop() {
		this.startTime = Long.MAX_VALUE;
	}
	
	public String getId() {
		return this.id;
	}
	
	public void setQueueIndex(Integer index) {
		this.queueIndex = index;
	}
	
	@Override
	public Integer getQueueIndex() {
		return this.queueIndex;
	}
}
