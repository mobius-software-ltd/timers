package com.mobius.software.common.dal.timers;

public interface TaskCallback<T extends Exception>
{
	void onSuccess();

	void onError(T exception);
}