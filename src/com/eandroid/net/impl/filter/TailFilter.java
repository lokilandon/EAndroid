/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-20
 * @version 1.0.0
 */
package com.eandroid.net.impl.filter;

import com.eandroid.net.Session;
import com.eandroid.util.EALog;

public class TailFilter extends BasicNetIOFilter{
	private static String TAG = "TailFilter";

	@Override
	public void onSessionCreated(NextFilterSelector next, Session session){
		try {
			if(session.getHandler() != null)
				session.getHandler().onSessionCreated(session);
		} catch (Exception e) {
			EALog.e(TAG, "Uncatch exception while handle response:"+e.getClass().getName());
			e.printStackTrace();
		}
		
	}

	@Override
	public void onRead(NextFilterSelector next, Session session, Object message){
		try{
		if(session.getHandler() != null)
			session.getHandler().onRead(session, message);
		}catch (Exception e) {
			EALog.e(TAG, "Uncatch exception while handle response:"+e.getClass().getName());
			e.printStackTrace();
		}
	}
	
	@Override
	public void onCatchException(NextFilterSelector next, Session session,
			Exception exception){
		try {
			if(session.getHandler() != null)
				session.getHandler().onCatchException(session, exception);
		} catch (Exception e) {
			EALog.e(TAG, "Uncatch exception while handle response:"+e.getClass().getName());
			e.printStackTrace();
		}
	}
}
