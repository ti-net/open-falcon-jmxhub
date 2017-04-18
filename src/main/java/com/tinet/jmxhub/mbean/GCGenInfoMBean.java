package com.tinet.jmxhub.mbean;

import java.lang.management.ManagementFactory;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinet.jmxhub.bean.FalconItem;
import com.tinet.jmxhub.bean.GCData;
import com.tinet.jmxhub.constant.Constants;
import com.tinet.jmxhub.enums.CounterTypeEnum;
import com.tinet.jmxhub.mbean.GCGenInfoMBean.GCGenInfo;

/**
 * GCGenInfoMBean：获取GC数据：
 * <li><b>parnew.gc.avg.time</b>（一分钟内，每次YoungGC（parnew）的平均耗时）</li>
 * <li><b>concurrentmarksweep.gc.avg.time</b>（一分钟内，每次CMSGC的平均耗时）</li>
 * <li><b>parnew.gc.count</b>（一分钟内，YoungGC（parnew）的总次数）</li>
 * <li><b>concurrentmarksweep.gc.count</b>（一分钟内，CMSGC的总次数）</li>
 * <p>
 * 2017年4月12日 - 上午11:30:36
 * </p>
 * 
 * @author wangll
 * @since
 * @version
 */
public class GCGenInfoMBean extends AbstractMBean<Map<String, GCGenInfo>> implements MBean<Map<String, GCGenInfo>> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private List<GarbageCollectorMXBean> gcMXBeanList;

	public GCGenInfoMBean(MBeanServerConnection connection, String jvmHost) {
		super(connection, jvmHost);
	}

	@Override
	public Map<String, GCGenInfo> call() throws IOException, MalformedObjectNameException {
		Map<String, GCGenInfo> result = new HashMap<String, GCGenInfo>();
		initGcMXBeanList();

		for (GarbageCollectorMXBean gcMXBean : gcMXBeanList) {

			long gcTotalTime = gcMXBean.getCollectionTime();
			long gcTotalCount = gcMXBean.getCollectionCount();

			GCData gcData = getJvmContext().getJvmData(getJmxHost()).getGcData(gcMXBean.getName());
			long lastGCTotalTime = gcData.getCollectionTime();
			long lastGCTotalCount = gcData.getCollectionCount();

			long tmpGCTime = gcTotalTime - lastGCTotalTime;
			long gcCount = gcTotalCount - lastGCTotalCount;
			if (lastGCTotalCount <= 0 || gcCount < 0) {
				gcCount = -1;
			}

			double avgGCTime = gcCount > 0 ? tmpGCTime / gcCount : 0;

			GCGenInfo gcGenInfo = new GCGenInfo(avgGCTime, gcCount);
			result.put(gcMXBean.getName(), gcGenInfo);

			if (logger.isInfoEnabled()) {
				StringBuilder builder = new StringBuilder();
				builder.append("mxbean=").append(gcMXBean.getName()).append(", gcTotalTime=").append(gcTotalTime)
						.append(", gcTotalCount=").append(gcTotalCount).append(", lastGCTotalTime=")
						.append(lastGCTotalTime).append(", lastGCTotalCount=").append(lastGCTotalCount)
						.append(", avgGCTime=").append(avgGCTime).append(", gcCount=").append(gcCount);
				logger.info(builder.toString());
			}

			// update last data
			gcData.setCollectionTime(gcTotalTime);
			gcData.setCollectionCount(gcTotalCount);
			gcData.setUnitTimeCollectionCount(gcCount);
		}
		return result;
	}

	@Override
	public List<FalconItem> build(Map<String, GCGenInfo> jmxResultData) {
		List<FalconItem> items = new ArrayList<FalconItem>();

		// 将jvm信息封装成openfalcon格式数据
		for (String gcMXBeanName : jmxResultData.keySet()) {
			FalconItem avgTimeItem = new FalconItem();
			avgTimeItem.setCounterType(CounterTypeEnum.GAUGE.toString());
			avgTimeItem.setEndpoint(getJmxHost());
			avgTimeItem.setMetric(
					StringUtils.lowerCase(gcMXBeanName + Constants.METRIC_SEPARATOR + Constants.GC_AVG_TIME));
			avgTimeItem.setStep(Constants.DEFAULT_STEP);
			avgTimeItem.setTags(StringUtils.lowerCase(Constants.TAGS_NAME_PREFIX + getJmxHost()));
			avgTimeItem.setTimestamp(System.currentTimeMillis() / 1000);
			avgTimeItem.setValue(jmxResultData.get(gcMXBeanName).getGcAvgTime());
			items.add(avgTimeItem);

			FalconItem countItem = new FalconItem();
			countItem.setCounterType(CounterTypeEnum.GAUGE.toString());
			countItem.setEndpoint(getJmxHost());
			countItem.setMetric(StringUtils.lowerCase(gcMXBeanName + Constants.METRIC_SEPARATOR + Constants.GC_COUNT));
			countItem.setStep(Constants.DEFAULT_STEP);
			countItem.setTags(StringUtils.lowerCase(Constants.TAGS_NAME_PREFIX + getJmxHost()));
			countItem.setTimestamp(System.currentTimeMillis() / 1000);
			countItem.setValue(jmxResultData.get(gcMXBeanName).getGcCount());
			items.add(countItem);
		}

		return items;
	}

	private void initGcMXBeanList() throws IOException, MalformedObjectNameException {
		gcMXBeanList = new ArrayList<>();

		ObjectName threadObjName = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");

		Set<ObjectName> mbeans = getConnection().queryNames(threadObjName, null);
		if (mbeans != null) {
			Iterator<ObjectName> iterator = mbeans.iterator();
			while (iterator.hasNext()) {
				ObjectName on = iterator.next();
				String name = ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",name="
						+ on.getKeyProperty("name");

				GarbageCollectorMXBean mBean = ManagementFactory.newPlatformMXBeanProxy(getConnection(), name,
						GarbageCollectorMXBean.class);
				gcMXBeanList.add(mBean);
			}
		}
	}

	public List<GarbageCollectorMXBean> getGcMXBeanList() {
		return gcMXBeanList;
	}

	public class GCGenInfo {
		private final double gcAvgTime;
		private final long gcCount;

		public GCGenInfo(double gcAvgTime, long gcCount) {
			this.gcAvgTime = gcAvgTime;
			this.gcCount = gcCount;
		}

		/**
		 * @return the gcAvgTime
		 */
		public double getGcAvgTime() {
			return gcAvgTime;
		}

		/**
		 * @return the gcCount
		 */
		public long getGcCount() {
			return gcCount;
		}
	}
}
