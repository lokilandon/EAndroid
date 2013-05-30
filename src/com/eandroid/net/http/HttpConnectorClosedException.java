/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-10
 * @version 0.1
 */
package com.eandroid.net.http;

public class HttpConnectorClosedException extends IllegalStateException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1869844765851273786L;

	public HttpConnectorClosedException(String msg,Throwable e){
		super(msg,e);
	}
	
	public HttpConnectorClosedException(String msg){
		super(msg);
	}
	
	public HttpConnectorClosedException(Throwable e){
		super(e);
	}
	
	public HttpConnectorClosedException() {
		super();
	}
}
