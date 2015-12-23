package cn.smart.dds;

import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;


public class DynamicDataSource extends AbstractRoutingDataSource {
	private String dataSourceNames;
	private String masterName = null;
	private String[] slaveNames = null;
	// 写数据源
	private DataSource masterDataSource;
	// 读数据源列表
	private DataSource[] slaveDataSourceArray = null;

	private AtomicInteger slaveIndex = new AtomicInteger();

	private void initJndiSource() throws DataSourceLookupFailureException {
		DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
		List<DataSource> dataSourcesList = new LinkedList<DataSource>();
		masterDataSource = dataSourceLookup.getDataSource(masterName);
		// 设置默认数据源
		super.setDefaultTargetDataSource(masterDataSource);
		
		for (int i = 0; i < slaveNames.length; i++)
			dataSourcesList.add(dataSourceLookup.getDataSource(slaveNames[i]));

		slaveDataSourceArray = dataSourcesList.toArray(new DataSource[0]);
	}

	@Override
	protected Object determineCurrentLookupKey() {
		return DynamicDataSourceHandler.getDataSouce();
	}

	@Override
	protected DataSource determineTargetDataSource() {
		String key = (String) determineCurrentLookupKey();
		
		if (cn.smart.dds.DataSource.SLAVE.equals(key)) {
			// 注解是slave的时候返回从数据库
			int idx = Math.abs(slaveIndex.getAndIncrement());
			return slaveDataSourceArray[idx % slaveDataSourceArray.length];
		} else if (key == null || cn.smart.dds.DataSource.MASTER.equals(key)) {
			// 注解是master或者是没有注解的情况下返回主数据库
			return masterDataSource;
		}

		return null;
	}

	/**
	 *    server.xml中的设置
    
   <GlobalNamingResources>
    <Resource auth="Container" driverClassName="com.mysql.jdbc.Driver" factory="com.alibaba.druid.pool.DruidDataSourceFactory" filters="stat" initialSize="1" maxActive="200" maxWait="60000" minIdle="1" name="testdbmaster" password="111111" singleton="true" testOnBorrow="false" testOnReturn="false" testWhileIdle="true" type="com.alibaba.druid.pool.DruidDataSource" url="jdbc:mysql://127.0.0.1:3306/testdbmaster" username="root" validationQuery="SELECT 'x'"/>
    <Resource auth="Container" driverClassName="com.mysql.jdbc.Driver" factory="com.alibaba.druid.pool.DruidDataSourceFactory" filters="stat" initialSize="1" maxActive="200" maxWait="60000" minIdle="1" name="testdbslave1" password="111111" singleton="true" testOnBorrow="false" testOnReturn="false" testWhileIdle="true" type="com.alibaba.druid.pool.DruidDataSource" url="jdbc:mysql://127.0.0.1:3306/testdbslave1" username="root" validationQuery="SELECT 'x'"/>
    <Resource auth="Container" driverClassName="com.mysql.jdbc.Driver" factory="com.alibaba.druid.pool.DruidDataSourceFactory" filters="stat" initialSize="1" maxActive="200" maxWait="60000" minIdle="1" name="testdbslave2" password="111111" singleton="true" testOnBorrow="false" testOnReturn="false" testWhileIdle="true" type="com.alibaba.druid.pool.DruidDataSource" url="jdbc:mysql://127.0.0.1:3306/testdbslave2" username="root" validationQuery="SELECT 'x'"/>
  </GlobalNamingResources>
    
     context.xml中的设置
     
	<ResourceLink global="testdbmaster"  name="lntestdbmaster"  type="com.alibaba.druid.pool.DruidDataSource" />
	<ResourceLink global="testdbslave1"  name="lntestdbslave1"  type="com.alibaba.druid.pool.DruidDataSource" />
	<ResourceLink global="testdbslave2"  name="lntestdbslave2"  type="com.alibaba.druid.pool.DruidDataSource" />
	<Environment name="dblist" value="master=lntestdbmaster;slaves=lntestdbslave1,lntestdbslave2" type="java.lang.String" override="false" />
 
	 * @param value
	 * @throws InvalidParameterException
	 * @throws DataSourceLookupFailureException
	 * @author wangyuhao
	 * @date 2015年12月11日 下午4:28:35
	 */
	public void setSourceNames(String value) throws InvalidParameterException,
			DataSourceLookupFailureException {
		
		if (value == null || value.length() == 0)
			throw new InvalidParameterException(value);

		String[] masterAndSlaves = value.split(";");
		if (masterAndSlaves.length == 1 || masterAndSlaves.length == 2) {
			String[] master = masterAndSlaves[0].split("=");
			if (master.length != 2 || !master[0].equals("master")
					|| master[1].trim().isEmpty())
				throw new InvalidParameterException(value);

			masterName = master[1];

			if (masterAndSlaves.length == 2) {
				String[] slaves = masterAndSlaves[1].split("=");
				if (slaves.length != 2 || !slaves[0].equals("slaves"))
					throw new InvalidParameterException(value);

				slaveNames = slaves[1].split(",");
				if (slaveNames.length == 0)
					throw new InvalidParameterException(value);
			}
		} else
			throw new InvalidParameterException(value);

		dataSourceNames = value;

		initJndiSource();
	}

	public String getSourceNames() {
		return dataSourceNames;
	}

}