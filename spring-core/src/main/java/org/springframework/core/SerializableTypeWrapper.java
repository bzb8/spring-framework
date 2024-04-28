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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Internal utility class that can be used to obtain wrapped {@link Serializable}
 * variants of {@link java.lang.reflect.Type java.lang.reflect.Types}.
 *
 * 内部实用程序类，可用于获取 {@link java.lang.reflect.Type java.lang.reflect.Types} 的包装 {@link Serializable} 变体。
 *
 *
 * 可用于获取包装的 {@link java.lang.reflect.Type java.lang.reflect.Types} 变体的内部实用程序类。
 *
 * <p>{@link #forField(Field) Fields} or {@link #forMethodParameter(MethodParameter)
 * MethodParameters} can be used as the root source for a serializable type.
 * Alternatively, a regular {@link Class} can also be used as source.
 *
 * {@link #forField(Field) Fields} 或 {@link #forMethodParameter(MethodParameter) MethodParameters} 可用作可序列化类型的根源。或者，也可以使用常规的 {@link Class} 作为源。
 *
 * {@link #forField(Field) Fields} 或 {@link #forMethodParameter(MethodParameter) MethodParameters} 可以用作可序列化类型的根源。
 * 或者，也可以使用常规的{@link Class}作为源。
 *
 * <p>The returned type will either be a {@link Class} or a serializable proxy of
 * {@link GenericArrayType}, {@link ParameterizedType}, {@link TypeVariable} or
 * {@link WildcardType}. With the exception of {@link Class} (which is final) calls
 * to methods that return further {@link Type Types} (for example
 * {@link GenericArrayType#getGenericComponentType()}) will be automatically wrapped.
 *
 * 返回的类型将是 {@link Class} 或 {@link GenericArrayType}、{@link ParameterizedType}、{@link TypeVariable} 或 {@link WildcardType} 的可序列化代理。
 * 除了 {@link Class}（这是最终的）之外，对返回进一步 {@link Type Types} 的方法（例如 {@link GenericArrayType#getGenericComponentType()}）的调用将被自动包装。
 *
 * 返回的类型可以是 {@link Class} 或 {@link GenericArrayType}、{@link ParameterizedType}、{@link TypeVariable} 或 {@link WildcardType} 的可序列化代理。
 * 除了 {@link Class}（这是最终的）之外，对返回更多 {@link Type Types} 的方法（例如 {@link GenericArrayType#getGenericComponentType()}）的调用将被自动包装。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class SerializableTypeWrapper {

	private static final Class<?>[] SUPPORTED_SERIALIZABLE_TYPES = {
			GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class};
	// TypeProvider的getType() -> jdk动态代理对象 TypeProxyInvocationHandler
	static final ConcurrentReferenceHashMap<Type, Type> cache = new ConcurrentReferenceHashMap<>(256);


	private SerializableTypeWrapper() {
	}


	/**
	 * Return a {@link Serializable} variant of {@link Field#getGenericType()}.
	 */
	@Nullable
	public static Type forField(Field field) {
		return forTypeProvider(new FieldTypeProvider(field));
	}

	/**
	 * Return a {@link Serializable} variant of
	 * {@link MethodParameter#getGenericParameterType()}.
	 */
	@Nullable
	public static Type forMethodParameter(MethodParameter methodParameter) {
		return forTypeProvider(new MethodParameterTypeProvider(methodParameter));
	}

	/**
	 * Unwrap the given type, effectively returning the original non-serializable type.
	 *
	 * 解包给定类型，有效地返回原始的不可序列化类型。
	 *
	 * @param type the type to unwrap
	 * @return the original non-serializable type
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Type> T unwrap(T type) {
		Type unwrapped = null;
		if (type instanceof SerializableTypeProxy) {
			unwrapped = ((SerializableTypeProxy) type).getTypeProvider().getType();
		}
		return (unwrapped != null ? (T) unwrapped : type);
	}

	/**
	 * Return a {@link Serializable} {@link Type} backed by a {@link TypeProvider} .
	 *
	 * 返回由 {@link TypeProvider} 支持的 {@link Serializable} {@link Type}。
	 *
	 * <p>If type artifacts are generally not serializable in the current runtime
	 * environment, this delegate will simply return the original {@code Type} as-is.
	 *
	 * 如果类型构建在当前运行时环境中通常不可序列化，则此委托将仅按原样返回原始 {@code Type}。
	 */
	@Nullable
	static Type forTypeProvider(TypeProvider provider) {
		// 获取Type
		Type providedType = provider.getType();
		if (providedType == null || providedType instanceof Serializable) {
			// No serializable type wrapping necessary (e.g. for java.lang.Class)
			// 无需序列化的类型包装（例如，对于 java.lang.Class）

			/*
			 * 作为获取原始类型还是与泛型相关的信息(泛型原始类型、参数类型)入口
			 * 注意：
			 *  1、原始类型，对应 Class，Class实现Serializable接口，直接返回
			 *  2、泛型类型如List<String>，返回其代理类
			 */
			return providedType;
		}
		if (NativeDetector.inNativeImage() || !Serializable.class.isAssignableFrom(Class.class)) {
			// Let's skip any wrapping attempts if types are generally not serializable in
			// the current runtime environment (even java.lang.Class itself, e.g. on GraalVM native images)
			// 如果类型在当前运行时环境中通常不可序列化（甚至是 java.lang.Class 本身，例如在 GraalVM 本机映像上），让我们跳过任何包装尝试
			return providedType;
		}

		// Obtain a serializable type proxy for the given provider...
		// 获取给定提供程序的可序列化类型代理...
		Type cached = cache.get(providedType);
		if (cached != null) {
			return cached;
		}
		// GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class
		for (Class<?> type : SUPPORTED_SERIALIZABLE_TYPES) {
			if (type.isInstance(providedType)) {
				ClassLoader classLoader = provider.getClass().getClassLoader();
				Class<?>[] interfaces = new Class<?>[] {type, SerializableTypeProxy.class, Serializable.class};
				InvocationHandler handler = new TypeProxyInvocationHandler(provider);
				// 场景provider的动态代理
				cached = (Type) Proxy.newProxyInstance(classLoader, interfaces, handler);
				cache.put(providedType, cached);
				return cached;
			}
		}
		throw new IllegalArgumentException("Unsupported Type class: " + providedType.getClass().getName());
	}


	/**
	 * Additional interface implemented by the type proxy.
	 * 由类型 proxy 实现的其他接口。
	 */
	interface SerializableTypeProxy {

		/**
		 * Return the underlying type provider.
		 *
		 * 返回基础类型提供程序。
		 */
		TypeProvider getTypeProvider();
	}


	/**
	 * A {@link Serializable} interface providing access to a {@link Type}.
	 * <p>
	 * 提供对{@link Type}的访问的{@link Serializable}接口。
	 *
	 */
	@SuppressWarnings("serial")
	interface TypeProvider extends Serializable {

		/**
		 * Return the (possibly non {@link Serializable}) {@link Type}.
		 * 返回 （可能非 {@link Serializable}） {@link Type}。
		 * 返回类型
		 */
		@Nullable
		Type getType();

		/**
		 * Return the source of the type, or {@code null} if not known.
		 * <p>The default implementations returns {@code null}.
		 * --
		 * 返回类型的源，如果未知，则返回 {@code null}。
		 * 默认实现返回 {@code null}。
		 * --
		 * 返回类型源，如Field
		 */
		@Nullable
		default Object getSource() {
			return null;
		}
	}


	/**
	 * {@link Serializable} {@link InvocationHandler} used by the proxied {@link Type}.
	 * Provides serialization support and enhances any methods that return {@code Type}
	 * or {@code Type[]}.
	 *
	 * {@link Serializable}{@link InvocationHandler} 由代理 {@link Type} 使用。提供序列化支持并增强{@code Type} 或 {@code Type[]} 返回 的任何方法。
	 *
	 * 关联的调用处理程序，当在代理实例上调用方法时，方法调用将被编码并分派到其调用处理程序的invoke方法。
	 * <p>实现了(GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class 其中之一)
	 * 和SerializableTypeProxy.class, Serializable.class。
	 * 目的是将原始Type的方法返回值变为jdk 动态代理类型 TypeProxyInvocationHandler
	 */
	@SuppressWarnings("serial")
	private static class TypeProxyInvocationHandler implements InvocationHandler, Serializable {
		// 原始目标对象，TypeProvider
		private final TypeProvider provider;

		public TypeProxyInvocationHandler(TypeProvider provider) {
			this.provider = provider;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					Object other = args[0];
					// Unwrap proxies for speed
					if (other instanceof Type) {
						other = unwrap((Type) other);
					}
					return ObjectUtils.nullSafeEquals(this.provider.getType(), other);
				case "hashCode":
					return ObjectUtils.nullSafeHashCode(this.provider.getType());

				/*
				 * 实现SerializableTypeProxy.getTypeProvider()方法
				 */
				case "getTypeProvider":
					// 执行getTypeProvider方法
					return this.provider;
			}

			// 方法返回值为Type并且方法参数为空
			/*
			 * 实现Type子接口返回Type类型的方法：
			 *   GenericArrayType.getGenericComponentType()
			 *   ParameterizedType.getRawType()/getOwnerType()
			 */
			if (Type.class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
				return forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, -1));
			}
			/*
			 * 实现Type子接口返回Type[]类型的方法：
			 *   ParameterizedType.getActualTypeArguments()
			 *   TypeVariable.getBounds()
			 *   WildcardType.getUpperBounds()/getLowerBounds()
			 */
			else if (Type[].class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
				/*
				 * 1: ParameterizedType.getActualTypeArguments()
				 *    返回一个表示此类型的实际类型参数的Type数组,eg: Hash<String, String>返回[class java.lang.String, class java.lang.String]
				 */
				Object returnValue = ReflectionUtils.invokeMethod(method, this.provider.getType());
				if (returnValue == null) {
					return null;
				}
				Type[] result = new Type[((Type[]) returnValue).length];
				for (int i = 0; i < result.length; i++) {
					result[i] = forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, i));
				}
				return result;
			}

			return ReflectionUtils.invokeMethod(method, this.provider.getType(), args);
		}
	}


	/**
	 * {@link TypeProvider} for {@link Type Types} obtained from a {@link Field}.
	 *
	 * {@link TypeProvider} 用于从 {@link Field} 获取的 {@link Type Types}。
	 *
	 */
	@SuppressWarnings("serial")
	static class FieldTypeProvider implements TypeProvider {

		private final String fieldName;

		private final Class<?> declaringClass;

		private transient Field field;

		public FieldTypeProvider(Field field) {
			this.fieldName = field.getName();
			this.declaringClass = field.getDeclaringClass();
			this.field = field;
		}

		@Override
		public Type getType() {
			return this.field.getGenericType();
		}

		@Override
		public Object getSource() {
			return this.field;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			try {
				this.field = this.declaringClass.getDeclaredField(this.fieldName);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not find original class structure", ex);
			}
		}
	}


	/**
	 * {@link TypeProvider} for {@link Type Types} obtained from a {@link MethodParameter}.
	 *
	 * {@link TypeProvider} 表示从 {@link MethodParameter} 获取的 {@link Type Types}。
	 */
	@SuppressWarnings("serial")
	static class MethodParameterTypeProvider implements TypeProvider {

		@Nullable
		private final String methodName;

		private final Class<?>[] parameterTypes;

		private final Class<?> declaringClass;

		private final int parameterIndex;

		private transient MethodParameter methodParameter;

		public MethodParameterTypeProvider(MethodParameter methodParameter) {
			this.methodName = (methodParameter.getMethod() != null ? methodParameter.getMethod().getName() : null);
			this.parameterTypes = methodParameter.getExecutable().getParameterTypes();
			this.declaringClass = methodParameter.getDeclaringClass();
			this.parameterIndex = methodParameter.getParameterIndex();
			this.methodParameter = methodParameter;
		}

		@Override
		public Type getType() {
			return this.methodParameter.getGenericParameterType();
		}

		@Override
		public Object getSource() {
			return this.methodParameter;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			try {
				if (this.methodName != null) {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes), this.parameterIndex);
				}
				else {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not find original class structure", ex);
			}
		}
	}


	/**
	 * {@link TypeProvider} for {@link Type Types} obtained by invoking a no-arg method.
	 */
	@SuppressWarnings("serial")
	static class MethodInvokeTypeProvider implements TypeProvider {

		// 委托给provider处理，目标对象
		private final TypeProvider provider;

		private final String methodName;

		private final Class<?> declaringClass;

		/**
		 * 数组索引值
		 * -1表示Type的方法返回值为Type
		 * 其他表示Type的方法返回值为Type[]
		 */
		private final int index;

		private transient Method method;

		// 方法返回值
		@Nullable
		private transient volatile Object result;

		public MethodInvokeTypeProvider(TypeProvider provider, Method method, int index) {
			this.provider = provider;
			this.methodName = method.getName();
			this.declaringClass = method.getDeclaringClass();
			this.index = index;
			this.method = method;
		}

		// 延迟实例化
		@Override
		@Nullable
		public Type getType() {
			Object result = this.result;
			if (result == null) {
				// Lazy invocation of the target method on the provided type
				// 对提供的类型延迟调用目标方法，result = 方法返回结果
				result = ReflectionUtils.invokeMethod(this.method, this.provider.getType());
				// Cache the result for further calls to getType()
				// 缓存结果以进一步调用 getType()
				this.result = result;
			}
			return (result instanceof Type[] ? ((Type[]) result)[this.index] : (Type) result);
		}

		@Override
		@Nullable
		public Object getSource() {
			return null;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			Method method = ReflectionUtils.findMethod(this.declaringClass, this.methodName);
			if (method == null) {
				throw new IllegalStateException("Cannot find method on deserialization: " + this.methodName);
			}
			if (method.getReturnType() != Type.class && method.getReturnType() != Type[].class) {
				throw new IllegalStateException(
						"Invalid return type on deserialized method - needs to be Type or Type[]: " + method);
			}
			this.method = method;
		}
	}

}
