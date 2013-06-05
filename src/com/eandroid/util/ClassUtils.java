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
		while(type != null){
			if(type instanceof ParameterizedType){
				try{
					Type[] _type = ((ParameterizedType)type).getActualTypeArguments();
					type = null;
					if(_type[0] instanceof Class<?>){
						genericType = (Class<?>)_type[0];
					}else{
						type = _type[0];
					}
				}catch(Exception e){
					genericType = null;
				}
			}else{
				type = null;
			}
		}

		return genericType;
	}


}
