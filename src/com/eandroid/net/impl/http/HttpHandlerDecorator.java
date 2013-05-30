/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 1.0.0
 */
package com.eandroid.net.impl.http;

import com.eandroid.net.http.HttpHandler;

public abstract class HttpHandlerDecorator<T> implements HttpHandler<T>{

	protected HttpHandler<T> decoratedHandler;

	public HttpHandlerDecorator(HttpHandler<T> handler) {
		this.decoratedHandler = handler;
	}

	@Override
	public void onRequestStart() {
		if(decoratedHandler != null)
			decoratedHandler.onRequestStart();
	}

	@Override
	public void onSuccess(T message) {
		if(decoratedHandler != null)
			decoratedHandler.onSuccess(message);
	}

	@Override
	public void onUploadProgress(int progress) {
		if(decoratedHandler != null)
			decoratedHandler.onUploadProgress(progress);
	}

	@Override
	public void onDownloadProgress(int progress) {
		if(decoratedHandler != null)
			decoratedHandler.onDownloadProgress(progress);
	}

	@Override
	public void onCatchException(Exception exception) {
		if(decoratedHandler != null)
			decoratedHandler.onCatchException(exception);
	}

}
