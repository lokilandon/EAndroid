/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 0.1
 */
package com.eandroid.net.impl.filter;

import com.eandroid.net.Session;
import com.eandroid.util.EALog;

public class LogFilter extends BasicNetIOFilter{
	
	boolean logSessionCreated;
	boolean logOnRead;
	boolean logOnWrite;
	boolean logException;
	private String TAG = "NetLogFilter";
	public LogFilter() {
		this(true, true, true, true);
	}
	public LogFilter(boolean logSessionCreated,boolean logOnRead,boolean logOnWrite,boolean logException){
		this.logSessionCreated = logSessionCreated;
		this.logOnRead = logOnRead;
		this.logOnWrite = logOnWrite;
		this.logException = logException;
	}

	@Override
	public void onSessionCreated(NextFilterSelector next, Session session){
		if(logSessionCreated)
			EALog.d(TAG+" onSessionCreated", session.toString());
		super.onSessionCreated(next, session);
	}

	@Override
	public void onRead(NextFilterSelector next, Session session, Object message){
		if(logOnRead)
			EALog.d(TAG+" onRead", message.toString());
		super.onRead(next, session, message);
	}

	@Override
	public void onWrite(NextFilterSelector next, Session session, Object message){
		if(logOnWrite)
			EALog.d(TAG+" onWrite", message.toString());
		super.onWrite(next, session, message);
	}

	@Override
	public void onCatchException(NextFilterSelector next, Session session,
			Exception exception) {
		if(logException){
			EALog.w(TAG+" onCatchException", exception.toString());
		}
		super.onCatchException(next, session, exception);
	}

}
