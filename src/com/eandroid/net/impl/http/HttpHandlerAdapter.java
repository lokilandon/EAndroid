/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-21
 * @version 0.1
 */
package com.eandroid.net.impl.http;

import com.eandroid.net.NetIOHandler;
import com.eandroid.net.Session;
import com.eandroid.net.http.HttpHandler;
import com.eandroid.net.http.ResponseEntity;

public class HttpHandlerAdapter<T> implements NetIOHandler{
	private HttpHandler<T> handler;
	public HttpHandlerAdapter(HttpHandler<T> httpHandler) {
		this.handler = httpHandler;
	}

	@Override
	public void onSessionCreated(Session session) {
		if(handler != null)
			handler.onRequestStart();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onRead(Session session, Object message) {
		ResponseEntity entity = (ResponseEntity)message;
		try{
			if(handler != null){
				T msg = (T)entity.getContent();
				if(entity.isCache())
					handler.onCache(msg);
				else
					handler.onSuccess(msg);
			}
		}catch (ClassCastException e) {
			throw new ClassCastException("An error occurs on handler httprequest result"+e.getMessage());
		}
	}

	@Override
	public void onReadProgress(Session session, int progress) {
		if(handler != null)
			handler.onDownloadProgress(progress);
	}

	@Override
	public void onWriteProgress(Session session, int progress) {
		if(handler != null)
			handler.onUploadProgress(progress);
	}

	@Override
	public void onCatchException(Session session, Exception exception) {
		if(handler != null)
			handler.onCatchException(exception);
	}


}
