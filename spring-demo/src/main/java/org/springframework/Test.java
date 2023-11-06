package org.springframework;

import org.springframework.beans.factory.BeanFactoryUtils;

/**
 * 测试方法
 *
 * @author: bzb
 * @date: 2023-10-24 19:43
 **/
public class Test {
	public static void main(String[] args) {
		String name = "&&&abc";
		System.out.println("BeanFactoryUtils.transformedBeanName(name) = " + BeanFactoryUtils.transformedBeanName(name));
	}
}
