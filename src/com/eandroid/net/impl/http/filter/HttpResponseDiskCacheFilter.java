/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 0.1
 */
package com.eandroid.net.impl.http.filter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;

import com.eandroid.cache.Cache;
import com.eandroid.cache.impl.DiskCache;
import com.eandroid.cache.impl.NotEnoughSpaceException;
import com.eandroid.cache.impl.DiskCache.DiskCacheParams;
import com.eandroid.net.Session;
import com.eandroid.net.http.HttpConnector;
import com.eandroid.net.http.RequestEntity;
import com.eandroid.net.http.ResponseEntity;
import com.eandroid.net.http.ResponseEntity.ResponseConfig;
import com.eandroid.net.impl.http.HttpRequestSession;
import com.eandroid.util.EALog;

public class HttpResponseDiskCacheFilter extends BasicHttpFilter{
	private static String TAG = "HttpResponseDiskCacheFilter";

	private volatile boolean cacheFile;
	private volatile boolean cacheBitmap;
	private volatile boolean cacheOtherObject;
	private volatile long cacheExpired;
	private volatile boolean continueOnCacheHit = true;
	private DiskCache<String, InputStream> diskCache;

	public HttpResponseDiskCacheFilter(boolean cacheFile,
			boolean cacheBitmap,
			boolean cacheOtherObject,
			long maxCacheSize,
			boolean continueOnCacheHit,
			long cacheExpired,
			String diskCacheDirectoryPath) throws IllegalArgumentException{
		this.cacheFile = cacheFile;
		this.cacheBitmap = cacheBitmap;
		this.cacheOtherObject = cacheOtherObject;
		this.continueOnCacheHit = continueOnCacheHit;
		this.cacheExpired = cacheExpired;
		DiskCache.DiskCacheParams param = new DiskCacheParams(diskCacheDirectoryPath,null,maxCacheSize,false);
		try {
			diskCache = new DiskCache<String,InputStream>(param);
		} catch (NotEnoughSpaceException e) {
			EALog.e(TAG, "Http response Disk Cache dose not running. "+e);
		}
	}

	@Override
	public void onWrite(NextFilterSelector next, Session session,RequestEntity message) {
		if(diskCache == null){
			super.onWrite(next, session, message);
			return;
		}
		
		ResponseConfig<?> config = message.getResponseConfig();
		if((config.hasConfigResponseCache() && !config.isResponseCache())){
			super.onWrite(next, session, message);
			return;
		}

		String key = message.key();
		if(key == null){
			super.onWrite(next, session, message);
			return;
		}

		long cacheExpiredTime = cacheExpired;
		if(config.hasConfigCacheExpiredTime())
			cacheExpiredTime = config.cacheExpiredTime();
		boolean continueOnCacheHit = this.continueOnCacheHit;
		if(config.hasConfigContinueOnCacheHit())
			continueOnCacheHit = config.isContinueOnCacheHit();

		boolean cacheHit = false;
		ResponseEntity cacheEntity = null;
		InputStream inputStream = null;
		try {
			inputStream = diskCache.load(key, cacheExpiredTime);
			if(inputStream != null){
				cacheHit = true;
				cacheEntity = ((HttpConnector)session.getConnector()).
						generateResonseEntity(message);
				cacheEntity.setCacheContent(inputStream);
				inputStream = null;
				((HttpRequestSession)session).resonse(cacheEntity, message);
				return;
			}
		} catch (Exception e) {
			EALog.e(TAG, "onWrite - " + e);
		} finally{
			if(inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {}
		}
		if(!cacheHit || continueOnCacheHit)
			super.onWrite(next, session, message);
	}

	public void onRead(NextFilterSelector next, Session session, ResponseEntity entity){
		ResponseConfig<?> config = entity.getConfig();
		if(diskCache == null){
			super.onRead(next, session,entity);
			return;
		}
		
		if(config.hasConfigResponseCache() && !config.isResponseCache()){
			super.onRead(next, session,entity);
			return;
		}

		Object responseContent = entity.getContent();
		if(entity.isCache() 
				|| (responseContent != null && !(responseContent instanceof InputStream))){
			super.onRead(next, session,entity);
			return;
		}

		String key = entity.getRequestKey();
		if(key == null){
			super.onRead(next, session,entity);
			return;
		}

		Class<?> resultClass = entity.getConfig().getResponseClass();
		
		boolean isCache = false;
		if(resultClass != null){
			if(resultClass == File.class){
				isCache = cacheFile;
			}else if(resultClass == Bitmap.class){
				isCache = cacheBitmap;
			}else{
				isCache = cacheOtherObject;
			}
			if(config.hasConfigResponseCache())
				isCache = config.isResponseCache();
		}

		if(!isCache){
			super.onRead(next, session,entity);
			return;
		}

		InputStream in = (InputStream)responseContent;
		InputStream cacheIn = null;
		try {
			diskCache.put(key, in);
			cacheIn = diskCache.load(key);
		} catch(Exception e){
			EALog.e(TAG, "onRead - " + e);
		} finally{
			if(cacheIn != null){
				entity.setContent(cacheIn);
			}else {
				in = null;
			}
			try {
				if(in != null){
					in.close();
				}
			} catch (IOException e) {}
		}
		super.onRead(next, session,entity);
	};
	
	public void setCacheExpired(long expiredTime){
		this.cacheExpired = expiredTime;
	}

	public void setContinueOnCacheHit(boolean continueOnCacheHit){
		this.continueOnCacheHit = continueOnCacheHit;
	}

	public void setCacheFile(boolean cacheFile) {
		this.cacheFile = cacheFile;
	}

	public void setCacheBitmap(boolean cacheBitmap) {
		this.cacheBitmap = cacheBitmap;
	}

	public void setCacheOtherObject(boolean cacheOtherObject) {
		this.cacheOtherObject = cacheOtherObject;
	}
	public void setDiskCacheDirectoryPath(String diskCacheDirectoryPath){
		if(!diskCache.isInited()){
			((DiskCacheParams)diskCache.getCacheParams()).setDiskCacheDirectoryPath(diskCacheDirectoryPath);
		}else {
			throw new IllegalStateException(TAG + "setDiskCacheDirectoryName - fail!Diskcache has been inited");
		}
	}
	public void setDiskCacheSize(long diskCacheSize){
		if(!diskCache.isInited()){
			((DiskCacheParams)diskCache.getCacheParams()).setDiskCacheSize(diskCacheSize);
		}else{
			throw new IllegalStateException(TAG + "setDiskCacheSize - fail!Diskcache has been inited");
		}
	}
	
	public void clearCache(){
		diskCache.clear();
	}

	public void flushCache(){
		diskCache.flush();
	}

	public void closeCache(){
		diskCache.close();
	}
	
	public Cache<String, InputStream> getCache(){
		return diskCache;
	}
}
