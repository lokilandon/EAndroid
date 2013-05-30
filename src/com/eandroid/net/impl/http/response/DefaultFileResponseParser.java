/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.net.impl.http.response;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Observer;

import com.eandroid.content.EContext;
import com.eandroid.net.http.ResponseEntity.ResponseConfig;
import com.eandroid.net.http.response.FileResponseParser;
import com.eandroid.net.http.response.ResponseParseException;
import com.eandroid.util.CommonUtils;
import com.eandroid.util.FileUtils;

public class DefaultFileResponseParser implements FileResponseParser{
//	private static String TAG = "FileResponseParser";
	private final String tempFilePath = EContext.getTempFilePath() +"fileparser/";

	@Override
	public File parseObject(ResponseConfig<File> config, InputStream in,Charset defauCharset,
			Observer readObserver) throws ResponseParseException {
		File file = null;
		boolean res = false;

		if(config.isDownloadResponse()){
			res = FileUtils.save(config.getDownloadPath(), in, readObserver);
			if(!res)
				throw new ResponseParseException("Download failed.file save unsuccess.Check file's path access");
			file = new File(config.getDownloadPath());
			return file;
		}
		String tempFileSavePath = tempFilePath+CommonUtils.generateSequenceNo() + ".tmp";
		res = FileUtils.save(tempFileSavePath, in, readObserver);
		if(!res)
			throw new ResponseParseException("An error occured while parse file response."+config.toString());
		file = new File(tempFileSavePath);
		return file;
	}
}
