/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-2
 * @version 1.0.0
 */
package com.eandroid.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Observer;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

public class BitmapUtils {

//	public static int DEFAULT_MAX_DECODE_BITMAP_WIDTH = 320;
//	public static int DEFAULT_MAX_DECODE_BITMAP_HEIGHT = 480;
//	public static int DEFAULT_BITMAP_COMPRESS_QUANLITY = 80;
	public static CompressFormat DEFAULT_BITMAP_COMPRESS_FORMAT = CompressFormat.JPEG;
	
	/**
	 * 加载图片
	 * 返回的图片宽不大于320像素，高不大于480像素
	 * @param filePath 图片存放的文件路径
	 * @return
	 */
	public static Bitmap decode(String filePath){
		return decode(filePath, -1, -1,false);
	}

	/**
	 * 加载图片
	 * @param filePath 图片存放的文件路径
	 * @param decodeWidth 图片加载的宽度（当原始图片的宽度小于此宽度时，实际返回图片的宽度可能会小于此宽度）
	 * @param decodeHeight 图片加载的高度（当原始图片的高度小于此高度时，实际返回图片的高度可能会小于此高度）
	 * @param inNativeAlloc 图片内存是否占用dalvik heap。如果为true的话，内存不在存放再dalvik heap中，因此这部分内存也不受gc的管理。用户必须确保当图片不使用时立即调用recycle方法。
	 * @return
	 */
	public static Bitmap decode(String filePath,int decodeWidth,int decodeHeight,boolean inNativeAlloc){
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		//开始读入图片，此时把options.inJustDecodeBounds 设回true了
		newOpts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath,newOpts);//此时返回bm为空
		fillDefaultBitmapOptions(newOpts,decodeWidth,decodeHeight,null,inNativeAlloc);
		return BitmapFactory.decodeFile(filePath,newOpts);
	}
	
	/**
	 * 加载图片
	 * 返回的图片宽不大于320像素，高不大于480像素
	 * @param FileDescriptor 图片文件的描述
	 * @return
	 */
	public static Bitmap decode(FileDescriptor fd){
		return decode(fd, -1, -1,false);
	}

	/**
	 * 加载图片
	 * @param FileDescriptor 图片文件的描述
	 * @param decodeWidth 图片加载的宽度（当原始图片的宽度小于此宽度时，实际返回图片的宽度可能会小于此宽度）
	 * @param decodeHeight 图片加载的高度（当原始图片的高度小于此高度时，实际返回图片的高度可能会小于此高度）
	 * @param inNativeAlloc 图片内存是否占用dalvik heap。如果为true的话，内存不在存放再dalvik heap中，因此这部分内存也不受gc的管理。用户必须确保当图片不使用时立即调用recycle方法。
	 * @return
	 */
	public static Bitmap decode(FileDescriptor fd,int decodeWidth,int decodeHeight,boolean inNativeAlloc){
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		//开始读入图片，此时把options.inJustDecodeBounds 设回true了
		newOpts.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fd, null, newOpts);//此时返回bm为空
		fillDefaultBitmapOptions(newOpts,decodeWidth,decodeHeight,null,inNativeAlloc);
		Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fd, null, newOpts);
		return bitmap;
	}
	
	/**
	 * 加载图片
	 * @param in 图片输入流
	 * @return
	 */
	public static Bitmap decode(InputStream in){
		return decode(in, -1, -1,false,null);
	}

	/**
	 * 加载图片
	 * @param in 图片输入流
	 * @param 如果需要观察输入流的读取进度，可传入该参数
	 * @return
	 */
	public static Bitmap decode(InputStream in,Observer observer){
		return decode(in, -1, -1,false,observer);
	}

	/**
	 * 加载图片
	 * @param in 图片输入流
	 * @param decodeWidth 图片加载的宽度（当原始图片的宽度小于此宽度时，实际返回图片的宽度可能会小于此宽度）
	 * @param decodeHeight 图片加载的高度（当原始图片的高度小于此高度时，实际返回图片的高度可能会小于此高度）
	 * @param inNativeAlloc 图片内存是否占用dalvik heap。如果为true的话，内存不在存放再dalvik heap中，因此这部分内存也不受gc的管理。用户必须确保当图片不使用时立即调用recycle方法。
	 * @param 如果需要观察输入流的读取进度，可传入该参数
	 * @return
	 */
	public static Bitmap decode(InputStream in,int decodeWidth,int decodeHeight,boolean inNativeAlloc,Observer observer){
		byte[] bytes = IOUtils.inputStream2ByteArray(in,observer);
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		//开始读入图片，此时把options.inJustDecodeBounds 设回true了
		newOpts.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(bytes, 0, bytes.length, newOpts);
		fillDefaultBitmapOptions(newOpts,decodeWidth,decodeHeight,null,inNativeAlloc);
		Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, newOpts);
		return bitmap;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void fillDefaultBitmapOptions(Options options,
			int decodeWidth,int decodeHeight,Bitmap inBitmap,boolean inNativeAlloc){
		options.inJustDecodeBounds = false;
		options.inPreferredConfig = Config.ARGB_8888;
		//设置inPurgeable这个值可以让系统在回收内存的时候把图片 pixels 占用的内存(在c中分配的)回收掉。被回收的图片如果需要再次显示的话，系统会重新解码、载入。因为需要重新编码，因此需要保存原始的编码数据来支持。
		//inInputShareable这个参数就是设置输入的原始编码数据是否可共享，如果设置为true，那就会保持编码数据的引用，如果为false就需要进行深度拷贝。
		options.inPurgeable = false;
		options.inInputShareable = false;
		options.inSampleSize = calculateInSampleSize(options,decodeWidth,decodeHeight);;//设置缩放比例
		if(SystemUtils.hasHoneycomb()){
			options.inBitmap = inBitmap;
			options.inMutable = true;
		}
		try {
			if(inNativeAlloc)
				BitmapFactory.Options.class.getField("inNativeAlloc").setBoolean(options,true);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if ((height > reqHeight || width > reqWidth)
        		&& reqHeight > 0 && reqWidth > 0) {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).
            final float totalPixels = width * height;
            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }
	
	/**
	 * 保存Bitmap（JPEG格式、quanlity 80）
	 * @param filePath
	 * @param bmp 
	 * @return
	 */
	public static boolean save(String filePath,Bitmap bmp) {				
		return save(filePath, bmp, DEFAULT_BITMAP_COMPRESS_FORMAT, 100,false);
	}
	/**
	 * 保存Bitmap（JPEG格式）
	 * @param filePath
	 * @param bmp 
	 * @param quanlity The format of the compressed image quality  Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress
	 * @param coverOnExists 如果文件路径已存在，是否覆盖此文件
	 * @return
	 */
	public static boolean save(String filePath,Bitmap bmp,int quanlity) {				
		return save(filePath, bmp, DEFAULT_BITMAP_COMPRESS_FORMAT, quanlity,false);
	}

	/**
	 * 保存Bitmap
	 * @param filePath
	 * @param bmp 
	 * @param format quanlity for max quality. Some formats, like PNG which is lossless, will ignore the quality setting
	 * @param quanlity The format of the compressed image quality  Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress
	 * @return
	 */
	public static boolean save(String filePath,Bitmap bmp,CompressFormat format,int quanlity,boolean coverOnExists) {				
		final File f = new File(filePath); 
		File parentFile = f.getParentFile();
		if (!parentFile.exists()) { // 目录不存在建立目录
			parentFile.mkdirs();
		}

		FileOutputStream fos = null;
		try {
			if(!f.exists())
				f.createNewFile();
			else if(!coverOnExists)
				return true;
			fos = new FileOutputStream(filePath);
			bmp.compress(format, quanlity, fos);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}finally{
			if(fos != null){
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		return true;
	}

	/**
	 * 异步保存Bitmap
	 * @param filePath
	 * @param bmp 
	 * @param format quanlity for max quality. Some formats, like PNG which is lossless, will ignore the quality setting
	 * @param quanlity The format of the compressed image quality  Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress
	 * @return
	 */
	public static void saveAsync(final String filePath,
			final Bitmap bmp,
			final CompressFormat format,
			final int quanlity,
			final boolean coverOnExists,
			final ImageCallback callback) {				
		if(!coverOnExists){
			File file = new File(filePath);
			if(file.exists())
				return;
		}
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.obj != null) {
					boolean res = (Boolean)msg.obj;
					callback.result(res,filePath,bmp); 	//从网上加载到图片后的回调方法
				}else{
					callback.result(false,filePath,null);
				}
			}
		};
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				boolean res = BitmapUtils.save(filePath, bmp, format, quanlity, coverOnExists);
				Message msg = handler.obtainMessage();
				msg.obj = res;
				handler.sendMessage(msg);
			}
		};
		Thread t = new Thread(runnable);
		CommonThreadPool.execute(t);
	}

	/**
	 * 异步执行图片的 回调接口
	 */
	public interface ImageCallback {
		void result(boolean result,String path,Bitmap bitmap);
	}
	
	/**
	 * Get the size in bytes of a bitmap in a BitmapDrawable.
	 * @param value
	 * @return size in bytes
	 */
	@TargetApi(12)
	public static int getBitmapSize(Bitmap value) {
		if (SystemUtils.hasHoneycombMR1()) {
			return value.getByteCount();
		}
		// Pre HC-MR1
		return value.getRowBytes() * value.getHeight();
	}
}
