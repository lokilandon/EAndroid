/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-10
 * @version 0.1
 */
package com.eandroid.view.util;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.eandroid.util.EALog;

/**
 * A BitmapDrawable that keeps track of whether it is being displayed or cached.
 * When the drawable is no longer being displayed or cached,
 * {@link Bitmap#recycle() recycle()} will be called on this drawable's bitmap.
 */
public class RecyclingBitmapDrawable extends BitmapDrawable {

	static final String TAG = "RecyclingBitmapDrawable";

	private AtomicInteger mCacheRefCount = new AtomicInteger(0);
	private AtomicInteger mDisplayRefCount = new AtomicInteger(0);
	private boolean autoRecycle = true;

	private volatile boolean mHasBeenDisplayed;


	@SuppressWarnings("deprecation")
	public RecyclingBitmapDrawable(Bitmap bitmap) {
		super(bitmap);
	}

	public RecyclingBitmapDrawable(Resources res, Bitmap bitmap) {
		super(res, bitmap);
	}

	/**
	 * Notify the drawable that the displayed state has changed. Internally a
	 * count is kept so that the drawable knows when it is no longer being
	 * displayed.
	 *
	 * @param isDisplayed - Whether the drawable is being displayed or not
	 */
	public void setIsDisplayed(boolean isDisplayed) {
		if (isDisplayed) {
			mDisplayRefCount.incrementAndGet();
			mHasBeenDisplayed = true;
		} else {
			mDisplayRefCount.decrementAndGet();
		}

		// Check to see if recycle() can be called
		checkState();
	}

	/**
	 * Notify the drawable that the cache state has changed. Internally a count
	 * is kept so that the drawable knows when it is no longer being cached.
	 *
	 * @param isCached - Whether the drawable is being cached or not
	 */
	public void setIsCached(boolean isCached) {
		if (isCached) {
			mCacheRefCount.incrementAndGet();
		} else {
			mCacheRefCount.decrementAndGet();
		}

		// Check to see if recycle() can be called
		checkState();
	}

	private synchronized void checkState() {
		// If the drawable cache and display ref counts = 0, and this drawable
		// has been displayed, then recycle
		if(!autoRecycle)
			return;
		if (mCacheRefCount.get() <= 0 && mDisplayRefCount.get() <= 0 && mHasBeenDisplayed
				&& hasValidBitmap()) {
			EALog.d(TAG, "No longer being used or cached so recycling. "
					+ toString());
				getBitmap().recycle();
		}
	}

	private synchronized boolean hasValidBitmap() {
		Bitmap bitmap = getBitmap();
		return bitmap != null && !bitmap.isRecycled();
	}

	@Override
	protected void finalize() throws Throwable {
		EALog.i(TAG, "finalize");
		super.finalize();
	}

	public boolean isAutoRecycle() {
		return autoRecycle;
	}

	public void setAutoRecycle(boolean autoRecycle) {
		this.autoRecycle = autoRecycle;
	}

	
}
