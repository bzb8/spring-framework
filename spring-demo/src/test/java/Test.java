package cn.test;

import java.lang.reflect.Type;

public class Test {

	public static void printInterface(Class<?>[] cs) {
		System.out.print(cs.length + "\t");
		for (Class<?> c : cs) {
			System.out.print(c.getCanonicalName() + "\t");
		}
		System.out.println();
	}

	public static void printInterface(Type[] cs) {
		System.out.print(cs.length + "\t");
		for (Type c : cs) {
			System.out.print(c.toString() + "\t");
		}
		System.out.println();
	}

	public static void main(String[] args) {
		//IStudent
		System.out.print("IStudent.class.getInterfaces()\t");
		printInterface(IStudent.class.getInterfaces());
		System.out.print("IStudent.class.getGenericInterfaces()\t");
		printInterface(IStudent.class.getGenericInterfaces());

		//Test
		System.out.print("Test.class.getInterfaces()\t");
		printInterface(Test.class.getInterfaces());
		System.out.print("Test.class.getGenericInterfaces()\t");
		printInterface(Test.class.getGenericInterfaces());

		//Object
		System.out.print("Object.class.getGenericInterfaces()\t");
		printInterface(Object.class.getGenericInterfaces());
		System.out.print("Object.class.getInterfaces()\t");
		printInterface(Object.class.getInterfaces());

		//void
		System.out.print("void.class.getInterfaces()\t");
		printInterface(void.class.getInterfaces());
		System.out.print("void.class.getGenericInterfaces()\t");
		printInterface(void.class.getGenericInterfaces());

		//int[]
		System.out.print("int[].class.getInterfaces()\t");
		printInterface(int[].class.getInterfaces());
		System.out.print("int[].class.getGenericInterfaces()\t");
		printInterface(int[].class.getGenericInterfaces());
	}

}

interface IPerson<T> {

}

interface IWalk<T> {

}

interface IStudent extends IPerson<Test>, IWalk<Object>, Cloneable {

}