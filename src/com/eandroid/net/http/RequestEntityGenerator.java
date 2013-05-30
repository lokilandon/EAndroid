/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-23
 * @version 0.1
 */
package com.eandroid.net.http;

import java.util.Map;

import com.eandroid.net.http.response.ResponseParser;
import com.eandroid.net.http.util.HttpParams;


public interface RequestEntityGenerator {
	
	public <T> RequestEntity generatePostEntity(String url,
			String downloadPath,
			HttpParams params,
			HttpSession session,
			ResponseParser<T> parser,
			Class<T> responseClass);
	
	
	public <T> RequestEntity generateGetEntity(String url,
			String downloadPath,
			HttpParams params,
			HttpSession session,
			ResponseParser<T> parser,
			Class<T> responseClass);
	
	public String generateRequestKey(String url,
			Map<String, ? extends Object> paramMap);
 
}
