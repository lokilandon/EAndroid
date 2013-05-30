/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-14
 * @version 1.0.0
 */
package com.eandroid.cache.impl;

import java.io.IOException;

public class NotEnoughSpaceException extends IOException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4051926687051855891L;

	public NotEnoughSpaceException(String msg) {
		super(msg);
	}
	
}
