/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-2
 * @version 0.1
 */
package com.eandroid.content;

import java.io.File;

import com.eandroid.util.EALog;

import android.os.Environment;

public class EContext {

	public final static String TEMP_RELATIVE_PATH = "eandroid/temp/";
	public final static String CACHE_RELATIVE_PATH = "eandroid/cache/";
	

	public static String getTempFilePath(){
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
					+ EContext.TEMP_RELATIVE_PATH;
		}else {
			return Environment.getDownloadCacheDirectory().getAbsolutePath() + File.separator
					+ EContext.TEMP_RELATIVE_PATH;
		}
	}
	
	public static String getCacheFilePath(){
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
					+ EContext.CACHE_RELATIVE_PATH;
		}else {
			return Environment.getDownloadCacheDirectory().getAbsolutePath() + File.separator
					+ EContext.CACHE_RELATIVE_PATH;
		}
	}
	
	public static void debug(boolean debug){
		if(debug)
			EALog.TAG_FILTER = EALog.DEBUG;
		else
			EALog.TAG_FILTER = EALog.ERROR;
	}
}
