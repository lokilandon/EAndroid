/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-11
 * @version 1.0.0
 */
package com.eandroid.cache.impl;

import java.lang.ref.SoftReference;
import java.util.Iterator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import com.eandroid.cache.ReusableBitmapCache;
import com.eandroid.util.BitmapUtils;
import com.eandroid.util.SystemUtils;
import com.eandroid.view.util.RecyclingBitmapDrawable;

public class BitmapDrawableMemoryCache extends MemoryCache<String, BitmapDrawable> implements ReusableBitmapCache{

	/**
	 * Create a new BitmapDrawableMemoryCache object using the specified parameters. This should not be
	 * called directly by other classes, instead use
	 * {@link BitmapDrawableMemoryCache#getInstance()} to fetch an BitmapMemoryCacheLoader
	 * instance.
	 *
	 * @param cacheParams The cache parameters to use to initialize the cache
	 */
	public BitmapDrawableMemoryCache(MemoryCacheParams cacheParams) {
		super(cacheParams);
	}

	@Override
	protected void entryRemoved(boolean evicted, String key,
			BitmapDrawable oldValue, BitmapDrawable newValue) {
		if (RecyclingBitmapDrawable.class.isInstance(oldValue)) {
			// The removed entry is a recycling drawable, so notify it 
			// that it has been removed from the memory cache
			((RecyclingBitmapDrawable) oldValue).setIsCached(false);
		} else {
			// The removed entry is a standard BitmapDrawable

			if (SystemUtils.hasHoneycomb()) {
				// We're running on Honeycomb or later, so add the bitmap
				// to a SoftRefrence set for possible use with inBitmap later
				addBitmapToReusableSet(new SoftReference<Bitmap>(oldValue.getBitmap()));
			}
		}
	}

	@Override
	protected int sizeOf(String key, BitmapDrawable value) {
		final int bitmapSize = BitmapUtils.getBitmapSize(value.getBitmap()) / 1024;
		return bitmapSize == 0 ? 1 : bitmapSize;
	}
	

	/**
	 * @param options - BitmapFactory.Options with out* options populated
	 * @return Bitmap that case be used for inBitmap
	 */
	@Override
	public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
		if(options.inSampleSize <= 0 
				|| options.outHeight <= 0 || options.outWidth <= 0)
			return null;
		
		Bitmap bitmap = null;
		if (mReusableBitmaps != null && !mReusableBitmaps.isEmpty()) {
			final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
			Bitmap item;

			while (iterator.hasNext()) {
				item = iterator.next().get();
				
				if (null != item && item.isMutable()) {
					// Check to see it the item can be used for inBitmap
					if (canUseForInBitmap(item, options)) {
						bitmap = item;

						// Remove from reusable set so it can't be used again
						iterator.remove();
						break;
					}
				} else {
					// Remove from the set if the reference has been cleared.
					iterator.remove();
				}
			}
		}

		return bitmap;
	}
	
	@Override
	public void addBitmapToReusableSet(SoftReference<Bitmap> bitmapSR){
		if(mReusableBitmaps == null)
			return;
		mReusableBitmaps.add(bitmapSR);
	}
	
	/**
	 * @param candidate - Bitmap to check
	 * @param targetOptions - Options that have the out* value populated
	 * @return true if <code>candidate</code> can be used for inBitmap re-use with
	 *      <code>targetOptions</code>
	 */
	private static boolean canUseForInBitmap(
			Bitmap candidate, BitmapFactory.Options targetOptions) {
		int width = targetOptions.outWidth / targetOptions.inSampleSize;
		int height = targetOptions.outHeight / targetOptions.inSampleSize;

		return candidate.getWidth() == width && candidate.getHeight() == height;
	}
}
