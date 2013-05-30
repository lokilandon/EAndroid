/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-20
 * @version 0.1
 */
package com.eandroid.net.impl.filter;

import java.io.IOException;

import com.eandroid.net.Session;

public class HeadFilter extends BasicNetIOFilter{

	@Override
	public void onWrite(NextFilterSelector next, Session session,
			Object message){
		try {
			session.getConnector().write(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
