/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-2
 * @version 1.0.0
 */
package com.eandroid.net.http.util;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import com.eandroid.util.ArrayDeque;

public class SerialExecutor implements Executor {
	private ThreadPoolExecutor threadPoolExecutor;
	public SerialExecutor(ThreadPoolExecutor threadPoolExecutor){
		this.threadPoolExecutor = threadPoolExecutor;
	}
    final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
    Runnable mActive;
    public synchronized void execute(final Runnable r) {
        mTasks.offer(new Runnable() {
            public void run() {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        if (mActive == null) {
            scheduleNext();
        }
    }
    protected synchronized void scheduleNext() {
        if ((mActive = mTasks.poll()) != null) {
        	threadPoolExecutor.execute(mActive);
        }
    }
}