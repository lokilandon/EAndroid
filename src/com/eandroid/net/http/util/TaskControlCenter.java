/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-10
 * @version 0.1
 */
package com.eandroid.net.http.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.eandroid.net.Http;
import com.eandroid.net.http.HttpHandler;
import com.eandroid.net.http.RequestEntity;
import com.eandroid.net.http.RequestEntity.RequestConfig;
import com.eandroid.net.http.RequestEntity.RequestMethod;


public class TaskControlCenter {

	final Object pauseLockObject = new Object();

	private List<Task> futureTaskList = new ArrayList<Task>(); 

	private AtomicBoolean paused = new AtomicBoolean(false);

	public void addTask(Task task) {
		if(task.future.isDone()){
			return;
		}
		synchronized (TaskControlCenter.class) {
			futureTaskList.add(task);	
		}
	}

	public boolean isPaused() {
		return paused.get();
	}

	public void pause() {
		paused.set(true);
	}

	public void unPause() {
		paused.set(false);
		synchronized (pauseLockObject) {
			pauseLockObject.notifyAll();
		}
	}

	public void stopAllTask(boolean removeAfterStop){
		synchronized (TaskControlCenter.class) {
			Iterator<Task> it = futureTaskList.iterator();
			while (it.hasNext()) {
				Task task = it.next();
				if(!task.future.isCancelled() && !task.future.isDone()){
					task.cancel();
					if(removeAfterStop)
						it.remove();
				}else{
					it.remove(); 
				}
			}	
		}
	}

	public void clear(){
		synchronized (TaskControlCenter.class) {
			futureTaskList.clear();
		}
	}

	public synchronized void restartAllCancellledTask(){
		synchronized (TaskControlCenter.class) {
			Iterator<Task> it = futureTaskList.iterator();
			while (it.hasNext()) {
				Task task = it.next();
				if(task.future.isCancelled()){
					task.resend();
				}
			}
		}
	}

	public static class Task{
		private Future<?> future;
		private WeakReference<Http> http;
		private RequestEntity request;
		private WeakReference<HttpHandler<?>> handler;
		private boolean isSerial;
		public Task(Future<?> future,Http http,RequestEntity request,HttpHandler<?> handler,boolean isSerial) {
			if(http == null || future == null)
				throw new IllegalArgumentException();
			this.future = future;
			this.http = new WeakReference<Http>(http);
			this.request = request;
			this.handler = new WeakReference<HttpHandler<?>>(handler);
			this.isSerial = isSerial;
		}

		@SuppressWarnings("unchecked")
		public <T> void resend() {
			if(future.isCancelled()){
				Http http = this.http.get();
				HttpHandler<T> handler = (HttpHandler<T>) this.handler.get();
				if(http == null || handler == null)
					return;
				RequestConfig<T> config = (RequestConfig<T>) request.getConfig();
				if(request.getRequestMethod() == RequestMethod.Get){
					future = http.get(request.getUrl(),
							config.getDownloadPath(), 
							config.getHttpParams(), 
							config.getResponseParser(), 
							config.getResponseClass(),
							handler,
							isSerial,
							null);
				}else if(request.getRequestMethod() == RequestMethod.Post){
					future = http.get(request.getUrl(),
							config.getDownloadPath(), 
							config.getHttpParams(), 
							config.getResponseParser(), 
							config.getResponseClass(),
							handler,
							isSerial,
							null);
				}
			}
		}

		public void cancel(){
			future.cancel(true);
		}
	}
}
