package com.tinet.jmxhub.bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 用来缓存通过JMX获取到的JVM数据
 * 
 * @author Stephan gao
 * @since 2016年4月26日
 *
 */
public class JVMContext {

	/**
	 * port -> jvm data
	 */
	@JsonProperty
	private Map<String, JVMData> jvmDatas = new ConcurrentHashMap<String, JVMData>();

	public JVMData getJvmData(String jmxPort) {
		if (jvmDatas.containsKey(jmxPort)) {
			return jvmDatas.get(jmxPort);
		} else {
			JVMData jvmData = new JVMData();
			jvmDatas.put(jmxPort, jvmData);
			return jvmData;
		}
	}

}
