/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-10
 * @version 0.1
 */
package com.eandroid.view.util;

import java.util.concurrent.Future;

import android.content.res.Resources;
import android.graphics.Bitmap;

/**
 * A BitmapDrawable that keeps track of whether it is being displayed or cached.
 * When the drawable is no longer being displayed or cached,
 * {@link Bitmap#recycle() recycle()} will be called on this drawable's bitmap.
 */
public class AsyncBitmapDrawable extends RecyclingBitmapDrawable {

	static final String TAG = "AsyncBitmapDrawable";
	private Future<Bitmap> task;

    public AsyncBitmapDrawable(Resources res, Bitmap bitmap,Future<Bitmap> task) {
        super(res, bitmap);
        this.task = task;
    }

	public AsyncBitmapDrawable(Bitmap bitmap, Future<Bitmap> task) {
		super(bitmap);
		this.task = task;
	}

	public Future<Bitmap> getTask() {
		return task;
	}

	public void setTask(Future<Bitmap> task) {
		this.task = task;
	}
        
	
}
