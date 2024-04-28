import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public class MethodParameterTests {

	public static class TestItem {
		public TestItem(List<String> list) {

		}

		public void m1(List<Integer> intParam) {

		}
	}

	@Test
	public void test() throws NoSuchMethodException {

		{
			Method method = TestItem.class.getMethod("m1", List.class);
			MethodParameter parameter = MethodParameter.forMethodOrConstructor(method, 0);
			System.out.println(parameter.getParameterType());
			System.out.println(parameter.getGenericParameterType());
		}

		{
			Constructor constructor = TestItem.class.getConstructor(List.class);
			MethodParameter parameter = MethodParameter.forMethodOrConstructor(constructor, 0);
			System.out.println(parameter.getParameterType());
			System.out.println(parameter.getGenericParameterType());
		}
	}
}