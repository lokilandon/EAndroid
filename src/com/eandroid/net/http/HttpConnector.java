/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-23
 * @version 1.0.0
 */
package com.eandroid.net.http;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;

import com.eandroid.net.Connector;

public interface HttpConnector extends Connector,RequestEntityGenerator,ResponseEntityGenerator{

	public void request(RequestEntity request) throws ClientProtocolException, IOException, HttpRequestException;
	
	public CookieStore getCookieStore();
	
	public void setCookieStore(CookieStore cookieStore);
	
}
