/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-20
 * @version 0.1
 */
package com.eandroid.net.impl.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.eandroid.net.Connector;
import com.eandroid.net.NetIOFilterChain;
import com.eandroid.net.NetIOHandler;
import com.eandroid.net.http.HttpConnector;
import com.eandroid.net.http.HttpHandler;
import com.eandroid.net.http.HttpSession;
import com.eandroid.net.http.RequestEntity;
import com.eandroid.net.http.RequestEntityGenerator;
import com.eandroid.net.http.ResponseEntity;
import com.eandroid.net.http.SessionClosedException;
import com.eandroid.net.http.response.ResponseParser;
import com.eandroid.net.http.util.HttpParams;
import com.eandroid.net.impl.NetIOHandlerDecorator;
import com.eandroid.util.EALog;


public class HttpRequestSession implements HttpSession,RequestEntityGenerator{
	private final static String TAG = "HttpRequestSession";
	private final NetIOFilterChain filterChain;
	private final HttpConnector httpConnector;
	private NetIOHandler handler;
	private List<RequestEntity> requestList;
	private AtomicBoolean closed = new AtomicBoolean();

	public <T> HttpRequestSession(HttpConnector connector,
			NetIOFilterChain filterChain,
			HttpHandler<T> handler) {
		this.filterChain = filterChain;
		this.httpConnector = connector;
		this.handler = new HttpHandlerAdapter<T>(handler);
		this.requestList = new ArrayList<RequestEntity>();
		try {
			filterChain.sessionCreated(this);
		} catch (Exception e) {
			catchExcepton(e);
		}
	}

	@Override
	public Connector getConnector() {
		return httpConnector;
	}

	@Override
	public NetIOHandler getHandler() {
		return handler;
	}

	public void decoratorHandler(NetIOHandlerDecorator handlerDecorator){
		handlerDecorator.setDecoratedHandler(handler);
		handler = handlerDecorator;
	}

	@Override
	@Deprecated
	public void write(Object message) {
		try {
			get((String)message,null,null,null,null);
		} catch (ClassCastException e) {
			catchExcepton(new ClassCastException("RequestSession only support write string(url)"+e.getMessage()));
		}
	}

	@Override
	public void request(RequestEntity entity) {
		try{
			synchronized (requestList) {
				if(isClosed())
					throw new SessionClosedException("Request session has been closed");
				requestList.add(entity);
			}
			filterChain.write(this, entity);
		}catch (Exception e) {
			removeRequestEntity(entity);
			catchExcepton(e);
		}
	}

	@Override
	public void read(Object message) {
		try {
			resonse((ResponseEntity)message,null);
		} catch (ClassCastException e) {
			catchExcepton(new ClassCastException("RequestSession only support read ResponseEntity"+e.getMessage()));
		}
	}

	@Override
	public void resonse(ResponseEntity responseEntity,RequestEntity requestEntity) {
		try {
			if(isClosed())
				throw new SessionClosedException("Request session has been closed");
			filterChain.read(this, responseEntity);
			removeRequestEntity(requestEntity);
		} catch (Exception e) {
			if(responseEntity != null)
				responseEntity.finish();
			removeRequestEntity(requestEntity);
			catchExcepton(e);
		} 
	}

	@Override
	public void catchExcepton(Exception e) {
		if(isClosed())
			EALog.d(TAG, "An error occured while RequestSession closing"+e.getMessage());
		else{
			filterChain.catchException(this, e);
		}
	}

	private void removeRequestEntity(RequestEntity entity){
		synchronized (requestList) {
			if(entity != null){
				entity.release();
				requestList.remove(entity);
			}
		}
	}

	@Override
	public boolean isClosed() {
		return closed.get();
	}

	@Override
	public void close() {
		if(closed.compareAndSet(false, true)){
			closed.set(true);
			synchronized (requestList) {
				for(RequestEntity entity:requestList){
					entity.release();
				}
			}
			requestList.clear();
		}
	}

	@Override
	public <T> void post(String url,
			String downPath,
			HttpParams params,
			ResponseParser<T> parser,
			Class<T> responseClazz) {
		RequestEntity entity = httpConnector.generatePostEntity(url,
				downPath,
				params,
				this,
				parser,
				responseClazz);
		request(entity);
	}

	@Override
	public <T> void get(String url,
			String downPath,
			HttpParams params,
			ResponseParser<T> parser,
			Class<T> responseClazz) {
		RequestEntity entity = httpConnector.generateGetEntity(url,
				downPath,
				params,
				this,
				parser,
				responseClazz);
		request(entity);
	}

	@Override
	public <T> HttpSession get(String url,
			String downPath,
			HttpParams params,
			ResponseParser<T> parser,
			Class<T> responseClazz,
			HttpHandler<T> handler) {
		HttpRequestSession session = new HttpRequestSession(httpConnector, filterChain, handler);
		RequestEntity entity = httpConnector.generateGetEntity(url,
				downPath,
				params,
				this,
				parser,
				responseClazz);
		session.request(entity);
		return session;
	}

	@Override
	public <T> HttpSession post(String url,
			String downPath,
			HttpParams params,
			ResponseParser<T> parser,
			Class<T> responseClazz,
			HttpHandler<T> handler) {
		HttpRequestSession session = new HttpRequestSession(httpConnector, filterChain, handler);
		RequestEntity entity = httpConnector.generatePostEntity(url,
				downPath,
				params,
				this,
				parser,
				responseClazz);
		session.request(entity);
		return session;
	}

	@Override
	public <T> RequestEntity generatePostEntity(String url, 
			String downloadPath,
			HttpParams params,
			HttpSession session,
			ResponseParser<T> parser,
			Class<T> responseClazz) {
		return httpConnector.generatePostEntity(url,
				downloadPath,
				params,
				this,
				parser,
				responseClazz);
	}

	@Override
	public <T> RequestEntity generateGetEntity(String url, 
			String downloadPath,
			HttpParams params,
			HttpSession session,
			ResponseParser<T> parser,
			Class<T> responseClazz) {
		return httpConnector.generateGetEntity(url,
				downloadPath,
				params,
				this,
				parser,
				responseClazz);
	}

	@Override
	public String generateRequestKey(String url,
			Map<String, ? extends Object> paramMap) {
		return httpConnector.generateRequestKey(url, paramMap);
	}

}
