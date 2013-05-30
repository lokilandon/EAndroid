/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-15
 * @version 0.1
 */
package com.eandroid.database.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表与表的关联信息
 * @author Kain
 *
 */
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Associate {
	
	public static enum TYPE{MANY_TO_ONE,ONE_TO_MANY};
	
	public TYPE type();//关联类型
	public String pkOfOneInMany();//关联的列信息，此值才需要。多对一关系时，该字段应指向关联表在本表中的所对应的主键id字段。一对多关系时，该字段应指向本表中的主键id在关联表中所对应的字段
	public Class<?> table() default Object.class;//关联的表信息，如果设置了table,那字段的类型必须和设置的class类型相同（除了Map类型）
}
