/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-9
 * @version 0.1
 */
package com.eandroid.util;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;

import android.text.format.DateFormat;

public class CommonUtils {

	private final static NumberFormat numberFormat = new DecimalFormat("00");
	private static FieldPosition FIEDPOSITION = new FieldPosition(0);
	private static int seq = 0;
	private static final int MAX = 99;
	
	public synchronized static String generateSequenceNo(){
		String dateFormat = "yyyyMMdd_kkmmss_ms";
		
		String key = DateFormat.format(dateFormat,System.currentTimeMillis()).toString();
		StringBuffer sb = new StringBuffer(key);
		numberFormat.format(seq, sb, FIEDPOSITION);
		if (seq == MAX) {
			seq = 0;
		} else {
			seq++;
		}
		key = sb.toString();
		sb.delete(0, sb.length());
		EALog.d("key",key);
		return key;
	}
}
