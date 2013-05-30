/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 0.1
 */
package com.eandroid.net.http;

import java.nio.charset.Charset;

import org.apache.http.Header;

import com.eandroid.net.http.response.ResponseParser;


public interface ResponseEntity {

	public String getRequestKey();

	public Object getContent();

	public boolean isCache();

	public void setCacheContent(Object cache);

	public void setContent(Object content);

	public Header getContentType();

	public long getContentLength();

	public ResponseConfig<?> getConfig();

	public void finish();

	public interface ResponseConfig<T> {

		public boolean isDownloadResponse();

		public String getDownloadPath();

		public Charset getCharset();

		public Class<T> getResponseClass();

		public ResponseParser<T> getResponseParser();

		public void setAttribute(String key,Object name);

		public Object getAttribute(String key);

		public boolean isResponseCache();

		public boolean isContinueOnCacheHit();

		public long cacheExpiredTime();

		public boolean hasConfigResponseCache();

		public boolean hasConfigContinueOnCacheHit();

		public boolean hasConfigCacheExpiredTime();
	}
}
