/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-15
 * @version 1.0.0
 */
package com.eandroid.database;

import java.lang.reflect.Method;



/**
 * 外键
 * @author Kain
 *
 */
public class ForeignKey extends Column{

	private Class<?> mainTableClazz;//主表对应的Class
	private Table mainTable;//主表

	public ForeignKey(String name, boolean unique, Method getMethod,
			Method setMethod, Class<?> dataType,Class<?> mainTableClazz) {
		super(name, unique, getMethod, setMethod, dataType);
		this.mainTableClazz = mainTableClazz;
	}

	public Class<?> getMainTableClazz() {
		return mainTableClazz;
	}

	public void setMainTableClazz(Class<?> mainTableClazz) {
		this.mainTableClazz = mainTableClazz;
	}

	public Table getMainTable() {
		return mainTable;
	}

	public void setMainTable(Table mainTable) {
		this.mainTable = mainTable;
	}

}
