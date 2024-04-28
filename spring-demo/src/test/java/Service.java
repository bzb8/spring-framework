import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

interface Service<N, M> {
}

class ServiceImpl<A, B> implements Service<String, Integer> {
	public ServiceImpl(List<List<String>> list, Map<Double, Map<Float, Integer>> map) {
	}

	@Test
	public void forClassTest() {
		ResolvableType resolvableType = ResolvableType.forClass(ServiceImpl.class);
		// getType 保存原始的 Type 类型
		assertEquals(ServiceImpl.class, resolvableType.getType());
		// resolve 将 Type 解析为 Class， 如果无法解析返回 null
		assertEquals(ServiceImpl.class, resolvableType.resolve());
	}

}
