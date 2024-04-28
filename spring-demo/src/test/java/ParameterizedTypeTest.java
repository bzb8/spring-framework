import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.util.*;

public class ParameterizedTypeTest<T> {

	List<Set> a1;           // 返回 Set，Class 类型
	List<Set<String>> a2;   // 返回 Set<String>，ParameterizedType 类型
	List<T> a3;             // 返回 T，TypeVariable 类型
	List<? extends Set> a4; // 返回 WildcardType 类型
	List<Set<String>[]> a5; // 返回 GenericArrayType 类型
	Map.Entry<String, Integer> me;

	@Test
	public void test() {
		// 1. 获取类上声明的泛型变量 getTypeParameters
		TypeVariable<Class<TypeVariable>> typeVariable = TypeVariable.class.getTypeParameters()[0];
		// 2. 获取泛型变量的上边界 java.lang.reflect.GenericDeclaration
		System.out.println(Arrays.toString(typeVariable.getBounds()));
	}

	@Test
	public void ownerTypeTest() throws Exception {
		Field field = getClass().getDeclaredField("me");
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		// java.util.Map
		System.out.println(type.getOwnerType());
	}

	@Test
	public void rawTypeTest() throws Exception {
		Field field = getClass().getDeclaredField("me");
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		// java.util.Map$Entry
		System.out.println(type.getRawType());
	}

	@Test
	public void testGetActualTypeArguments() throws Exception {
		// TypeVariable T, ParameterizedType Set<String>, GenericArrayType List<String>[] a
		Method method = getClass().getMethod("test", List.class);
		Type[] types = method.getGenericParameterTypes();
		ParameterizedType pType = (ParameterizedType) types[0];
		Type[] type = pType.getActualTypeArguments();
		// sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl
		System.out.println(type[0].getClass().getName());

		System.out.println("---------------------------------");
		for (Field declaredField : getClass().getDeclaredFields()) {
			Type genericType = declaredField.getGenericType();
			if (genericType instanceof ParameterizedType) {
				ParameterizedType pType1 = (ParameterizedType) genericType;
				Type[] type1 = pType1.getActualTypeArguments();
				System.out.println("field: " + declaredField.getName() + " type: " + type1[0].getClass().getName());
			}
		}
	}

	public void test(List<ArrayList<String>[]> a) {
	}
}
