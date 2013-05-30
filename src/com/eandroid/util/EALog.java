/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 1.0.0
 */
package com.eandroid.util;

import android.util.Log;

public class EALog {

	public static final int ERROR =1;
	public static final int WRAN =2;
	public static final int INFO =3;
	public static final int DEBUG =4;
	public static final int VERBOSE =5;
	
	public static int TAG_Filter =6;
	
	public static void e(String tag,String msg){
		if(TAG_Filter>=ERROR){
			Log.e(tag, String.valueOf(msg));
		}
	}
	public static void w(String tag,String msg){
		if(TAG_Filter>=WRAN){
			Log.w(tag, String.valueOf(msg));
		}
	}
	public static void i(String tag,String msg){
		if(TAG_Filter>=INFO){
			Log.i(tag, String.valueOf(msg));
		}
	}
	public static void d(String tag,String msg){
		if(TAG_Filter>=DEBUG){
			Log.d(tag, String.valueOf(msg));
		}
	}
	public static void v(String tag,String msg){
		if(TAG_Filter>=VERBOSE){
			Log.v(tag, String.valueOf(msg));
		}
	}
}
