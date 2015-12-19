package cn.smart.dds;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DataSource {
	
	public static final String MASTER = "master";
	
	public static final String SLAVE = "slave";
	
    String value();
}