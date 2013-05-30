/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-8
 * @version 0.1
 */
package com.eandroid.util;

import android.annotation.SuppressLint;
import java.security.MessageDigest;

public class EncoderUtils {
	
	public static String generateRequestKey(){
		return null;
	}
	
	public static String MD5HEX(String str){
		String returnStr;
		try {
			MessageDigest localMessageDigest = MessageDigest.getInstance("MD5");
			localMessageDigest.update(str.getBytes());
			returnStr = byteToHexString(localMessageDigest.digest());
			return returnStr;
		} catch (Exception e) {
			e.printStackTrace();
			return str;
		}
	}

	/**
	 * 将指定byte数组转换成16进制字符串
	 */
	@SuppressLint("DefaultLocale")
	private static String byteToHexString(byte[] b) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			hexString.append(hex.toUpperCase());
		}
		return hexString.toString();
	}
}
