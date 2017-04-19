package com.tinet.jmxhub.task;

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinet.jmxhub.bean.FalconItem;
import com.tinet.jmxhub.bean.HostStatus;
import com.tinet.jmxhub.constant.Constants;
import com.tinet.jmxhub.factory.MBeanServerConnectionFactory;
import com.tinet.jmxhub.mbean.GCGenInfoMBean;
import com.tinet.jmxhub.mbean.GCThroughputMbean;
import com.tinet.jmxhub.mbean.MBean;
import com.tinet.jmxhub.mbean.MemoryUsedMBean;
import com.tinet.jmxhub.mbean.MemoryUsedMBean.MemoryUsedInfo;
import com.tinet.jmxhub.mbean.ThreadingMBean;
import com.tinet.jmxhub.mbean.ThreadingMBean.ThreadInfo;

/**
 * OpenFalconDataCollector：获取jmx信息并推送到小米在本机的agent
 * <p>
 * 2017年4月10日 - 下午3:54:59
 * </p>
 * 
 * @author wangll
 * @since 1.3.3
 * @version 1.3.3
 */
@Component
public class OpenFalconDataProvider {
	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String DEFAULT_AGENT_URI = "http://localhost:1988/v1/push";
	private static final String MONITOR_JMX_HOSTS = "monitor.jmx.hosts";
	private static final String JMXHUB_AGENT_URI = "jmxhub.agent.uri";

	private Set<HostStatus> hosts = null;

	private PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
	ConnectionKeepAliveStrategy myStrategy = (response, context) -> 2000;

	private CloseableHttpClient httpClient = HttpClients.custom().setKeepAliveStrategy(myStrategy)
			.setConnectionManager(cm).build();

	@Autowired
	private Environment environment;

	@Scheduled(cron = "0 0/1 * * * ?")
	public void pushData() {

		if (hosts == null) {
			hosts = getHosts();
		}
		List<FalconItem> items = new ArrayList<>();

		hosts.forEach((host) -> {
			Optional<MBeanServerConnection> optCon = Optional.empty();
			String jmxHost = host.getHost();

			switch (host.getStatus()) {
			case Constants.HOST_STATUS_NORMAL:
				try {
					if (logger.isInfoEnabled()) {
						logger.info(jmxHost + "[获取连接]");
					}
					optCon = Optional.ofNullable(MBeanServerConnectionFactory.getInstance(host.getHost()));
					if (logger.isInfoEnabled()) {
						logger.info(jmxHost + "[获取连接成功]");
					}
				} catch (IOException e) {
					logger.error(jmxHost + "[获取连接失败]");
					host.addFailCount();
				}
				break;
			case Constants.HOST_STATUS_FAIL:
				host.doCountDown();
				break;
			case Constants.HOST_STATUS_RETRY:
				try {
					if (logger.isInfoEnabled()) {
						logger.info(jmxHost + "[获取连接]");
					}
					optCon = Optional.ofNullable(MBeanServerConnectionFactory.getInstance(host.getHost()));
					if (logger.isInfoEnabled()) {
						logger.info(jmxHost + "[获取连接成功]");
					}
					host.initial();
				} catch (IOException e) {
					logger.error(jmxHost + "[重新连接失败]");
					host.resetCountDown();
				}
				break;

			default:
				// do nothing
				break;
			}

			optCon.ifPresent((connection) -> {
				MBean<ThreadInfo> threadingMBean = new ThreadingMBean(connection, jmxHost);
				GCGenInfoMBean gcGenInfoMBean = new GCGenInfoMBean(connection, jmxHost);
				MBean<Double> gcThroughputMbean = new GCThroughputMbean(connection, jmxHost, gcGenInfoMBean);
				MBean<MemoryUsedInfo> memoryUsedMBean = new MemoryUsedMBean(connection, jmxHost, gcGenInfoMBean);
				try {
					items.addAll(gcGenInfoMBean.getResult());
					items.addAll(gcThroughputMbean.getResult());
					items.addAll(threadingMBean.getResult());
					items.addAll(memoryUsedMBean.getResult());
				} catch (IOException e) {
					logger.error(e.toString(), e);
				} catch (MalformedObjectNameException e) {
					logger.error(e.toString(), e);
				}
			});
		});

		HttpPost post = new HttpPost(getUri());
		post.setHeader("Charset", "UTF-8");
		RequestConfig config = RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).build();
		post.setConfig(config);

		HttpEntity entity = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			entity = new StringEntity(mapper.writeValueAsString(items), "UTF-8");
			if (logger.isInfoEnabled()) {
				logger.info(mapper.writeValueAsString(items));
			}
			post.setEntity(entity);
		} catch (UnsupportedCharsetException e) {
			logger.error(e.toString(), e);
		} catch (JsonProcessingException e) {
			logger.error(e.toString(), e);
		}
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(post);
		} catch (ClientProtocolException e) {
			logger.error("监控数据推送失败：[agent=" + getUri() + "]");
		} catch (IOException e) {
			logger.error("监控数据推送失败：[agent=" + getUri() + "]");
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private Set<HostStatus> getHosts() {
		Optional<String> hostArray = Optional.ofNullable(environment.getProperty(MONITOR_JMX_HOSTS));
		Set<HostStatus> hs = new HashSet<>();
		hostArray.ifPresent((host) -> {
			String[] hosts = host.split(",");
			for (int i = 0; i < hosts.length; i++) {
				HostStatus status = new HostStatus();
				status.setHost(hosts[i]);
				hs.add(status);
			}
		});
		return hs;
	}

	private String getUri() {
		Optional<String> uri = Optional.ofNullable(environment.getProperty(JMXHUB_AGENT_URI));
		return uri.orElse(DEFAULT_AGENT_URI);
	}
}
