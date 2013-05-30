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

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class AdapterViewEventListenerProxy implements OnItemClickListener,OnItemLongClickListener,OnItemSelectedListener{
	
	private String mInvokeMethodName;
	private Object mInvokeTarget;
	
	private AdapterViewEventListenerProxy(String invokeMethodName,Object invokeTarget){
		mInvokeMethodName = invokeMethodName;
		mInvokeTarget = invokeTarget;
	}
	
	public static AdapterViewEventListenerProxy newEventListenerProxy(String invokeMethodName,Object invokeTarget){
		return new AdapterViewEventListenerProxy(invokeMethodName, invokeTarget);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		try {
			Method method = mInvokeTarget.getClass().getMethod(
					mInvokeMethodName, AdapterView.class,View.class,Integer.class,Long.class);
			method.invoke(mInvokeTarget, parent,view,position,id);
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
	public void onNothingSelected(AdapterView<?> parent) {
		try {
			Method method = mInvokeTarget.getClass().getMethod(
					mInvokeMethodName, AdapterView.class);
			method.invoke(mInvokeTarget, parent);
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
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		try {
			Method method = mInvokeTarget.getClass().getMethod(
					mInvokeMethodName, AdapterView.class,View.class,Integer.class,Long.class);
			return (Boolean)method.invoke(mInvokeTarget, parent,view,position,id);
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
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		try {
			Method method = mInvokeTarget.getClass().getMethod(
					mInvokeMethodName, AdapterView.class,View.class,Integer.class,Long.class);
			method.invoke(mInvokeTarget, parent,view,position,id);
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
