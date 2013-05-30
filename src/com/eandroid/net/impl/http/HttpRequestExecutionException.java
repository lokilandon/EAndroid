/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-1
 * @version 0.1
 */
package com.eandroid.net.impl.http;

import java.util.concurrent.ExecutionException;

public class HttpRequestExecutionException extends ExecutionException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4956903539523379202L;

	public HttpRequestExecutionException(Throwable e) {
		super(e);
	}
	
	public HttpRequestExecutionException(String msg,Throwable e) {
		super(msg,e);
	}
}
