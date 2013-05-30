/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-25
 * @version 1.0.0
 */
package com.eandroid.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

	private final static SimpleDateFormat sdfyymmddhhmmss = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault());

	public static String date2String_YYYY_MM_DD_HH_MM_SS(Date date){
		if(date == null)
			return null;
		return sdfyymmddhhmmss.format(date);
	}

	public static Date string2Date_YYYY_MM_DD_HH_MM_SS(String str){
		if(str == null)
			return null;
		
		try {
			return sdfyymmddhhmmss.parse(str);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}

