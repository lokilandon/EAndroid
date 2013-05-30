/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-16
 * @version 0.1
 */
package com.eandroid.database.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.eandroid.database.AssociateProperty;
import com.eandroid.database.Column;
import com.eandroid.database.ForeignKey;
import com.eandroid.database.PrimaryKey;
import com.eandroid.database.annotation.MainTable;
import com.eandroid.database.annotation.Table;
import com.eandroid.util.EALog;
import com.eandroid.util.StringUtils;

public class TableUtils {

	public static boolean isMainTable(Class<?> clazz) {
		if(clazz == null)
			return false;

		return clazz.isAnnotationPresent(MainTable.class);
	}

	/**
	 * 获取表名
	 * @param clazz
	 * @return
	 */
	public static String getTableName(Class<?> clazz){
		if(clazz == null)
			return null;

		Table table = clazz.getAnnotation(Table.class);
		if(table == null || StringUtils.isEmpty(table.name())){
			return clazz.getName().replace('.', '_');
		}
		return table.name();
	}

	/**
	 * 获取表中的列（不包括主键、外键）
	 * @param clazz
	 * @return
	 * @throws NoSuchMethodException
	 */
	public static List<Column> getTableColumnList(Class<?> clazz) throws NoSuchMethodException{
		if(clazz == null)
			return null;

		Class<?> superClazz = clazz.getSuperclass();
		Map<String, Field> fieldsMap = new HashMap<String, Field>();
		if(superClazz != null){
			//获取从父类那继承来的属性
			Map<String, Field> inheriteFieldsMap = getBaseInheriteColumn(clazz);
			if(inheriteFieldsMap != null && !inheriteFieldsMap.isEmpty())
				fieldsMap.putAll(inheriteFieldsMap);
		}
		Field[] fields = clazz.getDeclaredFields();
		boolean hasDefinedPrimaryKey = false;
		if(fields != null && fields.length > 0){
			for(Field field : fields){
				String name = field.getName();
				//如果从父类继承的属性，子类中已经定义过了，则判断重新定义的属性是否依然为数据库字段的基本类型,如果不是则移除
				Field inheriteField = fieldsMap == null?null:fieldsMap.get(name);
				if(inheriteField != null){
					if(!ColumnUtils.isBaseColumnData(field)){
						fieldsMap.remove(field.getName());
					}
				}else if(ColumnUtils.isBaseColumnData(field)){
					fieldsMap.put(name, field);
				}
				if(ColumnUtils.isPrimaryKey(field)){
					hasDefinedPrimaryKey = true;
				}
			}
			if(!hasDefinedPrimaryKey){
				fieldsMap.remove("id");
			}
		}

		Iterator<Field> iterator = fieldsMap.values().iterator();
		List<Column> columnList = new ArrayList<Column>();
		while(iterator.hasNext()){
			Field field = iterator.next();
			columnList.add(ColumnUtils.getColumn(field,clazz));
		}
		return columnList;
	}

	/**
	 * 查找类中继承来的属性（能够作为数据库列,不包括主键、外键及设置为transient的字段），包括从父类的父类继承来的属性（递归）
	 * @param clazz 类
	 * @return
	 * @throws NoSuchMethodException
	 */
	private static Map<String,Field> getBaseInheriteColumn(Class<?> clazz) throws NoSuchMethodException{
		if(clazz == null)
			return null;

		Map<String, Field> inheriteFields = getInheriteField(clazz);
		if(inheriteFields != null && !inheriteFields.isEmpty()){
			Iterator<String> keyIterator = inheriteFields.keySet().iterator();
			while(keyIterator.hasNext()){
				String fieldName = keyIterator.next();
				Field field = inheriteFields.get(fieldName);
				if(!ColumnUtils.isBaseColumnData(field)){
					keyIterator.remove();
				}
			}
		}
		return inheriteFields;
	}

	/**
	 * 查找类中继承来的属性（能够作为数据库列），包括从父类的父类继承来的属性（递归）
	 * 得到的column中
	 * @param clazz 类
	 * @return
	 * @throws NoSuchMethodException
	 */
	private static Map<String,Field> getInheriteField(Class<?> clazz) throws NoSuchMethodException{
		if(clazz == null)
			return null;

		Class<?> superClass = clazz.getSuperclass();
		boolean samePackage = false;
		Map<String, Field> inheriteFields = null;
		if(superClass != null){
			//获取父类继承的属性
			inheriteFields = getInheriteField(superClass);
			samePackage = clazz.getPackage() == superClass.getPackage();
		}else{
			return inheriteFields;
		}

		//如果子类和父类不在同一个包中，则包父类中继承的default属性去除
		if(!samePackage && inheriteFields != null 
				&& !inheriteFields.isEmpty()){
			Iterator<Field> fIterator = inheriteFields.values().iterator();
			while(fIterator.hasNext()){
				Field inheriteField = fIterator.next();
				if(ColumnUtils.isDefaultModifier(inheriteField)){
					fIterator.remove();
				}
			}
		}

		//获取父类定义的所有属性
		Field[] fields = superClass.getDeclaredFields();
		if(fields == null || fields.length == 0)
			return inheriteFields;

		for(Field field : fields){
			String name = field.getName();
			Field inheriteField = inheriteFields == null?null:inheriteFields.get(name);
			if(inheriteField != null){
				//如果是父类继承的属性，判断是否在类中将其的访问权限改为private,或者在属性名称已经指向其他非数据类型的属性，如果改了则移除此属性
				if(Modifier.isPrivate(field.getModifiers())
						|| !ColumnUtils.isValidColumnDataType(field)){
					inheriteFields.remove(field.getName());
				}
			}else if(ColumnUtils.isInheritedField(field, samePackage)
					&& ColumnUtils.isValidColumnDataType(field)){
				//判断此属性的访问权限是否能被子类继承,并且能够符合数据字段插入类型,符合条件的加入到map中
				if(inheriteFields == null)
					inheriteFields = new HashMap<String, Field>();
				inheriteFields.put(name, field);
			}
		}
		return inheriteFields;
	}

	/**
	 * 获取主键，不能通过父类继承<br/>
	 * 如果为定义注解，默认取id字段为主键
	 * @param clazz
	 * @return
	 * @throws NoSuchMethodException
	 */
	public static PrimaryKey getPrimaryKey(Class<?> clazz) throws NoSuchMethodException{
		if(clazz == null || clazz == Object.class)
			return null;

		Field[] fieldArray = clazz.getDeclaredFields();
		PrimaryKey pk = null;
		Field pkField = null;
		if(fieldArray != null && fieldArray.length > 0){
			for(Field field : fieldArray){
				if(ColumnUtils.isPrimaryKey(field)){
					pkField = field;
				}
			}
			if(pk == null){
				try {
					Field idField = clazz.getDeclaredField("id");
					pkField = idField;
				} catch (NoSuchFieldException e) {}
			}
		}
		if(pk == null){
			Field idField = null;
			Map<String, Field> inheriteFields = getInheriteField(clazz);
			if(inheriteFields != null && !inheriteFields.isEmpty()){
				Iterator<String> keyIterator = inheriteFields.keySet().iterator();
				while(keyIterator.hasNext()){
					String fieldName = keyIterator.next();
					Field field = inheriteFields.get(fieldName);
					if(ColumnUtils.isPrimaryKey(field)){
						pkField = field;
						break;
					}else if("id".equals(field.getName())){
						idField = field;
					}
				}
			}
			if(pkField == null)
				pkField = idField;
		}
		if (pkField != null) {
			pk = ColumnUtils.getPrimaryKey(pkField,clazz);
		}
		return pk;
	}

	/**
	 * 获取外键，不能从父类继承<br/>
	 * @param clazz
	 * @return
	 * @throws NoSuchMethodException 
	 */
	public static List<ForeignKey> getForeignKey(Class<?> clazz) throws NoSuchMethodException{
		if(clazz == null)
			return null;

		Field[] fieldArray = clazz.getDeclaredFields();
		if(fieldArray == null || fieldArray.length == 0){
			return null;
		}
		List<ForeignKey> fks = new ArrayList<ForeignKey>();
		for(Field field : fieldArray){
			if(ColumnUtils.isForeignKey(field)){
				ForeignKey fk = ColumnUtils.getForeignKey(field);
				if(fk == null){
					EALog.e("TableUtils", "getForeignKey failed");
					continue;
				}
				fks.add(fk);
			}
		}
		return fks;
	}

	/**
	 * 获取所有与clazz对应表的关联属性
	 * @param clazz
	 * @return
	 * @throws NoSuchMethodException 
	 */
	public static List<AssociateProperty> getAssociates(Class<?> clazz) throws NoSuchMethodException{
		if(clazz == null)
			return null;

		Field[] fieldArray = clazz.getDeclaredFields();
		if(fieldArray == null || fieldArray.length == 0){
			return null;
		}

		List<AssociateProperty> apList = new ArrayList<AssociateProperty>();
		for(Field field : fieldArray){
			if(ColumnUtils.isAssociate(field)){
				AssociateProperty ap = ColumnUtils.getAssociate(field);
				if(ap != null)
					apList.add(ap);
			}
		}
		return apList;
	}

	public static Class<?>[] getSubtableClazz(Class<?> clazz){
		if(clazz == null)
			return null;

		MainTable mTable = clazz.getAnnotation(MainTable.class);
		if(mTable == null)
			return null;

		Class<?>[] subTableClazzes = mTable.subtables();
		return subTableClazzes;
	}

	public static String[] getSubtableAssociateColumnNames(Class<?> clazz){
		if(clazz == null)
			return null;

		MainTable mTable = clazz.getAnnotation(MainTable.class);
		if(mTable == null)
			return null;

		String[] columns = mTable.columns();
		return columns;
	}
}
