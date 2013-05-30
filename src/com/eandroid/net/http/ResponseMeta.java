/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-2
 * @version 1.0.0
 */
package com.eandroid.net.http;

import java.nio.charset.Charset;

import com.eandroid.net.http.response.ResponseParser;

public interface ResponseMeta<T> {

	public boolean isDownloadResponse();

	public String getDownloadPath();

	public Charset getCharset();

	public Class<T> getResponseClass();

	public ResponseParser<T> getResponseParser();
	
	public void setAttribute(String key,Object name);
	
	public Object getAttribute(String key);
}
