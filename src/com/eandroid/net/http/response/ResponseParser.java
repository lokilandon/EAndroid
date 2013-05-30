/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-1
 * @version 1.0.0
 */
package com.eandroid.net.http.response;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Observer;

import com.eandroid.net.http.ResponseEntity.ResponseConfig;


public interface ResponseParser<T> {

	public T parseObject(ResponseConfig<T> config,
			InputStream in,
			Charset defauCharset,
			Observer readObserver) throws ResponseParseException;

}
