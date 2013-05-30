/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.net.impl.http;

import android.os.Handler;
import android.os.Looper;

import com.eandroid.net.http.HttpHandler;

public class UIThreadHttpHandlerDecorator<T> extends CurrentThreadResponseHandlerDecorator<T>{

	protected final static Handler threadHandler = new Handler(Looper.getMainLooper());
	
	public UIThreadHttpHandlerDecorator(HttpHandler<T> handler) {
		super(handler);
	}

}
