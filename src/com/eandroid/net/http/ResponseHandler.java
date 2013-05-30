/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.net.http;



public abstract class ResponseHandler<T> implements HttpHandler<T>{
	
	@Override
	public void onUploadProgress(int progress) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDownloadProgress(int progress) {
		// TODO Auto-generated method stub
		
	}
	
	public void onCache(T message) {};
	
}
