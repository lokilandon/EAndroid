/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-1
 * @version 1.0.0
 */
package com.eandroid.net.http;

import java.io.IOException;

public class HttpRequestException extends IOException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4956903539523379202L;

	public HttpRequestException(Throwable e) {
		super(e.getMessage());
	}
}
