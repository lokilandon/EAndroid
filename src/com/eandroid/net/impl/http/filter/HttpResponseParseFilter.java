
/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 1.0.0
 */
package com.eandroid.net.impl.http.filter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Observer;

import android.graphics.Bitmap;

import com.eandroid.content.EContext;
import com.eandroid.net.Session;
import com.eandroid.net.http.ResponseEntity;
import com.eandroid.net.http.ResponseEntity.ResponseConfig;
import com.eandroid.net.http.response.BitmapResponseParser;
import com.eandroid.net.http.response.FileResponseParser;
import com.eandroid.net.http.response.ObjectResponseParser;
import com.eandroid.net.http.response.ResponseParseException;
import com.eandroid.net.http.response.ResponseParser;
import com.eandroid.net.http.util.HeadLoader;
import com.eandroid.net.http.util.ReadObserver;
import com.eandroid.net.impl.http.response.DefaultBitmapResponseParser;
import com.eandroid.net.impl.http.response.DefaultFileResponseParser;
import com.eandroid.net.impl.http.response.DefaultObjectResponseParser;
import com.eandroid.util.CommonThreadPool;

public class HttpResponseParseFilter extends BasicHttpFilter{

	private volatile ObjectResponseParser objectParser;
	private volatile FileResponseParser fileParser;
	private volatile BitmapResponseParser bitmapParser;
	private volatile Charset defaultCharset;

	public HttpResponseParseFilter(){
		this(null, null, null, Charset.forName("utf-8"));
	}

	public HttpResponseParseFilter(ObjectResponseParser parser,
			boolean cacheBitmap,
			boolean cacheOtherObject){
		this(parser, null, null,Charset.forName("utf-8"));
	}

	public HttpResponseParseFilter(ObjectResponseParser parser,
			boolean cacheBitmap,
			boolean cacheOtherObject,
			Charset defaultCharset){
		this(parser, null, null,defaultCharset);
	}

	public HttpResponseParseFilter(ObjectResponseParser parser,
			boolean cacheBitmap){
		this(parser, null, null,Charset.forName("utf-8"));
	}

	public HttpResponseParseFilter(ObjectResponseParser parser,
			boolean cacheBitmap,
			Charset defaultCharset){
		this(parser, null, null,defaultCharset);
	}

	public HttpResponseParseFilter(ObjectResponseParser parser,
			FileResponseParser fileParser,
			BitmapResponseParser bitmapParser,
			Charset charset){
		if(objectParser == null)
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
			
			Charset charset = entity.getConfig().getCharset();
			charset = charset==null?HeadLoader.loadContentTypeCharset(entity.getContentType()):charset;
			charset = charset==null?defaultCharset:charset;
			Observer observer = new ReadObserver(session, session.getHandler(), entity.getContentLength());
			
			
			if(entity.getConfig().getResponseParser() != null){
				@SuppressWarnings("rawtypes")
				ResponseParser parser = entity.getConfig().getResponseParser();
				@SuppressWarnings("rawtypes")
				ResponseConfig config = entity.getConfig();
				parsedObject = parser.parseObject(config, in,charset,observer);
			}
			if(resultClass == File.class){	
				if(parsedObject == null)
					parsedObject = fileParser.parseObject((ResponseConfig<File>)entity.getConfig(),in, null,observer);
			}else if(resultClass == Bitmap.class){
				if(parsedObject == null)
					parsedObject = bitmapParser.parseObject((ResponseConfig<Bitmap>)entity.getConfig(),in, null,observer);
			}else if(parsedObject == null){
				@SuppressWarnings("rawtypes")
				ResponseConfig config = (ResponseConfig<Object>)entity.getConfig();
				parsedObject = objectParser.parseObject(config, in,charset,observer);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
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
