/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-20
 * @version 0.1
 */
package com.eandroid.net;

import java.io.IOException;


public interface Connector {

	public void write(Object message) throws IOException;
	
	public void close();
	
	public boolean isClosed();
}
