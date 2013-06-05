/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-30
 * @version 0.1
 */
package com.eandroid.net.http.util;

import com.eandroid.net.NetIOHandler;
import com.eandroid.net.Session;
import com.eandroid.net.http.HttpSession;
import com.eandroid.net.http.RequestEntity;
import com.eandroid.net.http.ResponseEntity;
import com.eandroid.net.impl.NetIOHandlerDecorator;
import com.eandroid.net.impl.http.HttpRequestExecutionException;
import com.eandroid.net.impl.http.HttpRequestSession;

public class HttpSyncHandler <Result>{

	private volatile HttpRequestSession session;
	private Object outcome;
    public HttpSession getSession() {    	
		return session;
	}
	public HttpSyncHandler(HttpRequestSession session){
		this.session = session;
		if(session == null)
			throw new IllegalArgumentException("An error occured while HttpAsyncTask init requestSession or reqUrl must not be null!");
		session.decoratorHandler(new HttpSyncTaskReponseHandler(null));
		outcome = null;
	}
	@SuppressWarnings("unchecked")
	public  Result request(RequestEntity requestEntity) throws HttpRequestExecutionException{
		session.request(requestEntity);
		if(outcome == null)
			return null;
		else if(outcome instanceof Exception)
			throw new HttpRequestExecutionException(((Exception)outcome));
		else {
			return (Result)outcome;
		}
    }
	
	private class HttpSyncTaskReponseHandler extends NetIOHandlerDecorator {
    	public HttpSyncTaskReponseHandler(NetIOHandler handler) {
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
}
