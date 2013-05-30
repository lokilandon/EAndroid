/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-20
 * @version 1.0.0
 */
package com.eandroid.net.impl.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import com.eandroid.net.http.HttpConnector;
import com.eandroid.net.http.HttpConnectorClosedException;
import com.eandroid.net.http.HttpRequestException;
import com.eandroid.net.http.HttpSession;
import com.eandroid.net.http.RequestEntity;
import com.eandroid.net.http.ResponseEntity;
import com.eandroid.net.http.SessionClosedException;
import com.eandroid.net.http.ResponseEntity.ResponseConfig;
import com.eandroid.net.http.httpclient.entity.mime.GZipEntity;
import com.eandroid.net.http.httpclient.entity.mime.content.StringBody;
import com.eandroid.net.http.response.ResponseParser;
import com.eandroid.net.http.util.HeadLoader;
import com.eandroid.net.http.util.HttpParams;
import com.eandroid.net.http.util.HttpRequestParamsWriter;
import com.eandroid.net.http.util.WriteObserver;
import com.eandroid.util.EncoderUtils;
import com.eandroid.util.StringUtils;

public class HttpClientConnector implements HttpConnector{
	private DefaultHttpClient client;
	private HttpContext context;

	private static final String HEAD_ACCEPT_ENCODING = "Accept-Encoding";
	public static final String ACCEPT_ENCODING_GZIP = "gzip";
	public String ACCEPT_ENCODING = ACCEPT_ENCODING_GZIP;

	public static int connectionTimeout = 20;
	public static int socketTimeout = 15;

	public static boolean tcpNoDelay = false;
	public static boolean staleCheck = true;
	public static int linger = 0;
	public static int socketBufferSize = 8*1024;
	public static Charset contentCharset = Charset.forName("utf-8");
	public static Charset httpElementCharset = Charset.forName("utf-8");
	public static String userAgent = "";
	public static int maxTotalConnections = 20;
	public static HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler();
	public AtomicBoolean closed = new AtomicBoolean();

	public HttpClientConnector(){
		this(null);
	}

	public HttpClientConnector(HttpContext context){
		org.apache.http.params.HttpParams httpParams = new BasicHttpParams();		

		HttpProtocolParams.setContentCharset(httpParams, contentCharset.displayName());//设置请求内容字符集编码
		HttpProtocolParams.setHttpElementCharset(httpParams, httpElementCharset.displayName());//设置http头信息的字符集编码
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);//设置http协议版本
		HttpProtocolParams.setUseExpectContinue(httpParams, false);//设置是否使用握手协议（每次请求时，先发送请求询问服务器是否接受该请求）
		HttpProtocolParams.setUserAgent(httpParams, userAgent);//设置user-agent头信息

		HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout * 1000);//设置连接超时时间
		HttpConnectionParams.setSoTimeout(httpParams, socketTimeout  * 1000);//设置socket连接后，等待的超时时间
		HttpConnectionParams.setTcpNoDelay(httpParams, tcpNoDelay);//设置是否使用 Nagle 算法。Nagle 算法视图通过最小化发送的分组数 量来节省带宽。为减少延时增加性能时可设置为true
		HttpConnectionParams.setLinger(httpParams, linger);//设置socket关闭后，当输出缓冲区中还有内容时，是强制退出，还是等待一段时间看是否能够发送完毕。
		HttpConnectionParams.setSocketBufferSize(httpParams, socketBufferSize);//设置socket缓冲区大小。当传输大文件时应该增大。
		HttpConnectionParams.setStaleCheckingEnabled(httpParams, staleCheck);//设置是否检查连接的有效性，设置true的话，每次连接都会花30ms的时间去测试连接有效性，设置false会导致，有可能在服务器端已关闭的连接上读取数据并导致SocketException，默认的retryhandler不会对该异常进行重试操作。

		ConnManagerParams.setMaxTotalConnections(httpParams, maxTotalConnections);//设置最大的连接数
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParams, schemeRegistry);//设置线程安全的Connection 管理。

		client = new DefaultHttpClient(cm, httpParams);
		client.setHttpRequestRetryHandler(retryHandler);//设置重试规则处理

		if(context == null){
			context = new BasicHttpContext();
			CookieStore cookieStore = new BasicCookieStore();
			context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);//设置cookie
		}else {
			this.context = context;
		}

		client.addRequestInterceptor(new HttpRequestInterceptor() {
			@Override
			public void process(HttpRequest request, HttpContext context)
					throws HttpException, IOException {
				Header[] headers = request.getHeaders(HEAD_ACCEPT_ENCODING);
				if(headers == null || headers.length == 0)
					request.addHeader(HEAD_ACCEPT_ENCODING, ACCEPT_ENCODING);
			}
		});
		client.addResponseInterceptor(new HttpResponseInterceptor() {
			@Override
			public void process(HttpResponse response, HttpContext context)
					throws HttpException, IOException {
				Header header = response.getEntity().getContentEncoding();
				if(header == null)
					return;
				boolean isGZipEncoding = HeadLoader.isContentEncoding(header,
						ACCEPT_ENCODING_GZIP);
				if(isGZipEncoding)
					response.setEntity(new GZipEntity(response.getEntity()));
			}
		});
	}

	public void configAcceptEncoding(String acceptEncoding){
		ACCEPT_ENCODING = acceptEncoding;
	}

	public void configTimeoutInSeconds(int connectionTimeout,int socketTimeout){
		org.apache.http.params.HttpParams httpParams = client.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout * 1000);//设置连接超时时间
		HttpConnectionParams.setSoTimeout(httpParams, socketTimeout  * 1000);//设置socket连接后，等待的超时时间
	}

	public void configTCPNoDelay(boolean tcpNoDelay){
		org.apache.http.params.HttpParams httpParams = client.getParams();
		HttpConnectionParams.setTcpNoDelay(httpParams, tcpNoDelay);//设置是否使用 Nagle 算法。Nagle 算法视图通过最小化发送的分组数 量来节省带宽。为减少延时增加性能时可设置为true
	}

	public void configStaleCheck(boolean staleCheck){
		org.apache.http.params.HttpParams httpParams = client.getParams();
		HttpConnectionParams.setStaleCheckingEnabled(httpParams, staleCheck);//设置是否检查连接的有效性，设置true的话，每次连接都会花30ms的时间去测试连接有效性，设置false会导致，有可能在服务器端已关闭的连接上读取数据并导致SocketException，默认的retryhandler不会对该异常进行重试操作。
	}

	public void configContentCharset(Charset charset){
		org.apache.http.params.HttpParams httpParams = client.getParams();
		HttpProtocolParams.setContentCharset(httpParams, contentCharset.displayName());//设置请求内容字符集编码
	}

	public void configHeadCharset(Charset charset){
		org.apache.http.params.HttpParams httpParams = client.getParams();
		HttpProtocolParams.setHttpElementCharset(httpParams, httpElementCharset.displayName());//设置http头信息的字符集编码
	}

	public void configUserAgent(String userAgent){
		org.apache.http.params.HttpParams httpParams = client.getParams();
		HttpProtocolParams.setUserAgent(httpParams, userAgent);//设置user-agent头信息
	}

	public void configRetryHandlers(HttpRequestRetryHandler retryHandler){
		client.setHttpRequestRetryHandler(retryHandler);//设置重试规则处理
	}

	public void configRetryHandlers(int retryCount,boolean requestSentRetryEnabled,int retrySpacingTime){
		client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(retryCount, requestSentRetryEnabled, retrySpacingTime));//设置重试规则处理
	}

	@Override
	public CookieStore getCookieStore() {
		if(context == null)
			return null;
		Object cs = context.getAttribute(ClientContext.COOKIE_STORE);
		return cs == null? null : (CookieStore)cs;
	}

	@Override
	public void setCookieStore(CookieStore cookieStore) {
		if(context != null)
			context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	}

	@Override
	@Deprecated
	public void write(Object message) throws IOException{
		if(message == null)
			return;
		try {
			request((HttpClientRequestEntity)message);
		} catch (ClassCastException e) {
			throw new ClassCastException("Httpsession only support write Request"+e.getMessage());
		} 
	}

	@Override
	public <T> RequestEntity generatePostEntity(String url,
			String downloadPath,
			HttpParams params,
			HttpSession session,
			ResponseParser<T> parser,
			Class<T> responseClass){
		HttpPost post = new HttpPost(url);
		Map<String,String> headers = null;
		Map<String,Object> reqParams = null;
		Charset requestCharset = null;
		if(params != null){
			headers = params.getHeaders();
			reqParams = params.getReqParam();
			requestCharset = params.getCharet();
		}
		if(headers != null){
			Set<String> keySet = headers.keySet();
			Iterator<String> it = keySet.iterator();
			while (it.hasNext()) {
				String keyString = it.next();
				post.addHeader(keyString, headers.get(keyString));
			}
		}
		HttpResponseConfig<T> config = new HttpResponseConfig<T>(downloadPath, responseClass,parser,params);
		String key = generateRequestKey(url,params.getReqParam());
		return new HttpClientRequestEntity(post,session, reqParams,requestCharset,config,key);
	}

	@Override
	public <T> RequestEntity generateGetEntity(String url,
			String downloadPath,
			HttpParams params,
			HttpSession session,
			ResponseParser<T> parser,
			Class<T> responseClass) {
		HttpGet get = new HttpGet(url);
		Map<String,String> headers = null;
		Map<String,Object> reqParams = null;
		Charset requestCharset = null;
		if(params != null){
			headers = params.getHeaders();
			reqParams = params.getReqParam();
			requestCharset = params.getCharet();
		}
		if(headers != null){
			Set<String> keySet = headers.keySet();
			Iterator<String> it = keySet.iterator();
			while (it.hasNext()) {
				String keyString = it.next();
				get.addHeader(keyString, headers.get(keyString));
			}
		}
		HttpResponseConfig<T> config = new HttpResponseConfig<T>(downloadPath, responseClass,parser,params);
		String key = generateRequestKey(url,params.getReqParam());
		return new HttpClientRequestEntity(get,session, reqParams,requestCharset,config,key);
	}

	@Override
	public ResponseEntity generateResonseEntity(RequestEntity requestEntity) {
		ResponseConfig<?> config = requestEntity.getResponseConfig();
		ResponseEntity entity = new HttpClientResponseEntity(config, null,requestEntity.key());
		return entity;
	}

	@Override
	public void request(RequestEntity requestEntity) throws IOException,ClientProtocolException, HttpRequestException{
		HttpClient client = this.client;
		if(requestEntity == null || isClosed())
			throw new HttpConnectorClosedException("Connector has been closed");

		HttpClientRequestEntity httpRequestEntity = (HttpClientRequestEntity)requestEntity;
		HttpSession session = httpRequestEntity.getRequestSession();
		HttpUriRequest request = httpRequestEntity.getRequest();
		if(session == null || request == null)
			return;
		if(session.isClosed())
			throw new SessionClosedException("session has been closed");

		Charset charset = contentCharset;
		Header charsetHeader = request.getFirstHeader(HTTP.CONTENT_TYPE);
		Charset headCharset = HeadLoader.loadContentTypeCharset(charsetHeader);
		if(headCharset != null){
			charset = headCharset;
		}
		if(httpRequestEntity.getCharset() != null){
			charset = httpRequestEntity.getCharset();
		}

		try {
			HttpRequestParamsWriter.write(request, httpRequestEntity,charset,new WriteObserver(session, session.getHandler(),-1));
			HttpResponse response = client.execute(request,context);
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
				HttpEntity httpEntity = response.getEntity();
				ResponseConfig<?> config = httpRequestEntity.getResponseConfig();
				HttpClientResponseEntity responseEntity = new HttpClientResponseEntity(config, httpEntity,requestEntity.key());
				//				responseEntity.set
				session.resonse(responseEntity,httpRequestEntity);
			}else{
				throw new HttpResponseException(response.getStatusLine().getStatusCode(),
						"reponse code is not 200");
			}
		} catch (ClientProtocolException e) {
			request.abort();
			throw e;
		} catch (IOException e) {
			request.abort();
			throw e;
		} catch (Exception e) {
			throw new HttpRequestException(e);
		}
	}

	public static class HttpClientRequestEntity implements RequestEntity{

		private final HttpUriRequest request;
		private final HttpSession session;
		private final Map<String, ? extends Object> paramMap;
		private final Map<String, String> httpParamMap;
		private final Charset charset;
		private final ResponseConfig<?> responseConfig;
		private final String key;

		private HttpClientRequestEntity(HttpUriRequest request,
				HttpSession session,
				Map<String, ? extends Object> param,
				Charset charset,
				ResponseConfig<?> responseConfig,
				String key){
			this.request = request;
			this.session = session;
			this.paramMap = param;
			this.httpParamMap = new HashMap<String, String>();
			this.charset = charset;
			this.responseConfig = responseConfig;
			this.key = key;
		}


		@Override
		public String key() {
			return key;
		}

		public HttpUriRequest getRequest(){
			return request;
		}

		@Override
		public HttpSession getRequestSession() {
			return session;
		}

		@Override
		public void addHeader(String name, String value) {
			request.addHeader(name, value);
		}

		@Override
		public Map<String, ? extends Object> getRequestParams() {
			return paramMap;
		}

		@Override
		public Map<String, String> getHttpParams() {
			return httpParamMap;
		}

		@Override
		public void release() {
			if(request != null)
				request.abort();
			//			if(paramMap != null)
			//				paramMap.clear();
			//			if(httpParamMap != null)
			//				httpParamMap.clear();
		}

		@Override
		public Charset getCharset() {
			return charset;
		}

		@Override
		public ResponseConfig<?> getResponseConfig() {
			return responseConfig;
		}

		
	}

	public static class HttpClientResponseEntity implements ResponseEntity{

		private HttpEntity entity;
		private Header contentType;
		private long contentLength;
		private Object content;
		private ResponseConfig<?> config;
		private boolean isCache;
		private String requestKey;

		public HttpClientResponseEntity(ResponseConfig<?> config,HttpEntity httpEntity,String key){
			this.requestKey = key;
			this.config = config;
			this.entity = httpEntity;
			try {
				if(entity != null){
					this.content = httpEntity.getContent();
					this.contentLength = httpEntity.getContentLength();
					this.contentType = httpEntity.getContentType();
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@Override
		public Object getContent(){
			return content;
		}

		@Override
		public void setContent(Object content) {
			this.content = content;
		}

		@Override
		public void finish() {
			try {
				if(entity != null)
					entity.consumeContent();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(content instanceof InputStream){
				InputStream in = (InputStream)content;
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public Header getContentType() {
			return contentType;
		}

		@Override
		public long getContentLength() {
			return contentLength;
		}
		@Override
		public String getRequestKey() {
			return requestKey;
		}

		@Override
		public boolean isCache() {
			return isCache;
		}
		@Override
		public void setCacheContent(Object cache) {
			this.isCache = true;
			this.content = cache;
		}

		@Override
		public ResponseConfig<?> getConfig() {
			return config;
		}
	}

	public class HttpResponseConfig<T> implements ResponseConfig<T> {
		private final String downPath;
		private final Charset charset;
		private final Class<T> responseClass;
		private final ResponseParser<T> parser;
		private Map<String, Object> attr;
		private Boolean continueOnCacheHit;
		private Boolean cache;
		private Long cacheExpiredTIme;

		public HttpResponseConfig(String downloadPath,Class<T> responseClass,ResponseParser<T> parser,HttpParams params){
			this.downPath = downloadPath;
			if(params != null){
				this.charset = params.getResponseCharet();
				if(params.hasConfigCacheExpired())
					cacheExpiredTIme = params.getCacheExpired();
				if(params.hasConfigContinueOnCacheHit())
					continueOnCacheHit = params.isContinueOnCacheHit();
				if(params.hasConfigResponseCache())
					cache = params.isResponseCache();
			}else {
				this.charset = null;
			}
			this.attr = params.getConfigParam();
			this.responseClass = responseClass;
			this.parser = parser;

		}

		@Override
		public Charset getCharset() {
			return charset;
		}
		@Override
		public Class<T> getResponseClass() {
			return responseClass;
		}
		@Override
		public ResponseParser<T> getResponseParser() {
			return parser;
		}
		@Override
		public void setAttribute(String key, Object name) {
			if(attr == null)
				attr = new HashMap<String, Object>();
			attr.put(key, name);
		}
		@Override
		public Object getAttribute(String key) {
			if(attr == null)
				return null;
			return attr.get(key);
		}

		@Override
		public boolean isDownloadResponse() {
			return StringUtils.isNotEmpty(downPath);
		}

		@Override
		public String getDownloadPath() {
			return downPath;
		}

		@Override
		public boolean isResponseCache() {
			return cache;
		}

		@Override
		public boolean isContinueOnCacheHit() {
			return continueOnCacheHit;
		}

		@Override
		public long cacheExpiredTime() {
			return cacheExpiredTIme;
		}

		@Override
		public boolean hasConfigResponseCache() {
			return cache != null;
		}

		@Override
		public boolean hasConfigContinueOnCacheHit() {
			return continueOnCacheHit != null;
		}

		@Override
		public boolean hasConfigCacheExpiredTime() {
			return cacheExpiredTIme != null;
		}

	}


	@Override
	public void close() {
		if(closed.compareAndSet(false, true)){
			if(client != null){
				client.getConnectionManager().shutdown();
				client = null;
			}
		}
	}

	@Override
	public boolean isClosed() {
		return closed.get();
	}

	@Override
	public String generateRequestKey(String url, Map<String, ? extends Object> paramMap) {
		String key = "";
		if(paramMap == null || paramMap.isEmpty())
			key = EncoderUtils.MD5HEX(url);
		else{
			String params = convertHttpParamKey(paramMap);
			if(params != null)
				key = EncoderUtils.MD5HEX(url+params);
			else 
				key = null;
		}
		return key;
	}
	
	private String convertHttpParamKey(Map<String, ? extends Object> map){
		if(map == null || map.isEmpty())
			return null;
		StringBuilder sb = new StringBuilder();
		Set<String> keySet = map.keySet();
		Iterator<String> it = keySet.iterator();
		boolean noKey = false;
		while(it.hasNext()){
			String key = it.next();
			Object value = map.get(key);
			if(value instanceof String){
				sb.append(key).append(value);
			}else if(value instanceof StringBody){
				String str = ((StringBody)value).getText();
				if(str != null){
					sb.append(key).append(str);
				}else{
					noKey = true;
					break;
				}
			}else{ 
				noKey = true;
				break;
			}
		}
		if(noKey){
			sb.delete(0, sb.length());
			return null;
		}
		String result = sb.toString();
		sb.delete(0, sb.length());
		return result;
	}

}
