package com.tinet.jmxhub.factory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * MBeanServerConnectionFactory：通过此工厂类获取MBeanServerConnection对象
 * <p>
 * 2017年4月13日 - 上午10:51:05
 * </p>
 * 
 * @author wangll
 * @since
 * @version
 */
public class MBeanServerConnectionFactory {

	private static final ThreadFactory daemonThreadFactory = new DaemonThreadFactory();

	private static Map<String, MBeanServerConnection> cache = new ConcurrentHashMap<>();

	private static final String JMXURL = "service:jmx:rmi:///jndi/rmi://{0}:{1}/jmxrmi";

	private MBeanServerConnectionFactory() {
	}

	public static MBeanServerConnection getInstance(String host, String port) throws IOException {
		JMXServiceURL serviceURL = null;
		JMXConnector connector = null;
		MBeanServerConnection mbsc = cache.get(host + port);
		if (mbsc == null) {
			serviceURL = new JMXServiceURL(MessageFormat.format(JMXURL, host, port));
			connector = connectWithTimeout(serviceURL, 1000, TimeUnit.MILLISECONDS);
			mbsc = connector.getMBeanServerConnection();
			cache.put(host + port, mbsc);
		}

		return mbsc;
	}

	/**
	 * 获取JMXConnector对象
	 * 
	 * @param url
	 *            JMXServiceURL对象
	 * @param timeout
	 *            超时时间
	 * @param unit
	 *            时间单位
	 * @return
	 * @throws IOException
	 */
	private static JMXConnector connectWithTimeout(final JMXServiceURL url, long timeout, TimeUnit unit)
			throws IOException {
		final BlockingQueue<Object> mailbox = new ArrayBlockingQueue<Object>(1);
		ExecutorService executor = Executors.newSingleThreadExecutor(daemonThreadFactory);
		executor.submit(() -> {
			try {
				JMXConnector connector = JMXConnectorFactory.connect(url);
				if (!mailbox.offer(connector))
					connector.close();
			} catch (Throwable t) {
				mailbox.offer(t);
			}
		});
		Object result;
		try {
			result = mailbox.poll(timeout, unit);
			if (result == null) {
				if (!mailbox.offer(""))
					result = mailbox.take();
			}
		} catch (InterruptedException e) {
			mailbox.offer("");
			throw initCause(new InterruptedIOException(e.getMessage()), e);
		} finally {
			executor.shutdown();
		}
		if (result == null) {
			throw new SocketTimeoutException("Connect timed out: " + url);
		}
		if (result instanceof JMXConnector) {
			return (JMXConnector) result;
		}
		try {
			throw (Throwable) result;
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Error e) {
			throw e;
		} catch (Throwable e) {
			// In principle this can't happen but we wrap it anyway
			throw new IOException(e.toString(), e);
		}
	}

	private static <T extends Throwable> T initCause(T wrapper, Throwable wrapped) {
		wrapper.initCause(wrapped);
		return wrapper;
	}

	private static class DaemonThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			return t;
		}
	}
}
