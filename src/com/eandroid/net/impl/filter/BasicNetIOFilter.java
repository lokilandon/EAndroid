/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-26
 * @version 1.0.0
 */
package com.eandroid.net.impl.filter;

import com.eandroid.net.NetIOFilter;
import com.eandroid.net.Session;

public class BasicNetIOFilter implements NetIOFilter{

	@Override
	public void onSessionCreated(NextFilterSelector next, Session session){
		next.sessionCreated(session);
	}

	@Override
	public void onRead(NextFilterSelector next, Session session, Object message){
		next.read(session, message);
	}
	
	@Override
	public void onWrite(NextFilterSelector next, Session session, Object message){
		next.write(session, message);
	}

	@Override
	public void onCatchException(NextFilterSelector next, Session session,
			Exception exception) {
		next.catchException(session, exception);
	}

}
