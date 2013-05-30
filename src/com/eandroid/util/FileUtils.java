/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Observer;

import android.os.Handler;
import android.os.Message;
import android.os.StatFs;

public class FileUtils {

	public static boolean save(String folderPath,String fileName,InputStream in) {
		return save(folderPath+System.getenv(File.separator)+fileName, in,true,null);
	}

	public static boolean save(String folderPath,String fileName,InputStream in,boolean coverOnExists) {
		return save(folderPath+System.getenv(File.separator)+fileName, in,coverOnExists,null);
	}

	public static boolean save(String filePath,InputStream in) {
		return save(filePath, in,true,null);
	}

	public static boolean save(String filePath,InputStream in,Observer observer) {
		return save(filePath, in,true,observer);
	}

	public static boolean save(String filePath,InputStream in,boolean coverOnExists,Observer observer) {
		boolean result = false;
		File file = new File(filePath);
		if(file.exists() && !coverOnExists)
			return true;
		File parentFile = file.getParentFile();
		if(parentFile != null && !parentFile.exists())
			parentFile.mkdirs();
		BufferedOutputStream bos = null;
		BufferedInputStream bis = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(file),8*1024);
			bis = new BufferedInputStream(in,8*1024);
			byte[] buf = new byte[8 * 1024];
			long writeLength = 0;
			int readLength = 0;
			while((readLength = bis.read(buf)) != -1){
				bos.write(buf,0,readLength);
				writeLength += buf.length;
				if(observer != null)
					observer.update(null,writeLength);
			}
			bos.flush();
			result = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			result = false;
		} catch (IOException e) {
			e.printStackTrace();
			result = false;
		}finally{
			if(bis != null)
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if(bos != null)
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		if(!result && file.exists())
			file.delete();
		return result;
	}

	/**
	 * 异步保存文件
	 * @param filePath
	 * @param in
	 * @param coverOnExists
	 * @param observer
	 * @param callback
	 */
	public static void saveAsync(final String filePath,
			final InputStream in,
			final boolean coverOnExists,
			final Observer observer,
			final FileCallback callback) {			
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
					File file = null;
					if(res)
						file = new File(filePath);
					callback.result(res,filePath,file); 	//从网上加载到图片后的回调方法
				}else{
					callback.result(false,filePath,null);
				}
			}
		};
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				boolean res = FileUtils.save(filePath, in, coverOnExists,observer);
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
	public interface FileCallback {
		void result(boolean result,String path,File file);
	}
	
	public static long getUsableSpace(File path) {
    	try{
    		 final StatFs stats = new StatFs(path.getPath());
    	     return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    	}catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
       
    }

}
