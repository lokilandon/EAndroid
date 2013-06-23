/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 0.1
 */
package com.eandroid.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Observer;

public class IOUtils {

	public static String inputStream2String(InputStream in , Charset charset) throws IOException{
		return inputStream2String(in, charset, null);
	}
	public static String inputStream2String(InputStream in , Charset charset,Observer observer) throws IOException{
		BufferedReader br = null;
		if(charset == null)
			br = new BufferedReader(new InputStreamReader(in),8192);
		else {
			br = new BufferedReader(new InputStreamReader(in,charset),8192);
		}
		String line = null;
		StringBuilder sb = new StringBuilder();
		long readLength = 0;
		while((line = br.readLine()) != null){
			sb.append(line);
			readLength += line.getBytes().length;
			if(observer != null)
				observer.update(null, readLength);
		}
		String result = sb.toString();
		sb.delete(0, sb.length());
		return result;
	}

	public static byte[] inputStream2ByteArray(InputStream in){
		return inputStream2ByteArray(in, null);
	}
	public static byte[] inputStream2ByteArray(InputStream in,Observer observer){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int readLength = 0;
		int curReadLength;
		try {
			while ((curReadLength = in.read(buf)) != -1) {
				baos.write(buf,0,curReadLength);
				readLength += curReadLength;
				if(observer != null)
					observer.update(null, readLength);
			}
			return baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally{
			if(baos != null)
				try {
					baos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

}
