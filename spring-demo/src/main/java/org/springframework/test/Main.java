package org.springframework.test;

import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.util.List;

public class Main<T> {
	List<List<T>> list;
	public static void main(String[] args) {

	}

	@Test
	public void test() throws NoSuchFieldException {
		Field list = Main.class.getDeclaredField("list");
		Type genericType = list.getGenericType();
		if (genericType instanceof ParameterizedType) {
			Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
			for (Type actualTypeArgument : actualTypeArguments) {
				System.out.println(actualTypeArgument);
			}
		}
	}

}

class MyClass {
	public void myMethod(String str, int num) {
		// 方法体
	}
}
