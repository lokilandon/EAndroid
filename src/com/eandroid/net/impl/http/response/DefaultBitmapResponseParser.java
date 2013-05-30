/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.net.impl.http.response;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Observer;

import android.graphics.Bitmap;

import com.eandroid.content.EContext;
import com.eandroid.net.http.ResponseEntity.ResponseConfig;
import com.eandroid.net.http.response.BitmapResponseParser;
import com.eandroid.net.http.response.ResponseParseException;
import com.eandroid.util.BitmapUtils;
import com.eandroid.util.CommonUtils;
import com.eandroid.util.EALog;
import com.eandroid.util.FileUtils;

public class DefaultBitmapResponseParser implements BitmapResponseParser{
	private static final String TAG = "DefaultBitmapResponseParser";
	@Override
	public Bitmap parseObject(ResponseConfig<Bitmap> config, InputStream in,Charset defauCharset,
			Observer readObserver) throws ResponseParseException {
		if(in instanceof FileInputStream){
			FileInputStream fis = (FileInputStream)in;
			try {
				FileDescriptor fd = fis.getFD();
				Bitmap bitmap = BitmapUtils.decode(fd);
				if(bitmap != null)
					return bitmap;
			} catch (IOException e) {
				EALog.e(TAG, "parseObject - "+e);
			}
		}
		String tempFileSavePath = EContext.getTempFilePath() + CommonUtils.generateSequenceNo() + ".tmp";
		boolean res = FileUtils.save(tempFileSavePath, in, readObserver);
		if(!res)
			throw new ResponseParseException("An error occured while parse bitmap response."+config.toString());
		
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {}
		
		Bitmap bitmap = BitmapUtils.decode(tempFileSavePath);
		if(bitmap == null)
			throw new ResponseParseException("An error occured while parse bitmap response."+config.toString());
		
		return bitmap;
	}
}

