/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 1.0.0
 */
package com.eandroid.net.http.httpclient.entity.mime;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

public class GZipEntity extends HttpEntityWrapper{

	public GZipEntity(HttpEntity wrapped) {
		super(wrapped);
	}

	@Override
	public InputStream getContent() throws IOException {
		return new GZIPInputStream(super.getContent());
	}
	
	@Override
	public long getContentLength() {
		return -1;
	}
}
