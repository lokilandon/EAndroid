/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-27
 * @version 0.1
 */
package com.eandroid.net.impl.http.filter;

import com.eandroid.net.Session;
import com.eandroid.net.http.RequestEntity;
import com.eandroid.net.http.ResponseEntity;
import com.eandroid.net.impl.filter.BasicNetIOFilter;

public class BasicHttpFilter extends BasicNetIOFilter{

	
	@Override
	@Deprecated
	public void onRead(NextFilterSelector next, Session session, Object message) {
		if(!(message instanceof ResponseEntity)){
			throw new IllegalArgumentException("Http filter only support to read ResponseEntity!");
		}
		onRead(next, session, (ResponseEntity)message);
	}
	
	public void onRead(NextFilterSelector next, Session session, ResponseEntity response) {
		super.onRead(next, session, response);
	}
	
	@Override
	@Deprecated
	public void onWrite(NextFilterSelector next, Session session, Object message){
		if(!(message instanceof RequestEntity)){
			throw new IllegalArgumentException("Http filter only support to write RequestEntity!");
		}
		onWrite(next, session, (RequestEntity)message);
	}
	
	public void onWrite(NextFilterSelector next, Session session, RequestEntity message){
		next.write( session, message);
	}
	
}
