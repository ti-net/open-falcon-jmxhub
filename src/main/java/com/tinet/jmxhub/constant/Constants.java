package com.tinet.jmxhub.constant;

public class Constants {

	// 一分钟内，每次YoungGC的平均耗时
	public static final String GC_AVG_TIME = "gc.avg.time";
	// 一分钟内，每次FullGC的平均耗时
	public static final String GC_COUNT = "gc.count";
	// GC的总吞吐率（应用运行时间/进程总运行时间）
	public static final String GC_THROUGH_PUT = "gc.throughput";
	// 一分钟内，新生代的内存晋升总大小
	public static final String NEW_GEN_PROMOTION = "new.gen.promotion";
	// 一分钟内，平均每次YoungGC的新生代内存晋升大小
	public static final String NEW_GEN_AVG_PROMOTION = "new.gen.avg.promotion";
	// 老年代的内存使用量
	public static final String OLD_GEN_MEM_USED = "old.gen.mem.used";
	// 老年代的内存使用率
	public static final String OLD_GEN_MEM_RATIO = "old.gen.mem.ratio";
	// 当前活跃线程数
	public static final String THREAD_ACTIVE_COUNT = "thread.active.count";
	// 峰值线程数
	public static final String THREAD_PEAK_COUNT = "thread.peak.count";

	public static final String TAG_SEPARATOR = ",";
	public static final String TAGS_NAME_PREFIX = "jmxhost=";
	public static final String METRIC_SEPARATOR = ".";
	public static final int DEFAULT_STEP = 60; // 单位秒

	// 正常状态
	public static final int HOST_STATUS_NORMAL = 0;
	// 连接失败
	public static final int HOST_STATUS_FAIL = 1;
	// 等待重试
	public static final int HOST_STATUS_RETRY = 2;

}
