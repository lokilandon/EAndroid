package com.eandroid.ioc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

	public int value();
	
	public String clickMethod() default "";
	
	public String longClickMethod() default "";
	
	public String focusChangedMethod() default "";
	
	public String touchMethod() default "";
	
	public String itemClickMethod() default "";
	
	public String itemLongClickMethod() default "";
	
	public String itemSelectedMethod() default "";
}
