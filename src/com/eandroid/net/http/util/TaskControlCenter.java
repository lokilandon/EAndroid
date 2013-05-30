/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-10
 * @version 1.0.0
 */
package com.eandroid.net.http.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;


public class TaskControlCenter {

	final Object pauseLockObject = new Object();

	private List<Future<?>> futureTaskList = new ArrayList<Future<?>>(); 

	private AtomicBoolean paused = new AtomicBoolean(false);

	void addTask(Future<?> task) {
		futureTaskList.add(task);
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

	public void cancelAllTask(){
		Iterator<Future<?>> it = futureTaskList.iterator();
		while (it.hasNext()) {
			it.next().cancel(true);
			it.remove();
		}
	}
}
