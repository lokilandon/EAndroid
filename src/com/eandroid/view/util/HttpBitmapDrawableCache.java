/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-12
 * @version 0.1
 */
package com.eandroid.view.util;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Observer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.eandroid.cache.Cache;
import com.eandroid.cache.ReusableBitmapCache;
import com.eandroid.cache.impl.BitmapDrawableMemoryCache;
import com.eandroid.cache.impl.DiskCache.DiskCacheParams;
import com.eandroid.cache.impl.MemoryCache.MemoryCacheParams;
import com.eandroid.cache.util.BitmapCacheUtils;
import com.eandroid.content.EContext;
import com.eandroid.net.Http;
import com.eandroid.net.http.HttpConnector;
import com.eandroid.net.http.HttpHandler;
import com.eandroid.net.http.RequestEntity.RequestConfig;
import com.eandroid.net.http.ResponseHandler;
import com.eandroid.net.http.response.BitmapResponseParser;
import com.eandroid.net.http.response.ResponseParseException;
import com.eandroid.net.http.util.HttpParams;
import com.eandroid.net.http.util.TaskControlCenter;
import com.eandroid.net.impl.http.HttpRequestExecutionException;
import com.eandroid.net.impl.http.ResponseFuture;
import com.eandroid.util.CommonUtils;
import com.eandroid.util.EALog;
import com.eandroid.util.FileUtils;
import com.eandroid.util.SystemUtils;

public class HttpBitmapDrawableCache implements Cache<String, BitmapDrawable>{
	private static final String TAG = "AsyncBitmapDrawable";
	private static final String HTTP_TAG = "AsyncBitmapLoader";
	private ReusableBitmapCache memoryCache;
	private Http bitmapLoader;
	private HttpCacheParams mCacheParams;

	public HttpBitmapDrawableCache(HttpCacheParams cacheParams,
			int threadPoolMaxSize,
			int threadPoolCoreSize,
			int connectTimeOut,
			int socketReadTimeOut,
			long diskCacheExpiredTime,
			final int imageWidth,
			final int imageHeight,
			HttpConnector connector){
		if(cacheParams == null || cacheParams.memoryCacheParams == null
				|| cacheParams.diskCacheParams == null){
			throw new IllegalArgumentException("AsyncBitmapDrawableCache init failed");
		}
		this.mCacheParams = cacheParams;
		memoryCache = new BitmapDrawableMemoryCache(cacheParams.memoryCacheParams);

		DiskCacheParams diskCacheParams = cacheParams.diskCacheParams;
		bitmapLoader = Http.open(HTTP_TAG,connector).configThreadPoolCoreSize(threadPoolCoreSize)
				.configThreadPoolMaxSize(threadPoolMaxSize)
				.configTimeoutInSeconds(connectTimeOut, socketReadTimeOut)
				.configResponseCache(false, diskCacheExpiredTime)
				.configResponseCache(true, false, false)
				.configResponseCacheDirectoryPath(diskCacheParams.diskCacheDir.getPath())
				.configResponseCacheMaxSize(diskCacheParams.diskCacheSize)
				.configBitmapResponseParser(new BitmapResponseParser() {
					@Override
					public Bitmap parseObject(RequestConfig<Bitmap> config,
							InputStream in, Charset defauCharset,
							Observer readObserver)
									throws ResponseParseException {
						int w = 0;
						int h = 0;
						try {
							w = (Integer)config.getAttribute("imageWidth");
							h = (Integer)config.getAttribute("imageHeight");
						} catch (Exception e) {}
						if(w <= 0){w = imageWidth;}
						if(h <= 0){h = imageHeight;}

						if(in instanceof FileInputStream){
							FileInputStream fis = (FileInputStream)in;
							try {
								FileDescriptor fd = fis.getFD();
								Bitmap bitmap = BitmapCacheUtils.decode(fd,w,h, memoryCache);
								if(bitmap != null)
									return bitmap;
							} catch (IOException e) {
								EALog.w(TAG, "parseObject - "+e);
							}
						}
						String tempFileSavePath = EContext.getTempFilePath() + CommonUtils.generateSequenceNo() + ".tmp";
						boolean res = FileUtils.save(tempFileSavePath, in, readObserver);
						if(!res)
							throw new ResponseParseException("An error occured while (temp save)/read bitmap response."+config.toString());

						//						try {
						//							Thread.sleep(400);
						//						} catch (InterruptedException e) {}

						Bitmap bitmap = BitmapCacheUtils.decode(tempFileSavePath,w,h, memoryCache);
						if(bitmap == null){
							throw new ResponseParseException("An error occured while decode bitmap response."+config.toString());
						}
						return bitmap;
					}
				});
	}

	@Override
	public BitmapDrawable put(String key, BitmapDrawable value) {
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public BitmapDrawable load(String key) {
		if(key == null)
			return null;
		BitmapDrawable bd = memoryCache.load(key);
		if(bd != null){
			return bd;
		}
		try {
			Bitmap bitmap = bitmapLoader.getSync(key,null, null, Bitmap.class);
			if(SystemUtils.hasHoneycomb())
				return new BitmapDrawable(bitmap);
			else {
				return new RecyclingBitmapDrawable(bitmap);
			}
		} catch (HttpRequestExecutionException e) {
			return null;
		}

	}

	public ResponseFuture<Bitmap> load(final String key,
			final Resources resources,
			int imageWidth,
			int imageHeight,
			HttpParams httpParams,
			TaskControlCenter tcc,
			final BitmapDrawableLoadHandler handler) {
		if(key == null)
			return null;
		BitmapDrawable bd = memoryCache.load(key);
		if(bd != null){
			handler.onCache(bd);
			return null;
		}
		if(httpParams == null){
			httpParams = new HttpParams().
					putConfig("imageWidth", imageWidth).
					putConfig("imageHeight", imageHeight);
		}
		final ResponseFuture<Bitmap> future = bitmapLoader.get(key,
				httpParams,
				new ResponseHandler<Bitmap>() {
			@Override
			public void onSuccess(final Bitmap bitmap) {
				if(SystemUtils.hasHoneycomb()){
					BitmapDrawable bd = new BitmapDrawable(resources, bitmap);
					memoryCache.put(key, bd);
					handler.onSuccess(bd);
				}else {
					RecyclingBitmapDrawable rbd = new RecyclingBitmapDrawable(resources, bitmap);
					rbd.setIsCached(true);
					memoryCache.put(key, rbd);
					handler.onCache(rbd);
				}
			}
			@Override
			public void onCache(final Bitmap bitmap) {
				if(SystemUtils.hasHoneycomb()){
					BitmapDrawable bd = new BitmapDrawable(resources, bitmap);
					memoryCache.put(key, bd);
					handler.onCache(bd);
				}else {
					RecyclingBitmapDrawable rbd = new RecyclingBitmapDrawable(resources, bitmap);
					rbd.setIsCached(true);
					memoryCache.put(key, rbd);
					handler.onCache(rbd);
				}
			}
			@Override
			public void onCatchException(Exception exception) {
				handler.onCatchException(exception);
			}
			@Override
			public void onRequestStart() {}
		},tcc);
		return future;
	}

	Http getHttp(){
		return bitmapLoader;
	}
	@Override
	public boolean remove(String key) {
		if(key == null)
			return false;

		memoryCache.remove(key);
		bitmapLoader.getCache().remove(key);
		return false;
	}

	@Override
	public void clear() {
		memoryCache.clear();
		bitmapLoader.clearCache();
	}

	@Override
	public void flush() {
		memoryCache.flush();
		bitmapLoader.flushCache();
	}

	@Override
	public void close() {
		memoryCache.close();
		bitmapLoader.close();
	}
	
	public void resetMemoryCache(MemoryCacheParams cacheParams) {
		memoryCache.close();
		memoryCache = new BitmapDrawableMemoryCache(cacheParams);
	}

	@Override
	public boolean isClose() {
		return memoryCache.isClose() || bitmapLoader.getCache().isClose();
	}

	@Override
	public CacheParams getCacheParams() {
		return mCacheParams;
	}

	public static class HttpCacheParams implements CacheParams{
		private DiskCacheParams diskCacheParams;
		private MemoryCacheParams memoryCacheParams;
		public HttpCacheParams(DiskCacheParams diskParams,MemoryCacheParams memoryCacheParams) {
			this.diskCacheParams = diskParams;
			this.memoryCacheParams = memoryCacheParams;
		}

		public DiskCacheParams getDiskCacheParams() {
			return diskCacheParams;
		}

		public MemoryCacheParams getMemoryCacheParams() {
			return memoryCacheParams;
		}

		public long getDiskCacheSize() {
			return diskCacheParams.getCacheSize();
		}

		public long getMemoryCacheSize() {
			return memoryCacheParams.getCacheSize();
		}

		@Override
		public long getCacheSize() {
			return diskCacheParams.getCacheSize() + memoryCacheParams.getCacheSize();
		}		
	}

	public static abstract class BitmapDrawableLoadHandler implements HttpHandler<BitmapDrawable>{
		@Override
		public void onRequestStart() {}

		@Override
		public void onUploadProgress(int progress) {}

		@Override
		public void onDownloadProgress(int progress) {}
	}

	public String generateRequestKey(String url){
		return bitmapLoader.generateRequestKey(url, null);
	}

}
