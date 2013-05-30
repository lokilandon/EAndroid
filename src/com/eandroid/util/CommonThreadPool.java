/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-3
 * @version 1.0.0
 */
package com.eandroid.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CommonThreadPool {
	private final static String TAG = "EAndroid-common-threadpool";

    private static ThreadFactory defaultThreadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);
        public Thread newThread(Runnable r) {
        	Thread thread = new Thread(r, TAG+" #" + mCount.getAndIncrement());
        	thread.setPriority(3);
            return thread;
        }
	};
	private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(20);
    private static volatile ThreadPoolExecutor commonThreadPool = new ThreadPoolExecutor(0, 1, 1,
            TimeUnit.SECONDS, sPoolWorkQueue, defaultThreadFactory,
            new ThreadPoolExecutor.DiscardOldestPolicy());
    
    public static void execute(Runnable command){
    	commonThreadPool.execute(command);
    }
    
    public static void shutdownNow(){
    	commonThreadPool.shutdownNow();
    }
}
