/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-22
 * @version 0.1
 */
package com.eandroid.database;

import java.util.ArrayList;
import java.util.List;


public class SQL {

	private String sql;
	private List<Object> bindArgs = new ArrayList<Object>();

	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	public List<Object> getBindArgs() {
		return bindArgs;
	}
	public void setBindArgs(List<Object> bindArgs) {
		this.bindArgs = bindArgs;
	}

	public void addBindArg(Object arg){
		if(arg == null)
			return;
		bindArgs.add(arg);
	}

	public Object[] getBindArgsAsArray(){
		return bindArgs.toArray();
	}

	public String[] getBindArgsAsStringArray() {
		if(bindArgs ==null){
			return null;
		}
		String[] strArray = new String[bindArgs.size()];
		for(int i = 0;i<bindArgs.size();i++){
			strArray[i]=bindArgs.get(i).toString();
		}
		return strArray;
	}
}
