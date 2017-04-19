/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.tinet.jmxhub.bean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.sun.management.GarbageCollectorMXBean;
import com.sun.management.GcInfo;

public class MemoryPoolProxy {
	private String poolName;
	private MBeanServerConnection connection;
	private ObjectName objName;
	private MemoryPoolMXBean pool;
	private Map<ObjectName, Long> gcMBeans;
	private GcInfo lastGcInfo;

	public MemoryPoolProxy(MBeanServerConnection connection, ObjectName objName) throws IOException {
		this.connection = connection;
		this.objName = objName;
		init(connection, objName);
	}

	private void init(MBeanServerConnection connection, ObjectName objName) throws IOException {
		this.pool = ManagementFactory.newPlatformMXBeanProxy(connection, objName.toString(), MemoryPoolMXBean.class);
		this.poolName = this.pool.getName();
		this.gcMBeans = new HashMap<ObjectName, Long>();
		this.lastGcInfo = null;
		String[] mgrNames = pool.getMemoryManagerNames();
		for (String name : mgrNames) {
			try {
				ObjectName mbeanName = new ObjectName(
						ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",name=" + name);
				if (connection.isRegistered(mbeanName)) {
					gcMBeans.put(mbeanName, new Long(0));
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}

		}
	}

	public boolean isCollectedMemoryPool() {
		return (gcMBeans.size() != 0);
	}

	public ObjectName getObjectName() {
		return objName;
	}

	public MemoryPoolStat getStat() throws java.io.IOException {
		long usageThreshold = (pool.isUsageThresholdSupported() ? pool.getUsageThreshold() : -1);
		long collectThreshold = (pool.isCollectionUsageThresholdSupported() ? pool.getCollectionUsageThreshold() : -1);
		long lastGcStartTime = 0;
		long lastGcEndTime = 0;
		MemoryUsage beforeGcUsage = null;
		MemoryUsage afterGcUsage = null;
		long gcId = 0;
		if (lastGcInfo != null) {
			gcId = lastGcInfo.getId();
			lastGcStartTime = lastGcInfo.getStartTime();
			lastGcEndTime = lastGcInfo.getEndTime();
			beforeGcUsage = lastGcInfo.getMemoryUsageBeforeGc().get(poolName);
			afterGcUsage = lastGcInfo.getMemoryUsageAfterGc().get(poolName);
		}

		Set<Map.Entry<ObjectName, Long>> set = gcMBeans.entrySet();
		for (Map.Entry<ObjectName, Long> e : set) {
			GarbageCollectorMXBean gc = ManagementFactory.newPlatformMXBeanProxy(connection, e.getKey().toString(),
					GarbageCollectorMXBean.class);

			Long gcCount = e.getValue();
			Long newCount = gc.getCollectionCount();
			if (newCount > gcCount) {
				gcMBeans.put(e.getKey(), new Long(newCount));
				lastGcInfo = gc.getLastGcInfo();
				if (lastGcInfo.getEndTime() > lastGcEndTime) {
					gcId = lastGcInfo.getId();
					lastGcStartTime = lastGcInfo.getStartTime();
					lastGcEndTime = lastGcInfo.getEndTime();
					beforeGcUsage = lastGcInfo.getMemoryUsageBeforeGc().get(poolName);
					afterGcUsage = lastGcInfo.getMemoryUsageAfterGc().get(poolName);
					assert (beforeGcUsage != null);
					assert (afterGcUsage != null);
				}
			}
		}

		MemoryUsage usage = pool.getUsage();
		return new MemoryPoolStat(poolName, usageThreshold, usage, gcId, lastGcStartTime, lastGcEndTime,
				collectThreshold, beforeGcUsage, afterGcUsage);
	}
}
