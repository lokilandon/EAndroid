/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-22
 * @version 1.0.0
 */
package com.eandroid.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eandroid.database.MainTable.SubTable;
import com.eandroid.database.annotation.Associate;
import com.eandroid.database.util.TableUtils;
import com.eandroid.util.StringUtils;

public class TableFactory {

	private static Map<String, Table> tableMap = new HashMap<String, Table>();

	public static synchronized Table getTable(Class<?> clazz){
		if(clazz == null)
			throw new IllegalArgumentException("Table get() - failed,the clazz is null.");

		Table table = tableMap.get(clazz.getName());
		if(table != null)
			return table;

		String tableName = TableUtils.getTableName(clazz);		
		try {
			List<Column> columnList = TableUtils.getTableColumnList(clazz);
			if(columnList == null || columnList.isEmpty()){
				throw new IllegalArgumentException("Table get() - failed, the class table" + clazz.getName() +"'s property is empty.");
			}
			PrimaryKey pk = TableUtils.getPrimaryKey(clazz);
			if(pk == null)
				throw new IllegalArgumentException("Table get() - failed,the primarykey is not find");
			List<ForeignKey> fks = TableUtils.getForeignKey(clazz);

			if(TableUtils.isMainTable(clazz)){
				table = new MainTable(tableName,columnList,pk);
			}else{
				table = new Table(tableName,columnList,pk);
			}

			List<AssociateProperty> associates = TableUtils.getAssociates(clazz);

			table.fks = fks;
			table.associates = associates;
			//先将table引用放进map中，再递归查询外键、关联属性、subtable中的table引用
			//否则当循环引用时可能因为从缓存中取不到之前加载过的table,而循环解析clazz到Table而造成死循环。
			tableMap.put(clazz.getName(), table);
			
			if(table instanceof MainTable){
				SubTable[] subTables = getSubTables(clazz);
				((MainTable)table).setSubTables(subTables);
			}
			if(fks != null && !fks.isEmpty()){
				for(ForeignKey fk: fks){
					fk.setMainTable(getTable(fk.getMainTableClazz()));
				}
			}
			if(associates != null && !associates.isEmpty()){
				for (AssociateProperty ap : associates) {
					Table associateTable = getTable(ap.getBeAssociatedClazz());
					if(ap.getType() == Associate.TYPE.MANY_TO_ONE){
						ap.setPkOfOneInMany(table.getColumnByAssociate(ap));
					}else if(ap.getType() == Associate.TYPE.ONE_TO_MANY){
						ap.setPkOfOneInMany(associateTable.getColumnByAssociate(ap));
					}
					if(ap.getPkOfOneInMany() == null){
						throw new IllegalStateException("Table get() - failed,the associate "+ap.getField().getName()+" column is not found.");
					}
				}
			}
			return table;
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Table get() - failed."+e.toString());
		}
	}

	public static SubTable[] getSubTables(Class<?> clazz) throws NoSuchMethodException{
		if(clazz == null || !TableUtils.isMainTable(clazz)){
			return null;
		}

		Class<?>[] subTableClazzes = TableUtils.getSubtableClazz(clazz);
		String[] columns = TableUtils.getSubtableAssociateColumnNames(clazz);
		if(subTableClazzes == null || columns == null)
			return null;

		int length = subTableClazzes.length;
		if(columns.length != length)
			throw new IllegalStateException("TableUtils - getSubTables() failed,subtable's length != column's length");

		if(length > 0){
			SubTable[] subTables = new SubTable[length];
			for(int i = 0 ; i < length; i ++){
				Class<?> subtableClass = subTableClazzes[i];
				String columnFiledName = columns[i];
				Table table = getTable(subtableClass);
				if(StringUtils.isEmpty(columnFiledName)){
					throw new IllegalStateException("TableUtils - getSubTables() failed,subtable's column is empty");
				}
				SubTable subTable = new SubTable(table,columnFiledName);
				subTables[i] = subTable;
			}
			return subTables;
		}
		return null;
	}
}
