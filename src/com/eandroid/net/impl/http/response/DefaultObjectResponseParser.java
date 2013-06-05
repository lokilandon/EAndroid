/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-3
 * @version 0.1
 */
package com.eandroid.net.impl.http.response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Observer;

import com.eandroid.net.http.RequestEntity.RequestConfig;
import com.eandroid.net.http.response.ObjectResponseParser;
import com.eandroid.net.http.response.ResponseParseException;
import com.eandroid.util.IOUtils;

public class DefaultObjectResponseParser implements ObjectResponseParser{

	@Override
	public Object parseObject(RequestConfig<Object> config, InputStream in,Charset defauCharset,
			Observer readObserver)
					throws ResponseParseException {
		Class<?> clazz = config.getResponseClass();
		if(String.class != clazz)
			throw new ResponseParseException("Default object response parser only support parse String.class");
		Charset charset = config.getRequestCharset() == null ? defauCharset:config.getRequestCharset();
		try {
			return IOUtils.inputStream2String(in, charset, readObserver);
		} catch (IOException e) {
			throw new ResponseParseException("An error occured while parse object:"+e.getClass().getName()+" "+e.getMessage());
		}
	}

}
