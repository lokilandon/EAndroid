/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-9
 * @version 1.0.0
 */
package com.eandroid.ioc;

import java.lang.reflect.Field;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;

import com.eandroid.ioc.annotation.Autowired;
import com.eandroid.ioc.annotation.ContentView;
import com.eandroid.ioc.event.AdapterViewEventListenerProxy;
import com.eandroid.ioc.event.ViewEventListenerProxy;
import com.eandroid.util.StringUtils;

public class AutowireHelper {

	/**
	 * 自动装载Activity的ContentView
	 *  
	 * @param activity 要装载的activity
	 * @return boolean  true 自动装载成功 false 自动装载失败
	 */
	public static boolean autowireContentView(Activity activity){
		if(activity == null)
			return false;
		
		if(activity.getClass().isAnnotationPresent(ContentView.class)){
			ContentView contentView = activity.getClass().getAnnotation(ContentView.class);
			try {
				activity.setContentView(contentView.value());
				return true;
			} catch (Exception e) {
				throw new RuntimeException("an error occured while autowire contentView(id:"+contentView.value()+")");
			}
		}
		return false;
	}
	
	/**
	 * 自动装载Activity的属性
	 * @param activity 要装载的activity
	 * @return boolean true 自动装载成功 false 自动装载失败
	 */
	public static boolean autowireField(Activity activity) {
		if(activity == null)
			return false;
		
		Field[] fields = activity.getClass().getDeclaredFields();
		boolean autowiredResult = false;
		for(Field field : fields){
			field.setAccessible(true);
			if(field.isAnnotationPresent(Autowired.class)
//					&& View.class.isAssignableFrom(field.getType()) //判断Field是否是View的子类，为了提升性能取消注释，若不是则抛出异常。
					){
				Autowired autowired = field.getAnnotation(Autowired.class);
				if(autowired == null)
					continue;
				View fieldValue = activity.findViewById(autowired.value());
				if(fieldValue == null)
					throw new RuntimeException("an error occured while autowire view(id:"+autowired.value()+")");
				try {
					field.set(activity, fieldValue);
				}catch (IllegalAccessException e) {
					throw new RuntimeException("an error occured while autowired argument " 
							+ field.getName(), e);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException("an error occured while autowired argument " 
							+ field.getName(), e);
				}

				if(StringUtils.isNotEmpty(autowired.clickMethod())){
					ViewEventListenerProxy clickListenerProxy = 
							ViewEventListenerProxy.newEventListenerProxy(autowired.clickMethod(), activity);
					fieldValue.setOnClickListener(clickListenerProxy);
				}

				if(StringUtils.isNotEmpty(autowired.longClickMethod())){
					ViewEventListenerProxy longClickListenerProxy = 
							ViewEventListenerProxy.newEventListenerProxy(autowired.longClickMethod(), activity);
					fieldValue.setOnLongClickListener(longClickListenerProxy);
				}

				if(StringUtils.isNotEmpty(autowired.focusChangedMethod())){
					ViewEventListenerProxy onFocusChangeListenerProxy = 
							ViewEventListenerProxy.newEventListenerProxy(autowired.focusChangedMethod(), activity);
					fieldValue.setOnFocusChangeListener(onFocusChangeListenerProxy);
				}

				if(StringUtils.isNotEmpty(autowired.touchMethod())){
					ViewEventListenerProxy onTouchListenerProxy = 
							ViewEventListenerProxy.newEventListenerProxy(autowired.touchMethod(), activity);
					fieldValue.setOnTouchListener(onTouchListenerProxy);
				}

				if(StringUtils.isNotEmpty(autowired.itemClickMethod())){
					checkAdapterView(fieldValue);
					AdapterViewEventListenerProxy eventListener = 
							AdapterViewEventListenerProxy.newEventListenerProxy(autowired.itemClickMethod(), activity);
					((AdapterView<?>)fieldValue).setOnItemClickListener(eventListener);
				}

				if(StringUtils.isNotEmpty(autowired.itemLongClickMethod())){
					checkAdapterView(fieldValue);
					AdapterViewEventListenerProxy eventListener = 
							AdapterViewEventListenerProxy.newEventListenerProxy(autowired.itemLongClickMethod(), activity);
					((AdapterView<?>)fieldValue).setOnItemLongClickListener(eventListener);
				}

				if(StringUtils.isNotEmpty(autowired.itemSelectedMethod())){
					checkAdapterView(fieldValue);
					AdapterViewEventListenerProxy eventListener = 
							AdapterViewEventListenerProxy.newEventListenerProxy(autowired.itemSelectedMethod(), activity);
					((AdapterView<?>)fieldValue).setOnItemSelectedListener(eventListener);
				}
				autowiredResult = true;
			}
		}
		return autowiredResult;
	}

	private static void checkAdapterView(View v){
		if(v instanceof AdapterView<?>){
			throw new RuntimeException("an error occured while eventlistener autowire,View(id:"+v.getId()+") is not a AdapterView");
		}
	}

}
