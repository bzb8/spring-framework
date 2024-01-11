/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.core.SerializableTypeWrapper.FieldTypeProvider;
import org.springframework.core.SerializableTypeWrapper.MethodParameterTypeProvider;
import org.springframework.core.SerializableTypeWrapper.TypeProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Encapsulates a Java {@link java.lang.reflect.Type}, providing access to
 * {@link #getSuperType() supertypes}, {@link #getInterfaces() interfaces}, and
 * {@link #getGeneric(int...) generic parameters} along with the ability to ultimately
 * {@link #resolve() resolve} to a {@link java.lang.Class}.
 *
 * 封装 Java {@link java.lang.reflect.Type}，提供对 {@link #getSuperType() 超类型}、{@link #getInterfaces() 接口}和 {@link #getGeneric(int...) 泛型参数}
 * 的访问以及最终将 {@link #resolve() 解析} 为 {@link java.lang.Class} 的能力。
 * 可以看作是封装Java Type的元信息类
 *
 * <p>A {@code ResolvableType} may be obtained from a {@linkplain #forField(Field) field},
 * a {@linkplain #forMethodParameter(Method, int) method parameter},
 * a {@linkplain #forMethodReturnType(Method) method return type}, or a
 * {@linkplain #forClass(Class) class}. Most methods on this class will themselves return
 * a {@code ResolvableType}, allowing for easy navigation. For example:
 *
 * 可以从{@linkplain #forField(Field) 字段}、{@linkplain #forMethodParameter(Method, int) 方法参数}、
 * {@linkplain #forMethodReturnType(Method) 方法返回类型}获取{@code ResolvableType} ，或一个 {@linkplain #forClass(Class) 类}。
 * 此类上的大多数方法本身都会返回一个 {@code ResolvableType}，以便轻松导航。例如
 *
 * <pre class="code">
 * private HashMap<Integer, List<String>> myMap;
 * private HashMap<Integer, List<Integer,String>> myMap;
 *
 * public void example() {
 *     ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("myMap"));
 *     t.getSuperType(); // AbstractMap&lt;Integer, List&lt;String&gt;&gt;
 *     t.asMap(); // Map&lt;Integer, List&lt;String&gt;&gt;
 *     t.getGeneric(0).resolve(); // Integer
 *     t.getGeneric(1).resolve(); // List
 *     t.getGeneric(1); // List&lt;String&gt;
 *     t.resolveGeneric(1, 0); // String
 * }
 * </pre>
 *
 * 解析泛型的工具类。ResolvableType 是对 Class，Field，Method 获取 Type 的抽象。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 4.0
 * @see #forField(Field)
 * @see #forMethodParameter(Method, int)
 * @see #forMethodReturnType(Method)
 * @see #forConstructorParameter(Constructor, int)
 * @see #forClass(Class)
 * @see #forType(Type)
 * @see #forInstance(Object)
 * @see ResolvableTypeProvider
 */
@SuppressWarnings("serial")
public class ResolvableType implements Serializable {

	/**
	 * {@code ResolvableType} returned when no value is available. {@code NONE} is used
	 * in preference to {@code null} so that multiple method calls can be safely chained.
	 *
	 * 如果没有可用的值，就返回 {@code ResolvableType} 。{@code NONE} 优先于 {@code null}，以便可以安全地链接多个方法调用。
	 */
	public static final ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, null, 0);

	/**
	 * 空类型数组
	 */
	private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

	/**
	 * ResolvableType对象映射缓存
	 */
	private static final ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
			new ConcurrentReferenceHashMap<>(256);


	/**
	 * The underlying Java type being managed.
	 * 正在管理的底层 Java 类型。
	 */
	private final Type type;

	/**
	 * Optional provider for the type.
	 */
	// 底层类型的提供者，静态工厂方法中如果未提供类型，则根据该类型提供者获取类型
	@Nullable
	private final TypeProvider typeProvider;

	/**
	 * The {@code VariableResolver} to use or {@code null} if no resolver is available.
	 */
	// 用于将 TypeVariable 解析为 ResolvableType 的解析器
	@Nullable
	private final VariableResolver variableResolver;

	/**
	 * The component type for an array or {@code null} if the type should be deduced.
	 */
	// 如果底层类型为泛型数组，则保存数组的元素类型
	@Nullable
	private final ResolvableType componentType;

	// 缓存的哈希码
	@Nullable
	private final Integer hash;

	/**
	 * 将{@link #type}解析成Class对象
	 */
	@Nullable
	private Class<?> resolved;

	// 带泛型的父类型
	@Nullable
	private volatile ResolvableType superType;

	// 当前类型实现的带泛型的接口
	@Nullable
	private volatile ResolvableType[] interfaces;

	// 当前类型的泛型参数
	@Nullable
	private volatile ResolvableType[] generics;

	@Nullable
	private volatile Boolean unresolvableGenerics;


	/**
	 * Private constructor used to create a new {@link ResolvableType} for cache key purposes,
	 * with no upfront resolution.
	 *
	 * 私有构造函数，用于创建新的{@link ResolvableType}以用于缓存key目的，无需预先解析
	 */
	private ResolvableType(
			Type type, @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {

		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = null;
		this.hash = calculateHashCode();
		this.resolved = null;
	}

	/**
	 * Private constructor used to create a new {@link ResolvableType} for cache value purposes,
	 * with upfront resolution and a pre-calculated hash.
	 * @since 4.2
	 *
	 * 私有构造函数，用于创建新的 {@link ResolvableType} 以用于缓存值，具有前期解析和预先计算的哈希值。
	 */
	private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
			@Nullable VariableResolver variableResolver, @Nullable Integer hash) {

		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = null;
		this.hash = hash;
		// 将type解析成Class赋值给resolved
		this.resolved = resolveClass();
	}

	/**
	 * Private constructor used to create a new {@link ResolvableType} for uncached purposes,
	 * with upfront resolution but lazily calculated hash.
	 *
	 * 私有构造函数用于创建一个新的 {@link ResolvableType} 以用于未缓存的目的，具有预先解析但延迟计算的哈希值。
	 */
	private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
			@Nullable VariableResolver variableResolver, @Nullable ResolvableType componentType) {

		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = componentType;
		this.hash = null;
		// 将type解析成Class赋值给resolved
		this.resolved = resolveClass();
	}

	/**
	 * Private constructor used to create a new {@link ResolvableType} on a {@link Class} basis.
	 * <p>Avoids all {@code instanceof} checks in order to create a straight {@link Class} wrapper.
	 *
	 * 用于在 {@link Class} 的基础上创建新的 {@link ResolvableType} 的私有构造函数。
	 * 避免所有 {@code instanceof} 检查，以便创建直接的 {@link Class} 包装器。
	 *
	 * @since 4.2
	 */
	private ResolvableType(@Nullable Class<?> clazz) {
		this.resolved = (clazz != null ? clazz : Object.class);
		this.type = this.resolved;
		this.typeProvider = null;
		this.variableResolver = null;
		this.componentType = null;
		this.hash = null;
	}


	/**
	 * Return the underling Java {@link Type} being managed.
	 *
	 * 返回正在管理的底层 Java {@link Type}。
	 */
	public Type getType() {
		// 解包给定类型，有效地返回原始的不可序列化类型。
		return SerializableTypeWrapper.unwrap(this.type);
	}

	/**
	 * Return the underlying Java {@link Class} being managed, if available;
	 * otherwise {@code null}.
	 *
	 * 返回正在管理的底层 Java {@link Class}（如果可用）；否则{@code null}。
	 */
	@Nullable
	public Class<?> getRawClass() {
		// 如果已解析类型等于受管理的基础Java{@link Type}
		if (this.type == this.resolved) {
			// 返回已解析类型
			return this.resolved;
		}
		Type rawType = this.type;
		if (rawType instanceof ParameterizedType) {
			rawType = ((ParameterizedType) rawType).getRawType();
		}
		// 如果rawType是Class的子类或本身，就将rawType强转为Class对象并返回，否则返回null
		return (rawType instanceof Class ? (Class<?>) rawType : null);
	}

	/**
	 * Return the underlying source of the resolvable type. Will return a {@link Field},
	 * {@link MethodParameter} or {@link Type} depending on how the {@link ResolvableType}
	 * was constructed. This method is primarily to provide access to additional type
	 * information or meta-data that alternative JVM languages may provide.
	 *
	 * 返回可解析类型的基础源。将返回 {@link Field}、{@link MethodParameter} 或 {@link Type}，
	 * 具体取决于 {@link ResolvableType} 的构造方式。此方法主要用于提供对其他 JVM 语言可能提供的其他类型信息或元数据的访问。
	 */
	public Object getSource() {
		Object source = (this.typeProvider != null ? this.typeProvider.getSource() : null);
		return (source != null ? source : this.type);
	}

	/**
	 * Return this type as a resolved {@code Class}, falling back to
	 * {@link java.lang.Object} if no specific class can be resolved.
	 *
	 * 返回此类型作为已解析的{@code Class},如果没有特定的类可以解析，则返回
	 * {@link java.lang.Object}
	 *
	 * @return the resolved {@link Class} or the {@code Object} fallback
	 * @since 5.1
	 * @see #getRawClass()
	 * @see #resolve(Class)
	 */
	public Class<?> toClass() {
		return resolve(Object.class);
	}

	/**
	 * Determine whether the given object is an instance of this {@code ResolvableType}.
	 * 确定给定的对象是否是此{@code ResolvableType}的实例
	 *
	 * @param obj the object to check
	 * @since 4.2
	 * @see #isAssignableFrom(Class)
	 */
	public boolean isInstance(@Nullable Object obj) {
		return (obj != null && isAssignableFrom(obj.getClass()));
	}

	/**
	 * Determine whether this {@code ResolvableType} is assignable from the
	 * specified other type.
	 *
	 * 确定是否可以从指定的其他类型分配此{@code ResolvableType}
	 *
	 * @param other the type to be checked against (as a {@code Class})
	 * @since 4.2
	 * @see #isAssignableFrom(ResolvableType)
	 */
	public boolean isAssignableFrom(Class<?> other) {
		return isAssignableFrom(forClass(other), null);
	}

	/**
	 * Determine whether this {@code ResolvableType} is assignable from the
	 * specified other type.
	 *
	 * 确定是否可以从指定的其他类型分配此{@code ResolvableType}
	 *
	 * <p>Attempts to follow the same rules as the Java compiler, considering
	 * whether both the {@link #resolve() resolved} {@code Class} is
	 * {@link Class#isAssignableFrom(Class) assignable from} the given type
	 * as well as whether all {@link #getGenerics() generics} are assignable.
	 *
	 * 尝试遵循与Java编译器相同的规则，并考虑是否{@code link #resovle() resolved}
	 * {@code Class}也是{@link Class#isAssignableFrom(Class) assignable from}
	 * 分配给给定类型是否所有{@link #getGenerics() generics} 是可分配的。
	 *
	 * @param other the type to be checked against (as a {@code ResolvableType}) --要检查的类型{作为{@code ResolvableType})
	 * @return {@code true} if the specified other type can be assigned to this
	 * {@code ResolvableType}; {@code false} otherwise
	 *
	 *  --如果指定另一个类型可以分配给此{@code ResovlableType} 就返回{@code true},
	 * 	否则返回{@code false}
	 */
	public boolean isAssignableFrom(ResolvableType other) {
		return isAssignableFrom(other, null);
	}

	/**
	 * <p>确定是否可以从指定的其他类型分配此{@code ResolvableType}</p>
	 * <p>
	 *     尝试遵循与Java编译器相同的规则，并考虑是否{@code link #resovle() resolved}
	 *     {@code Class}也是{@link Class#isAssignableFrom(Class) assignable from}
	 *     分配给给定类型是否所有{@link #getGenerics() generics} 是可分配的。
	 * </p>
	 * @param other 另一个ResolvableType
	 * @param matchedBefore 匹配之前的映射,表示已经判断可分配
	 */
	private boolean isAssignableFrom(ResolvableType other, @Nullable Map<Type, Type> matchedBefore) {
		// 如果other为null，抛出异常
		Assert.notNull(other, "ResolvableType must not be null");

		// If we cannot resolve types, we are not assignable
		// 如果无法解析类型，则无法分配
		// 如果本对象为NONE，或者另一个ResolvableType对象为NONE
		if (this == NONE || other == NONE) {
			// 返回false，表示不能分配
			return false;
		}

		// Deal with array by delegating to the component type
		// 通过委托给组件类型来处理数组
		if (isArray()) {
			return (other.isArray() && getComponentType().isAssignableFrom(other.getComponentType()));
		}

		// 如果mathcedBefore不为null 且 从metchedBefore获取本类对象的type属性值对应的ResolvableType对象
		// 等于other的type属性时
		if (matchedBefore != null && matchedBefore.get(this.type) == other.type) {
			// 返回true，表示可分配
			return true;
		}

		// Deal with wildcard bounds
		// 处理 通配符范围
		// 获取本类对象的ResolvableType.WildcardBounds实例
		WildcardBounds ourBounds = WildcardBounds.get(this);
		WildcardBounds typeBounds = WildcardBounds.get(other);

		// In the form X is assignable to <? extends Number>
		//  X的形式可分配给 <? extends Number>
		if (typeBounds != null) {
			// (如果ourBounds不为null且ourBounds与typeBound的界限相同) 且 （ourBound是可分配
			// typeBounds的界限，返回true,表示本类对象可分配给other；否则返回false，表示本类对象不可分配给other
			return (ourBounds != null && ourBounds.isSameKind(typeBounds) &&
					ourBounds.isAssignableFrom(typeBounds.getBounds()));
		}

		// In the form <? extends Number> is assignable to X...
		//  <? extends Number>的形式可分配给X
		// 如果ourBounds不为null
		if (ourBounds != null) {
			return ourBounds.isAssignableFrom(other);
		}

		// Main assignability check about to follow
		// 主要可分配性检查如下
		// 如果mathedBefore不为null，完成匹配标记设置为true；否则为false
		boolean exactMatch = (matchedBefore != null);  // We're checking nested generic variables now... -- 现在正在检查嵌套的泛型变量..
		// 初始化检查泛型标记为true
		boolean checkGenerics = true;
		// 初始化本类对象的已解析类为null
		Class<?> ourResolved = null;
		// 如果本类对象被管理的底层类型是TypeVariable的子类或本类
		if (this.type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			// Try default variable resolution
			// 尝试默认遍历解析
			// 如果变量解析器不为null
			if (this.variableResolver != null) {
				// 使用变量解析器解析variable得到ResolvableType对象
				ResolvableType resolved = this.variableResolver.resolveVariable(variable);
				// 如果resolved对象不为null
				if (resolved != null) {
					// 获取resolved的type属性值解析为Class对象并设置到ourResolved
					ourResolved = resolved.resolve();
				}
			}
			if (ourResolved == null) {
				// Try variable resolution against target type -- 尝试针对目标类型进行变量解析
				if (other.variableResolver != null) {
					// 通过other的遍历解析器解析variable得到ResolvableType对象
					ResolvableType resolved = other.variableResolver.resolveVariable(variable);
					if (resolved != null) {
						// 获取resolved的type属性值解析为Class对象并设置到ourResolved
						ourResolved = resolved.resolve();
						// 设置检查泛型标记为false
						checkGenerics = false;
					}
				}
			}
			if (ourResolved == null) {
				// Unresolved type variable, potentially nested -> never insist on exact match
				// 未解析的类型变量，可能嵌套 -> 从不坚持完全匹配
				// 设置完成匹配标记为false
				exactMatch = false;
			}
		}
		if (ourResolved == null) {
			// 将本类对象的type属性值解析为Class,如果无法解析该类型，则返回Object.Class
			ourResolved = resolve(Object.class);
		}
		// 获取other的type属性值作为解析的Class,如果没有特定的类可以解析，则返回 Object
		Class<?> otherResolved = other.toClass();

		// We need an exact type match for generics -- 我们需要泛型的精确类型匹配
		// List<CharSequence> is not assignable from List<String> -- List<CharSequence>不能分配给List<String>
		// 如果需要完全匹配，就判断ourResolved等于otherResolved对象 ；否则判断是否可以将otherResolved分配给ourResolved，
		// 假设可以通过反射进行设置。将原始包装类视为可分配给相应的原始类型。
		if (exactMatch ? !ourResolved.equals(otherResolved) : !ClassUtils.isAssignable(ourResolved, otherResolved)) {
			return false;
		}

		// 如果需要检查泛型
		if (checkGenerics) {
			// Recursively check each generic -- 递归检查每个泛型
			// 获取表现本类对象的泛型参数的ResolvableTypes数组。
			ResolvableType[] ourGenerics = getGenerics();
			// 将other作为ourResoulved的ResolvableType对象，获取其表示泛型参数的ResolvableType数组
			ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
			// 如果ourGenerics的长度不等于typeGenerics的长度
			if (ourGenerics.length != typeGenerics.length) {
				return false;
			}
			if (matchedBefore == null) {
				//IdentityHashMap:它添加保存重复的键名，并不会覆盖原有的对，其判断值是否
				// 相等是比较对象的地址而不是hashCode和equal方法
				//初始化一个最大size为1的IdentityHashMap对象
				matchedBefore = new IdentityHashMap<>(1);
			}
			// 将本类对象的type属性和other的type属性添加到matchedBefore集合中
			matchedBefore.put(this.type, other.type);
			for (int i = 0; i < ourGenerics.length; i++) {
				// 如果第i个ourGenerics元素不能分配给第i个typeGenerics元素
				if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBefore)) {
					// 返回false，表示ourResolved不可分配给otherResolved
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Return {@code true} if this type resolves to a Class that represents an array.
	 *
	 * 如果此类型解析为表示数组的Class,则返回{@code true}
	 *
	 * @see #getComponentType()
	 */
	public boolean isArray() {
		if (this == NONE) {
			return false;
		}
		return ((this.type instanceof Class && ((Class<?>) this.type).isArray()) ||
				// GenericArrayType是Type的子接口，用于表示“泛型数组”，描述的是形如：A<T>[]或T[]的类型。
				// 其实也就是描述ParameterizedType类型以及TypeVariable类型的数组，即形如：classA<T>[][]、T[]等。
				this.type instanceof GenericArrayType || resolveType().isArray());
	}

	/**
	 * Return the ResolvableType representing the component type of the array or
	 * {@link #NONE} if this type does not represent an array.
	 *
	 * 返回表示数组的组件类型的ResolvableType;如果此类型不能表示数组，就返回{@link #NONE}
	 *
	 * @see #isArray()
	 */
	public ResolvableType getComponentType() {
		// 如果本对象是NONE
		if (this == NONE) {
			// 直接返回NONE，因为NONE表示没有可用的值，相当与null
			return NONE;
		}
		if (this.componentType != null) {
			return this.componentType;
		}
		// 如果type是Class的子类或者本身
		if (this.type instanceof Class) {
			// 将type强转为Class对象来获取组件类型
			Class<?> componentType = ((Class<?>) this.type).getComponentType();
			// 返回由variableResolver支持的componentType的ResolvableType对象
			return forType(componentType, this.variableResolver);
		}
		// 如果type是GenericArrayType的子类或本类
		// GenericArrayType是Type的子接口，用于表示“泛型数组”，描述的是形如：A<T>[]或T[]的类型。
		// 其实也就是描述ParameterizedType类型以及TypeVariable类型的数组，即形如：classA<T>[][]、T[]等。
		if (this.type instanceof GenericArrayType) {
			// GenericArrayType.getGenericComponentType：获取“泛型数组”中元素的类型，要注意的是：
			// 无论从左向右有几个[]并列，这个方法仅仅脱去最右边的[]之后剩下的内容就
			// 作为这个方法的返回值。
			// 返回由给定variableResolver支持的指定Type的ResolvableType
			return forType(((GenericArrayType) this.type).getGenericComponentType(), this.variableResolver);
		}
		return resolveType().getComponentType();
	}

	/**
	 * Convenience method to return this type as a resolvable {@link Collection} type.
	 * <p>Returns {@link #NONE} if this type does not implement or extend
	 *
	 *  一种方便的方法，用于将此类型返回可解析的{@link Collection}类型。
	 * 	如果此类型未实现或未继承，则返回{@link #NONE}
	 *
	 * {@link Collection}.
	 * @see #as(Class)
	 * @see #asMap()
	 */
	public ResolvableType asCollection() {
		return as(Collection.class);
	}

	/**
	 * Convenience method to return this type as a resolvable {@link Map} type.
	 * <p>Returns {@link #NONE} if this type does not implement or extend
	 * {@link Map}.
	 * @see #as(Class)
	 * @see #asCollection()
	 */
	public ResolvableType asMap() {
		return as(Map.class);
	}

	/**
	 * Return this type as a {@link ResolvableType} of the specified class. Searches
	 * {@link #getSuperType() supertype} and {@link #getInterfaces() interface}
	 * hierarchies to find a match, returning {@link #NONE} if this type does not
	 * implement or extend the specified class.
	 *
	 * 将此类型作为指定类的{@link ResolvableType}返回。搜索{@link #getSuperType() supertype}
	 * 和{@link #getInterfaces() interface}层次结构以找到匹配项，如果此类不会实现或者继承指定类，
	 * 返回{@link #NONE}
	 *
	 * @param type the required type (typically narrowed) --{所需的类型（通常缩小）}
	 * @return a {@link ResolvableType} representing this object as the specified
	 * type, or {@link #NONE} if not resolvable as that type -- 表示此对象作为指定类型的{@link ResolvableType},或者无法解析该类型，则为{@link #NONE}
	 * @see #asCollection()
	 * @see #asMap()
	 * @see #getSuperType()
	 * @see #getInterfaces()
	 */
	public ResolvableType as(Class<?> type) {
		// 如果本类对象为NONE, 还是返回NONE
		if (this == NONE) {
			return NONE;
		}
		// 将type属性值解析为Class,如果无法解析，则返回null
		Class<?> resolved = resolve();
		// 如果resolved为null或者resolved就是传进来的type
		if (resolved == null || resolved == type) {
			return this;
		}
		// 遍历所有本类对象表示此type属性实现的直接接口的ResolvableType数组
		for (ResolvableType interfaceType : getInterfaces()) {
			// 将interfaceType作为传进来的type的ResolvableType对象
			ResolvableType interfaceAsType = interfaceType.as(type);
			if (interfaceAsType != NONE) {
				return interfaceAsType;
			}
		}
		return getSuperType().as(type);
	}

	/**
	 * Return a {@link ResolvableType} representing the direct supertype of this type.
	 * <p>If no supertype is available this method returns {@link #NONE}.
	 * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
	 *
	 * <p>
	 * 返回表示此类型的直接父类的{@link ResolvableType}.如果没有父类可以用，此方法
	 * 返回{@link #NONE}
	 * 注意：生成的{@link ResolvableType}实例可能不是{@link Serializable}的
	 *
	 * getGenericSuperclass
	 * </p>
	 * @see #getInterfaces()
	 *
	 */
	public ResolvableType getSuperType() {
		// 将type属性值解析为Class,如果无法解析，则返回null
		Class<?> resolved = resolve();
		if (resolved == null) {
			return NONE;
		}
		try {
			// getGenericSuperclass():返回本类的父类,包含泛型参数信息
			// getSuperclass():返回本类的父类
			// 如果resolved为null或者没有拿到resolve的父类
			Type superclass = resolved.getGenericSuperclass();
			if (superclass == null) {
				return NONE;
			}
			ResolvableType superType = this.superType;
			if (superType == null) {
				// 将superclass包装成ResolvableType对象
				superType = forType(superclass, this);
				// 缓存解析出来的ResolvableType数组到本类对象的superType属性，以防止下次调用此方法时，重新解析
				this.superType = superType;
			}
			return superType;
		}
		catch (TypeNotPresentException ex) {
			// Ignore non-present types in generic signature
			// 忽略通用签名中不存在的类型
			return NONE;
		}
	}

	/**
	 * Return a {@link ResolvableType} array representing the direct interfaces
	 * implemented by this type. If this type does not implement any interfaces an
	 * empty array is returned.
	 * <p>Note: The resulting {@link ResolvableType} instances may not be {@link Serializable}.
	 * @see #getSuperType()
	 *
	 * <p>
	 * 返回一个{@link ResolvableType}数组，该数组表示此类型的实现的直接接口。如果
	 * 此类型不会实现任何接口，则返回空数组
	 * 注意：生成的{@link ResolvableType}实例可能不是{@link Serializable}
	 * </p>
	 */
	public ResolvableType[] getInterfaces() {
		Class<?> resolved = resolve();
		if (resolved == null) {
			return EMPTY_TYPES_ARRAY;
		}
		ResolvableType[] interfaces = this.interfaces;
		if (interfaces == null) {
			// getGenericInterfaces：返回实现接口信息的Type数组，包含泛型信息
			// getInterfaces:返回实现接口信息的Class数组，不包含泛型信息
			Type[] genericIfcs = resolved.getGenericInterfaces();
			interfaces = new ResolvableType[genericIfcs.length];
			for (int i = 0; i < genericIfcs.length; i++) {
				// 将genericIfcs[i]的第i个TypeVariable对象封装成ResolvableType对象，并赋值给
				// interfaces的第i个ResolvableType对象
				interfaces[i] = forType(genericIfcs[i], this);
			}
			// 缓存解析出来的ResolvableType数组到本类对象的interfaces属性，以防止下次调用此方法时，重新解析
			this.interfaces = interfaces;
		}
		return interfaces;
	}

	/**
	 * Return {@code true} if this type contains generic parameters.
	 * @see #getGeneric(int...)
	 * @see #getGenerics()
	 */
	public boolean hasGenerics() {
		return (getGenerics().length > 0);
	}

	/**
	 * Return {@code true} if this type contains unresolvable generics only,
	 * that is, no substitute for any of its declared type variables.
	 *
	 * 如果此类型只包含不可解析的泛型，则返回{@code true},即不能替代其声明的任何类型变量
	 */
	boolean isEntirelyUnresolvable() {
		if (this == NONE) {
			return false;
		}
		ResolvableType[] generics = getGenerics();
		for (ResolvableType generic : generics) {
			// 如果generic不是无法通过关联变量解析器解析的类型变量 且 generic不是表示无特点边界的通配符
			if (!generic.isUnresolvableTypeVariable() && !generic.isWildcardWithoutBounds()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determine whether the underlying type has any unresolvable generics:
	 * either through an unresolvable type variable on the type itself
	 * or through implementing a generic interface in a raw fashion,
	 * i.e. without substituting that interface's type variables.
	 * The result will be {@code true} only in those two scenarios.
	 *
	 * 确定基础类型是否具有任何不可解析的泛型：通过类型本身的不可解析类型
	 * 变量或通过以原始方式实现通用接口，即不替换该接口的类型变量。仅在这
	 * 两种情况下结果才为{@code true}
	 */
	public boolean hasUnresolvableGenerics() {
		if (this == NONE) {
			return false;
		}
		Boolean unresolvableGenerics = this.unresolvableGenerics;
		if (unresolvableGenerics == null) {
			unresolvableGenerics = determineUnresolvableGenerics();
			this.unresolvableGenerics = unresolvableGenerics;
		}
		return unresolvableGenerics;
	}

	private boolean determineUnresolvableGenerics() {
		ResolvableType[] generics = getGenerics();
		for (ResolvableType generic : generics) {
			if (generic.isUnresolvableTypeVariable() || generic.isWildcardWithoutBounds()) {
				return true;
			}
		}
		Class<?> resolved = resolve();
		if (resolved != null) {
			try {
				for (Type genericInterface : resolved.getGenericInterfaces()) {
					if (genericInterface instanceof Class) {
						if (((Class<?>) genericInterface).getTypeParameters().length > 0) {
							return true;
						}
					}
				}
			}
			catch (TypeNotPresentException ex) {
				// Ignore non-present types in generic signature
			}
			Class<?> superclass = resolved.getSuperclass();
			if (superclass != null && superclass != Object.class) {
				return getSuperType().hasUnresolvableGenerics();
			}
		}
		// 返回false，表示不具有任何不可解析的泛型
		return false;
	}

	/**
	 * Determine whether the underlying type is a type variable that
	 * cannot be resolved through the associated variable resolver.
	 *
	 * 确定基础类型是否是无法通过关联变量解析器解析的类型变量
	 */
	private boolean isUnresolvableTypeVariable() {
		if (this.type instanceof TypeVariable) {
			if (this.variableResolver == null) {
				return true;
			}
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			ResolvableType resolved = this.variableResolver.resolveVariable(variable);
			if (resolved == null || resolved.isUnresolvableTypeVariable()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether the underlying type represents a wildcard
	 * without specific bounds (i.e., equal to {@code ? extends Object}).
	 *
	 * 确定基础类型是否表示没有特定边界的通配符（即等于 {@code ? extends Object}）。
	 */
	private boolean isWildcardWithoutBounds() {
		if (this.type instanceof WildcardType) {
			WildcardType wt = (WildcardType) this.type;
			if (wt.getLowerBounds().length == 0) {
				Type[] upperBounds = wt.getUpperBounds();
				if (upperBounds.length == 0 || (upperBounds.length == 1 && Object.class == upperBounds[0])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return a {@link ResolvableType} for the specified nesting level.
	 * <p>See {@link #getNested(int, Map)} for details.
	 * @param nestingLevel the nesting level
	 * @return the {@link ResolvableType} type, or {@code #NONE}
	 */
	public ResolvableType getNested(int nestingLevel) {
		return getNested(nestingLevel, null);
	}

	/**
	 * Return a {@link ResolvableType} for the specified nesting level.
	 * 返回指定嵌套等级的{@link ResolvableType}对象
	 *
	 * <p>The nesting level refers to the specific generic parameter that should be returned.
	 * A nesting level of 1 indicates this type; 2 indicates the first nested generic;
	 * 3 the second; and so on. For example, given {@code List<Set<Integer>>} level 1 refers
	 * to the {@code List}, level 2 the {@code Set}, and level 3 the {@code Integer}.
	 * 嵌套级别指的是应返回的特定泛型参数。
	 * 嵌套级别为1表示该类型本身；2表示第一个嵌套的泛型；3表示第二个嵌套的泛型，依此类推。
	 * 例如，给定{@code List<Set<Integer>>}，级别1指的是{@code List}，级别2指的是{@code Set}，级别3指的是{@code Integer}。
	 *
	 *
	 * <p>The {@code typeIndexesPerLevel} map can be used to reference a specific generic
	 * for the given level. For example, an index of 0 would refer to a {@code Map} key;
	 * whereas, 1 would refer to the value. If the map does not contain a value for a
	 * specific level the last generic will be used (e.g. a {@code Map} value).
	 * {@code typeIndexesPerLevel}映射可以用来引用给定级别的特定泛型。
	 * 例如，索引为0表示{@code Map}的键；而索引为1表示值。如果映射不包含特定级别的值，则将使用最后一个泛型（例如，{@code Map}的值）。
	 *
	 * <p>Nesting levels may also apply to array types; for example given
	 * {@code String[]}, a nesting level of 2 refers to {@code String}.
	 * 嵌套级别也可以应用于数组类型；例如，给定{@code String[]}，嵌套级别为2表示{@code String}。
	 *
	 * <p>If a type does not {@link #hasGenerics() contain} generics the
	 * {@link #getSuperType() supertype} hierarchy will be considered.
	 * 如果类型不{@link #hasGenerics() 包含}泛型，则将考虑{@link #getSuperType() 超类型}层次结构。
	 *
	 * @param nestingLevel the required nesting level, indexed from 1 for the
	 * current type, 2 for the first nested generic, 3 for the second and so on
	 * @param typeIndexesPerLevel a map containing the generic index for a given
	 * nesting level (may be {@code null}) -- 包含给定嵌套等级的泛型索引的映射，如 key=1，value=2,表示第1级的第2个索引位置的泛型
	 * @return a {@link ResolvableType} for the nested level, or {@link #NONE}
	 */
	public ResolvableType getNested(int nestingLevel, @Nullable Map<Integer, Integer> typeIndexesPerLevel) {
		ResolvableType result = this;
		for (int i = 2; i <= nestingLevel; i++) {
			if (result.isArray()) {
				result = result.getComponentType();
			}
			else {
				// Handle derived types
				while (result != ResolvableType.NONE && !result.hasGenerics()) {
					result = result.getSuperType();
				}
				Integer index = (typeIndexesPerLevel != null ? typeIndexesPerLevel.get(i) : null);
				index = (index == null ? result.getGenerics().length - 1 : index);
				result = result.getGeneric(index);
			}
		}
		return result;
	}

	/**
	 * Return a {@link ResolvableType} representing the generic parameter for the
	 * given indexes. Indexes are zero based; for example given the type
	 * {@code Map<Integer, List<String>>}, {@code getGeneric(0)} will access the
	 * {@code Integer}. Nested generics can be accessed by specifying multiple indexes;
	 * for example {@code getGeneric(1, 0)} will access the {@code String} from the
	 * nested {@code List}. For convenience, if no indexes are specified the first
	 * generic is returned.
	 * <p>If no generic is available at the specified indexes {@link #NONE} is returned.
	 * @param indexes the indexes that refer to the generic parameter
	 * (may be omitted to return the first generic)
	 *
	 * <p>
	 *  返回表示给定索引的泛型参数的{@link ResolvableType}.索引从零开始;例如，给定类型
	 *  {@code Map<Integer,List<String>>},{@code getGeneric(0)}将访问{@code Integer}.
	 *  嵌套泛型可以通过指定多个索引来访问，例如{@code getGeneric(1,0)从嵌套{@code List}
	 *  中访问{@code String}.为了方便起见，如果没有指定索引，则返回第一个泛型</p>
	 *  如果指定索引出没有可用的泛型，就返回{@link #NONE}
	 *  索引引用泛型参数的索引(可以省略以返回第一个泛型)
	 * </p>
	 *
	 * @return a {@link ResolvableType} for the specified generic, or {@link #NONE}
	 * @see #hasGenerics()
	 * @see #getGenerics()
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public ResolvableType getGeneric(@Nullable int... indexes) {
		// 获取所有的泛型参数
		ResolvableType[] generics = getGenerics();
		if (indexes == null || indexes.length == 0) {
			return (generics.length == 0 ? NONE : generics[0]);
		}
		ResolvableType generic = this;
		for (int index : indexes) {
			// 通过循环索引，支持嵌套的泛型参数
			generics = generic.getGenerics();
			if (index < 0 || index >= generics.length) {
				return NONE;
			}
			// 如实例表示的类型为 Map<String,List<Integer>>，参数为 [1,0]
			// 第一次获取到的 generics 为 [String,List<Integer>],取索引位置 1 的 List<Integer>
			// 第二次获取到的 generics [Integer],取索引位置 0 的 Integer
			generic = generics[index];
		}
		return generic;
	}

	/**
	 * Return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters of
	 * this type. If no generics are available an empty array is returned. If you need to
	 * access a specific generic consider using the {@link #getGeneric(int...)} method as
	 * it allows access to nested generics and protects against
	 * {@code IndexOutOfBoundsExceptions}.
	 *
	 * <p>
	 * 返回表示此类型的泛型参数的{@link ResolvableType ResolvableTypes}数组。如果没有可用的泛型，
	 * 则返回一个空的数组。如果需要访问特定的泛型，请考虑使用{@link #getGeneric(int...)} 方法，
	 * 因为它允许访问嵌套的泛型并防止{@code IndexOutBoundsExceptions}
	 * </p>
	 *
	 * @return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters
	 * (never {@code null})
	 * @see #hasGenerics()
	 * @see #getGeneric(int...)
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public ResolvableType[] getGenerics() {
		if (this == NONE) {
			return EMPTY_TYPES_ARRAY;
		}
		//优先使用之前解析的值
		ResolvableType[] generics = this.generics;
		if (generics == null) {
			// 处理Class
			if (this.type instanceof Class) {
				// 从type中获取一个代表该泛型声明中声明的类型变量TypeVariable对象的数组。
				Type[] typeParams = ((Class<?>) this.type).getTypeParameters();
				generics = new ResolvableType[typeParams.length];
				for (int i = 0; i < generics.length; i++) {
					generics[i] = ResolvableType.forType(typeParams[i], this);
				}
			}
			else if (this.type instanceof ParameterizedType) {
				//处理参数化类型
				Type[] actualTypeArguments = ((ParameterizedType) this.type).getActualTypeArguments();
				generics = new ResolvableType[actualTypeArguments.length];
				for (int i = 0; i < actualTypeArguments.length; i++) {
					generics[i] = forType(actualTypeArguments[i], this.variableResolver);
				}
			}
			else {
				// 其他情况，如通配符类型、类型变量，先进行解析，然后获取解析后类型的泛型参数
				generics = resolveType().getGenerics();
			}
			this.generics = generics;
		}
		return generics;
	}

	/**
	 * Convenience method that will {@link #getGenerics() get} and
	 * {@link #resolve() resolve} generic parameters.
	 *
	 * <p>将{@link #getGenerics() 获取} 和{@link #resolve() 解析}泛型参数的方便方法  </p>
	 *
	 * @return an array of resolved generic parameters (the resulting array
	 * will never be {@code null}, but it may contain {@code null} elements})
	 * -- 解析的泛型参数数组(结果数组用于不会是{@code null},但是它可能包含{@code null}元素)
	 * @see #getGenerics()
	 * @see #resolve()
	 */
	public Class<?>[] resolveGenerics() {
		ResolvableType[] generics = getGenerics();
		Class<?>[] resolvedGenerics = new Class<?>[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvedGenerics[i] = generics[i].resolve();
		}
		return resolvedGenerics;
	}

	/**
	 * Convenience method that will {@link #getGenerics() get} and {@link #resolve()
	 * resolve} generic parameters, using the specified {@code fallback} if any type
	 * cannot be resolved.
	 * @param fallback the fallback class to use if resolution fails
	 * @return an array of resolved generic parameters
	 * @see #getGenerics()
	 * @see #resolve()
	 */
	public Class<?>[] resolveGenerics(Class<?> fallback) {
		ResolvableType[] generics = getGenerics();
		Class<?>[] resolvedGenerics = new Class<?>[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvedGenerics[i] = generics[i].resolve(fallback);
		}
		return resolvedGenerics;
	}

	/**
	 * Convenience method that will {@link #getGeneric(int...) get} and
	 * {@link #resolve() resolve} a specific generic parameters.
	 * @param indexes the indexes that refer to the generic parameter
	 * (may be omitted to return the first generic)
	 * @return a resolved {@link Class} or {@code null}
	 * @see #getGeneric(int...)
	 * @see #resolve()
	 */
	@Nullable
	public Class<?> resolveGeneric(int... indexes) {
		return getGeneric(indexes).resolve();
	}

	/**
	 * Resolve this type to a {@link java.lang.Class}, returning {@code null}
	 * if the type cannot be resolved. This method will consider bounds of
	 * {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
	 * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
	 *
	 * 将此类型解析为{@link java.lang.Class},如果无法解析，则返回{@code null}.如果
	 * 直接解析失败，则此方法将考虑{@link TypeVariable TypeVariables}和{@link WildcardType
	 * WildcardTypes}的边界；但是，{@code Object.class}的边界将被忽略
	 *
	 * <p>If this method returns a non-null {@code Class} and {@link #hasGenerics()}
	 * returns {@code false}, the given type effectively wraps a plain {@code Class},
	 * allowing for plain {@code Class} processing if desirable.
	 *
	 * 如果此方法返回非空的{@code Class},并且{@link #hasGenerics()} 返回{@code false}，
	 * 则给定类型有效地包装一个{@code class} ，如果需要允许使用普通的{@code Class}
	 *
	 * @return the resolved {@link Class}, or {@code null} if not resolvable
	 * @see #resolve(Class)
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	@Nullable
	public Class<?> resolve() {
		return this.resolved;
	}

	/**
	 * Resolve this type to a {@link java.lang.Class}, returning the specified
	 * {@code fallback} if the type cannot be resolved. This method will consider bounds
	 * of {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
	 * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
	 *
	 * 将此类型解析为{@link java.lang.Class}，如果无法解析该类型，则返回指定的{@code fallback}。
	 * 如果直接解析失败，则此方法考虑{@Code TypeVariable TypeVariables}和{@link WildcardType WildcardTypes}的bounds，
	 * 但是{@code Object.class}的bounds将被忽略
	 *
	 * @param fallback the fallback class to use if resolution fails
	 * @return the resolved {@link Class} or the {@code fallback}
	 * @see #resolve()
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public Class<?> resolve(Class<?> fallback) {
		// 如果resolved不为null，就返回resolved，否则返回fallback
		return (this.resolved != null ? this.resolved : fallback);
	}

	/**
	 * 解析类
	 */
	@Nullable
	private Class<?> resolveClass() {
		if (this.type == EmptyType.INSTANCE) {
			// type为空类型对象, 返回null
			return null;
		}
		if (this.type instanceof Class) {
			// 如果type是Class的子类或本身, 直接返回
			return (Class<?>) this.type;
		}
		// 如果type是GenericArrayType的子类或本类
		// GenericArrayType是Type的子接口，用于表示“泛型数组”，描述的是形如：A<T>[]或T[]的类型。
		// 	其实也就是描述ParameterizedType类型以及TypeVariable类型的数组，即形如：classA<T>[][]、T[]等。
		if (this.type instanceof GenericArrayType) {
			// 获取type的表示数组的组件类型的ResolvableType;如果此类型不能表示数组，就返回NONE
			Class<?> resolvedComponent = getComponentType().resolve();
			return (resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null);
		}
		return resolveType().resolve();
	}

	/**
	 * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
	 *
	 * 按单个级别解析此类型，返回解析的值或{@link #NONE}
	 *
	 * <p>Note: The returned {@link ResolvableType} should only be used as an intermediary
	 * as it cannot be serialized.
	 *
	 * 注意：返回的 {@link ResolvableType} 只能用作中介，因为它不能序列化。
	 */
	ResolvableType resolveType() {
		if (this.type instanceof ParameterizedType) {
			// ParameterizeType.getRawType():返回最外层<>前面那个类型，即Map<K ,V>的Map。
			// 返回由给定variableResolver支持的指定type.getRawType()的ResolvableType对象
			return forType(((ParameterizedType) this.type).getRawType(), this.variableResolver);
		}
		// WildcardType：通配符表达式，泛型表达式，也可以说是，限定性的泛型，形如：? extends classA、? super classB
		// 如果type是WildcardType的子类或本身
		if (this.type instanceof WildcardType) {
			// WildcardType.getUppperBounds： 获得泛型表达式上界（上限），即父类
			Type resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
			if (resolved == null) {
				// WildcardType.getLowerBounds： 获得泛型表达式下界（下限），即子类
				resolved = resolveBounds(((WildcardType) this.type).getLowerBounds());
			}
			return forType(resolved, this.variableResolver);
		}
		// 如果type是TypeVariable的子类或本身
		if (this.type instanceof TypeVariable) {
			//如果管理的类型器是类型变量，返回变量解析器解析的结果或类型变量的边界类
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			// Try default variable resolution
			// 尝试默认变量解析
			if (this.variableResolver != null) {
				ResolvableType resolved = this.variableResolver.resolveVariable(variable);
				if (resolved != null) {
					return resolved;
				}
			}
			// Fallback to bounds
			return forType(resolveBounds(variable.getBounds()), this.variableResolver);
		}
		return NONE;
	}


	/**
	 * 解析typeVariable的上边界类型
	 * @param bounds typeVariable的边界
	 * @return 返回给定bounds的第一个元素类型，如果没有找到，返回null
	 */
	@Nullable
	private Type resolveBounds(Type[] bounds)
	{
		// 如果bounds长度为0或者第一个bounds的第一个元素类型为Object
		if (bounds.length == 0 || bounds[0] == Object.class) {
			return null;
		}
		return bounds[0];
	}

	/**
	 * 将{@code variable}解析包装成ResolvableType对象
	 * @param variable 类型变量对象
	 * @return 表示{@code variable}的ResolvableType对象，如果无法解析返回null
	 */
	@Nullable
	private ResolvableType resolveVariable(TypeVariable<?> variable) {
		if (this.type instanceof TypeVariable) {
			// 通过单级解析本类对象的type，得到ResolvableType对象，再将variable解析包装成
			// ResolvableType对象
			return resolveType().resolveVariable(variable);
		}
		if (this.type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) this.type;
			// 将type解析成Class对象，如果无法解析，则返回null
			Class<?> resolved = resolve();
			if (resolved == null) {
				return null;
			}
			// 从resolved中获取一个代表该泛型声明中声明的类型变量TypeVariable对象的数组。
			// 返回<>里面声明的TypeVariable
			TypeVariable<?>[] variables = resolved.getTypeParameters();
			for (int i = 0; i < variables.length; i++) {
				// 如果第i个variables元素对象的名称 等于 传进来的variable名称
				if (ObjectUtils.nullSafeEquals(variables[i].getName(), variable.getName())) {
					// ParameterizedType.getActualTypeArgments:获取泛型中的实际类型，可能会存在多个泛型，
					// 例如Map<K,V>,所以会返回Type[]数组；
					// 获取第i个parameterizedType泛型中的实际类型
					Type actualType = parameterizedType.getActualTypeArguments()[i];
					return forType(actualType, this.variableResolver);
				}
			}
			// Type getOwnerType()返回 Type 对象，表示此类型是其成员之一的类型。例如，如果此类型为 O.I，
			// 则返回 O 的表示形式。如果此类型为顶层类型，则返回 null
			Type ownerType = parameterizedType.getOwnerType();
			if (ownerType != null) {
				return forType(ownerType, this.variableResolver).resolveVariable(variable);
			}
		}
		if (this.type instanceof WildcardType) {
			ResolvableType resolved = resolveType().resolveVariable(variable);
			if (resolved != null) {
				return resolved;
			}
		}
		if (this.variableResolver != null) {
			// 使用本类对象的variableResolver属性对variable解析包装成ResolvableType对象
			return this.variableResolver.resolveVariable(variable);
		}
		return null;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ResolvableType)) {
			return false;
		}

		ResolvableType otherType = (ResolvableType) other;
		if (!ObjectUtils.nullSafeEquals(this.type, otherType.type)) {
			return false;
		}
		if (this.typeProvider != otherType.typeProvider &&
				(this.typeProvider == null || otherType.typeProvider == null ||
				!ObjectUtils.nullSafeEquals(this.typeProvider.getType(), otherType.typeProvider.getType()))) {
			return false;
		}
		if (this.variableResolver != otherType.variableResolver &&
				(this.variableResolver == null || otherType.variableResolver == null ||
				!ObjectUtils.nullSafeEquals(this.variableResolver.getSource(), otherType.variableResolver.getSource()))) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(this.componentType, otherType.componentType)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return (this.hash != null ? this.hash : calculateHashCode());
	}

	/**
	 * 计算本类的哈希值
	 */
	private int calculateHashCode() {
		// 获取被管理的基础类型
		int hashCode = ObjectUtils.nullSafeHashCode(this.type);
		// 如果当前类型的可选提供者不为null
		if (this.typeProvider != null) {
			//叠加计算哈希值
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.typeProvider.getType());
		}
		if (this.variableResolver != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.variableResolver.getSource());
		}
		if (this.componentType != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.componentType);
		}
		return hashCode;
	}

	/**
	 * Adapts this {@link ResolvableType} to a {@link VariableResolver}.
	 *
	 * 将此{@link ResolvableType}适配为一个{@link VariableResolver}
	 */
	@Nullable
	VariableResolver asVariableResolver() {
		if (this == NONE) {
			return null;
		}
		return new DefaultVariableResolver(this);
	}

	/**
	 * Custom serialization support for {@link #NONE}.
	 */
	private Object readResolve() {
		return (this.type == EmptyType.INSTANCE ? NONE : this);
	}

	/**
	 * Return a String representation of this type in its fully resolved form
	 * (including any generic parameters).
	 */
	@Override
	public String toString() {
		if (isArray()) {
			return getComponentType() + "[]";
		}
		if (this.resolved == null) {
			return "?";
		}
		if (this.type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			if (this.variableResolver == null || this.variableResolver.resolveVariable(variable) == null) {
				// Don't bother with variable boundaries for toString()...
				// Can cause infinite recursions in case of self-references
				return "?";
			}
		}
		if (hasGenerics()) {
			return this.resolved.getName() + '<' + StringUtils.arrayToDelimitedString(getGenerics(), ", ") + '>';
		}
		return this.resolved.getName();
	}


	// Factory methods

	/**
	 * Return a {@link ResolvableType} for the specified {@link Class},
	 * using the full generic type information for assignability checks.
	 * <p>For example: {@code ResolvableType.forClass(MyArrayList.class)}.
	 *
	 * 返回指定{@link Class}的{@link ResolvableType}对象，使用完整泛型
	 * 类型信息进行可分配性检查。例如{@code ResolvableType.forClass(MyArrayList.class)}
	 *
	 * @param clazz the class to introspect ({@code null} is semantically
	 * equivalent to {@code Object.class} for typical use cases here) -- 自省的类({@code null}在语义上与{@code Object}在这里的典型用例等效)
	 * @return a {@link ResolvableType} for the specified class
	 * @see #forClass(Class, Class)
	 * @see #forClassWithGenerics(Class, Class...)
	 */
	public static ResolvableType forClass(@Nullable Class<?> clazz) {
		return new ResolvableType(clazz);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Class},
	 * doing assignability checks against the raw class only (analogous to
	 * {@link Class#isAssignableFrom}, which this serves as a wrapper for).
	 * <p>For example: {@code ResolvableType.forRawClass(List.class)}.
	 *
	 * 返回指定 {@link Class} 的 {@link ResolvableType}，仅对原始类执行可分配性检查（类似于 {@link Class#isAssignableFrom}，用作包装器）。
	 * 例如：{@code ResolvableType.forRawClass（List.class）}。
	 *
	 * @param clazz the class to introspect ({@code null} is semantically
	 * equivalent to {@code Object.class} for typical use cases here)
	 *
	 * 对于此处的典型用例，要 introspect （{@code null} 的类在语义上等同于 {@code Object.class}）
	 *
	 * @return a {@link ResolvableType} for the specified class
	 * @since 4.2
	 * @see #forClass(Class)
	 * @see #getRawClass()
	 */
	public static ResolvableType forRawClass(@Nullable Class<?> clazz) {
		return new ResolvableType(clazz) {
			@Override
			public ResolvableType[] getGenerics() {
				return EMPTY_TYPES_ARRAY;
			}
			@Override
			public boolean isAssignableFrom(Class<?> other) {
				return (clazz == null || ClassUtils.isAssignable(clazz, other));
			}
			@Override
			public boolean isAssignableFrom(ResolvableType other) {
				Class<?> otherClass = other.resolve();
				return (otherClass != null && (clazz == null || ClassUtils.isAssignable(clazz, otherClass)));
			}
		};
	}

	/**
	 * Return a {@link ResolvableType} for the specified base type
	 * (interface or base class) with a given implementation class.
	 * <p>For example: {@code ResolvableType.forClass(List.class, MyArrayList.class)}.
	 * @param baseType the base type (must not be {@code null})
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified base type backed by the
	 * given implementation class
	 * @see #forClass(Class)
	 * @see #forClassWithGenerics(Class, Class...)
	 */
	public static ResolvableType forClass(Class<?> baseType, Class<?> implementationClass) {
		Assert.notNull(baseType, "Base type must not be null");
		ResolvableType asType = forType(implementationClass).as(baseType);
		return (asType == NONE ? forType(baseType) : asType);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Class} with pre-declared generics.
	 * @param clazz the class (or interface) to introspect
	 * @param generics the generics of the class
	 * @return a {@link ResolvableType} for the specific class and generics
	 * @see #forClassWithGenerics(Class, ResolvableType...)
	 */
	public static ResolvableType forClassWithGenerics(Class<?> clazz, Class<?>... generics) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(generics, "Generics array must not be null");
		ResolvableType[] resolvableGenerics = new ResolvableType[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvableGenerics[i] = forClass(generics[i]);
		}
		return forClassWithGenerics(clazz, resolvableGenerics);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Class} with pre-declared generics.
	 * @param clazz the class (or interface) to introspect
	 * @param generics the generics of the class
	 * @return a {@link ResolvableType} for the specific class and generics
	 * @see #forClassWithGenerics(Class, Class...)
	 */
	public static ResolvableType forClassWithGenerics(Class<?> clazz, ResolvableType... generics) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(generics, "Generics array must not be null");
		TypeVariable<?>[] variables = clazz.getTypeParameters();
		Assert.isTrue(variables.length == generics.length, () -> "Mismatched number of generics specified for " + clazz.toGenericString());

		Type[] arguments = new Type[generics.length];
		for (int i = 0; i < generics.length; i++) {
			ResolvableType generic = generics[i];
			Type argument = (generic != null ? generic.getType() : null);
			arguments[i] = (argument != null && !(argument instanceof TypeVariable) ? argument : variables[i]);
		}

		ParameterizedType syntheticType = new SyntheticParameterizedType(clazz, arguments);
		return forType(syntheticType, new TypeVariablesVariableResolver(variables, generics));
	}

	/**
	 * Return a {@link ResolvableType} for the specified instance. The instance does not
	 * convey generic information but if it implements {@link ResolvableTypeProvider} a
	 * more precise {@link ResolvableType} can be used than the simple one based on
	 * the {@link #forClass(Class) Class instance}.
	 * @param instance the instance (possibly {@code null})
	 * @return a {@link ResolvableType} for the specified instance,
	 * or {@code NONE} for {@code null}
	 * @since 4.2
	 * @see ResolvableTypeProvider
	 */
	public static ResolvableType forInstance(@Nullable Object instance) {
		if (instance instanceof ResolvableTypeProvider) {
			ResolvableType type = ((ResolvableTypeProvider) instance).getResolvableType();
			if (type != null) {
				return type;
			}
		}
		return (instance != null ? forClass(instance.getClass()) : NONE);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Field}.
	 * @param field the source field
	 * @return a {@link ResolvableType} for the specified field
	 * @see #forField(Field, Class)
	 */
	public static ResolvableType forField(Field field) {
		Assert.notNull(field, "Field must not be null");
 		return forType(null, new FieldTypeProvider(field), null);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Field} with a given
	 * implementation.
	 *
	 * <p>Use this variant when the class that declares the field includes generic
	 * parameter variables that are satisfied by the implementation class.
	 * @param field the source field
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified field
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Field} with a given
	 * implementation.
	 * <p>Use this variant when the class that declares the field includes generic
	 * parameter variables that are satisfied by the implementation type.
	 * @param field the source field
	 * @param implementationType the implementation type
	 * @return a {@link ResolvableType} for the specified field
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, @Nullable ResolvableType implementationType) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = (implementationType != null ? implementationType : NONE);
		owner = owner.as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Field} with the
	 * given nesting level.
	 * @param field the source field
	 * @param nestingLevel the nesting level (1 for the outer level; 2 for a nested
	 * generic type; etc)
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, int nestingLevel) {
		Assert.notNull(field, "Field must not be null");
		return forType(null, new FieldTypeProvider(field), null).getNested(nestingLevel);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Field} with a given
	 * implementation and the given nesting level.
	 * <p>Use this variant when the class that declares the field includes generic
	 * parameter variables that are satisfied by the implementation class.
	 *
	 * 为具有给定实现和给定嵌套级别的指定 {@link Field} 返回 {@link ResolvableType}。
	 * 当声明字段的类包含实现类满足的泛型参数变量时，请使用此变体。
	 *
	 * @param field the source field
	 * @param nestingLevel the nesting level (1 for the outer level; 2 for a nested
	 * generic type; etc)
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified field
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, int nestingLevel, @Nullable Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver()).getNested(nestingLevel);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Constructor} parameter.
	 * @param constructor the source constructor (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @return a {@link ResolvableType} for the specified constructor parameter
	 * @see #forConstructorParameter(Constructor, int, Class)
	 */
	public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex) {
		Assert.notNull(constructor, "Constructor must not be null");
		return forMethodParameter(new MethodParameter(constructor, parameterIndex));
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Constructor} parameter
	 * with a given implementation. Use this variant when the class that declares the
	 * constructor includes generic parameter variables that are satisfied by the
	 * implementation class.
	 * @param constructor the source constructor (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified constructor parameter
	 * @see #forConstructorParameter(Constructor, int)
	 */
	public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex,
			Class<?> implementationClass) {

		Assert.notNull(constructor, "Constructor must not be null");
		MethodParameter methodParameter = new MethodParameter(constructor, parameterIndex, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Method} return type.
	 * @param method the source for the method return type
	 * @return a {@link ResolvableType} for the specified method return
	 * @see #forMethodReturnType(Method, Class)
	 */
	public static ResolvableType forMethodReturnType(Method method) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(new MethodParameter(method, -1));
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Method} return type.
	 * <p>Use this variant when the class that declares the method includes generic
	 * parameter variables that are satisfied by the implementation class.
	 * @param method the source for the method return type
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified method return
	 * @see #forMethodReturnType(Method)
	 */
	public static ResolvableType forMethodReturnType(Method method, Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		MethodParameter methodParameter = new MethodParameter(method, -1, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Method} parameter.
	 * @param method the source method (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int, Class)
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(new MethodParameter(method, parameterIndex));
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Method} parameter with a
	 * given implementation. Use this variant when the class that declares the method
	 * includes generic parameter variables that are satisfied by the implementation class.
	 * @param method the source method (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int, Class)
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex, Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		MethodParameter methodParameter = new MethodParameter(method, parameterIndex, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link MethodParameter}.
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
		return forMethodParameter(methodParameter, (Type) null);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link MethodParameter} with a
	 * given implementation type. Use this variant when the class that declares the method
	 * includes generic parameter variables that are satisfied by the implementation type.
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @param implementationType the implementation type
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter,
			@Nullable ResolvableType implementationType) {

		Assert.notNull(methodParameter, "MethodParameter must not be null");
		implementationType = (implementationType != null ? implementationType :
				forType(methodParameter.getContainingClass()));
		ResolvableType owner = implementationType.as(methodParameter.getDeclaringClass());
		return forType(null, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
				getNested(methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link MethodParameter},
	 * overriding the target type to resolve with a specific given type.
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @param targetType the type to resolve (a part of the method parameter's type)
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter, @Nullable Type targetType) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		return forMethodParameter(methodParameter, targetType, methodParameter.getNestingLevel());
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link MethodParameter} at
	 * a specific nesting level, overriding the target type to resolve with a specific
	 * given type.
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @param targetType the type to resolve (a part of the method parameter's type)
	 * @param nestingLevel the nesting level to use
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @since 5.2
	 * @see #forMethodParameter(Method, int)
	 */
	static ResolvableType forMethodParameter(
			MethodParameter methodParameter, @Nullable Type targetType, int nestingLevel) {

		ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
		return forType(targetType, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
				getNested(nestingLevel, methodParameter.typeIndexesPerLevel);
	}

	/**
	 * Return a {@link ResolvableType} as an array of the specified {@code componentType}.
	 * @param componentType the component type
	 * @return a {@link ResolvableType} as an array of the specified component type
	 */
	public static ResolvableType forArrayComponent(ResolvableType componentType) {
		Assert.notNull(componentType, "Component type must not be null");
		Class<?> arrayClass = Array.newInstance(componentType.resolve(), 0).getClass();
		return new ResolvableType(arrayClass, null, null, componentType);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Type}.
	 * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
	 * @param type the source type (potentially {@code null})
	 * @return a {@link ResolvableType} for the specified {@link Type}
	 * @see #forType(Type, ResolvableType)
	 */
	public static ResolvableType forType(@Nullable Type type) {
		return forType(type, null, null);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Type} backed by the given
	 * owner type.
	 *
	 * 返回支持所有者类型的指定{@link Type}的{@link ResolvableType}
	 *
	 * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
	 *
	 * 注意：生成出来的{@link ResolvableType}实例可能不是{@link Serializable}
	 *
	 * @param type the source type or {@code null}
	 * @param owner the owner type used to resolve variables -- 用于解析变量的所有者类型
	 * @return a {@link ResolvableType} for the specified {@link Type} and owner -- 指定{@link Type}和所有者的{@link ResolvableType}
	 * @see #forType(Type)
	 */
	public static ResolvableType forType(@Nullable Type type, @Nullable ResolvableType owner) {
		VariableResolver variableResolver = null;
		if (owner != null) {
			// owner.asVariableResolver:将owner修改为DefaultVariableResolver,因为每个ResolvableType对象都具有VariableResolver的能力，
			// 通过DefaultVariableResolver调用
			variableResolver = owner.asVariableResolver();
		}
		return forType(type, variableResolver);
	}


	/**
	 * Return a {@link ResolvableType} for the specified {@link ParameterizedTypeReference}.
	 * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
	 * @param typeReference the reference to obtain the source type from
	 * @return a {@link ResolvableType} for the specified {@link ParameterizedTypeReference}
	 * @since 4.3.12
	 * @see #forType(Type)
	 */
	public static ResolvableType forType(ParameterizedTypeReference<?> typeReference) {
		return forType(typeReference.getType(), null, null);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
	 * {@link VariableResolver}.
	 *
	 * 返回由给定{@link VariableResolver}支持的指定{@link Type}的{@link ResolvableType}
	 *
	 * @param type the source type or {@code null}
	 * @param variableResolver the variable resolver or {@code null}
	 * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
	 */
	static ResolvableType forType(@Nullable Type type, @Nullable VariableResolver variableResolver) {
		return forType(type, null, variableResolver);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
	 * {@link VariableResolver}.
	 *
	 * 返回由给定{@link VariableResolver}支持的指定{@link Type}的{@link ResolvableType}
	 *
	 * @param type the source type or {@code null} -- 源类型
	 * @param typeProvider the type provider or {@code null} -- 类型提供者
	 * @param variableResolver the variable resolver or {@code null} -- 类型变量解析器，可以将类型变量解析为 ResolvableType
	 * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
	 * -- 指定的{@link Type}和{@link VariableResolver} 的{@link ResolvableType}
	 */
	static ResolvableType forType(
			@Nullable Type type, @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {

		// 如果type为null 且 typeProvider不为null
		if (type == null && typeProvider != null) {
			// 获取由typeProvider支持的序列化Type(代理对象)
			type = SerializableTypeWrapper.forTypeProvider(typeProvider);
		}
		if (type == null) {
			// 返回NONE，表示没有可用的值
			return NONE;
		}

		// For simple Class references, build the wrapper right away -
		// no expensive resolution necessary, so not worth caching...
		// 对于简单的class引用，请立即构建包装器 - 不需要昂贵的解析，，因此不值得缓存......
		if (type instanceof Class) {
			// 创建一个新的ResolvableType用于未缓存目标,其具有前期解析方案，但是以懒汉式形式计算哈希值
			return new ResolvableType(type, typeProvider, variableResolver, (ResolvableType) null);
		}

		// Purge empty entries on access since we don't have a clean-up thread or the like.
		// 由于我们没有清理线程等，因此清除访问时的空entries
		cache.purgeUnreferencedEntries();

		// 其他类型实例化后进行缓存

		// Check the cache - we may have a ResolvableType which has been resolved before...
		// 检查缓存-我们可能有一个ResolvableType，它已经在...之前解析了
		ResolvableType resultType = new ResolvableType(type, typeProvider, variableResolver);
		ResolvableType cachedType = cache.get(resultType);
		if (cachedType == null) {
			// 重新创建一个ResolvableType对象作为cacheType
			cachedType = new ResolvableType(type, typeProvider, variableResolver, resultType.hash);
			cache.put(cachedType, cachedType);
		}
		// 设置resultType的已解析类为cachedType的已解析类
		resultType.resolved = cachedType.resolved;
		return resultType;
	}

	/**
	 * Clear the internal {@code ResolvableType}/{@code SerializableTypeWrapper} cache.
	 * @since 4.2
	 */
	public static void clearCache() {
		cache.clear();
		SerializableTypeWrapper.cache.clear();
	}


	/**
	 * Strategy interface used to resolve {@link TypeVariable TypeVariables}.
	 * 解析{@link TypeVariable}的策略接口
	 */
	interface VariableResolver extends Serializable {

		/**
		 * Return the source of the resolver (used for hashCode and equals).
		 * 返回解析的源对象（用于hashCode 和 equals）
		 */
		Object getSource();

		/**
		 * Resolve the specified variable.
		 * 解析指定的变量
		 * @param variable the variable to resolve
		 * @return the resolved variable, or {@code null} if not found
		 * 解析后的变量，如果没有找到返回{@code null}
		 */
		@Nullable
		ResolvableType resolveVariable(TypeVariable<?> variable);
	}


	@SuppressWarnings("serial")
	private static class DefaultVariableResolver implements VariableResolver {
		/**
		 * 可解析类型源对象
		 */
		private final ResolvableType source;

		DefaultVariableResolver(ResolvableType resolvableType) {
			this.source = resolvableType;
		}

		@Override
		@Nullable
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			return this.source.resolveVariable(variable);
		}

		@Override
		public Object getSource() {
			return this.source;
		}
	}


	@SuppressWarnings("serial")
	private static class TypeVariablesVariableResolver implements VariableResolver {

		private final TypeVariable<?>[] variables;

		private final ResolvableType[] generics;

		public TypeVariablesVariableResolver(TypeVariable<?>[] variables, ResolvableType[] generics) {
			this.variables = variables;
			this.generics = generics;
		}

		@Override
		@Nullable
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			TypeVariable<?> variableToCompare = SerializableTypeWrapper.unwrap(variable);
			for (int i = 0; i < this.variables.length; i++) {
				TypeVariable<?> resolvedVariable = SerializableTypeWrapper.unwrap(this.variables[i]);
				if (ObjectUtils.nullSafeEquals(resolvedVariable, variableToCompare)) {
					return this.generics[i];
				}
			}
			return null;
		}

		@Override
		public Object getSource() {
			return this.generics;
		}
	}


	private static final class SyntheticParameterizedType implements ParameterizedType, Serializable {

		private final Type rawType;

		private final Type[] typeArguments;

		public SyntheticParameterizedType(Type rawType, Type[] typeArguments) {
			this.rawType = rawType;
			this.typeArguments = typeArguments;
		}

		@Override
		public String getTypeName() {
			String typeName = this.rawType.getTypeName();
			if (this.typeArguments.length > 0) {
				StringJoiner stringJoiner = new StringJoiner(", ", "<", ">");
				for (Type argument : this.typeArguments) {
					stringJoiner.add(argument.getTypeName());
				}
				return typeName + stringJoiner;
			}
			return typeName;
		}

		@Override
		@Nullable
		public Type getOwnerType() {
			return null;
		}

		@Override
		public Type getRawType() {
			return this.rawType;
		}

		@Override
		public Type[] getActualTypeArguments() {
			return this.typeArguments;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ParameterizedType)) {
				return false;
			}
			ParameterizedType otherType = (ParameterizedType) other;
			return (otherType.getOwnerType() == null && this.rawType.equals(otherType.getRawType()) &&
					Arrays.equals(this.typeArguments, otherType.getActualTypeArguments()));
		}

		@Override
		public int hashCode() {
			return (this.rawType.hashCode() * 31 + Arrays.hashCode(this.typeArguments));
		}

		@Override
		public String toString() {
			return getTypeName();
		}
	}


	/**
	 * Internal helper to handle bounds from {@link WildcardType WildcardTypes}.
	 * <p>内部辅助程序，用于处理{@link WildcardType WildcardTypes}的范围</p>
	 * <p>WildcardType：通配符表达式，泛型表达式，也可以说是，限定性的泛型，形如：? extends classA、? super classB</p>
	 *
	 */
	private static class WildcardBounds {

		/**
		 * 界限枚举对象
		 */
		private final Kind kind;

		/**
		 * 范围中的ResolvableType对象
		 */
		private final ResolvableType[] bounds;

		/**
		 * Internal constructor to create a new {@link WildcardBounds} instance.
		 *
		 * 内部构造函数，用于创建新的{@link WildcardBounds}实例
		 *
		 * @param kind the kind of bounds 范围的界限
		 * @param bounds the bounds
		 * @see #get(ResolvableType)
		 */
		public WildcardBounds(Kind kind, ResolvableType[] bounds) {
			this.kind = kind;
			this.bounds = bounds;
		}

		/**
		 * Return {@code true} if this bounds is the same kind as the specified bounds.
		 */
		public boolean isSameKind(WildcardBounds bounds) {
			return this.kind == bounds.kind;
		}

		/**
		 * Return {@code true} if this bounds is assignable to all the specified types.
		 * 如果此范围可分配给所有指定的类型，则返回{@code true}
		 * @param types the types to test against -- 要测试的ResovlableType对象
		 * @return {@code true} if this bounds is assignable to all types -- 如果此界限可分配给所有类型，则返回true
		 */
		public boolean isAssignableFrom(ResolvableType... types) {
			for (ResolvableType bound : this.bounds) {
				for (ResolvableType type : types) {
					if (!isAssignable(bound, type)) {
						return false;
					}
				}
			}
			return true;
		}

		/**
		 * 给定的类是否可分配
		 * @param source 源ResolvableType对象 -- ? extend T 或 ? super T 后面的T
		 * @param from 要比较ResolvableType对象 -- 前面的?
		 */
		private boolean isAssignable(ResolvableType source, ResolvableType from) {
			// 如果本类对象的界限枚举对象是上界限，判断source是否是from的父类或本身，并返回结果；否则判断from是否是source的父类或本身
			return (this.kind == Kind.UPPER ? source.isAssignableFrom(from) : from.isAssignableFrom(source));
		}

		/**
		 * Return the underlying bounds.
		 * 返回底层的界限
		 */
		public ResolvableType[] getBounds() {
			return this.bounds;
		}

		/**
		 * Get a {@link WildcardBounds} instance for the specified type, returning
		 * {@code null} if the specified type cannot be resolved to a {@link WildcardType}.
		 *
		 * 获取指定类型的{@link WildcardBounds}实例，如果给定类型不能解析成{@link WildcardType}返回null
		 *
		 * @param type the source type -- 源类型
		 * @return a {@link WildcardBounds} instance or {@code null}
		 */
		@Nullable
		public static WildcardBounds get(ResolvableType type) {
			// 将传入的type作为要解析成Wildcard的ResovlableType对象
			ResolvableType resolveToWildcard = type;
			// 如果resolveToWildcard的受管理的基础JavaType不是WildcardType的子类
			while (!(resolveToWildcard.getType() instanceof WildcardType)) {
				if (resolveToWildcard == NONE) {
					return null;
				}
				// 通过单级解析重新解析resolvedToWildcard对象
				resolveToWildcard = resolveToWildcard.resolveType();
			}
			WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
			// 如果wildcardType存在下边界，设置范围类型为下边界，否则为上边界
			Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
			Type[] bounds = (boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds());
			ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
			for (int i = 0; i < bounds.length; i++) {
				resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
			}
			return new WildcardBounds(boundsType, resolvableBounds);
		}

		/**
		 * The various kinds of bounds.
		 * 各种界限
		 */
		enum Kind {UPPER, LOWER}
	}


	/**
	 * Internal {@link Type} used to represent an empty value.
	 * 内部 {@link Type} 用于表示空值。
	 */
	@SuppressWarnings("serial")
	static class EmptyType implements Type, Serializable {

		static final Type INSTANCE = new EmptyType();

		Object readResolve() {
			return INSTANCE;
		}
	}

}
