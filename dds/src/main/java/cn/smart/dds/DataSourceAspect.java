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

        Class<?>[] classz = target.getClass().getInterfaces();

        Class<?>[] parameterTypes = ((MethodSignature) point.getSignature())
                .getMethod().getParameterTypes();
        try {
            Method m = classz[0].getMethod(method, parameterTypes);
            if (m != null && m.isAnnotationPresent(DataSource.class)) {
                DataSource data = m
                        .getAnnotation(DataSource.class);
                DynamicDataSourceHandler.putDataSource(data.value());
                logger.debug(data.value());
            } else {
            	// 设置默认注解值（主库）
            	DynamicDataSourceHandler.putDataSource(DataSource.MASTER);
            	logger.debug("Default DataSourcee is Master");
            }
            
        } catch (Exception e) {
        	logger.error(e);
        }
    }
}