package org.springframework;

import org.springframework.beans.factory.BeanFactoryUtils;

import java.util.ArrayList;
import java.util.List;

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

		System.out.println(name.getClass().getName());

		List<String> list1 = new ArrayList<>();
		list1.add("a");
		list1.add("b");
		List<String> list2 = new ArrayList<>();
		list2.add("c");
		list2.add("d");
		list1.addAll(list2);

		System.out.println(list1);
		list2.clear();
		System.out.println(list1);
	}
}
