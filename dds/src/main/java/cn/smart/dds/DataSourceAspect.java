package cn.smart.dds;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public class DataSourceAspect {

	Logger logger = Logger.getLogger(DataSourceAspect.class);
	
    public void before(JoinPoint point)
    {
        Object target = point.getTarget();
        String method = point.getSignature().getName();

        // 注解加在接口上
        Class<?>[] interfaces = target.getClass().getInterfaces();
        
        Class<?>[] parameterTypes = ((MethodSignature) point.getSignature())
                .getMethod().getParameterTypes();
        try {
        	Method m = null;
	        // 先判断接口是否存在
			if (interfaces != null && interfaces.length > 0) {
				m = interfaces[0].getMethod(method, parameterTypes);
			}
        	
			// 如果接口存在 再判断注解是否存在
			if (m != null && m.isAnnotationPresent(DataSource.class)) {
                DataSource data = m.getAnnotation(DataSource.class);
                DynamicDataSourceHandler.putDataSource(data.value());
                logger.debug(data.value());
            } else {
            	// 如果接口不存在或者接口上的注解不存在时，找对应的类上注解
            	m = target.getClass().getMethod(method, parameterTypes);
            	
            	if (m != null && m.isAnnotationPresent(DataSource.class)) {
                    DataSource data = m.getAnnotation(DataSource.class);
                    DynamicDataSourceHandler.putDataSource(data.value());
                    logger.debug(data.value());
                } else {
                	// 设置默认注解值（主库）
                	DynamicDataSourceHandler.putDataSource(DataSource.MASTER);
                	logger.debug("Default DataSourcee is Master");
                }
            }
            
        } catch (Exception e) {
        	logger.error(e);
        }
    }
}