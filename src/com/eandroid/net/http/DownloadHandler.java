/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 1.0.0
 */
package com.eandroid.net.http;




public abstract class DownloadHandler<T> implements HttpHandler<T>{
	public void onUploadProgress(int progress){}
}
