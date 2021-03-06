/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-9
 * @version 0.1
 */
package com.eandroid.net.http.util;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.eandroid.net.http.httpclient.entity.mime.content.ByteArrayBody;
import com.eandroid.net.http.httpclient.entity.mime.content.FileBody;
import com.eandroid.net.http.httpclient.entity.mime.content.InputStreamBody;
import com.eandroid.net.http.httpclient.entity.mime.content.StringBody;

public class HttpParams {

	private Map<String,Object> params;
	private Map<String,Object> config;

	public HttpParams() {
		params = new HashMap<String, Object>();
		config = new HashMap<String, Object>();
	}

	public HttpParams(Object... kv){
		this();
		int len = kv.length;
		if (len % 2 != 0)
			throw new IllegalArgumentException("arguments must be even");
		for (int i = 0; i < len; i += 2) {
			String key = String.valueOf(kv[i]);
			Object val = kv[i + 1];
			put(key, val);
		}
	}

	public HttpParams put(String key, Object value){
		if(key != null && value != null) {
			params.put(key, value);
		}
		return this;
	}

	public HttpParams putConfig(String key, Object value){
		if(key != null && value != null) {
			config.put(key, value);
		}
		return this;
	}

	public HttpParams putAll(Map<String,? extends Object> map){
		if(map != null && !map.isEmpty()) {
			Set<String> keySet = map.keySet();
			Iterator<String> itKey = keySet.iterator();
			while(itKey.hasNext()){
				String key = itKey.next();
				Object value = map.get(key);
				if(key != null && value != null)
					put(key, value);
			}
		}
		return this;
	}

	public HttpParams put(String key, File file,String fileName){
		put(key, new FileBody(file, fileName));
		return this;
	}

	public HttpParams put(String key, File file,String fileName,String mimeType,String charset){
		put(key, new FileBody(file, fileName, mimeType, charset));
		return this;
	}

	public HttpParams put(String key, InputStream in,String name){
		put(key, new InputStreamBody(in,name));
		return this;
	}

	public HttpParams put(String key, InputStream in,String mimeType,String name){
		put(key, new InputStreamBody(in, mimeType,name));
		return this;
	}

	public HttpParams put(String key, byte[] byteArray,String fileName){
		put(key, new ByteArrayBody(byteArray,fileName));
		return this;
	}

	public HttpParams put(String key, byte[] byteArray,String fileName,String mimeType){
		put(key, new ByteArrayBody(byteArray,mimeType,fileName));
		return this;
	}

	public HttpParams put(String key, String value){
		put(key, value);
		return this;
	}

	public HttpParams put(String key, String value,Charset charset,String mimeType) throws UnsupportedEncodingException{
		put(key, new StringBody(value, mimeType, charset));
		return this;
	}

	public Map<String,Object> getReqParam(){
		return params;
	}

	public Map<String, Object> getConfigParam(){
		return config;
	}

	public HttpParams configHead(Map<String,String> headers){
		putConfig("header", headers);
		return this;
	}

	public HttpParams configHead(String... kv){
		int len = kv.length;
		if (len % 2 != 0)
			throw new IllegalArgumentException("arguments must be even");
		Map<String,String> headers = new HashMap<String, String>();
		for (int i = 0; i < len; i += 2) {
			String key = kv[i];
			String val = kv[i + 1];
			headers.put(key, val);
		}
		putConfig("header", headers);
		return this;
	}

	public HttpParams configRequestCharet(Charset charset){
		putConfig("charset", charset);
		return this;
	}

	public HttpParams configResponseCharet(Charset charset){
		putConfig("resCharset", charset);
		return this;
	}

	public HttpParams configResponseCache(boolean cache){
		putConfig("cache", cache);
		return this;
	}

	public HttpParams configContinueOnCacheHit(boolean continueOnCacheHit){
		putConfig("continueOnCacheHit", continueOnCacheHit);
		return this;
	}

	public HttpParams configCacheExpired(long cacheExpiredTIme){
		putConfig("cacheExpiredTIme", cacheExpiredTIme);
		return this;
	}
	
	public HttpParams configListenUpload(boolean listen){
		putConfig("listenUpload", listen);
		return this;
	}
	
	/**
	 * 设置是否监听下载进度
	 * 需同时设置头信息Accept-Encoding为identity
	 * 
	 * 若http请求，返回应答的头信息，Content-Length为-1时不能监听
	 * 当返回应答的Content-Encoding不为identity（如gzip、compress），或者Transfer-Encoding为chunked时<br/>
	 * Content-Length始终为-1。
	 * 
	 * @param listen
	 * @return
	 */
	public HttpParams configListenDownload(boolean listen){
		putConfig("listenDownload", listen);
		return this;
	}

	@SuppressWarnings("unchecked")
	public Map<String,String> getHeaders(){
		return config.get("header")== null ? null : (Map<String,String>)config.get("header");
	}

	public Charset getRequestCharset(){
		return config.get("charset")== null ? null : (Charset)config.get("charset");
	}

	public Charset getResponseCharet(){
		return config.get("resCharset") == null ? null : (Charset)config.get("resCharset");
	}

	public boolean hasConfigResponseCache(){
		return config.get("cache") == null ? false:true;
	}

	public boolean hasConfigContinueOnCacheHit(){
		return config.get("continueOnCacheHit") == null ? false:true;
	}

	public boolean hasConfigCacheExpired(){
		return config.get("cacheExpiredTIme") == null ? false:true;
	}

	public boolean isResponseCache(){
		return config.get("cache") == null ? true:(Boolean)config.get("cache");
	}

	public boolean isContinueOnCacheHit(){
		return config.get("continueOnCacheHit") == null ? true:(Boolean)config.get("continueOnCacheHit");
	}

	public long getCacheExpired(){
		return config.get("cacheExpiredTIme") == null ? 0 : (Long)config.get("cacheExpiredTIme");
	}
	
	public boolean isListenDownload(){
		return config.get("listenDownload") == null?false:(Boolean)config.get("listenDownload");
	}
	
	public boolean isListenUpload(){
		return config.get("listenUpload") == null?false:(Boolean)config.get("listenUpload");
	}

}
