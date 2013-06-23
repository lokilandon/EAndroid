/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-1
 * @version 0.1
 */
package com.eandroid.net.http;


public class HttpResponseException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4956903539523379202L;

	public HttpResponseException(String msg) {
		super(msg);
	}

	public HttpResponseException(Throwable e) {
		super(e);
	}

	public HttpResponseException(String msg,Throwable e) {
		super(msg,e);
	}
}
