/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.net.http.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.AsyncTask;
import android.os.Process;

import com.eandroid.net.NetIOHandler;
import com.eandroid.net.Session;
import com.eandroid.net.http.RequestEntity;
import com.eandroid.net.http.ResponseEntity;
import com.eandroid.net.impl.NetIOHandlerDecorator;
import com.eandroid.net.impl.http.HttpRequestSession;

public class HttpAsyncTask<Result> implements Future<Result>{	
	private static final String TAG = "EasyHttpAsyncTask";

	private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
	private static final int MAXIMUM_POOL_SIZE = 128;
	private static final int KEEP_ALIVE = 1;

	private static ThreadFactory defaultThreadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);
		public Thread newThread(Runnable r) {
			Thread tread = new Thread(r, TAG+" #" + mCount.getAndIncrement());
			tread.setDaemon(true);
			return tread;
		}
	};
	private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(10);
	/**
	 * An {@link Executor} that can be used to execute tasks in parallel.
	 */
	private static volatile ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
			TimeUnit.SECONDS, sPoolWorkQueue, defaultThreadFactory,
			new ThreadPoolExecutor.DiscardOldestPolicy());
	/**
	 * An {@link Executor} that executes tasks one at a time in serial
	 * order.  This serialization is global to a particular process.
	 */
	public static final Executor SERIAL_EXECUTOR = new SerialExecutor(THREAD_POOL_EXECUTOR);

	private static volatile Executor sDefaultExecutor = THREAD_POOL_EXECUTOR;

	private WorkerRunnable<Result> workerRunnable;
	private FutureTask<Result> futureTask;
	private volatile Status mStatus = Status.PENDING;
	/**
	 * Indicates the current status of the task. Each status will be set only once
	 * during the lifetime of a task.
	 */
	public enum Status {
		/**
		 * Indicates that the task has not been executed yet.
		 */
		PENDING,
		/**
		 * Indicates that the task is running.
		 */
		RUNNING,
		/**
		 * Indicates that {@link AsyncTask#onPostExecute} has finished.
		 */
		FINISHED,
	}

	private final AtomicBoolean mCancelled = new AtomicBoolean();
	private volatile Object outcome = null;

	public static void configDefaultExecutor(Executor executor){
		sDefaultExecutor = executor;
	}

	public static void shutdownThreadPool(){
		THREAD_POOL_EXECUTOR.shutdownNow();
	}
	
	public HttpAsyncTask(final HttpRequestSession session,final TaskControlCenter controlCenter){
		if(session == null)
			throw new IllegalArgumentException("An error occured while HttpAsyncTask init requestSession or reqUrl must not be null!");
		session.decoratorHandler(new HttpAsyncTaskReponseHandler(null));
		workerRunnable = new WorkerRunnable<Result>(){
			@Override
			public Result call() throws Exception {
				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
				if(controlCenter != null){
					synchronized (controlCenter.pauseLockObject) {
						while (controlCenter.isPaused() && !isCancelled()) {
							try{
								controlCenter.pauseLockObject.wait();
							}catch(InterruptedException e){}
						}
					}
				}
				if(!isCancelled() && !session.isClosed())
					session.request(requestEntity);
				return null;
			}
		};
		futureTask = new FutureTask<Result>(workerRunnable){
			@Override
			protected void done() {
				finish();
				super.done();
			}
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				session.close();
				return super.cancel(mayInterruptIfRunning);
			}
		};
	}
	
	public void request(RequestEntity requestEntity){
		requestOnExecutor(sDefaultExecutor, requestEntity);
	}

	public void request(RequestEntity requestEntity,boolean isParallel){
		workerRunnable.requestEntity = requestEntity;
		Executor executor = null;
		if(isParallel){
			executor = THREAD_POOL_EXECUTOR;
		}else{
			executor = SERIAL_EXECUTOR;
		}
		requestOnExecutor(executor, requestEntity);
	}

	public void requestOnExecutor(Executor executor,RequestEntity requestEntity){
		if (mStatus != Status.PENDING) {
			switch (mStatus) {
			case RUNNING:
				throw new IllegalStateException("Cannot execute task:"
						+ " the task is already running.");
			case FINISHED:
				throw new IllegalStateException("Cannot execute task:"
						+ " the task has already been executed "
						+ "(a task can be executed only once)");
			default:
				break;
			}
		}
		mStatus = Status.RUNNING;
		workerRunnable.requestEntity = requestEntity;
		executor.execute(futureTask);
	}

	private abstract class WorkerRunnable<T> implements Callable<T>{
		protected RequestEntity requestEntity;
	}

	private class HttpAsyncTaskReponseHandler extends NetIOHandlerDecorator {
		public HttpAsyncTaskReponseHandler(NetIOHandler handler) {
			super(handler);
		}
		public void onRead(Session session,Object message) {
			ResponseEntity entity = (ResponseEntity)message;
			if(entity != null)
				outcome = entity.getContent();
			super.onRead(session,message);
		};
		@Override
		public void onCatchException(Session session,Exception exception) {
			if(outcome == null)
				outcome = exception;
			super.onCatchException(session,exception);
		}
	}

	private void finish() {
		mStatus = Status.FINISHED;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		mCancelled.set(true);
		return futureTask.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return mCancelled.get();
	}

	@Override
	public boolean isDone() {
		return futureTask.isDone();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Result get(long timeout, TimeUnit unit) throws InterruptedException,
	ExecutionException, TimeoutException {
		futureTask.get(timeout, unit);
		if(outcome == null)
			return null;
		if(outcome instanceof Exception)
			throw new ExecutionException((Exception)outcome);
		else {
			return (Result)outcome;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Result get() throws InterruptedException, ExecutionException {
		futureTask.get();
		if(outcome == null)
			return null;
		if(outcome instanceof Exception)
			throw new ExecutionException(((Exception)outcome).getCause());
		else {
			return (Result)outcome;
		}
	}
}
