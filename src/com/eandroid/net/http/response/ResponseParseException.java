/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-30
 * @version 0.1
 */
package com.eandroid.net.http.response;

import java.io.IOException;

public class ResponseParseException extends IOException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3194810459187165930L;
	
	public ResponseParseException(String msg) {
		super(msg);
	}

}
