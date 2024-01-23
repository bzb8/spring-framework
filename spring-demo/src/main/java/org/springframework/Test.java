package org.springframework;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 测试方法
 *
 * @author: bzb
 * @date: 2023-10-24 19:43
 **/
public class Test {
	public static void main(String[] args) {
		List<String> list = Arrays.asList("a", "b");
		System.out.println(StringUtils.collectionToDelimitedString(list, "]" + "["));
	}

}
