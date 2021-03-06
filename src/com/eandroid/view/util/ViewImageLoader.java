/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-13
 * @version 0.1
 */
package com.eandroid.view.util;

import java.util.concurrent.Future;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;

import com.eandroid.cache.impl.DiskCache.DiskCacheParams;
import com.eandroid.cache.impl.MemoryCache.MemoryCacheParams;
import com.eandroid.net.http.HttpConnector;
import com.eandroid.net.http.util.HttpParams;
import com.eandroid.net.http.util.TaskControlCenter;
import com.eandroid.net.impl.http.ResponseFuture;
import com.eandroid.util.StringUtils;
import com.eandroid.util.SystemUtils;
import com.eandroid.view.util.HttpBitmapDrawableCache.BitmapDrawableLoadHandler;
import com.eandroid.view.util.HttpBitmapDrawableCache.HttpCacheParams;

public class ViewImageLoader{

	private static final String TAG = "AsyncImageLoader";
	private Bitmap mDefaultLoadingBitmap;
	private Bitmap mDefaultFailLoadedBitmap;
	private int mDefaultLoadingResId;
	private int mDefaultFailLoadedResId;
	private boolean  mFadeIn = true;
	private int  mFadeInTime = 400;
	private int  mImageWidth = Integer.MAX_VALUE;
	private int  mImageHeight = Integer.MAX_VALUE;

	private int mThreadPoolMaxSize = 1;
	private int mThreadPoolCoreSize = 0;
	private int mConnectTimeOut = 10;
	private int mSocketReadTimeOut = 5;
	private long mDiskCacheExpiredTime = -1;
	private static String mDiskCacheDirectoryName = TAG;
	private static long mDiskCacheSize = 1024 * 1024 * 20;
	private static float mMemoryCachePercent = 0.2f;


	private HttpBitmapDrawableCache mImageLoader;
	private Resources mResources;

	private static volatile ViewImageLoader instance;

	public static ViewImageLoader getInstance(Context context) {
		return getInstance(context, null);
	}

	public static ViewImageLoader getInstance(Context context,HttpConnector connector) {
		if(instance == null){
			synchronized (ViewImageLoader.class) {
				if(instance == null)
					instance = new ViewImageLoader(context,connector);
			}
		}
		return instance;
	}

	private ViewImageLoader(Context context,HttpConnector connector){
		initImageLoader(context.getResources(),connector);
	}

	private void initImageLoader(Resources resources,HttpConnector connector){		
		mResources = resources;
		if(mImageLoader != null)
			return;
		MemoryCacheParams memParams = new MemoryCacheParams(mMemoryCachePercent);
		DiskCacheParams diskParams = new DiskCacheParams(mDiskCacheDirectoryName,null,mDiskCacheSize,false);
		HttpCacheParams cacheParams = new HttpCacheParams(diskParams, memParams);
		mImageLoader = new HttpBitmapDrawableCache(cacheParams,
				mThreadPoolMaxSize,
				mThreadPoolCoreSize,
				mConnectTimeOut,
				mSocketReadTimeOut,
				mDiskCacheExpiredTime,
				mImageWidth,
				mImageHeight,
				connector);
	}

	public void clearCache() {
		if(mImageLoader != null)
			mImageLoader.clear();
	}

	public void flushCache() {
		if(mImageLoader != null)
			mImageLoader.flush();
	}
	
	public void close() {
		if(mImageLoader != null)
			mImageLoader.close();
	}

	public void loadBackground(View view,String url){
		loadBackground(view,url,-1,-1,null,null,null,null,null);
	}

	public void loadBackground(View view,String url,TaskControlCenter tcc){
		loadBackground(view,url,-1,-1,null,null,null,tcc,null);
	}

	public void loadBackground(View view,String url,int imageSize){
		loadBackground(view,url,imageSize,imageSize,null,null,null,null,null);
	}

	public void loadBackground(View view,String url,int imageWidth,int imageHeight){
		loadBackground(view,url,imageWidth,imageHeight,null,null,null,null,null);
	}

	public void loadBackground(View view,String url,final Bitmap loadingBitmap,final Bitmap failedBitmap){
		loadBackground(view,url,-1,-1,loadingBitmap,failedBitmap,null,null,null);
	}

	public void loadBackground(final View view,
			final String url,
			int imageWidth,
			int imageHeight,
			final Bitmap loadingBitmap,
			final Bitmap failedBitmap,
			HttpParams httpParams,
			TaskControlCenter tcc,
			final ImageLoadCompleteListener listener){
		if(view == null)
			return;

		if(StringUtils.isEmpty(url))
			return;

		if(!cancelPotentialTask(view.getBackground(),url)){
			return;
		}

		ResponseFuture<Bitmap> future = mImageLoader.load(url,
				mResources,
				imageWidth,
				imageHeight,
				httpParams,
				tcc,
				new BitmapDrawableLoadHandler() {
			@Override
			public void onSuccess(BitmapDrawable bd) {
				if(listener != null){
					listener.loadComplete(view, bd);
				}else {
					finishLoadedBackground(view,bd,mFadeIn);
				}
			}

			@Override
			public void onCatchException(Exception exception) {
				if(listener != null){
					listener.loadComplete(view, getFailedDrawable(failedBitmap));
				}else{
					failedToLoadBackground(view,failedBitmap);
				}
			}

			@Override
			public void onCache(BitmapDrawable bd) {
				if(listener != null){
					listener.loadComplete(view, bd);
				}else {
					finishLoadedBackground(view,bd,mFadeIn);	
				}
			}
			

			@Override
			public void onMemoryCache(BitmapDrawable bd) {
				if(listener != null){
					listener.loadComplete(view, bd);
				}else{
					finishLoadedBackground(view,bd,false);
				}
			}
		});
		if(future != null)
			startLoadingBackground(view,loadingBitmap,future);
	}

	public void loadImage(ImageView view,String url){
		loadImage(view,url,-1,-1,null,null,null,null,null);
	}

	public void loadImage(ImageView view,String url,TaskControlCenter tcc){
		loadImage(view,url,-1,-1,null,null,null,tcc,null);
	}

	public void loadImage(ImageView view,String url,int imageSize){
		loadImage(view,url,imageSize,imageSize,null,null,null,null,null);
	}

	public void loadImage(ImageView view,String url,int imageWidth,int imageHeight){
		loadImage(view,url,imageWidth,imageHeight,null,null,null,null,null);
	}

	public void loadImage(ImageView view,String url,final Bitmap loadingBitmap,final Bitmap failedBitmap){
		loadImage(view,url,-1,-1,loadingBitmap,failedBitmap,null,null,null);
	}

	public void loadImage(final ImageView view,
			final String url,
			int imageWidth,
			int imageHeight,
			final Bitmap loadingBitmap,
			final Bitmap failedBitmap,
			HttpParams httpParams,
			TaskControlCenter tcc,
			final ImageLoadCompleteListener listener){
		if(view == null)
			return;

		if(StringUtils.isEmpty(url))
			return;

		if(!cancelPotentialTask(view.getDrawable(),url)){
			return;
		}
		view.setImageBitmap(null);
		ResponseFuture<Bitmap> future = mImageLoader.load(url,
				mResources,
				imageWidth,
				imageHeight,
				httpParams,
				tcc,
				new BitmapDrawableLoadHandler() {
			@Override
			public void onSuccess(BitmapDrawable bd) {
				if(listener != null){
					listener.loadComplete(view, bd);
				}else{
					finishLoadedImage(view,bd,mFadeIn);
				}
			}

			@Override
			public void onCatchException(Exception exception) {
				if(listener != null){
					listener.loadComplete(view, getFailedDrawable(failedBitmap));
				}else{
					failedToLoadImage(view,failedBitmap);
				}
			}

			@Override
			public void onCache(BitmapDrawable bd) {
				if(listener != null){
					listener.loadComplete(view, bd);
				}else{
					finishLoadedImage(view,bd,mFadeIn);
				}
			}

			@Override
			public void onMemoryCache(BitmapDrawable bd) {
				if(listener != null){
					listener.loadComplete(view, bd);
				}else{
					finishLoadedImage(view,bd,false);
				}
			}
		});
		if(future != null)
			startLoadingImage(view,loadingBitmap,future);
	}


	public boolean cancelPotentialTask(Drawable d,String url){
		if(d == null)
			return true;

		if(d instanceof AsyncBitmapDrawable){
			AsyncBitmapDrawable abd = (AsyncBitmapDrawable)d;
			Future<?> future = abd.getTask();
			if(future == null 
					|| future.isCancelled() || future.isDone()){
				return true;
			}
			if(StringUtils.isNotEmpty(url) && future instanceof ResponseFuture<?>){
				ResponseFuture<?> responseFuture = (ResponseFuture<?>)future;
				if(!responseFuture.sameKey(mImageLoader.generateRequestKey(url))){
					return responseFuture.cancel(true);
				}else{
					return false;
				}
			}else {
				return future.cancel(true);
			}
		}
		return true;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@SuppressWarnings("deprecation")
	protected void startLoadingBackground(View v,Bitmap loadingBitmap,Future<Bitmap> task){
		if(SystemUtils.hasJellyBean())
			v.setBackground(getLoadingDrawable(loadingBitmap, task));
		else
			v.setBackgroundDrawable(getLoadingDrawable(loadingBitmap, task));

	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@SuppressWarnings("deprecation")
	protected void finishLoadedBackground(View v,Drawable drawable,boolean fadeIn){
		if(drawable == null)
			return;
		if(fadeIn){
			Drawable layerDrawable = new BitmapDrawable(mResources);
			layerDrawable.setAlpha(0);
			final TransitionDrawable td =
					new TransitionDrawable(new Drawable[] {
							layerDrawable,
							drawable
					});
			if(SystemUtils.hasJellyBean()){
				v.setBackground(td);
			}else{
				v.setBackgroundDrawable(td);
			}
			td.startTransition(mFadeInTime);
		}else{
			if(SystemUtils.hasJellyBean()){
				v.setBackground(drawable);
			}else{
				v.setBackgroundDrawable(drawable);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	protected void failedToLoadBackground(View v,Bitmap failedBitmap){
		Drawable d = getFailedDrawable(failedBitmap);
		if(d != null)
			if(SystemUtils.hasJellyBean())
				v.setBackground(d);
			else
				v.setBackgroundDrawable(d);
	}

	protected void startLoadingImage(ImageView v,Bitmap loadingBitmap,Future<Bitmap> task){
		v.setImageDrawable(getLoadingDrawable(loadingBitmap, task));
	}

	protected void finishLoadedImage(ImageView v,Drawable drawable,boolean fadeIn){
		if(drawable == null)
			return;
		if(fadeIn){
			Drawable layerDrawable = new BitmapDrawable(mResources);
			layerDrawable.setAlpha(0);
			final TransitionDrawable td =
					new TransitionDrawable(new Drawable[] {
							layerDrawable,
							drawable
					});
//			if(v.getBackground() == null){
//				if(SystemUtils.hasJellyBean())
//					v.setBackground(v.getDrawable());
//				else
//					v.setBackgroundDrawable(v.getDrawable());
//			}
			v.setImageDrawable(td);
			td.startTransition(mFadeInTime);
		}else{
			v.setImageDrawable(drawable);
		}
	}

	protected void failedToLoadImage(ImageView v,Bitmap failedBitmap){
		Drawable d = getFailedDrawable(failedBitmap);
		if(d != null)
			v.setImageDrawable(d);
	}
	
	private Drawable getLoadingDrawable(Bitmap loadingBitmap,Future<Bitmap> task) {
		Bitmap bitmap = null;
		AsyncBitmapDrawable abd = null;
		if(loadingBitmap != null){
			bitmap = loadingBitmap;
			abd = new AsyncBitmapDrawable(mResources,bitmap,task);
			abd.setAutoRecycle(false);
		}else if(mDefaultLoadingResId > 0){
			bitmap = BitmapFactory.decodeResource(mResources, mDefaultLoadingResId);
			abd = new AsyncBitmapDrawable(mResources,bitmap,task);
		}else if(mDefaultLoadingBitmap != null){
			bitmap = mDefaultLoadingBitmap;
			abd = new AsyncBitmapDrawable(mResources,bitmap,task);
			abd.setAutoRecycle(false);
		}else{
			abd = new AsyncBitmapDrawable(mResources,null, task);
		}
		return abd;
	}
	
	private Drawable getFailedDrawable(Bitmap failedBitmap) {
		Bitmap bitmap = null;
		boolean autoRecycle = true;
		if(failedBitmap != null){
			bitmap = failedBitmap;
			autoRecycle = false;
		}else if(mDefaultFailLoadedResId > 0){
			bitmap = BitmapFactory.decodeResource(mResources, mDefaultFailLoadedResId);
		}else if(mDefaultFailLoadedBitmap != null){
			bitmap = mDefaultFailLoadedBitmap;
			autoRecycle = false;
		}

		if(bitmap != null){
			BitmapDrawable bd = null;
			if(SystemUtils.hasHoneycomb())
				bd = new BitmapDrawable(mResources,bitmap);
			else {
				bd = new RecyclingBitmapDrawable(mResources,bitmap);
				((RecyclingBitmapDrawable)bd).setAutoRecycle(autoRecycle);
			}
			return bd;
		}
		return null;
	}


	/**
	 * 设置默认的缓存文件夹路径<br/>
	 * 这个设置如果在磁盘缓存已经初始化后调用（磁盘缓存会在第一次使用到缓存时初始化），会抛出相关异常。<br/>
	 * 如果需要设置，请在得到ViewImageLoader对象后，还未使用他加载任何图片之前进行
	 * @param diskCacheDirectoryName
	 */
	public void setDiskCacheDirectoryPath(String diskCacheDirectoryPath) {
		mImageLoader.getHttp().configResponseCacheDirectoryPath(diskCacheDirectoryPath);
	}

	/**
	 * 设置默认的磁盘缓存大小<br/>
	 * 默认为10mb<br/>
	 * 这个设置如果在磁盘缓存已经初始化后调用（磁盘缓存会在第一次使用到缓存时初始化），会抛出相关异常。<br/>
	 * 如果更改了缓存目录，建议先调用clearCache()方法清楚之前目录的缓存<br/>
	 * 如果需要设置，请在得到ViewImageLoader对象后，还未使用他加载任何图片之前进行
	 * @param diskCacheSize 单位 byte
	 */
	public void setDiskCacheSize(long diskCacheSize) {
		mImageLoader.getHttp().configResponseCacheMaxSize(diskCacheSize);
	}

	/**
	 * 设置内存缓存占，总dalvik虚拟机分配内存的百分比<br/>
	 * 设置后会重置内存缓存<br/>
	 * 默认为0.2 百分之20
	 * @param memoryCachePercent
	 */
	public void setMemoryCachePercent(float memoryCachePercent) {
		mImageLoader.resetMemoryCache(new MemoryCacheParams(memoryCachePercent));
	}

	/**
	 * 设置网络请求的最大线程数量<br/>
	 * 默认为2
	 * @param threadPoolMaxSize
	 */
	public void setThreadPoolMaxSize(int threadPoolMaxSize) {
		mImageLoader.getHttp().configThreadPoolMaxSize(threadPoolMaxSize);
	}

	/**
	 * 设置网络请求的核心线程数量<br/>
	 * 默认为0
	 * @param threadPoolCoreSize
	 */
	public void setThreadPoolCoreSize(int threadPoolCoreSize) {
		mImageLoader.getHttp().configThreadPoolCoreSize(threadPoolCoreSize);
	}

	/**
	 * 设置网络请求的超时时间
	 * @param connectTimeOut 默认10s
	 * @param socketReadTimeOut 默认5s
	 */
	public void setConnectTimeOut(int connectTimeOut,int socketReadTimeOut) {
		mImageLoader.getHttp().configTimeoutInSeconds(connectTimeOut, socketReadTimeOut);
	}

	/**
	 * 设置磁盘缓存的过期时间<br/>
	 * 默认-1 不过期
	 * @param diskCacheExpiredTime
	 */
	public void setDiskCacheExpiredTime(long diskCacheExpiredTime) {
		mImageLoader.getHttp().configResponseCache(false, diskCacheExpiredTime);
	}

	/**
	 * 设置默认是否开启渐隐效果<br/>
	 * 默认开启
	 * @param fadeIn
	 */
	public void setFadeIn(boolean fadeIn) {
		mFadeIn = fadeIn;
	}

	/**
	 * 设置默认渐隐效果时间
	 * @param fadeInTime
	 */
	public void setFadeInTime(int fadeInTime) {
		mFadeInTime = fadeInTime;
	}

	/**
	 * 设置默认的图片显示宽度
	 * @param imageWidth
	 */
	public void setImageWidth(int imageWidth) {
		mImageWidth = imageWidth;
	}

	/**
	 * 设置默认的图片显示高度
	 * @param imageHeight
	 */
	public void setImageHeight(int imageHeight) {
		mImageHeight = imageHeight;
	}

	/**
	 * 设置加载中的图片
	 * @param loadingBitmap
	 */
	public void setLoadingBitmap(Bitmap loadingBitmap){
		mDefaultLoadingBitmap = loadingBitmap;
	}

	/**
	 * 设置加载失败的图片
	 * @param failLoadedBitmap
	 */
	public void setFailBitmap(Bitmap failLoadedBitmap){
		mDefaultFailLoadedBitmap = failLoadedBitmap;
	}

	/**
	 * 设置加载中的图片资源id
	 * @param id
	 */
	public void setLoadingResourceId(int id){
		mDefaultLoadingResId = id;
	}

	/**
	 * 设置加载失败时的图片资源id
	 * @param id
	 */
	public void setFailResourceId(int id){
		mDefaultFailLoadedResId = id;
	}

	public static class ImageTaskControlCenter extends TaskControlCenter{
		public static void cancel(View v){
			cancelTask(getTask(v.getBackground()));

			if(v instanceof ImageView){
				ImageView hiv = (ImageView)v;
				cancelTask(getTask(hiv.getDrawable()));
			}
		}

		private static Future<Bitmap> getTask(Drawable d){
			if(d == null)
				return null;

			if(d instanceof AsyncBitmapDrawable){
				AsyncBitmapDrawable abd = (AsyncBitmapDrawable)d;
				return abd.getTask();
			}else {
				return null;
			}
		}

		private static void cancelTask(Future<Bitmap> f){
			if(f == null)
				return;

			if(f != null && !f.isCancelled() && !f.isDone()){
				f.cancel(true);
			}
		}
	}

	public static interface ImageLoadCompleteListener{
		public void loadComplete(View view,Drawable drawable);
	}
}
