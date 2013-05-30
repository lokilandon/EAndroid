/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-29
 * @version 1.0.0
 */
package com.eandroid.net.impl;

import com.eandroid.net.NetIOHandler;
import com.eandroid.net.Session;

public class NetIOHandlerDecorator implements NetIOHandler{

	protected NetIOHandler decoratedHandler;

	public NetIOHandlerDecorator(NetIOHandler handler) {
		this.decoratedHandler = handler;
	}
	
	public void setDecoratedHandler(NetIOHandler decoratedHandler) {
		this.decoratedHandler = decoratedHandler;
	}

	@Override
	public void onSessionCreated(Session session) {
		if(decoratedHandler != null)
		decoratedHandler.onSessionCreated(session);
	}

	@Override
	public void onRead(Session session, Object message) {
		if(decoratedHandler != null)
			decoratedHandler.onRead(session, message);
	}

	@Override
	public void onReadProgress(Session session, int progress) {
		if(decoratedHandler != null)
			decoratedHandler.onReadProgress(session, progress);
	}

	@Override
	public void onWriteProgress(Session session, int progress) {
		if(decoratedHandler != null)
			decoratedHandler.onWriteProgress(session, progress);
	}

	@Override
	public void onCatchException(Session session, Exception exception) {
		if(decoratedHandler != null)
			decoratedHandler.onCatchException(session, exception);
	}


}
