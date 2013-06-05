/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 0.1
 */
package com.eandroid.net.http;

import org.apache.http.Header;

import com.eandroid.net.http.RequestEntity.RequestConfig;


public interface ResponseEntity {

	public String getRequestKey();

	public Object getContent();

	public boolean isCache();

	public void setCacheContent(Object cache);

	public void setContent(Object content);

	public Header getContentType();

	public long getContentLength();

	public RequestConfig<?> getConfig();

	public void finish();

	
}
