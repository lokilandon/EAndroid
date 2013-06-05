/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 0.1
 */
package com.eandroid.net.http.util;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;

import com.eandroid.net.http.RequestEntity;
import com.eandroid.net.http.RequestEntity.RequestConfig;
import com.eandroid.net.http.httpclient.entity.mime.MultipartEntity;
import com.eandroid.net.http.httpclient.entity.mime.ObservedHttpEntity;
import com.eandroid.net.http.httpclient.entity.mime.content.ByteArrayBody;
import com.eandroid.net.http.httpclient.entity.mime.content.FileBody;
import com.eandroid.net.http.httpclient.entity.mime.content.InputStreamBody;
import com.eandroid.net.http.httpclient.entity.mime.content.StringBody;
import com.eandroid.util.StringUtils;

public class HttpRequestParamsWriter {

	public static void write(HttpUriRequest request,RequestEntity requestEntity,Charset contentCharset,WriteObserver observer) throws UnsupportedEncodingException{
		if(request == null || requestEntity == null)
			return;

		Map<String, String> httpParamMap = requestEntity.getHttpParams();
		if(httpParamMap != null && !httpParamMap.isEmpty()){
			Iterator<Entry<String,String>> httpParamIt = httpParamMap.entrySet().iterator();
			org.apache.http.params.HttpParams params = request.getParams();
			if(params == null)params = new BasicHttpParams();
			while(httpParamIt.hasNext()){
				Entry<String,String> entry = httpParamIt.next();
				params.setParameter(entry.getKey(), entry.getValue());
			}
			request.setParams(params);
		}

		Map<String,? extends Object> requestParamMap = requestEntity.getRequestParams();
		if(requestParamMap != null && !requestParamMap.isEmpty()){
			if(request instanceof HttpPost){
				HttpPost post = (HttpPost)request;
				MultipartEntity multipartEntity = new MultipartEntity();
				Iterator<String> iterator = requestParamMap.keySet().iterator();
				while(iterator.hasNext()){
					String key = iterator.next();
					Object value = requestParamMap.get(key);
					if(value instanceof File){
						File file = (File)value;
						multipartEntity.addPart(key, new FileBody(file));
					}else if(value instanceof InputStream){
						InputStream in = (InputStream)value;
						multipartEntity.addPart(key, new InputStreamBody(in,key));
					}else if(value instanceof Byte[]){
						byte[] bytes = (byte[])value;
						multipartEntity.addPart(key, new ByteArrayBody(bytes,key));
					}else if(value instanceof String){
						String str = (String)value;
						multipartEntity.addPart(key, new StringBody(str,contentCharset));
					}else if(value instanceof FileBody){
						multipartEntity.addPart(key, (FileBody)value);
					}else if(value instanceof InputStream){
						multipartEntity.addPart(key, (InputStreamBody)value);
					}else if(value instanceof ByteArrayBody){
						multipartEntity.addPart(key, (ByteArrayBody)value);
					}else if(value instanceof StringBody){
						multipartEntity.addPart(key, (StringBody)value);
					}
				}
				RequestConfig<?> config = requestEntity.getConfig();
				HttpParams httpParams = config.getHttpParams();
				if(httpParams != null && httpParams.isListenUpload()){
					observer.setTotalLength(multipartEntity.getContentLength());
					ObservedHttpEntity observedHttpEntity = new ObservedHttpEntity(multipartEntity, observer);
					post.setEntity(observedHttpEntity);
				}else{
					post.setEntity(multipartEntity);
				}
			}else if(request instanceof HttpGet){

				List<BasicNameValuePair> lparams = new LinkedList<BasicNameValuePair>();
				Iterator<String> keyIterator = requestParamMap.keySet().iterator();
				while(keyIterator.hasNext()){
					String key = keyIterator.next();
					String value = (String)requestParamMap.get(key);
					lparams.add(new BasicNameValuePair(key, value));
				}
				String requestUrlString = request.getURI().toString();
				String queryString = request.getURI().getQuery();
				String paramString = URLEncodedUtils.format(lparams, contentCharset.displayName());
				if(StringUtils.isEmpty(queryString)){
					requestUrlString += "?" + paramString;
				}else{
					requestUrlString += "&" + paramString;
				}
				((HttpGet) request).setURI(URI.create(requestUrlString));
			}
		}
	}

}
