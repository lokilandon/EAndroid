/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 0.1
 */
package com.eandroid.net.http.util;

import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HeaderElement;

import com.eandroid.util.EALog;
import com.eandroid.util.StringUtils;

public class HeadLoader {
	
	public static Charset loadContentTypeCharset(Header header){
		if(header == null)
			return null;
		Charset charset = null;
		String headerValue = header.getValue().trim();
		int index = headerValue.indexOf("charset");
		if(index > 0){
			try {
				charset = Charset.forName(headerValue.substring(
						index + 8,headerValue.length()));
			} catch (Exception e) {
				EALog.e("HeadLoader loadContentTypeCharset error", e.getMessage());
			}
		}
		return charset;
	}
	
	public static boolean isContentEncoding(Header header,String encodingType){
		if(header == null || StringUtils.isEmpty(encodingType))
			return false;
		
		for(HeaderElement e : header.getElements()){
			if(e.getName().equalsIgnoreCase(encodingType)){
				return true;
			}
		}
		return false;
	}

}
