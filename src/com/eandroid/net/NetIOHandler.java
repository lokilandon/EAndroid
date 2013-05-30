/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-20
 * @version 0.1
 */
package com.eandroid.net;

public interface NetIOHandler {
	
	public void onSessionCreated(Session session);
	public void onRead(Session session,Object message);
	public void onReadProgress(Session session,int progress);
	public void onWriteProgress(Session session,int progress);
	public void onCatchException(Session session,Exception exception);
}
