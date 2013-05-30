/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-9
 * @version 1.0.0
 */
package com.eandroid.ioc;

public class AutowireException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5540488829751130949L;
	
	public AutowireException(String msg){
		super(msg);
	}
	
	public AutowireException(String msg,Throwable e){
		super(msg,e);
	}
	
	public AutowireException(Throwable e){
		super(e);
	}

}
