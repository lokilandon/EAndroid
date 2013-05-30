/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-12
 * @version 0.1
 */
package com.eandroid.cache;

import java.lang.ref.SoftReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

public interface ReusableBitmapCache extends Cache<String, BitmapDrawable>{

	public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options);
	
	public void addBitmapToReusableSet(SoftReference<Bitmap> bitmapSR);
}
