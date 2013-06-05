
/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 0.1
 */
package com.eandroid.net.impl.http.filter;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Observer;

import android.graphics.Bitmap;

import com.eandroid.content.EContext;
import com.eandroid.net.Session;
import com.eandroid.net.http.RequestEntity.RequestConfig;
import com.eandroid.net.http.ResponseEntity;
import com.eandroid.net.http.response.BitmapResponseParser;
import com.eandroid.net.http.response.FileResponseParser;
import com.eandroid.net.http.response.ObjectResponseParser;
import com.eandroid.net.http.response.ResponseParseException;
import com.eandroid.net.http.response.ResponseParser;
import com.eandroid.net.http.util.HeadLoader;
import com.eandroid.net.http.util.HttpParams;
import com.eandroid.net.http.util.ReadObserver;
import com.eandroid.net.impl.http.response.DefaultBitmapResponseParser;
import com.eandroid.net.impl.http.response.DefaultFileResponseParser;
import com.eandroid.net.impl.http.response.DefaultObjectResponseParser;
import com.eandroid.util.CommonThreadPool;

public class HttpResponseParseFilter extends BasicHttpFilter{

	private volatile ResponseParser<Object> objectParser;
	private volatile FileResponseParser fileParser;
	private volatile BitmapResponseParser bitmapParser;
	private volatile Charset defaultCharset;

	public HttpResponseParseFilter(){
		this(null, null, null, Charset.forName("utf-8"));
	}

	public HttpResponseParseFilter(ResponseParser<Object> parser,
			boolean cacheBitmap,
			boolean cacheOtherObject){
		this(parser, null, null,Charset.forName("utf-8"));
	}

	public HttpResponseParseFilter(ResponseParser<Object> parser,
			boolean cacheBitmap,
			boolean cacheOtherObject,
			Charset defaultCharset){
		this(parser, null, null,defaultCharset);
	}

	public HttpResponseParseFilter(ResponseParser<Object> parser,
			boolean cacheBitmap){
		this(parser, null, null,Charset.forName("utf-8"));
	}

	public HttpResponseParseFilter(ResponseParser<Object> parser,
			boolean cacheBitmap,
			Charset defaultCharset){
		this(parser, null, null,defaultCharset);
	}

	public HttpResponseParseFilter(ResponseParser<Object> parser,
			FileResponseParser fileParser,
			BitmapResponseParser bitmapParser,
			Charset charset){
		if(parser == null)
			objectParser = new DefaultObjectResponseParser();
		else
			this.objectParser = parser;
		if(fileParser == null){
			this.fileParser = new DefaultFileResponseParser();
		}else
			this.fileParser = fileParser;
		if(bitmapParser == null)
			this.bitmapParser = new DefaultBitmapResponseParser();
		else
			this.bitmapParser = bitmapParser;
		this.defaultCharset = charset;
		CommonThreadPool.execute(new CleanTempFileTask());
	}

	@SuppressWarnings("unchecked")
	public void onRead(NextFilterSelector next, Session session, ResponseEntity entity){
		Class<?> resultClass = entity.getConfig().getResponseClass();
		Object responseContent = entity.getContent();
		if((resultClass != null && resultClass.isAssignableFrom(ResponseEntity.class))
				|| (responseContent != null && !(responseContent instanceof InputStream))){
			super.onRead(next, session, entity);
			return;
		}

		Object parsedObject = null;
		try {
			InputStream in = (InputStream)responseContent;
			if(resultClass == null)
				throw new ResponseParseException("Response parse error:unknow parse type!");

			Charset charset = entity.getConfig().getRequestCharset();
			charset = charset==null?HeadLoader.loadContentTypeCharset(entity.getContentType()):charset;
			charset = charset==null?defaultCharset:charset;
			Observer observer = null;
			HttpParams httpParams= entity.getConfig().getHttpParams();
			if(httpParams != null && httpParams.isListenDownload()){
				observer = new ReadObserver(session, session.getHandler(), entity.getContentLength());
			}

			if(entity.getConfig().getResponseParser() != null){
				@SuppressWarnings("rawtypes")
				ResponseParser parser = entity.getConfig().getResponseParser();
				@SuppressWarnings("rawtypes")
				RequestConfig config = entity.getConfig();
				parsedObject = parser.parseObject(config, in,charset,observer);
			}else if(resultClass == File.class){	
				if(parsedObject == null)
					parsedObject = fileParser.parseObject((RequestConfig<File>)entity.getConfig(),in, null,observer);
			}else if(resultClass == Bitmap.class){
				if(parsedObject == null)
					parsedObject = bitmapParser.parseObject((RequestConfig<Bitmap>)entity.getConfig(),in, null,observer);
			}else if(parsedObject == null){
				@SuppressWarnings("rawtypes")
				RequestConfig config = (RequestConfig<Object>)entity.getConfig();
				parsedObject = objectParser.parseObject(config, in,charset,observer);
			}
		} finally{
			entity.finish();
		}
		entity.setContent(parsedObject);
		super.onRead(next, session, entity);
	};

	public void setObjectParser(ObjectResponseParser objectParser) {
		this.objectParser = objectParser;
	}

	public void setFileParser(FileResponseParser fileParser) {
		this.fileParser = fileParser;
	}

	public void setBitmapParser(BitmapResponseParser bitmapParser) {
		this.bitmapParser = bitmapParser;
	}

	public void setDefaultCharset(Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	public class CleanTempFileTask implements Runnable {
		public void run() {
			File file = new File(EContext.getTempFilePath());
			if(file.exists()){
				File[] listFiles = file.listFiles();
				for(File f : listFiles){
					f.delete();
				}
			}
		}
	}
}
