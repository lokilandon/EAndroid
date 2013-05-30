/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-15
 * @version 0.1
 */
package com.eandroid.database;

import java.lang.reflect.Method;

public class PrimaryKey extends Column{

	private boolean autoIncrement;

	public PrimaryKey(String name, boolean unique, Method getMethod,
			Method setMethod, Class<?> dataType,boolean autoIncrement) {
		super(name, unique, getMethod, setMethod, dataType);
		this.autoIncrement = autoIncrement;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public void setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
	}

}
