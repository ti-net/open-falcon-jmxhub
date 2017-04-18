package com.tinet.jmxhub.mbean;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServerConnection;

import org.apache.commons.lang.StringUtils;

import com.tinet.jmxhub.bean.FalconItem;
import com.tinet.jmxhub.constant.Constants;
import com.tinet.jmxhub.enums.CounterTypeEnum;

/**
 * GCThroughputMbean：获取吞吐率数据
 * <li><b>gc.throughput</b>（GC的总吞吐率（应用运行时间/进程总运行时间））</li>
 * <p>
 * 2017年4月13日 - 下午1:52:59
 * </p>
 * 
 * @author wangll
 * @since
 * @version
 */
public class GCThroughputMbean extends AbstractMBean<Double> implements MBean<Double> {

	private GCGenInfoMBean gCGenInfoMBean;

	public GCThroughputMbean(MBeanServerConnection connection, String jmxHost, GCGenInfoMBean gCGenInfoMBean) {
		super(connection, jmxHost);
		this.gCGenInfoMBean = gCGenInfoMBean;
	}

	@Override
	public Double call() throws IOException {
		RuntimeMXBean runtimeMXBean = ManagementFactory.newPlatformMXBeanProxy(getConnection(),
				ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);

		long upTime = runtimeMXBean.getUptime();
		long totalGCTime = 0;
		Collection<GarbageCollectorMXBean> list = gCGenInfoMBean.getGcMXBeanList();

		for (GarbageCollectorMXBean bean : list) {
			totalGCTime += bean.getCollectionTime();
		}

		double gcThroughput = (double) (upTime - totalGCTime) * 100 / (double) upTime;
		return gcThroughput;
	}

	@Override
	public List<FalconItem> build(Double jmxResultData) {
		List<FalconItem> items = new ArrayList<FalconItem>();
		// 将jvm信息封装成openfalcon格式数据
		FalconItem item = new FalconItem();
		item.setCounterType(CounterTypeEnum.GAUGE.toString());
		item.setEndpoint(getJmxHost());
		item.setMetric(StringUtils.lowerCase(Constants.GC_THROUGH_PUT));
		item.setStep(Constants.DEFAULT_STEP);
		item.setTags(StringUtils.lowerCase(Constants.TAGS_NAME_PREFIX + getJmxHost()));
		item.setTimestamp(System.currentTimeMillis() / 1000);
		item.setValue(jmxResultData);
		items.add(item);

		return items;
	}
}
