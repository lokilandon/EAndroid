/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.net.http.util;

import java.util.Observable;
import java.util.Observer;

import com.eandroid.net.NetIOHandler;
import com.eandroid.net.Session;

public class ReadObserver implements Observer{
	
	private Session session;
	private NetIOHandler handler;
	private long totalLength;
	private int currentProgress;
	public ReadObserver(Session session,NetIOHandler handler,long totalLength){
		this.totalLength = totalLength;
		this.session = session;
		this.handler = handler;
	}

	@Override
	public void update(Observable observable, Object data) {
		if(totalLength > 0 && handler != null){
			int progress = (int)((Long)data*100/totalLength);
			if(progress > currentProgress)
				handler.onReadProgress(session, progress);
			currentProgress = progress;
		}
	}
	
}
