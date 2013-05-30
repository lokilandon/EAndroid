/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-28
 * @version 0.1
 */
package com.eandroid.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ClassUtils {

	public static Class<?> loadGenericSuperClass(Class<?> clazz){
		Class<?> genericType = null;
		Type type = clazz.getGenericSuperclass();
		if(type instanceof ParameterizedType){
			genericType = (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0];
		}
		return genericType;
	}
	
	
}
