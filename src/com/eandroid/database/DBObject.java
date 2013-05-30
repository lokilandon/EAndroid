/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-26
 * @version 1.0.0
 */
package com.eandroid.database;

import android.content.Context;

import com.eandroid.database.EasyDB.EasyDBConfig;

public class DBObject {
	
	public boolean save(Context context){
		return EasyDB.open(context).insert(this);
	}
	
	public boolean saveAndBindId(Context context){
		return EasyDB.open(context).insertAndBindId(this);
	}
	
	public boolean delete(Context context){
		return EasyDB.open(context).delete(this);
	}
	
	public boolean update(Context context){
		return EasyDB.open(context).update(this);
	}
	
	public boolean save(EasyDBConfig config){
		return EasyDB.open(config).insert(this);
	}
	
	public boolean saveAndBindId(EasyDBConfig config){
		return EasyDB.open(config).insertAndBindId(this);
	}
	
	public boolean delete(EasyDBConfig config){
		return EasyDB.open(config).delete(this);
	}
	
	public boolean update(EasyDBConfig config){
		return EasyDB.open(config).update(this);
	}

}
