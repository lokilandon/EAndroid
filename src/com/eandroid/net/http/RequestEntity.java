/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.net.http;

import java.nio.charset.Charset;
import java.util.Map;

import com.eandroid.net.http.response.ResponseParser;
import com.eandroid.net.http.util.HttpParams;

public interface RequestEntity {		

	public static enum RequestMethod{
		Get,Post
	}
	/**
	 * 获取请求的唯一键
	 * @return
	 */
	public String key();
	
	public String getUrl();
	
	public RequestMethod getRequestMethod();
	/**
	 * 获取http连接
	 * @return
	 */
	public HttpSession getRequestSession();

	/**
	 * 增加头信息
	 * @param name
	 * @param value
	 */
	public void addHeader(String name,String value);
	/**
	 * 获取请求参数
	 * @return
	 */
	public Map<String, ?extends Object> getRequestParams();
	/**
	 * 获取请求的配置参数 如超时连接等
	 * @return
	 */
	public Map<String, String> getHttpParams();

	/**
	 * 是否资源
	 */
	public void release();
	/**
	 * 获取应答的字符编码
	 * @return
	 */
	public Charset getCharset();

	/**
	 * 获取应答的一些参数（如解析字符集编码）
	 */
	public RequestConfig<?> getConfig();

	public interface RequestConfig<T> {

		public boolean isDownloadRequest();

		public String getDownloadPath();

		public Charset getRequestCharset();

		public Class<?> getResponseClass();

		public ResponseParser<T> getResponseParser();

		public void setAttribute(String key,Object name);

		public Object getAttribute(String key);

		public HttpParams getHttpParams();

	}
}
