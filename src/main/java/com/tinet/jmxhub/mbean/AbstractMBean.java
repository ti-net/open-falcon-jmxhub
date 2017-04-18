package com.tinet.jmxhub.mbean;

import java.io.IOException;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;

import com.tinet.jmxhub.bean.FalconItem;
import com.tinet.jmxhub.bean.JVMContext;

/**
 * AbstractMBean：MBean的骨架类，定义了公用的
 * MBeanServerConnection对象、jmxHost名称以及静态的jvmContext用来缓存上次的监控数据
 * <p>
 * 2017年4月13日 - 上午10:10:55
 * </p>
 * 
 * @author wangll
 * @since
 * @version
 */
public abstract class AbstractMBean<T> implements MBean<T> {

	private MBeanServerConnection connection;

	private String jmxHost;

	private static JVMContext jvmContext = new JVMContext();

	public static JVMContext getJvmContext() {
		return jvmContext;
	}

	public AbstractMBean(MBeanServerConnection connection, String jmxHost) {
		this.connection = connection;
		this.jmxHost = jmxHost;
	}

	public List<FalconItem> getResult() throws IOException, MalformedObjectNameException {
		T t = call();
		List<FalconItem> result = build(t);
		return result;
	}

	protected MBeanServerConnection getConnection() {
		return connection;
	}

	protected String getJmxHost() {
		return jmxHost;
	}

}
