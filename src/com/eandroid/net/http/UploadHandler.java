/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 0.1
 */
package com.eandroid.net.http;




public abstract class UploadHandler<T> implements HttpHandler<T>{	
	public void onDownloadProgress(int progress){}
}
