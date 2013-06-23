/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.net.impl.http.filter;

import com.eandroid.net.Session;
import com.eandroid.net.http.RequestEntity;
import com.eandroid.net.http.ResponseEntity;
import com.eandroid.util.EALog;

public class HttpLogFilter extends BasicHttpFilter{

	boolean logSessionCreated;
	boolean logOnRead;
	boolean logOnWrite;
	boolean logException;
	private String TAG = "HttpLog";
	public HttpLogFilter() {
		this(true, true, true, true);
	}
	public HttpLogFilter(boolean logSessionCreated,boolean logOnRead,boolean logOnWrite,boolean logException){
		this.logSessionCreated = logSessionCreated;
		this.logOnRead = logOnRead;
		this.logOnWrite = logOnWrite;
		this.logException = logException;
	}

	@Override
	public void onRead(NextFilterSelector next, Session session,
			ResponseEntity response) {
		try{
			if(logOnRead){
				if(response.isCache())
					EALog.d(TAG+" cache", response.toString());
				else
					EALog.d(TAG+" response", response.toString());
			}
		}catch (Exception e) {
		}
		super.onRead(next, session, response);
	}
	@Override
	public void onWrite(NextFilterSelector next, Session session,
			RequestEntity message) {
		try{
			if(logOnWrite){
				EALog.i(TAG+" request",message.toString());
			}
		}catch (Exception e) {
		}
		super.onWrite(next, session, message);
	}
	@Override
	public void onSessionCreated(NextFilterSelector next,
			Session session) {
		try{
			if(logSessionCreated)
				EALog.d(TAG+" start",session.toString());
		}catch (Exception e) {
		}
		super.onSessionCreated(next, session);
	}
	@Override
	public void onCatchException(NextFilterSelector next,
			Session session, Exception exception) {
		try{
			if(logException){
				EALog.w(TAG+" error",exception.toString());
			}
		}catch (Exception e) {
		}
		super.onCatchException(next, session, exception);
	}
}
