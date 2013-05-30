/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-27
 * @version 1.0.0
 */
package com.eandroid.net.http.httpclient.entity.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Observer;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class ObservedHttpEntity implements HttpEntity{
	
	private HttpEntity entity;
	private Observer observer;
	public ObservedHttpEntity(HttpEntity entity,Observer observer){
		this.entity = entity;
		this.observer = observer;
	}

	@Override
	public void consumeContent() throws IOException {
		entity.consumeContent();
	}

	@Override
	public InputStream getContent() throws IOException, IllegalStateException {
		return entity.getContent();
	}

	@Override
	public Header getContentEncoding() {
		return entity.getContentEncoding();
	}

	@Override
	public long getContentLength() {
		return entity.getContentLength();
	}

	@Override
	public Header getContentType() {
		return entity.getContentType();
	}

	@Override
	public boolean isChunked() {
		return entity.isChunked();
	}

	@Override
	public boolean isRepeatable() {
		return entity.isRepeatable();
	}

	@Override
	public boolean isStreaming() {
		return entity.isStreaming();
	}

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		entity.writeTo(new ObservedOutputStream(outstream,observer,getContentLength()));
	}

	public class ObservedOutputStream extends OutputStream{
		private OutputStream outputStream;
		private Observer observer;
		private long currentLength = 0;
		private ObservedOutputStream(OutputStream outputStream,Observer observer,long totalLength){
			this.outputStream = outputStream;
			this.observer = observer;
		}
		@Override
		public void write(byte[] buffer) throws IOException {
			outputStream.write(buffer);
			currentLength += buffer.length;
			observer.update(null, currentLength);
		}
		@Override
		public void write(byte[] buffer, int offset, int count)
				throws IOException {
			outputStream.write(buffer, offset, count);
			currentLength += count;
			observer.update(null, currentLength);
		}
		@Override
		public void write(int oneByte) throws IOException {
			outputStream.write(oneByte);
			currentLength ++;
			observer.update(null, currentLength);
		}
		
	}

}
