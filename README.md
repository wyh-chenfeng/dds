# dds
 以JNDI的方式实现动态数据源
 
# Tomcat7.0配置JNDI数多据源
注意：${CATALINA_HOME}表示tomcat的安装路径，如：
（D:\Program Files (x86)\apache-tomcat-7.0.53）

一、拷贝数据库驱动到：${CATALINA_HOME}\lib下，这里是（mysql-connector-java-5.1.25.jar、druid-1.0.16.jar）

二、修改${CATALINA_HOME}\conf下的server.xml文件。
在server.xml文件的GlobalNamingResources节点中添加Resource 节点。如
<GlobalNamingResources>
    ...

<Resource name="testdbmaster" 
auth="Container" 
driverClassName="com.mysql.jdbc.Driver" 	factory="com.alibaba.druid.pool.DruidDataSourceFactory" 	filters="stat" 
initialSize="1" 
maxActive="200" 
maxWait="60000" 	
minIdle="1" 
singleton="true" 
testOnBorrow="false" 
testOnReturn="false" 	
testWhileIdle="true" 	type="com.alibaba.druid.pool.DruidDataSource" 	url="jdbc:mysql://10.9.53.187:3306/base_order" 	username="ddb_user"
password="qazwsx" 	 
validationQuery="SELECT 'x'"/>

	<Resource name="testdbslave1"  
auth="Container" 
driverClassName="com.mysql.jdbc.Driver" 		factory="com.alibaba.druid.pool.DruidDataSourceFactory" 		filters="stat" 
initialSize="1" 
maxActive="200" 
maxWait="60000" 	
minIdle="1" 				
singleton="true" 
testOnBorrow="false" 
testOnReturn="false" 		
testWhileIdle="true" 				type="com.alibaba.druid.pool.DruidDataSource" 	url="jdbc:mysql://10.9.53.56:3306/base_order" 	username="ddb_user" 
password="qazwsx" 
validationQuery="SELECT 'x'"/>
</GlobalNamingResources>

重要节点解释：
1、name="testdbslave1" ：数据库连接池名称。
2、url="jdbc:mysql://10.9.53.56:3306/base_order" ：数据库连接地址。
3、username="ddb_user" ：数据库用户名。
4、password="qazwsx" ：数据库密码。
5、validationQuery="SELECT 'x'" ：一个SQL查询语句，用于在连接被返回给应	用前的连接池验证，至少返回一行记录的SQL SELECT语句。

三、修改${CATALINA_HOME}\conf下的context.xml文件。
在context.xml文件的Context节点中添加ResourceLink节点和Environment节点。如：
<Context>
...
	<ResourceLink global="testdbmaster" name="lntestdbmaster"
		type="com.alibaba.druid.pool.DruidDataSource" />
	<ResourceLink global="testdbslave1" name="lntestdbslave1"
		type="com.alibaba.druid.pool.DruidDataSource" />

	<Environment name="dblist"
		value="master=lntestdbmaster;slaves=lntestdbslave1" 					type="java.lang.String"
		override="false" />
</Context>

ResourceLink 重要节点解释：
1、global="testdbmaster"  ：在server.xml中定义的数据库连接池名称。
2、name="lntestdbmaster" ：暴露给 Web 应用的数据源名称。
3、type ：数据库连接池类型。

Environment 重要说明：
1、name="dblist" ：这个节点不能改。
2、value="master=lntestdbmaster;slaves=lntestdbslave1" ：这个节点中	master和slaves不能改。“=”号后面的名字就是ResourceLink 节点中定义的name。 	slaves如果对应多个从数据源用“,”分割。如：slaves=dbslave1,dbslave2

四、在项目的pom文件中添加jar包。
<dependency>
		<groupId>cn.smart</groupId>
		<artifactId>dds</artifactId>
		<version>1.0.0</version>
</dependency>

五、在Spring配置文件（例：applicationContext-resource.xml）中添加JNDI数据源和面向切面的配置。
<!-- 引用JNDI配置 -->
<bean id="dataSourceList" 		class="org.springframework.jndi.JndiObjectFactoryBean">
		<property name="jndiName" value="java:/comp/env/dblist" />
</bean>

<!-- 引用JNDI数据源 -->
	<bean id="dataSource" class="cn.smart.dds.DynamicDataSource">
		<property name="targetDataSources">
			<map></map>
		</property>
    	<property name="sourceNames" ref="dataSourceList" />    
    </bean>

<!-- 面向切面的配置 -->
	<aop:aspectj-autoproxy />
	<bean id="manyDataSourceAspect" 	class="cn.smart.dds.DataSourceAspect" />
	<aop:config>
		<aop:aspect order="0" id="c" ref="manyDataSourceAspect">
			<aop:pointcut id="tx"
				expression="execution(* service..*.*(..))" />
			<aop:before pointcut-ref="tx" method="before" />
		</aop:aspect>
</aop:config>
总要节点说明： 
expression="execution(* service..*.*(..))" ：配置 @DataSource 注解所	在的包。这个切面应该是应用的service层。

六、在service层的接口上添加@DataSource注解
@DataSource(DataSource.MASTER) ： 主库，做插入个跟新操作
@DataSource(DataSource.SLAVE) ：从库，做查询操作

例如：
public interface SyndromeService {
	@DataSource(DataSource.MASTER)// 主库
void create(SyndromeCreateInput syndromeCreateInput);

    @DataSource(DataSource.SLAVE) //从库
    List<Syndrome> findAll();    
}


