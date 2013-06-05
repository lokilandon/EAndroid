/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.net.impl.http;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Looper;

import com.eandroid.net.http.HttpHandler;

public class CurrentThreadResponseHandlerDecorator<T> extends HttpHandlerDecorator<T>{

	protected volatile static Handler threadHandler;
	private WeakReference<HttpHandler<T>> handlerWeakReference;

	public CurrentThreadResponseHandlerDecorator(HttpHandler<T> handler) {
		super(handler);
		handlerWeakReference = new WeakReference<HttpHandler<T>>(decoratedHandler);
		if(threadHandler == null)
			synchronized (CurrentThreadResponseHandlerDecorator.class) {
				if(threadHandler == null){
					if (Looper.myLooper() == null) {
						throw new RuntimeException(
								"init HttpResponseHandler error:Can't create handler inside thread that has not called Looper.prepare()");
					}
					threadHandler = new Handler();
				}
			}
	}	
	
	@Override
	public void onCatchException(final Exception exception) {
		threadHandler.post(new Runnable() {
			@Override
			public void run() {
				if(handlerWeakReference == null)
					return;
				HttpHandler<T> handler = handlerWeakReference.get();
				if(handler != null)
					handler.onCatchException(exception);
			}
		});
	}

	@Override
	public void onDownloadProgress(final int progress) {
		threadHandler.post(new Runnable() {
			@Override
			public void run() {
				if(handlerWeakReference == null)
					return;
				HttpHandler<T> handler = handlerWeakReference.get();
				if(handler != null)
					handler.onDownloadProgress(progress);
			}
		});
	}

	@Override
	public void onRequestStart() {
		threadHandler.post(new Runnable() {
			@Override
			public void run() {
				if(handlerWeakReference == null)
					return;
				HttpHandler<T> handler = handlerWeakReference.get();
				if(handler != null)
					handler.onRequestStart();
			}
		});
	}

	public void onSuccess(final T message) {
		threadHandler.post(new Runnable() {
			@Override
			public void run() {
				if(handlerWeakReference == null)
					return;
				HttpHandler<T> handler = handlerWeakReference.get();
				if(handler != null){
					try{
						handler.onSuccess(message);
					}catch (ClassCastException e) {
						throw new ClassCastException("An error occurs on handler httprequest result: "+e.getMessage());
					}
				}
			}
		});
	};

	@Override
	public void onUploadProgress(final int progress) {
		threadHandler.post(new Runnable() {
			@Override
			public void run() {
				if(handlerWeakReference == null)
					return;
				HttpHandler<T> handler = handlerWeakReference.get();
				if(handler != null)
					handler.onUploadProgress(progress);
			}
		});
	}

	@Override
	public void onCache(final T message) {
		threadHandler.post(new Runnable() {
			@Override
			public void run() {
				if(handlerWeakReference == null)
					return;
				HttpHandler<T> handler = handlerWeakReference.get();
				if(handler != null){
					try{
						handler.onCache(message);
					}catch (ClassCastException e) {
						throw new ClassCastException("An error occurs on handler httprequest result"+e.getMessage());
					}
				}
			}
		});
	}


}
