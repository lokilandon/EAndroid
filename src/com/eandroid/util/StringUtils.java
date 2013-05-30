/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-9
 * @version 1.0.0
 */
package com.eandroid.util;

import java.util.Locale;


public class StringUtils {

	public static boolean isEmpty(String str){
		if(str == null || str.trim().length() == 0 
				|| "null".equals(str.toLowerCase(Locale.US))){
			return true;
		}
		return false;
	}

	public static boolean isNotEmpty(String str){
		return !isEmpty(str);
	}
}
