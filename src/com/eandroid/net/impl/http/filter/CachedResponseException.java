/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-6-14
 * @version 0.1
 */
package com.eandroid.net.impl.http.filter;

import com.eandroid.net.http.HttpResponseException;

public class CachedResponseException extends HttpResponseException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5357507393754114466L;

	public CachedResponseException(Throwable e) {
		super(e);
	}
	
	public CachedResponseException(String msg,Throwable e) {
		super(msg,e);
	}
	
	public CachedResponseException(String msg) {
		super(msg);
	}
}
