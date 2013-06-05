/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-6-4
 * @version 0.1
 */
package com.eandroid.net.impl.http;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.eandroid.net.http.RequestEntity.RequestConfig;
import com.eandroid.net.http.response.ResponseParser;
import com.eandroid.net.http.util.HttpParams;
import com.eandroid.util.StringUtils;

public class RequestConfigImpl<T> implements RequestConfig<T> {

	private final String downPath;
	private final Class<?> responseClass;
	private final ResponseParser<T> parser;
	private HttpParams httpParams;
	private Map<String, Object> attrs;


	public RequestConfigImpl(String downloadPath,Class<?> responseClass,ResponseParser<T> parser,HttpParams params){
		this.downPath = downloadPath;
		this.httpParams = params;
		this.responseClass = responseClass;
		this.parser = parser;

	}

	@Override
	public Charset getRequestCharset() {
		if(httpParams != null)
			return httpParams.getRequestCharset();
		else 
			return null;
	}
	@Override
	public Class<?> getResponseClass() {
		return responseClass;
	}

	@Override
	public ResponseParser<T> getResponseParser() {
		return parser;
	}

	@Override
	public void setAttribute(String key, Object name) {
		if(attrs == null)
			attrs = new HashMap<String, Object>();
		attrs.put(key, name);
	}
	
	@Override
	public Object getAttribute(String key) {
		if(attrs == null)
			return null;
		return attrs.get(key);
	}

	@Override
	public boolean isDownloadRequest() {
		return StringUtils.isNotEmpty(downPath);
	}

	@Override
	public String getDownloadPath() {
		return downPath;
	}

	@Override
	public HttpParams getHttpParams() {
		return httpParams;
	}

}
