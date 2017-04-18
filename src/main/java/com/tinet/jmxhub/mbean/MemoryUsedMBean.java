package com.tinet.jmxhub.mbean;

import static java.lang.management.ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.lang.StringUtils;

import com.tinet.jmxhub.bean.FalconItem;
import com.tinet.jmxhub.bean.GCData;
import com.tinet.jmxhub.bean.MemoryPoolProxy;
import com.tinet.jmxhub.constant.Constants;
import com.tinet.jmxhub.enums.CounterTypeEnum;
import com.tinet.jmxhub.mbean.MemoryUsedMBean.MemoryUsedInfo;

/**
 * MemoryUsedMBean：获取内存使用量数据
 * <li><b>new.gen.promotion</b>（一分钟内，新生代的内存晋升总大小）</li>
 * <li><b>new.gen.avg.promotion</b>（一分钟内，平均每次YoungGC的新生代内存晋升大小）</li>
 * <li><b>old.gen.mem.used</b>（老年代的内存使用量）</li>
 * <li><b>old.gen.mem.ratio</b>（老年代的内存使用率）</li>
 * <p>
 * 2017年4月13日 - 下午1:53:29
 * </p>
 * 
 * @author wangll
 * @since
 * @version
 */
public class MemoryUsedMBean extends AbstractMBean<MemoryUsedInfo> implements MBean<MemoryUsedInfo> {

	private GCGenInfoMBean gCGenInfoMBean;

	public MemoryUsedMBean(MBeanServerConnection connection, String jmxHost, GCGenInfoMBean gCGenInfoMBean) {
		super(connection, jmxHost);
		this.gCGenInfoMBean = gCGenInfoMBean;
	}

	@Override
	public MemoryUsedInfo call() throws IOException, MalformedObjectNameException {
		long oldGenUsed = 0;
		long maxOldGenMemory = 0;
		Collection<MemoryPoolProxy> memoryPoolList = new ArrayList<MemoryPoolProxy>();
		;
		ObjectName poolName = new ObjectName(MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",*");
		Set<ObjectName> mbeans = getConnection().queryNames(poolName, null);
		if (mbeans != null) {
			Iterator<ObjectName> iterator = mbeans.iterator();
			while (iterator.hasNext()) {
				ObjectName objName = iterator.next();
				MemoryPoolProxy p = new MemoryPoolProxy(getConnection(), objName);
				memoryPoolList.add(p);
			}
		}

		for (MemoryPoolProxy memoryPool : memoryPoolList) {
			String pn = memoryPool.getStat().getPoolName();
			/*
			 * see: http://stackoverflow.com/questions/16082004/how-to-identify-
			 * tenured-space/16083569#16083569
			 */
			if (pn.contains("Old Gen") || pn.contains("Tenured Gen")) {
				oldGenUsed = memoryPool.getStat().getUsage().getUsed();
				maxOldGenMemory = memoryPool.getStat().getUsage().getMax();
				break;
			}
		}
		double oldGenUsedRatio = maxOldGenMemory > 0 ? oldGenUsed * 100d / maxOldGenMemory : 0;

		/*
		 * see:
		 * http://stackoverflow.com/questions/32002001/how-to-get-minor-and-
		 * major-garbage-collection-count-in-jdk-7-and-jdk-8
		 */

		GarbageCollectorMXBean[] gcMXBeanArray = gCGenInfoMBean.getGcMXBeanList()
				.toArray(new GarbageCollectorMXBean[0]);

		// 在一个上报周期内，老年代的内存变化大小~=新生代晋升大小
		GarbageCollectorMXBean majorGCMXBean = gcMXBeanArray[1];
		GCData majorGcData = getJvmContext().getJvmData(getJmxHost()).getGcData(majorGCMXBean.getName());
		long lastOldGenMemoryUsed = majorGcData.getMemoryUsed();
		long newGenPromotion = oldGenUsed - lastOldGenMemoryUsed;
		if (lastOldGenMemoryUsed <= 0 || newGenPromotion < 0) {
			newGenPromotion = -1;
		}

		// 在一个上报周期内，YGC次数
		GarbageCollectorMXBean minorGCMXBean = gcMXBeanArray[0];
		GCData minorGcData = getJvmContext().getJvmData(getJmxHost()).getGcData(minorGCMXBean.getName());
		long gcCount = minorGcData.getUnitTimeCollectionCount();

		long newGenAvgPromotion = 0;
		if (gcCount > 0 && newGenPromotion > 0) {
			newGenAvgPromotion = (long) (newGenPromotion / gcCount);
		}

		MemoryUsedInfo memoryUsedInfo = new MemoryUsedInfo(oldGenUsed, oldGenUsedRatio, newGenPromotion,
				newGenAvgPromotion);

		// update last data
		majorGcData.setMemoryUsed(oldGenUsed);

		return memoryUsedInfo;
	}

	@Override
	public List<FalconItem> build(MemoryUsedInfo jmxResultData) {
		List<FalconItem> items = new ArrayList<FalconItem>();

		// 将jvm信息封装成openfalcon格式数据
		FalconItem oldGenUsedItem = new FalconItem();
		oldGenUsedItem.setCounterType(CounterTypeEnum.GAUGE.toString());
		oldGenUsedItem.setEndpoint(getJmxHost());
		oldGenUsedItem.setMetric(StringUtils.lowerCase(Constants.OLD_GEN_MEM_USED));
		oldGenUsedItem.setStep(Constants.DEFAULT_STEP);
		oldGenUsedItem.setTags(StringUtils.lowerCase(Constants.TAGS_NAME_PREFIX + getJmxHost()));
		oldGenUsedItem.setTimestamp(System.currentTimeMillis() / 1000);
		oldGenUsedItem.setValue(jmxResultData.getOldGenUsed());
		items.add(oldGenUsedItem);

		FalconItem oldGenUsedRatioItem = new FalconItem();
		oldGenUsedRatioItem.setCounterType(CounterTypeEnum.GAUGE.toString());
		oldGenUsedRatioItem.setEndpoint(getJmxHost());
		oldGenUsedRatioItem.setMetric(StringUtils.lowerCase(Constants.OLD_GEN_MEM_RATIO));
		oldGenUsedRatioItem.setStep(Constants.DEFAULT_STEP);
		oldGenUsedRatioItem.setTags(StringUtils.lowerCase(Constants.TAGS_NAME_PREFIX + getJmxHost()));
		oldGenUsedRatioItem.setTimestamp(System.currentTimeMillis() / 1000);
		oldGenUsedRatioItem.setValue(jmxResultData.getOldGenUsedRatio());
		items.add(oldGenUsedRatioItem);

		FalconItem newGenPromotionItem = new FalconItem();
		newGenPromotionItem.setCounterType(CounterTypeEnum.GAUGE.toString());
		newGenPromotionItem.setEndpoint(getJmxHost());
		newGenPromotionItem.setMetric(StringUtils.lowerCase(Constants.NEW_GEN_PROMOTION));
		newGenPromotionItem.setStep(Constants.DEFAULT_STEP);
		newGenPromotionItem.setTags(StringUtils.lowerCase(Constants.TAGS_NAME_PREFIX + getJmxHost()));
		newGenPromotionItem.setTimestamp(System.currentTimeMillis() / 1000);
		newGenPromotionItem.setValue(jmxResultData.getNewGenPromotion());
		items.add(newGenPromotionItem);

		FalconItem newGenAvgPromotionItem = new FalconItem();
		newGenAvgPromotionItem.setCounterType(CounterTypeEnum.GAUGE.toString());
		newGenAvgPromotionItem.setEndpoint(getJmxHost());
		newGenAvgPromotionItem.setMetric(StringUtils.lowerCase(Constants.NEW_GEN_AVG_PROMOTION));
		newGenAvgPromotionItem.setStep(Constants.DEFAULT_STEP);
		newGenAvgPromotionItem.setTags(StringUtils.lowerCase(Constants.TAGS_NAME_PREFIX + getJmxHost()));
		newGenAvgPromotionItem.setTimestamp(System.currentTimeMillis() / 1000);
		newGenAvgPromotionItem.setValue(jmxResultData.getNewGenAvgPromotion());
		items.add(newGenAvgPromotionItem);

		return items;
	}

	public class MemoryUsedInfo {
		private final double oldGenUsedRatio;
		private final long oldGenUsed;
		private final long newGenPromotion;
		private final long newGenAvgPromotion;

		public MemoryUsedInfo(long oldGenUsed, double oldGenUsedRatio, long newGenPromotion, long newGenAvgPromotion) {
			this.oldGenUsed = oldGenUsed;
			this.oldGenUsedRatio = oldGenUsedRatio;
			this.newGenPromotion = newGenPromotion;
			this.newGenAvgPromotion = newGenAvgPromotion;
		}

		/**
		 * @return the oldGenUsedRatio
		 */
		public double getOldGenUsedRatio() {
			return oldGenUsedRatio;
		}

		/**
		 * @return the oldGenUsed
		 */
		public long getOldGenUsed() {
			return oldGenUsed;
		}

		/**
		 * @return the newGenPromotion
		 */
		public long getNewGenPromotion() {
			return newGenPromotion;
		}

		/**
		 * @return the newGenAvgPromotion
		 */
		public long getNewGenAvgPromotion() {
			return newGenAvgPromotion;
		}

	}
}
