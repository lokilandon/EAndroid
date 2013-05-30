/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-9
 * @version 0.1
 */
package com.eandroid.ioc.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;

public class ViewEventListenerProxy implements OnClickListener,OnLongClickListener,OnTouchListener,OnFocusChangeListener{
	
	private String mInvokeMethodName;
	private Object mInvokeTarget;
	
	private ViewEventListenerProxy(String invokeMethodName,Object invokeTarget){
		mInvokeMethodName = invokeMethodName;
		mInvokeTarget = invokeTarget;
	}
	
	public static ViewEventListenerProxy newEventListenerProxy(String invokeMethodName, Object invokeTarget){
		return new ViewEventListenerProxy(invokeMethodName,invokeTarget);
	}

	@Override
	public void onClick(View v) {
		try {
			Method method = mInvokeTarget.getClass().getMethod(mInvokeMethodName, View.class);
			method.invoke(mInvokeTarget, v);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("no such method exceotion:" + mInvokeMethodName ,e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean onLongClick(View v) {
		try {
			Method method = mInvokeTarget.getClass().getMethod(mInvokeMethodName, View.class);
			return (Boolean)method.invoke(mInvokeTarget, v);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("no such method exceotion:" + mInvokeMethodName ,e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		try {
			Method method = mInvokeTarget.getClass().getMethod(mInvokeMethodName, View.class, MotionEvent.class);
			return (Boolean)method.invoke(mInvokeTarget, v, event);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("no such method exceotion:" + mInvokeMethodName ,e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		try {
			Method method = mInvokeTarget.getClass().getMethod(mInvokeMethodName, View.class, Boolean.class);
			method.invoke(mInvokeTarget, v, hasFocus);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("no such method exceotion:" + mInvokeMethodName ,e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

}
