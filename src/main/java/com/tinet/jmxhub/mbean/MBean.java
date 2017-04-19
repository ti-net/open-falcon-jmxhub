package com.tinet.jmxhub.mbean;

import java.io.IOException;
import java.util.List;

import javax.management.MalformedObjectNameException;

import com.tinet.jmxhub.bean.FalconItem;

/**
 * Mbean：接口定义所有Mbean的方法以及抛出异常情况
 * <p>
 * 2017年4月12日 - 上午11:15:12
 * </p>
 * 
 * @author wangll
 * @since
 * @version
 */
public interface MBean<T> {

	/**
	 * 调用jmx接口获取数据
	 * 
	 * @return
	 * @throws IOException
	 * @throws MalformedObjectNameException 
	 */
	T call() throws IOException, MalformedObjectNameException;
	

	/**
	 * 将jmx获取到的数据组装成Openfalcon数据返回
	 * 
	 * @param jmxResultData
	 * @return
	 * @throws Exception
	 */
	List<FalconItem> build(T jmxResultData);

	/**
	 * 获取最终结果
	 * @return
	 * @throws IOException
	 * @throws MalformedObjectNameException 
	 */
	List<FalconItem> getResult() throws IOException, MalformedObjectNameException;
}
