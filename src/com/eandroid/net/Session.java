/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-20
 * @version 1.0.0
 */
package com.eandroid.net;

import com.eandroid.net.impl.NetIOHandlerDecorator;

public interface Session {
	
	public Connector getConnector();
	public NetIOHandler getHandler();
	public void decoratorHandler(NetIOHandlerDecorator handlerDecorator);
	public void write(Object message);
	public void read(Object message);
	public void catchExcepton(Exception e);
	public void close(); 
	public boolean isClosed(); 
}
