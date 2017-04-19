package com.tinet;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Application.java：
 * <p>
 * 2017年4月1日-下午12:43:47
 * </p>
 * 
 * @author wangll
 * @since
 * @version
 */
public class Application {

	public static void main(String[] args) {
		// 加载Spring容器
		@SuppressWarnings({ "resource", "unused" })
		ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:spring/spring-*.xml");

		synchronized (Application.class) {
			while (true) {
				try {
					Application.class.wait();
				} catch (Throwable e) {
				}
			}
		}
	}
}
