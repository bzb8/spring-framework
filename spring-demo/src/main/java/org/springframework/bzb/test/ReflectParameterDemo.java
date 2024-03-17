package org.springframework.bzb.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

/**
 * Created by 韩超 on 2018/3/1.
 */
public class ReflectParameterDemo {

	/**
	 * <p>Title: java反射-参数Parameter</p>
	 *
	 * @author 韩超 2018/3/1 15:56
	 */
	public static void main(String[] args) throws NoSuchMethodException {
		//===================================== 通过Class对象获取Parameter对象 =====================================
		System.out.println("===================================== 通过Class对象获取Parameter对象 =====================================");
		//首先获取Class对象
		Class<User> userClass = User.class;
		System.out.println("首先获取Class对象：" + userClass);
		//然后获取Method（或者Constructor）对象
		Method method = userClass.getDeclaredMethod("initUser", String.class, String.class);
		System.out.println("然后获取Method或者Constructor对象：" + method);
		//然后获取Parameter对象数组
		Parameter[] parameters = method.getParameters();
		System.out.println("然后通过getParameters()获取Parameter对象数组");
		//然后获取Parameter对象
		Parameter parameter = parameters[0];
		System.out.println("最终获得参数Parameter对象" + parameter + "\n");

		//===================================== Parameter信息获取 =====================================
		System.out.println("===================================== Parameter信息获取 =====================================");
		System.out.println("通过parameter.getModifiers()获取参数修饰符:" + Modifier.toString(parameter.getModifiers()));
		System.out.println("通过parameter.getName()获取参数名：" + parameter.getName());
		System.out.println("通过parameter.getParameterizedType()获取参数化类型(泛型)：" + parameter.getParameterizedType());
		System.out.println("通过parameter.toString()获取参数的字符串描述：" + parameter.toString());
		System.out.println("通过parameter.isSynthetic()判断参数是否是合成的：" + parameter.isSynthetic());
		System.out.println("通过parameter.isImplicit()判断参数是否是隐式的：" + parameter.isImplicit());
		System.out.println("通过parameter.isNamePresent()判断参数是否以类文件名命名：" + parameter.isNamePresent());
		System.out.println("通过parameter.isVarArgs()判断参数是否是可变的：" + parameter.isVarArgs() + "\n");

		//===================================== Parameter注解信息 =====================================
		System.out.println("===================================== Parameter注解信息 =====================================");
		//通过parameter.getAnnotatedType()获取注解的类型（组合类型）
		AnnotatedType annotatedType = parameter.getAnnotatedType();
		System.out.println("通过parameter.getAnnotatedType()获取注解的类型（组合类型）--参数类型：" + annotatedType.getType() + "\n");

		//通过parameter.getAnnotation()和parameter.getDeclaredAnnotation()获取参数的一个注解
		System.out.println("通过parameter.getAnnotation()获取参数的一个注解:" + parameter.getAnnotation(MyAnnotationB.class));
		System.out.println("通过parameter.getDeclaredAnnotation()获取参数的一个注解:" + parameter.getDeclaredAnnotation(MyAnnotationB.class) + "\n");

		//通过parameter.getAnnotationsByType(annotation.class)获取一类注解
		Annotation[] typeAnnotations = parameter.getAnnotationsByType(MyAnnotationB.class);
		for (Annotation annotation : typeAnnotations) {
			System.out.println("通过parameter.getAnnotationsByType(annotation.class)获取一类注解：" + annotation);
		}
		//通过parameter.getDeclaredAnnotationsByType(annotation.class)获取一类注解
		Annotation[] typeAnnotations1 = parameter.getDeclaredAnnotationsByType(MyAnnotationB.class);
		for (Annotation annotation : typeAnnotations1) {
			System.out.println("通过parameter.getDeclaredAnnotationsByType(annotation.class)获取一类注解：" + annotation);
		}
		System.out.println("");

		//通过parameter.getAnnotations()获取全部注解
		Annotation[] annotations = parameter.getAnnotations();
		for (Annotation annotation : annotations) {
			System.out.println("通过parameter.getAnnotations()获取全部注解:" + annotation);
		}
		//通过parameter.getDeclaredAnnotations()获取全部注解
		Annotation[] annotations1 = parameter.getDeclaredAnnotations();
		for (Annotation annotation : annotations1) {
			System.out.println("通过parameter.getDeclaredAnnotations()获取全部注解:" + annotation);
		}

		Method testMethod = ReflectParameterDemo.class.getMethod("testMethod", String[].class);
		Parameter[] testMethodParameters = testMethod.getParameters();
		for (Parameter parameter1 : testMethodParameters) {
			System.out.println("Parameter Name: " + parameter1.getName());
			System.out.println("Is Implicit: " + parameter1.isImplicit());
			System.out.println("---------------------------");
		}

	}


	public static void testMethod(String... args) {
	}
}