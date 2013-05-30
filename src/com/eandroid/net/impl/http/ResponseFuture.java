/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-25
 * @version 1.0.0
 */
package com.eandroid.net.impl.http;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ResponseFuture <T> implements Future<T>{

	private final Future<T> future;
	private final String requestKey;
	public ResponseFuture(Future<T> future,String requestKey){
		this.future = future;
		this.requestKey = requestKey;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return future.isCancelled();
	}

	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public T get() throws InterruptedException, HttpRequestExecutionException {	
		try{
			return future.get();
		}catch (ExecutionException e) {
			throw new HttpRequestExecutionException(e);
		}
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException,
	HttpRequestExecutionException, TimeoutException {
		try{
			return future.get(timeout,unit);
		}catch (ExecutionException e) {
			throw new HttpRequestExecutionException(e);
		}
	}

	public boolean sameKey(String key) {
		if (key == null) return false;
        if (requestKey == null) return false;
        return requestKey.equals(key);
	}
}
