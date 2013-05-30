/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-15
 * @version 1.0.0
 */
package com.eandroid.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.eandroid.util.DateUtils;
import com.eandroid.util.StringUtils;


public class Column {

	protected String name;
	protected String defaultValue;
	protected boolean notNull;
	protected boolean unique;
	protected Method getMethod;
	protected Method setMethod;
	protected Class<?> dataType;

	public Column(String name,boolean unique,Method getMethod,Method setMethod,Class<?> dataType){
		this.name = name;
		this.unique = unique;
		this.getMethod = getMethod;
		this.setMethod = setMethod;
		this.dataType = dataType;
	}

	public String getDefaultValue() {
		return defaultValue;
	}
	public boolean hasDefaultValue(){
		return StringUtils.isNotEmpty(defaultValue);
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public boolean isNotNull() {
		return notNull;
	}

	public void setNotNull(boolean notNull) {
		this.notNull = notNull;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isUnique() {
		return unique;
	}
	public void setUnique(boolean unique) {
		this.unique = unique;
	}
	public boolean setValue(Object receiver,Object value){
		try {
			if(value == null)
				setMethod.invoke(receiver, (Object)null);

			if(dataType == String.class)
				setMethod.invoke(receiver, value.toString());
			else if(dataType == Integer.class || dataType == int.class)
				setMethod.invoke(receiver, Integer.parseInt(value.toString()));
			else if(dataType == Long.class || dataType == long.class)
				setMethod.invoke(receiver, Long.parseLong(value.toString()));
			else if(dataType == Double.class || dataType == double.class)
				setMethod.invoke(receiver, Double.parseDouble(value.toString()));
			else if(dataType == Float.class || dataType == float.class)
				setMethod.invoke(receiver, Float.parseFloat(value.toString()));
			else if(dataType == Boolean.class || dataType == boolean.class)
				setMethod.invoke(receiver, Boolean.parseBoolean(value.toString()));
			else if(dataType == java.util.Date.class)
				setMethod.invoke(receiver, DateUtils.string2Date_YYYY_MM_DD_HH_MM_SS(value.toString()));
			else if(dataType == java.sql.Date.class){
				java.util.Date date = DateUtils.string2Date_YYYY_MM_DD_HH_MM_SS(value.toString());
				if(date != null)
					setMethod.invoke(receiver, new java.sql.Date(date.getTime()));
			}else
				setMethod.invoke(receiver, value);
		} catch (IllegalArgumentException e) {
			return false;
		} catch (IllegalAccessException e) {
			return false;
		} catch (InvocationTargetException e) {
			return false;
		}
		return true;
	}
	
	public Object getValue(Object receiver){
		try {
			Object object = getMethod.invoke(receiver, (Object[])null);
			if(dataType == java.util.Date.class || dataType == java.sql.Date.class){
				return DateUtils.date2String_YYYY_MM_DD_HH_MM_SS((java.util.Date)object);
			}
			return object;
		} catch (IllegalArgumentException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		} catch (InvocationTargetException e) {
			return null;
		}
	}
	public Method getGetMethod() {
		return getMethod;
	}
	public void setGetMethod(Method getMethod) {
		this.getMethod = getMethod;
	}
	public Method getSetMethod() {
		return setMethod;
	}
	public void setSetMethod(Method setMethod) {
		this.setMethod = setMethod;
	}

	public Class<?> getDataType() {
		return dataType;
	}

	public void setDataType(Class<?> dataType) {
		this.dataType = dataType;
	}
	
	public boolean isIntegerDataType(){
		if(dataType == int.class
				|| dataType == Integer.class
				|| dataType == long.class
				|| dataType == Long.class){
			return true;
		}		
		return false;
	}	
	
	public boolean isNullValue(Object receiver){
		if((dataType == int.class 
				&& Integer.parseInt(getValue(receiver).toString()) == 0)
				|| (dataType == long.class 
				&& Long.parseLong(getValue(receiver).toString()) == 0)
				|| getValue(receiver) == null){
			return true;
		}
		return false;
	}
}
