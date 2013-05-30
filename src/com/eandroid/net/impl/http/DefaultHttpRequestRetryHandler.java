/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-24
 * @version 0.1
 */
package com.eandroid.net.impl.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.os.SystemClock;

public class DefaultHttpRequestRetryHandler implements HttpRequestRetryHandler{

	/** the number of times a method will be retried */
	private final int retryCount;

	/** Whether or not methods that have successfully sent their request will be retried */
	private final boolean requestSentRetryEnabled;

	private long retrySpacingTime;

	/**
	 * Default constructor
	 */
	public DefaultHttpRequestRetryHandler(int retryCount, boolean requestSentRetryEnabled,long retrySpacingTime) {
		super();
		this.retryCount = retryCount;
		this.requestSentRetryEnabled = requestSentRetryEnabled;
		this.retrySpacingTime = retrySpacingTime;
	}

	/**
	 * Default constructor
	 */
	public DefaultHttpRequestRetryHandler() {
		this(3, true,1);
	}
	/** 
	 * Used <code>retryCount</code> and <code>requestSentRetryEnabled</code> to determine
	 * if the given method should be retried.
	 */
	public boolean retryRequest(
			final IOException exception, 
			int executionCount,
			final HttpContext context) {
		if (exception == null) {
			throw new IllegalArgumentException("Exception parameter may not be null");
		}
		if (context == null) {
			throw new IllegalArgumentException("HTTP context may not be null");
		}
		boolean retry = true;
		if (executionCount > this.retryCount) {
			// Do not retry if over max retry count
			retry = false;
		}else if (exception instanceof NoHttpResponseException) {
			// Retry if the server dropped connection on us
			retry =  true;
		}else if (exception instanceof InterruptedIOException) {
			// Timeout
			retry = false;
		}else if (exception instanceof UnknownHostException) {
			// Unknown host
			retry =  false;
		}else if (exception instanceof SSLHandshakeException) {
			// SSL handshake exception
			retry =  false;
		}else{
			Boolean b = (Boolean)
					context.getAttribute(ExecutionContext.HTTP_REQ_SENT);
			boolean sent = (b != null && b.booleanValue());
			if (!sent || this.requestSentRetryEnabled) {
				// Retry if the request has not been sent fully or
				// if it's OK to retry methods that have been sent
				retry = true;
			}
		}

		if(retry)
			SystemClock.sleep(retrySpacingTime);
		return retry;
	}

	/**
	 * @return <code>true</code> if this handler will retry methods that have 
	 * successfully sent their request, <code>false</code> otherwise
	 */
	public boolean isRequestSentRetryEnabled() {
		return requestSentRetryEnabled;
	}

	/**
	 * @return the maximum number of times a method will be retried
	 */
	public int getRetryCount() {
		return retryCount;
	}
}
