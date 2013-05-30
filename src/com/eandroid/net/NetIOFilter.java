/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-20
 * @version 0.1
 */
package com.eandroid.net;



public interface NetIOFilter {
	
	public void onSessionCreated(NextFilterSelector next,Session session);
	public void onRead(NextFilterSelector next,Session session,Object message);
	public void onWrite(NextFilterSelector next,Session session,Object message);
	public void onCatchException(NextFilterSelector next,Session session,Exception exception);
	
	public interface NextFilterSelector{
		public void sessionCreated(Session session);
		public void read(Session session,Object message);
		public void write(Session session,Object message);		
		public void catchException(Session session,Exception exception);
	}
}
