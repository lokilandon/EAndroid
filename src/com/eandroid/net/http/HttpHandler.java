/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-21
 * @version 1.0.0
 */
package com.eandroid.net.http;






public interface HttpHandler<T> {
	
	public void onRequestStart();
	public void onSuccess(T message);
	public void onCache(T message);
	public void onUploadProgress(int progress);
	public void onDownloadProgress(int progress);
	/**
	 * 	ClientProtocolException, HttpRequestException ,IOException ,ResponseParserException
	 *  SessionClosedException,HttpConnectorClosedException
	 * @param exception
	 */
	public void onCatchException(Exception exception);
	
}
