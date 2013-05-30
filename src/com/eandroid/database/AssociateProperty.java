/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-15
 * @version 1.0.0
 */
package com.eandroid.database;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.eandroid.database.annotation.Associate.TYPE;


public class AssociateProperty {
	
	private TYPE type;//关联类型
	private Field field;
	private Method getMethod;
	private Method setMethod;
	
	private String pkNameOfOneInMany;
	private Column pkOfOneInMany;
	private Class<?> beAssociatedClazz;//关联的表对应的Class
	
	public AssociateProperty(TYPE type,String pkNameOfOneInMany,Class<?> beAssociatedClazz,Method getMethod,Method setMethod,Field field){
		this.type = type;
		this.pkNameOfOneInMany  = pkNameOfOneInMany;
		this.beAssociatedClazz = beAssociatedClazz;
		this.getMethod = getMethod;
		this.setMethod = setMethod;
		this.field = field;
	}
	
	public TYPE getType() {
		return type;
	}
	public void setType(TYPE type) {
		this.type = type;
	}

	public Class<?> getBeAssociatedClazz() {
		return beAssociatedClazz;
	}

	public void setBeAssociatedClazz(Class<?> beAssociatedClazz) {
		this.beAssociatedClazz = beAssociatedClazz;
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
	public String getPkNameOfOneInMany() {
		return pkNameOfOneInMany;
	}

	public void setPkNameOfOneInMany(String pkNameOfOneInMany) {
		this.pkNameOfOneInMany = pkNameOfOneInMany;
	}

	public Column getPkOfOneInMany() {
		return pkOfOneInMany;
	}

	public void setPkOfOneInMany(Column pkOfOneInMany) {
		this.pkOfOneInMany = pkOfOneInMany;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}
	public boolean setBeAssociatedObject(Object receiver,Object value){
		try {
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
	public Object getBeAssociatedObject(Object receiver){
		try {
			return getMethod.invoke(receiver, (Object[])null);
		} catch (IllegalArgumentException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		} catch (InvocationTargetException e) {
			return null;
		}
	}
}
