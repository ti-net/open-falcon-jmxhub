package com.tinet.jmxhub.bean;

import com.tinet.jmxhub.constant.Constants;

/**
 * HostStatus：
 * <p>
 * 2017年4月14日 - 上午11:54:02
 * </p>
 * 
 * @author wangll
 * @since
 * @version
 */
public class HostStatus {

	private static final int FAIL_THRESHOLD = 3;
	private static final int RETRIES_THRESHOLD = 10;

	private String host;
	private int status;
	private int failCounter;
	private int retriesCountDown;

	public void addFailCount() {
		failCounter++;
		if (failCounter >= FAIL_THRESHOLD) {
			status = Constants.HOST_STATUS_FAIL;
		}
	}

	public void doCountDown() {
		retriesCountDown++;
		if (retriesCountDown >= RETRIES_THRESHOLD) {
			status = Constants.HOST_STATUS_RETRY;
		}
	}

	public void resetCountDown() {
		retriesCountDown = 0;
		status = Constants.HOST_STATUS_FAIL;
	}

	public void initial() {
		status = Constants.HOST_STATUS_NORMAL;
		failCounter = 0;
		retriesCountDown = 0;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getFailCounter() {
		return failCounter;
	}

	public int getRetriesCountDown() {
		return retriesCountDown;
	}

	public int getStatus() {
		return status;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HostStatus other = (HostStatus) obj;
		if (failCounter != other.failCounter)
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (retriesCountDown != other.retriesCountDown)
			return false;
		if (status != other.status)
			return false;
		return true;
	}
}
