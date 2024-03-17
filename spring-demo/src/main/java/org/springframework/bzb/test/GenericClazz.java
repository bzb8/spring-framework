package org.springframework.bzb.test;

import org.springframework.core.ResolvableType;

import java.util.HashMap;
import java.util.List;

public class GenericClazz<T extends String> {

    private HashMap<String, List<Integer>> param;
	private T a;

    public static void main(String[] args) throws NoSuchFieldException {
        //printParmaBySpring();
		test2();
    }

	private static void test2() throws NoSuchFieldException {
		ResolvableType param = ResolvableType.forField(GenericClazz.class.getDeclaredField("param"));
		ResolvableType[] generics = param.getGenerics();
		for (ResolvableType generic : generics) {
			System.out.println(generic);
		}

		Class<?>[] classes = param.resolveGenerics();
		for (Class<?> aClass : classes) {
			System.out.println(aClass);
		}
	}

	private static void printParmaBySpring() throws NoSuchFieldException {
        ResolvableType param = ResolvableType.forField(GenericClazz.class.getDeclaredField("param"));
        System.out.println("从 HashMap<String, List<Integer>> 中获取 String:" + param.getGeneric(0).resolve());
        System.out.println("从 HashMap<String, List<Integer>> 中获取 List<Integer> :" + param.getGeneric(1));
        System.out.println(
            "从 HashMap<String, List<Integer>> 中获取 List :" + param.getGeneric(1).resolve());
        System.out.println("从 HashMap<String, List<Integer>> 中获取 Integer:" + param.getGeneric(1,0));
        System.out.println("从 HashMap<String, List<Integer>> 中获取父类型:" +param.getSuperType());
    }
}
