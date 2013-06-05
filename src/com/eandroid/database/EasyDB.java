/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-15
 * @version 0.1
 */
package com.eandroid.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import com.eandroid.database.annotation.Associate;
import com.eandroid.database.annotation.Associate.TYPE;
import com.eandroid.database.util.CursorUtils;
import com.eandroid.database.util.SQLog;
import com.eandroid.database.util.SqlBuilder;
import com.eandroid.util.EALog;
import com.eandroid.util.StringUtils;

public class EasyDB {

	private final static String TAG = "EasyDB";
	private static Map<String, EasyDB> dbMap = new HashMap<String, EasyDB>();

	private SQLiteOpenHelper dbHelper;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private EasyDB(EasyDBConfig config){
		dbHelper = new EasyDbOpenHelper(config.getContext(),
				config.getName(),config.getFactory(),config.getVersion(),config.getDbListener());
		//		if(SystemUtils.hasJellyBean()){
		//			dbHelper.getWritableDatabase().setForeignKeyConstraintsEnabled(true);
		//		}

	}

	public static EasyDB open(Context context){
		EasyDBConfig config = getDefaultDBConfig(context);
		EasyDB db = dbMap.get(config.getName());
		if(db == null){
			synchronized (EasyDB.class) {
				db = new EasyDB(config);
				dbMap.put(config.getName(), db);
			}
		}
		return db;
	}

	public static EasyDB open(EasyDBConfig config){
		EasyDB db = dbMap.get(config.getName());
		if(db == null){
			synchronized (EasyDB.class) {
				db = new EasyDB(config);
				dbMap.put(config.getName(), db);
			}
		}
		return db;
	} 

	private static EasyDBConfig getDefaultDBConfig(Context context){
		return new EasyDBConfig().setContext(context);
	}

	public boolean insert(Object entity) {
		try{
			return insert(entity, false);
		}catch (SQLException e) {
			EALog.w(TAG, e.getMessage());
		}
		return false;
	}

	public boolean insertOrThrow(Object entity) {
		return insert(entity, false);
	}

	public boolean insertAndBindId(Object entity) {
		try{
			return insert(entity, true);
		}catch (SQLException e) {
			EALog.w(TAG, e.getMessage());
		}
		return false;
	}

	public boolean insertAndBindIdOrThrow(Object entity) {
		return insert(entity, true);
	}

	private boolean insert(Object entity,boolean bindRowIdInPk){
		if(entity == null)
			return false;

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		boolean transcationEnable = false;
		try{
			Table table = TableFactory.getTable(entity.getClass());
			if(!checkTableState(table))
				return false;
			List<ForeignKey> fks = table.getFks();
			if(fks != null && !fks.isEmpty()){
				for(ForeignKey fk:fks){
					AssociateProperty associate = table.getAssociateByForeignKey(fk);
					if(associate == null){
						continue;
					}
					Object beAssociateEntity = associate.getBeAssociatedObject(entity);
					if(beAssociateEntity == null)
						continue;

					if(!db.inTransaction()){
						db.beginTransaction();//如果有外键关联的对象需要插入，开启事物
						transcationEnable = true;
					}
					PrimaryKey associatedTablePK = TableFactory.getTable(associate.getBeAssociatedClazz()).getPk();
					boolean _bindRowIdInPk = false; 
					//如果要外键关联的对象插入时，主键为空，则设置插入时自动绑定rowId到对象的主键
					if(associatedTablePK.isNullValue(beAssociateEntity)){
						_bindRowIdInPk = true;
					}
					try {
						if(!insert(beAssociateEntity,_bindRowIdInPk))
							return false;
					} catch (SQLiteConstraintException e) {
						//TODO 查询beAssociateEntity是否因为主键已存在造成插入失败，如果是的话忽略异常，继续执行
						boolean pkConflict = true;
						if(!pkConflict){
							throw e;
						}else{
							EALog.w(TAG, e.toString());
						}
					}
					//如果外键为空，则将刚刚插入对象的主键赋值给外键
					if(fk.isNullValue(entity)){
						fk.setValue(entity, associatedTablePK.getValue(beAssociateEntity));
					}
				}
			}

			ContentValues contentValues = SqlBuilder.generateContentValues(entity);
			if(contentValues == null)
				return false;
			SQLog.insert(TAG, table.getName(), contentValues);
			long id = db.insertOrThrow(table.getName(), null, contentValues);
			if(id != -1 && bindRowIdInPk){
				//只有int或者long的主键类型，允许插入之前设置为null值，让sqlite自动赋值主键
				//其他类型都必须事先设置好主键再进行插入，因此不需要使用insertAndBindId()方法插入数据。
				if(table.getPk().isIntegerDataType())
					table.getPk().setValue(entity, id);
			}
			if(transcationEnable)
				db.setTransactionSuccessful();
			return true;
		}finally{
			if(transcationEnable)
				db.endTransaction();
		}
	}

	public boolean insertWithAssociate(Object entity){
		if(entity == null)
			return false;
		try{
			return insertWithAssociateOrThrow(entity);                                                                   
		}catch (SQLException e) {
			EALog.w(TAG, e.toString());
		}
		return false;
	}

	public boolean insertWithAssociateOrThrow(Object entity){
		if(entity == null)
			return false;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		boolean res = false;
		try{
			res = insertWithAssociate(entity,null);
			if(res)
				db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		return res;
	}

	/**
	 * 将entity插入数据库（如果存在关联关系，会一并插入）<br/>
	 * 如果数据库表不存在会自动创建,插入后如果主键是Autoincrement并且插入前为null,则会将rowId绑定到主键上。
	 * 如果设置throwException为false时，一个对象插入失败，将不会继续插入他的关联对象，但不会终止和他同层次对象的插入。
	 * 比如A类的对象a 关联了 2个实例对象为b、c分别对应为B类，和C类
	 * 当a插入失败时，不会继续插入b、c
	 * 但当b插入失败时，会继续尝试插入c
	 * @param entity 要插入数据的对象
	 * @param throwException 设置为true 将会抛出SQLException异常 设置为false不会抛出
	 * @return
	 */
	private boolean insertWithAssociate(Object entity,Set<Object> insertCache){
		if(entity == null)
			return false;
		if(insertCache == null){
			insertCache = new HashSet<Object>();
			insertCache.add(entity);
		}
		boolean res = true;
		Table table = TableFactory.getTable(entity.getClass());
		List<AssociateProperty> associates = table.getAssociates();
		if(associates != null && !associates.isEmpty()){
			for(AssociateProperty ap: associates){
				if(ap.getType() == Associate.TYPE.MANY_TO_ONE){
					if(ap.getPkOfOneInMany() instanceof ForeignKey)
						continue;
					Object beAssociatedObject = ap.getBeAssociatedObject(entity);
					if(beAssociatedObject == null)
						continue;
					if(!insertCache.contains(beAssociatedObject)){
						insertCache.add(beAssociatedObject);
						res &= insertWithAssociate(beAssociatedObject, insertCache);
					}
					if(ap.getPkOfOneInMany().isNullValue(entity)){
						Table beAssociatedTable = TableFactory.getTable(ap.getBeAssociatedClazz());
						ap.getPkOfOneInMany().setValue(entity, beAssociatedTable.getPk().getValue(beAssociatedObject));
					}
				}
			}
			res &= insertAndBindIdOrThrow(entity);
			for(AssociateProperty ap: associates){
				if(ap.getType() == Associate.TYPE.ONE_TO_MANY){
					List<?> beAssociatedListEntity = (List<?>)ap.getBeAssociatedObject(entity);
					if(beAssociatedListEntity == null)
						continue;
					for(Object beAssociatedObject : beAssociatedListEntity){
						if(ap.getPkOfOneInMany().isNullValue(beAssociatedObject)){
							ap.getPkOfOneInMany().setValue(beAssociatedObject, table.getPk().getValue(entity));
						}
						if(!insertCache.contains(beAssociatedObject)){
							insertCache.add(beAssociatedObject);
							res &= insertWithAssociate(beAssociatedObject, insertCache);
						}

					}	
				}
			}
		}else {
			res &= insertAndBindIdOrThrow(entity);
		}
		return res;
	}

	public <T> T queryById(Object id,Class<T> clazz){
		if(id == null || clazz == null)
			return null;

		Table table = TableFactory.getTable(clazz);
		if(table == null)
			return null;

		SQL sql = SqlBuilder.buildSelectSql(clazz, id);
		SQLog.d(TAG, sql);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		if(sql != null){
			Cursor cursor = db.rawQuery(sql.getSql(), sql.getBindArgsAsStringArray());
			try{
				T t = CursorUtils.getEntity(cursor, clazz);
				return t;
			}finally {
				if(cursor != null)
					cursor.close();
			}
		}
		return null;
	}

	public <T> T queryByIdWithAssociate(Object id,Class<T> clazz){
		return queryByIdWithAssociate(id, clazz, null);
	}

	@SuppressWarnings("unchecked")
	private <T> T queryByIdWithAssociate(Object id,Class<T> clazz,Map<String,Object> queryCache){
		if(id == null || clazz == null)
			return null;
		if(queryCache == null)
			queryCache = new HashMap<String,Object>();

		Table table = TableFactory.getTable(clazz);
		String queryId = generateQueryId(table, id);
		if(queryCache.containsKey(queryId)){
			return (T)queryCache.get(queryId);
		}
		T t = queryById(id, clazz);
		if(t == null)
			return null;
		queryCache.put(queryId, t);

		List<AssociateProperty> associates = table.getAssociates();
		if(associates != null && !associates.isEmpty()){
			for(AssociateProperty ap: associates){
				if(ap.getType() == TYPE.MANY_TO_ONE){
					Object pkValueOfOneInMany = ap.getPkOfOneInMany().getValue(t);
					if(pkValueOfOneInMany == null){
						continue;
					}

					Class<?> beAssociatedClazz = ap.getBeAssociatedClazz();
					Object object = queryByIdWithAssociate(pkValueOfOneInMany, beAssociatedClazz, queryCache);
					ap.setBeAssociatedObject(t, object);
				}else if(ap.getType() == TYPE.ONE_TO_MANY){
					Class<?> beAssociatedClazz = ap.getBeAssociatedClazz();
					List<?> list = queryAllWithAssociate(beAssociatedClazz, 
							ap.getPkNameOfOneInMany() + "=" +id, 
							null, 
							null,
							queryCache);
					ap.setBeAssociatedObject(t, list);
				}
			}
		}
		return t;
	}

	public <T> List<T> queryAll(Class<T> clazz,String where){
		return queryAll(clazz, where, null,null);
	}

	public <T> List<T> queryAll(Class<T> clazz,String where,String limit){
		return queryAll(clazz, where, limit,null);
	}

	public <T> List<T> queryAll(Class<T> clazz,String where,String limit,String orderBy){
		if(clazz == null)
			return null;

		Table table = TableFactory.getTable(clazz);
		if(table == null)
			return null;

		SQL sql = SqlBuilder.buildSelectSql(clazz, where,limit,orderBy);
		SQLog.d(TAG, sql);
		if(sql != null){
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor cursor = db.rawQuery(sql.getSql(), sql.getBindArgsAsStringArray());
			try{
				List<T> ts = CursorUtils.getListEntity(cursor, clazz);
				return ts;
			}finally {
				if(cursor != null)
					cursor.close();
			}
		}
		return null;
	}

	public <T> List<T> queryAllWithAssociate(Class<T> clazz,String where){
		return queryAllWithAssociate(clazz, where, null, null, null);
	}

	public <T> List<T> queryAllWithAssociate(Class<T> clazz,String where,String limit){
		return queryAllWithAssociate(clazz, where, limit, null, null);
	}

	public <T> List<T> queryAllWithAssociate(Class<T> clazz,String where,String limit,String orderBy){
		return queryAllWithAssociate(clazz, where, limit, orderBy, null);
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> queryAllWithAssociate(Class<T> clazz,String where,String limit,String orderBy,Map<String,Object> queryCache){
		if(clazz == null)
			return null;
		if(queryCache == null)
			queryCache = new HashMap<String,Object>();

		Table table = TableFactory.getTable(clazz);
		String queryId = generateQueryId(table, where,limit,orderBy);
		if(queryCache.containsKey(queryId)){
			return (List<T>) queryCache.get(queryId);
		}
		List<T> tList = queryAll(clazz, where, limit, orderBy);
		if(tList == null || tList.isEmpty())
			return null;
		queryCache.put(queryId, tList);

		List<AssociateProperty> associates = table.getAssociates();
		if(associates != null && !associates.isEmpty()){
			for(T t:tList){
				for(AssociateProperty ap: associates){
					if(ap.getType() == TYPE.MANY_TO_ONE){
						Object pkValueOfOneInMany = ap.getPkOfOneInMany().getValue(t);
						if(pkValueOfOneInMany == null){
							continue;
						}

						Class<?> beAssociatedClazz = ap.getBeAssociatedClazz();
						Object object = queryByIdWithAssociate(pkValueOfOneInMany, beAssociatedClazz, queryCache);
						ap.setBeAssociatedObject(t, object);
					}else if(ap.getType() == TYPE.ONE_TO_MANY){
						Class<?> beAssociatedClazz = ap.getBeAssociatedClazz();
						Object pkValue = table.getPk().getValue(t);
						List<?> list = queryAllWithAssociate(beAssociatedClazz, 
								ap.getPkNameOfOneInMany() + "=" +pkValue, 
								null, 
								null,
								queryCache);
						ap.setBeAssociatedObject(t, list);
					}
				}
			}
		}
		return tList;
	}

	public <T> List<T> queryBySql(Class<T> clazz,String sql){
		if(clazz == null)
			return null;

		Table table = TableFactory.getTable(clazz);
		if(table == null)
			return null;

		if(sql != null){
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor cursor = db.rawQuery(sql, null);
			try {
				List<T> ts = CursorUtils.getListEntity(cursor, clazz);
				return ts;	
			}finally{
				if(cursor != null)
					cursor.close();
			}
		}
		return null;
	}

	public boolean update(Object entity){
		SQL sql = SqlBuilder.buildUpdateSql(entity);
		SQLog.d(TAG, sql);
		try{
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.execSQL(sql.getSql(), sql.getBindArgsAsArray());
			return true;
		}catch (Exception e) {
			EALog.w(TAG, e.toString());
		}
		return false;
	}

	public boolean updateOrThrow(Object entity){
		SQL sql = SqlBuilder.buildUpdateSql(entity);
		SQLog.d(TAG, sql);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.execSQL(sql.getSql(), sql.getBindArgsAsArray());
		return true;
	}

	public boolean update(Object entity,String where){
		SQL sql = SqlBuilder.buildUpdateSql(entity,where);
		SQLog.d(TAG, sql);
		try{
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.execSQL(sql.getSql(), sql.getBindArgsAsArray());
			return true;
		}catch (Exception e) {
			EALog.w(TAG, e.toString());
		}
		return false;
	}

	public boolean updateOrThrow(Object entity,String where){
		SQL sql = SqlBuilder.buildUpdateSql(entity,where);
		SQLog.d(TAG, sql);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.execSQL(sql.getSql(), sql.getBindArgsAsArray());
		return true;
	}

	public boolean delete(Object entity){
		SQL sql = SqlBuilder.buildDeleteSql(entity);
		SQLog.d(TAG, sql);
		try{
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.execSQL(sql.getSql(), sql.getBindArgsAsArray());
			return true;
		}catch (Exception e) {
			EALog.w(TAG, e.toString());
		}
		return false;
	}

	public boolean deleteOrThrow(Object entity){
		SQL sql = SqlBuilder.buildDeleteSql(entity);
		SQLog.d(TAG, sql);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.execSQL(sql.getSql(), sql.getBindArgsAsArray());
		return true;
	}

	public boolean deleteById(Class<?> clazz,Object id){
		SQL sql = SqlBuilder.buildDeleteSql(clazz,id);
		SQLog.d(TAG, sql);
		try{
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.execSQL(sql.getSql(), sql.getBindArgsAsArray());
			return true;
		}catch (Exception e) {
			EALog.w(TAG, e.toString());
		}
		return false;
	}

	public boolean deleteByIdOrThrow(Class<?> clazz,Object id){
		SQL sql = SqlBuilder.buildDeleteSql(clazz,id);
		SQLog.d(TAG, sql);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.execSQL(sql.getSql(), sql.getBindArgsAsArray());
		return true;
	}

	public boolean delete(Class<?> clazz,String where){
		SQL sql = SqlBuilder.buildDeleteSql(clazz,where);
		SQLog.d(TAG, sql);
		try{
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.execSQL(sql.getSql(), sql.getBindArgsAsArray());
			return true;
		}catch (Exception e) {
			EALog.w(TAG, e.toString());
		}
		return false;
	}

	public boolean deleteOrThrow(Class<?> clazz,String where){
		SQL sql = SqlBuilder.buildDeleteSql(clazz,where);
		SQLog.d(TAG, sql);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.execSQL(sql.getSql(), sql.getBindArgsAsArray());
		return true;
	}

	public boolean drop(Class<?> clazz){
		Table table = TableFactory.getTable(clazz);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		return table.drop(db);
	}

	public void close(){
		dbHelper.close();
	}

	private boolean checkTableState(Table table) {
		if(table.isExist()){
			return true;
		}else{
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			return table.create(db);
		}
	}

	private String generateQueryId(Table table,Object id){
		if(table == null)
			return null;

		StringBuilder idBuilder = new StringBuilder();
		idBuilder.append(table.getName());
		if(id != null)
			idBuilder.append(id.toString());
		return idBuilder.toString();
	}

	private String generateQueryId(Table table,String where,String limit,String orderBy){
		if(table == null)
			return null;

		StringBuilder idBuilder = new StringBuilder();
		idBuilder.append(table.getName());
		if(StringUtils.isNotEmpty(where))
			idBuilder.append(where);
		if(StringUtils.isNotEmpty(limit))
			idBuilder.append(limit);
		if(StringUtils.isNotEmpty(orderBy))
			idBuilder.append(orderBy);
		return idBuilder.toString();
	}

	public static class EasyDBConfig{
		private int version = 1;
		private CursorFactory factory;
		private String name = "eandroid.db";
		private Context context;
		private EasyDBListener dbListener;
		public int getVersion() {
			return version;
		}
		public EasyDBConfig setVersion(int version) {
			this.version = version;
			return this;
		}
		public CursorFactory getFactory() {
			return factory;
		}
		public EasyDBConfig setFactory(CursorFactory factory) {
			this.factory = factory;
			return this;
		}
		public String getName() {
			return name;
		}
		public EasyDBConfig setName(String name) {
			this.name = name;
			return this;
		}
		public Context getContext() {
			return context;
		}
		public EasyDBConfig setContext(Context context) {
			this.context = context;
			return this;
		}
		public EasyDBListener getDbListener() {
			return dbListener;
		}
		public EasyDBConfig setDbListener(EasyDBListener dbListener) {
			this.dbListener = dbListener;
			return this;
		}
	}
	public interface EasyDBListener{
		public void onCreate(SQLiteDatabase db);
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);	
	}

	private class EasyDbOpenHelper extends SQLiteOpenHelper{
		private EasyDBListener dbListener;

		public EasyDbOpenHelper(Context context, String name,
				CursorFactory factory, int version,EasyDBListener dbListener) {
			super(context, name, factory, version);
			this.dbListener = dbListener;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			if(dbListener != null)
				dbListener.onCreate(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(dbListener != null)
				dbListener.onUpgrade(db, oldVersion, newVersion);
		}
	}
}
