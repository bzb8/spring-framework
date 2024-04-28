import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResolvableTypeTest {

	private Service<Double, Float> service;
	private List<List<String>> list;
	private Map<String, Map<String, Integer>> map;
	private List<String>[] array;

	@Test
	public void forFieldTest() {
		// 1. Service<Double, Float> service
		Field filed = ReflectionUtils.findField(ResolvableTypeTest.class, "service");
		ResolvableType resolvableType = ResolvableType.forField(filed);
		Type genericType = filed.getGenericType();
		// getType() 保存原始的 Type 类型
		assertEquals(genericType, resolvableType.getType());
		// resolve() 对于 ParameterizedType 类型保存的是 <> 之前的类型，即 Service.class
		assertEquals(((ParameterizedType) genericType).getRawType(), resolvableType.resolve());

		Class<?> clazz = resolvableType.getGeneric(0).resolve();
		assertEquals(Double.class, clazz);

		// 2. List<List<String>> list
		resolvableType = ResolvableType.forField(ReflectionUtils.findField(ResolvableTypeTest.class, "list"));
		// 下面两种获取泛型的方式等价
		clazz = resolvableType.getGeneric(0).getGeneric(0).resolve();
		assertEquals(String.class, clazz);
		clazz = resolvableType.getGeneric(0, 0).resolve();
		assertEquals(String.class, clazz);

		// 3. Map<String, Map<String, Integer>> map
		resolvableType = ResolvableType.forField(ReflectionUtils.findField(ResolvableTypeTest.class, "map"));
		clazz = resolvableType.getGeneric(1).getGeneric(1).resolve();
		assertEquals(Integer.class, clazz);

		// 4. List<String>[] array
		resolvableType = ResolvableType.forField(ReflectionUtils.findField(ResolvableTypeTest.class, "array"));
		assertTrue(resolvableType.isArray());
		assertEquals(List.class, resolvableType.getComponentType().resolve());
		assertEquals(String.class, resolvableType.getComponentType().getGeneric(0).resolve());
	}

	@Test
	public void forMethodTest() {
		// 1. 方法的返回值类型
		ResolvableType returnType = ResolvableType.forMethodReturnType(
				ReflectionUtils.findMethod(ServiceImpl.class, "method"));
		assertEquals(Double.class, returnType.getGeneric(1, 0).resolve());

		// 2. 构造器 ServiceImpl(List<List<String>> list, Map<Double, Map<Float, Integer>> map)
		ResolvableType parameterType = ResolvableType.forConstructorParameter(
				ClassUtils.getConstructorIfAvailable(ServiceImpl.class, List.class, Map.class), 0);
		// List<List<String>> 的泛型第一层为 <List<String>>，第二层为 <String>
		assertEquals(String.class, parameterType.getGeneric(0, 0).resolve());

		parameterType = ResolvableType.forConstructorParameter(
				ClassUtils.getConstructorIfAvailable(ServiceImpl.class, List.class, Map.class), 1);
		assertEquals(Double.class, parameterType.getGeneric(0).resolve());
		assertEquals(Float.class, parameterType.getGeneric(1, 0).resolve());
		assertEquals(Integer.class, parameterType.getGeneric(1, 1).resolve());
	}

	@Test
	public void test() {
		// HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable
		ResolvableType resolvableType = ResolvableType.forClass(HashMap.class);
		// 1. getInterfaces 获取接口
		assertEquals(Map.class, resolvableType.getInterfaces()[0].resolve());

		// 2. getSuperType 获取父类
		assertEquals(AbstractMap.class, resolvableType.getSuperType().resolve());

		// 3. as 向上转型 Map<K,V>
		ResolvableType mapResolvableType = resolvableType.as(Map.class);
		assertEquals(Map.class, mapResolvableType.resolve());
		// 4. getRawClass 当 type 是 ParameterizedType 时有效
		assertEquals(Map.class, mapResolvableType.getRawClass());
		assertEquals(HashMap.class.getGenericInterfaces()[0], mapResolvableType.getType());

		// 5. getGeneric 获取泛型 class ServiceImpl<A, B> implements Service<String, Integer>
		resolvableType = ResolvableType.forClass(ServiceImpl.class);
		// 当 Type 无法找到具体的 class 类型时返回 null
		assertEquals("A", resolvableType.getGeneric(0).getType().getTypeName());
		assertEquals(null, resolvableType.getGeneric(0).resolve());
		// 以下两种获取泛型的 Class 类型方式等价
		assertEquals(String.class, resolvableType.as(Service.class).getGeneric(0).resolve());
		assertEquals(String.class, resolvableType.as(Service.class).resolveGeneric(0));

		// 5. getComponentType 获取数组泛型 List<String>[] array
		resolvableType = ResolvableType.forField(
				ReflectionUtils.findField(ResolvableTypeTest.class, "array"));
		assertEquals(List.class, resolvableType.getComponentType().resolve());
	}

	@Test
	public void test2() {
		ResolvableType resolvableType1 = ResolvableType.forClassWithGenerics(List.class, String.class);
		ResolvableType resolvableType2 = ResolvableType.forArrayComponent(resolvableType1);
		resolvableType2.getComponentType().getGeneric(0).resolve();

		// List<String>[] array
		ResolvableType resolvableType3 = ResolvableType.forField(
				ReflectionUtils.findField(ResolvableTypeTest.class, "array"));
		assertTrue(resolvableType3.isAssignableFrom(resolvableType2));

		assertTrue(ResolvableType.forClass(Object.class).isAssignableFrom(
				ResolvableType.forClass(String.class)));
	}


}
