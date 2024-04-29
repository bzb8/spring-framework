import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TypeVariableTest<E> {

	List<E>[][] key;

	@Test
	public void testGenericArrayTest() throws NoSuchFieldException {
		Type type = TypeVariableTest.class.getDeclaredField("key").getGenericType();
		System.out.println(((GenericArrayType) type).getGenericComponentType());

	}


	@Test
	public void getGenericDeclarationTest() {
		// 1. 类上声明泛型
		TypeVariable<Class<TypeVariableTest>> classType = TypeVariableTest.class.getTypeParameters()[0];
		Class<TypeVariableTest> clazzDeclaration = classType.getGenericDeclaration();
		// class com.github.binarylei.spring01.day0728.test.TypeVariableTest
		System.out.println(clazzDeclaration);

		// 2. 方法上声明泛型
		Method[] methods = TypeVariableTest.class.getMethods();
		Method method = Arrays.stream(methods)
				.filter(m -> m.getName().equals("test"))
				.collect(Collectors.toList())
				.get(0);
		TypeVariable methodType = (TypeVariable) method.getGenericParameterTypes()[0];
		GenericDeclaration methodDeclaration = methodType.getGenericDeclaration();
		// public void com.github.binarylei.TypeVariableTest.test(java.lang.Object)
		System.out.println(methodDeclaration);

		// 3. 构造器上声明泛型
	}

	public <T> void test(T t) {
	}
}
