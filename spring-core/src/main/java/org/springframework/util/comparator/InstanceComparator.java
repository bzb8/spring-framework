/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.comparator;

import java.util.Comparator;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Compares objects based on an arbitrary class order. Allows objects to be sorted based
 * on the types of class that they inherit &mdash; for example, this comparator can be used
 * to sort a list of {@code Number}s such that {@code Long}s occur before {@code Integer}s.
 *
 * 根据任意类顺序比较对象。允许根据对象继承的类类型对对象进行排序，例如，此比较器可用于对 {@code Number} 列表进行排序，以便 {@code Long} 出现在 {@code Integer} 之前。
 *
 * <p>Only the specified {@code instanceOrder} classes are considered during comparison.
 * If two objects are both instances of the ordered type this comparator will return a
 * value of {@code 0}. Consider combining with {@link Comparator#thenComparing(Comparator)}
 * if additional sorting is required.
 *
 * 在比较过程中，仅考虑指定的 {@code instanceOrder} 类。
 * 如果两个对象都是有序类型的实例，则此比较器将返回值 {@code 0}。如果需要其他排序，请考虑与 {@link Comparator#thenComparing(Comparator)} 结合使用。
 *
 * @author Phillip Webb
 * @since 3.2
 * @param <T> the type of objects that may be compared by this comparator
 * @see Comparator#thenComparing(Comparator)
 */
public class InstanceComparator<T> implements Comparator<T> {

	private final Class<?>[] instanceOrder;


	/**
	 * Create a new {@link InstanceComparator} instance.
	 * @param instanceOrder the ordered list of classes that should be used when comparing
	 * objects. Classes earlier in the list will be given a higher priority.
	 *
	 * 比较对象时应使用的类的有序列表。列表中前面的类将被赋予更高的优先级。
	 */
	public InstanceComparator(Class<?>... instanceOrder) {
		Assert.notNull(instanceOrder, "'instanceOrder' array must not be null");
		this.instanceOrder = instanceOrder;
	}


	@Override
	public int compare(T o1, T o2) {
		int i1 = getOrder(o1);
		int i2 = getOrder(o2);
		return (Integer.compare(i1, i2));
	}

	private int getOrder(@Nullable T object) {
		// 返回参数object对应的instanceOrder的索引下标
		if (object != null) {
			for (int i = 0; i < this.instanceOrder.length; i++) {
				if (this.instanceOrder[i].isInstance(object)) {
					return i;
				}
			}
		}
		// object为null，返回instanceOrder的长度
		return this.instanceOrder.length;
	}

}
