/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-12
 * @version 1.0.0
 */
package com.eandroid.cache.util;

import java.io.FileDescriptor;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.eandroid.cache.ReusableBitmapCache;
import com.eandroid.util.BitmapUtils;
import com.eandroid.util.SystemUtils;

public class BitmapCacheUtils {
	

	/**
     * Decode and sample down a bitmap from resources to the requested width and height.
     *
     * @param res The resources object containing the image data
     * @param resId The resource id of the image data
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decode(Resources res, int resId, ReusableBitmapCache cache) {
        return decode(res, resId, Integer.MAX_VALUE,Integer.MAX_VALUE,cache);
    }

    
	/**
     * Decode and sample down a bitmap from resources to the requested width and height.
     *
     * @param res The resources object containing the image data
     * @param resId The resource id of the image data
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decode(Resources res, int resId,
            int reqWidth, int reqHeight, ReusableBitmapCache cache) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // If we're running on Honeycomb or newer, try to use inBitmap
        Bitmap inBitmap = null;
        if (SystemUtils.hasHoneycomb() && cache != null) {
        	inBitmap = cache.getBitmapFromReusableSet(options);
        }
        BitmapUtils.fillDefaultBitmapOptions(options, reqWidth, reqHeight, inBitmap, false);
        return BitmapFactory.decodeResource(res, resId, options);
    }
    
    /**
     * Decode and sample down a bitmap from a file to the requested width and height.
     *
     * @param filename The full path of the file to decode
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decode(String filename,ReusableBitmapCache cache) {
        return decode(filename, Integer.MAX_VALUE,Integer.MAX_VALUE,cache);
    }
    
	/**
     * Decode and sample down a bitmap from a file to the requested width and height.
     *
     * @param filename The full path of the file to decode
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decode(String filename,
            int reqWidth, int reqHeight, ReusableBitmapCache cache) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // If we're running on Honeycomb or newer, try to use inBitmap
        Bitmap inBitmap = null;
        if (SystemUtils.hasHoneycomb() && cache != null) {
        	inBitmap = cache.getBitmapFromReusableSet(options);
        }
        BitmapUtils.fillDefaultBitmapOptions(options, reqWidth, reqHeight, inBitmap, false);
        return BitmapFactory.decodeFile(filename, options);
    }
    
    /**
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     *
     * @param fileDescriptor The file descriptor to read from
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decode(FileDescriptor fileDescriptor,  ReusableBitmapCache cache) {
    	return decode(fileDescriptor, Integer.MAX_VALUE, Integer.MAX_VALUE, cache);
    }

    /**
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     *
     * @param fileDescriptor The file descriptor to read from
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @param cache The ImageCache used to find candidate bitmaps for use with inBitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decode(
            FileDescriptor fileDescriptor, int reqWidth, int reqHeight, ReusableBitmapCache cache) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // If we're running on Honeycomb or newer, try to use inBitmap
        Bitmap inBitmap = null;
        if (SystemUtils.hasHoneycomb() && cache != null) {
        	inBitmap = cache.getBitmapFromReusableSet(options);
        }
        BitmapUtils.fillDefaultBitmapOptions(options, reqWidth, reqHeight, inBitmap, false);
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }


}
