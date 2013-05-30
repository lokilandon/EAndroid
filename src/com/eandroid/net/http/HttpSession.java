/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-26
 * @version 0.1
 */
package com.eandroid.net.http;

import com.eandroid.net.Session;
import com.eandroid.net.http.response.ResponseParser;
import com.eandroid.net.http.util.HttpParams;

public interface HttpSession extends Session{
	
	public void resonse(ResponseEntity responseEntity,RequestEntity requestEntity);
	
	public void request(RequestEntity entity);

	public <T> void post(String url,
			String downPath,
			HttpParams params,
			ResponseParser<T> parser,
			Class<T> responseClazz);
	
	
	
	public <T> void get(String url,
			String downPath,
			HttpParams params,
			ResponseParser<T> parser,
			Class<T> responseClazz);
	
	public <T> HttpSession get(String url,
			String downPath,
			HttpParams params,
			ResponseParser<T> parser,
			Class<T> responseClazz,
			HttpHandler<T> handler);
	
	public <T> HttpSession post(String url,
			String downPath,
			HttpParams params,
			ResponseParser<T> parser,
			Class<T> responseClazz,
			HttpHandler<T> handler);
	
	
}
