/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-11
 * @version 0.1
 */
package com.eandroid.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.eandroid.net.http.HttpConnector;
import com.eandroid.net.http.util.HttpParams;
import com.eandroid.util.StringUtils;
import com.eandroid.view.util.ViewImageLoader;
import com.eandroid.view.util.ViewImageLoader.ImageTaskControlCenter;

public class AsyncImageView extends RecyclingImageView{
	
	private static ViewImageLoader mImageLoader;
	private HttpParams mHttpParam;
	
	public AsyncImageView(Context context) {
		super(context);
		initImageLoader(context);
	}

	public AsyncImageView(Context context, AttributeSet attrs) {
		super(context, attrs, 0);
		initImageLoader(context);
	}

	public AsyncImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initImageLoader(context);
	}
	
	public static void initImageLoader(Context context,HttpConnector connector){
		if(mImageLoader == null)
			mImageLoader = ViewImageLoader.getInstance(context,connector);	
	}

	private void initImageLoader(Context context){
		if(isInEditMode())
			return;
		if(mImageLoader == null)
			mImageLoader = ViewImageLoader.getInstance(context);
	}

	public void setImageURL(String url){
		setImageURL(url,-1,-1,null,null,null);
	}

	public void setImageURL(String url,ImageTaskControlCenter btcc){
		setImageURL(url,-1,-1,null,null,btcc);
	}
	
	public void setImageURL(String url,int imageSize){
		setImageURL(url,imageSize,imageSize,null,null,null);
	}

	public void setImageURL(String url,int imageWidth,int imageHeight){
		setImageURL(url,imageWidth,imageHeight,null,null,null);
	}

	public void setImageURL(String url,final Bitmap loadingDrawable,final Bitmap failedDrawable){
		setImageURL(url,-1,-1,loadingDrawable,failedDrawable,null);
	}

	public void setImageURL(String url,
			int imageWidth,
			int imageHeight,
			final Bitmap loadingBitmap,
			final Bitmap failedBitmap,
			ImageTaskControlCenter itcc){
		if(StringUtils.isEmpty(url))
			return;

		mImageLoader.loadImage(this, url, imageWidth, imageHeight, loadingBitmap, failedBitmap,mHttpParam, itcc);
	}

	public void setBackGroundURL(String url){
		setBackGroundURL(url,-1,-1,null,null,null);
	}
	
	public void setBackGroundURL(String url,ImageTaskControlCenter btcc){
		setBackGroundURL(url,-1,-1,null,null,btcc);
	}

	public void setBackGroundURL(String url,int imageSize){
		setBackGroundURL(url,imageSize,imageSize,null,null,null);
	}

	public void setBackGroundURL(String url,int imageWidth,int imageHeight){
		setBackGroundURL(url,imageWidth,imageHeight,null,null,null);
	}

	public void setBackGroundURL(String url,final Bitmap loadingDrawable,final Bitmap failedDrawable){
		setBackGroundURL(url,-1,-1,loadingDrawable,failedDrawable,null);
	}

	public void setBackGroundURL(String url,
			int imageWidth,
			int imageHeight,
			final Bitmap loadingBitmap,
			final Bitmap failedBitmap,
			ImageTaskControlCenter itcc){
		if(StringUtils.isEmpty(url))
			return;

		mImageLoader.loadBackground(this, url, imageWidth, imageHeight, loadingBitmap, failedBitmap,mHttpParam, itcc);
	}
	
	/**
	 * 设置默认的连接参数
	 * @param fadeIn
	 */
	public AsyncImageView setHttpParams(HttpParams httpParams){
		mHttpParam = httpParams;
		return this;
	}
	
	/**
	 * 获取ImageLoader类，可对其进行配置
	 * @return
	 */
	public static ViewImageLoader getImageLoader(Context context) {
		if(mImageLoader == null)
			mImageLoader = ViewImageLoader.getInstance(context);
		return mImageLoader;
	}
	
}
