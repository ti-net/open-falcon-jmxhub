package com.tinet.jmxhub.mbean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;

import org.apache.commons.lang.StringUtils;

import com.tinet.jmxhub.bean.FalconItem;
import com.tinet.jmxhub.constant.Constants;
import com.tinet.jmxhub.enums.CounterTypeEnum;
import com.tinet.jmxhub.mbean.ThreadingMBean.ThreadInfo;

/**
 * ThreadingMBean：获取线程数据
 * <li><b>thread.active.count</b>（当前活跃线程数）</li>
 * <li><b>thread.peak.count</b>（峰值线程数）</li>
 * <p>
 * 2017年4月13日 - 下午1:54:09
 * </p>
 * 
 * @author wangll
 * @since
 * @version
 */
public class ThreadingMBean extends AbstractMBean<ThreadInfo> implements MBean<ThreadInfo> {

	public ThreadingMBean(MBeanServerConnection connection, String jmxHost) {
		super(connection, jmxHost);
	}

	@Override
	public ThreadInfo call() throws IOException {
		ThreadMXBean threadMBean = ManagementFactory.newPlatformMXBeanProxy(getConnection(),
				ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
		int threadNum = threadMBean.getThreadCount();
		int peakThreadNum = threadMBean.getPeakThreadCount();
		return new ThreadInfo(threadNum, peakThreadNum);
	}

	@Override
	public List<FalconItem> build(ThreadInfo jmxResultData) {
		List<FalconItem> items = new ArrayList<FalconItem>();
		// 将jvm信息封装成openfalcon格式数据
		FalconItem threadNumItem = new FalconItem();
		threadNumItem.setCounterType(CounterTypeEnum.GAUGE.toString());
		threadNumItem.setEndpoint(getJmxHost());
		threadNumItem.setMetric(StringUtils.lowerCase(Constants.THREAD_ACTIVE_COUNT));
		threadNumItem.setStep(Constants.DEFAULT_STEP);
		threadNumItem.setTags(StringUtils.lowerCase(Constants.TAGS_NAME_PREFIX + getJmxHost()));
		threadNumItem.setTimestamp(System.currentTimeMillis() / 1000);
		threadNumItem.setValue(jmxResultData.getThreadNum());
		items.add(threadNumItem);

		FalconItem peakThreadNumItem = new FalconItem();
		peakThreadNumItem.setCounterType(CounterTypeEnum.GAUGE.toString());
		peakThreadNumItem.setEndpoint(getJmxHost());
		peakThreadNumItem.setMetric(StringUtils.lowerCase(Constants.THREAD_PEAK_COUNT));
		peakThreadNumItem.setStep(Constants.DEFAULT_STEP);
		peakThreadNumItem.setTags(StringUtils.lowerCase(Constants.TAGS_NAME_PREFIX + getJmxHost()));
		peakThreadNumItem.setTimestamp(System.currentTimeMillis() / 1000);
		peakThreadNumItem.setValue(jmxResultData.getPeakThreadNum());
		items.add(peakThreadNumItem);

		return items;
	}

	public class ThreadInfo {
		private final int threadNum;
		private final int peakThreadNum;

		public ThreadInfo(int threadNum, int peakThreadNum) {
			this.threadNum = threadNum;
			this.peakThreadNum = peakThreadNum;
		}

		/**
		 * @return the threadNum
		 */
		public int getThreadNum() {
			return threadNum;
		}

		/**
		 * @return the peakThreadNum
		 */
		public int getPeakThreadNum() {
			return peakThreadNum;
		}
	}

}
