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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * JDK-based {@link AopProxy} implementation for the Spring AOP framework,
 * based on JDK {@link java.lang.reflect.Proxy dynamic proxies}.
 *
 * Spring AOP 框架的基于 JDK 的 {@link AopProxy} 实现，基于 JDK {@link java.lang.reflect.Proxy 动态代理}。
 *
 * <p>Creates a dynamic proxy, implementing the interfaces exposed by
 * the AopProxy. Dynamic proxies <i>cannot</i> be used to proxy methods
 * defined in classes, rather than interfaces.
 *
 * 创建一个动态代理，实现 AopProxy 公开的接口。动态代理不能用于代理类中定义的方法，而不是接口中定义的方法。
 *
 * <p>Objects of this type should be obtained through proxy factories,
 * configured by an {@link AdvisedSupport} class. This class is internal
 * to Spring's AOP framework and need not be used directly by client code.
 *
 * 这种类型的对象应该通过代理工厂获取，并由 {@link AdvisedSupport} 类配置。该类是 Spring AOP 框架的内部类，不需要由客户端代码直接使用。
 *
 * <p>Proxies created using this class will be thread-safe if the
 * underlying (target) class is thread-safe.
 *
 * 如果底层（目标）类是线程安全的，则使用此类创建的代理将是线程安全的。
 *
 * <p>Proxies are serializable so long as all Advisors (including Advices
 * and Pointcuts) and the TargetSource are serializable.
 * 只要所有 Advisor（包括 Advice 和 Pointcut）和 TargetSource 都是可序列化的，代理就是可序列化的。
 * --
 * 采用jdk动态代理的方式创建代理对象，并处理代理对象的所有方法调用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author Sergey Tsypanov
 * @see java.lang.reflect.Proxy
 * @see AdvisedSupport
 * @see ProxyFactory
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 5531744639992436476L;


	/*
	 * NOTE: We could avoid the code duplication between this class and the CGLIB
	 * proxies by refactoring "invoke" into a template method. However, this approach
	 * adds at least 10% performance overhead versus a copy-paste solution, so we sacrifice
	 * elegance for performance (we have a good test suite to ensure that the different
	 * proxies behave the same :-)).
	 * This way, we can also more easily take advantage of minor optimizations in each class.
	 * 我们可以通过将“invoke”重构为模板方法来避免此类和CGLIB代理之间的代码重复。然而，这种方法会增加至少10%的性能开销，相对于复制粘贴解决方案，
	 * 我们选择了性能而不是优雅（我们有一个良好的测试套件来确保不同的代理行为相同 :-))。这样一来，我们还可以更容易地利用每个类中的微小优化。
	 */

	/** We use a static Log to avoid serialization issues. */
	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

	/**
	 * Config used to configure this proxy.
	 * 用于配置此代理的配置信息。
	 */
	private final AdvisedSupport advised;

	/**
	 * 代理需要被代理的所有接口列表
	 */
	private final Class<?>[] proxiedInterfaces;

	/**
	 * Is the {@link #equals} method defined on the proxied interfaces?
	 * 需要被代理的接口中是否定义了equals方法
	 */
	private boolean equalsDefined;

	/**
	 * Is the {@link #hashCode} method defined on the proxied interfaces?
	 * 需要被代理的接口中是否定义了hashCode方法
	 */
	private boolean hashCodeDefined;


	/**
	 * Construct a new JdkDynamicAopProxy for the given AOP configuration.
	 * 为给定的 AOP 配置构造一个新的 JdkDynamicAopProxy。
	 *
	 * @param config the AOP configuration as AdvisedSupport object
	 * @throws AopConfigException if the config is invalid. We try to throw an informative
	 * exception in this case, rather than let a mysterious failure happen later.
	 */
	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
		Assert.notNull(config, "AdvisedSupport must not be null");
		if (config.getAdvisorCount() == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		this.advised = config;
		// 根据advised的信息获取代理需要被代理的所有接口列表
		this.proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
		// 查找被代理的接口中是否定义了equals、hashCode方法
		findDefinedEqualsAndHashCodeMethods(this.proxiedInterfaces);
	}

	/**
	 * 生成一个代理对象
	 * @return
	 */
	@Override
	public Object getProxy() {
		return getProxy(ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Object getProxy(@Nullable ClassLoader classLoader) {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
		}

		/**
		 * 这个大家应该很熟悉吧，通过jdk动态代理创建代理对象，注意最后一个参数是this
		 * 表示当前类，当前类是InvocationHandler类型的，当调用代理对象的任何方法的时候
		 * 都会被被当前类的 invoke 方法处理
		 */
		return Proxy.newProxyInstance(determineClassLoader(classLoader), this.proxiedInterfaces, this);
	}

	/**
	 * Determine whether the JDK bootstrap or platform loader has been suggested ->
	 * use higher-level loader which can see Spring infrastructure classes instead.
	 * --
	 * 确定是否建议使用 JDK 引导程序或平台加载器 -> 使用可以看到 Spring 基础结构类的更高级别加载器。
	 */
	private ClassLoader determineClassLoader(@Nullable ClassLoader classLoader) {
		if (classLoader == null) {
			// JDK bootstrap loader -> use spring-aop ClassLoader instead.
			// JDK bootstrap loader -> 使用 spring-aop ClassLoader 代替。
			return getClass().getClassLoader();
		}
		if (classLoader.getParent() == null) {
			// Potentially the JDK platform loader on JDK 9+
			// 可能是 JDK 9+ 上的 JDK platform loader
			ClassLoader aopClassLoader = getClass().getClassLoader();
			ClassLoader aopParent = aopClassLoader.getParent();
			while (aopParent != null) {
				if (classLoader == aopParent) {
					// Suggested ClassLoader is ancestor of spring-aop ClassLoader
					// -> use spring-aop ClassLoader itself instead.
					// 建议的 ClassLoader 是 spring-aop ClassLoader 的祖先 -> 使用 spring-aop ClassLoader 本身。
					return aopClassLoader;
				}
				aopParent = aopParent.getParent();
			}
		}
		// Regular case: use suggested ClassLoader as-is.
		return classLoader;
	}

	/**
	 * Finds any {@link #equals} or {@link #hashCode} method that may be defined
	 * on the supplied set of interfaces.
	 * 判断需要代理的接口中是否定义了这几个方法（equals、hashCode）
	 *
	 * @param proxiedInterfaces the interfaces to introspect
	 */
	private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
		for (Class<?> proxiedInterface : proxiedInterfaces) {
			// 获取接口中定义的方法
			Method[] methods = proxiedInterface.getDeclaredMethods();
			for (Method method : methods) {
				// 是否是equals方法
				if (AopUtils.isEqualsMethod(method)) {
					this.equalsDefined = true;
				}
				// 是否是hashCode方法
				if (AopUtils.isHashCodeMethod(method)) {
					this.hashCodeDefined = true;
				}
				// 如果发现这2个方法都定义了，结束循环查找
				if (this.equalsDefined && this.hashCodeDefined) {
					return;
				}
			}
		}
	}


	/**
	 * Implementation of {@code InvocationHandler.invoke}.
	 * <p>Callers will see exactly the exception thrown by the target,
	 * unless a hook method throws an exception.
	 * {@code InvocableHandler.invoke} 的实现。
	 * 调用者将准确地看到目标抛出的异常，除非钩子方法抛出异常。
	 * 当在程序中调用代理对象的任何方法，最终都会被下面这个invoke方法处理
	 */
	@Override
	@Nullable
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 旧的代理对象
		Object oldProxy = null;
		// 用来标记是否需要将代理对象暴露在ThreadLocal中
		boolean setProxyContext = false;
		// 获取目标源
		TargetSource targetSource = this.advised.targetSource;
		// 目标对象
		Object target = null;

		// 下面进入代理方法的处理阶段
		try {
			// 处理equals方法：被代理的接口中没有定义equals方法 && 当前调用是equals方法
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// The target does not implement the equals(Object) method itself.
				// 目标本身不实现 equals(Object) 方法。
				// 直接调用当前类中的equals方法
				return equals(args[0]);
			}
			// 处理hashCode方法：被代理的接口中没有定义hashCode方法 && 当前调用是hashCode方法
			else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// The target does not implement the hashCode() method itself.
				// 直接调用当前类中的hashCode方法
				return hashCode();
			}
			/**
			 * 方法来源于 DecoratingProxy 接口，这个接口中定义了一个方法
			 * 用来获取原始的被代理的目标类，主要是用在嵌套代理的情况下（所谓嵌套代理：代理对象又被作为目标对象进行了代理）
			 */
			else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// There is only getDecoratedClass() declared -> dispatch to proxy config.
				/// 仅声明了 getDecolatedClass() -> 分派到代理配置。
				// 调用AopProxyUtils工具类的方法，内部通过循环遍历的方式，找到最原始的被代理的目标类
				return AopProxyUtils.ultimateTargetClass(this.advised);
			}
			// 方法来源于 Advised 接口，代理对象默认情况下会实现 Advised 接口，可以通过代理对象来动态向代理对象中添加通知等
			else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// Service invocations on ProxyConfig with the proxy config...
				// 使用代理配置对 ProxyConfig 进行服务调用...
				// this.advised是AdvisedSupport类型的，AdvisedSupport实现了Advised接口中的所有方法
				// 所以最终通过通过反射方式交给this.advised来响应当前调用
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}

			// 用来记录方法返回值
			Object retVal;

			if (this.advised.exposeProxy) {
				// Make invocation available if necessary.
				// 如有必要，使调用可用。
				// 将代理对象暴露在上线文中，即暴露在threadLocal中，那么在当前线程中可以通过静态方法
				// AopContext#currentProxy获取当前被暴露的代理对象，这个是非常有用的，稍后用案例来讲解，瞬间就会明白
				oldProxy = AopContext.setCurrentProxy(proxy);
				// 将setProxyContext标记为true
				setProxyContext = true;
			}

			// Get as late as possible to minimize the time we "own" the target,
			// in case it comes from a pool.
			// 尽可能晚地到达，以尽量减少我们“拥有”目标的时间，以防它来自池。
			// 通过目标源获取目标对象
			target = targetSource.getTarget();
			// 获取目标对象类型
			Class<?> targetClass = (target != null ? target.getClass() : null);

			// Get the interception chain for this method.
			// 获取当前方法的拦截器链
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			// Check whether we have any advice. If we don't, we can fall back on direct
			// reflective invocation of the target, and avoid creating a MethodInvocation.
			// 看看我们是否有任何advice。如果不这样做，我们可以依赖于目标的直接反射调用，并避免创建 MethodInitation。
			// 拦截器链为空的情况下，表示这个方法上面没有找到任何增强的通知，那么会直接通过反射直接调用目标对象。
			if (chain.isEmpty()) {
				// We can skip creating a MethodInvocation: just invoke the target directly
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.

				// 我们可以跳过创建方法调用：直接调用目标请注意，最终调用者必须是 InvokerInterceptor，
				// 因此我们知道它除了对目标进行反射操作之外什么也不做，并且没有热交换或花哨的代理。
				// 获取方法请求的参数（有时候方法中有可变参数，所谓可变参数就是带有省略号(...)这种格式的参数，
				// 传入的参数类型和这种类型不一样的时候，会通过下面的adaptArgumentsIfNecessary方法进行转换）
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				// 通过反射直接调用目标方法
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			}
			else {
				// We need to create a method invocation...
				// 我们需要创建一个方法调用...
				// 创建一个方法调用器（包含了代理对象、目标对象、调用的方法、参数、目标类型、方法拦截器链）
				MethodInvocation invocation =
						new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
				// Proceed to the joinpoint through the interceptor chain.
				// 通过拦截器链前往连接点。
				// 调用拦截器链，最终会调用到目标方法，获得目标方法的返回值
				retVal = invocation.proceed();
			}

			// Massage return value if necessary.
			// 下面会根据方法返回值的类型，做一些处理，比如方法返回的类型为自己，则最后需要将返回值置为代理对象
			Class<?> returnType = method.getReturnType();
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				// Special case: it returned "this" and the return type of the method
				// is type-compatible. Note that we can't help if the target sets
				// a reference to itself in another returned object.
				// 特殊情况：它返回“this”，并且方法的返回类型与类型兼容。请注意，如果目标在另一个返回的对象中设置了对自身的引用，我们将无济于事。
				// 将返回值设置为代理对象
				retVal = proxy;
			}
			// 方法的返回值类型returnType为原始类型（即int、byte、double等这种类型的） && retVal为null，
			// 此时如果将null转换为原始类型会报错，所以此处直接抛出异常
			else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
			// 返回方法调用结果
			return retVal;
		}
		finally {
			// 目标对象不为null && 目标源不是静态的
			// 所谓静态的，你可以理解为是否是单例的
			// isStatic为true，表示目标对象是单例的，同一个代理对象中所有方法共享一个目标对象
			// isStatic为false的时候，通常每次调用代理的方法，target对象是不一样的，所以方法调用之后需要进行释放，可能有些资源清理，连接的关闭等操作
			if (target != null && !targetSource.isStatic()) {
				// Must have come from TargetSource.
				// 必须释放来自TargetSource中的目标对象
				targetSource.releaseTarget(target);
			}

			// setProxyContext为ture
			if (setProxyContext) {
				// Restore old proxy.
				// 需要将旧的代理再放回到上线文中
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}


	/**
	 * Equality means interfaces, advisors and TargetSource are equal.
	 * <p>The compared object may be a JdkDynamicAopProxy instance itself
	 * or a dynamic proxy wrapping a JdkDynamicAopProxy instance.
	 *
	 * Equality意味着接口、advisors和 TargetSource 是平等的。
	 * 比较的对象可能是 JdkDynamicAopProxy 实例本身，也可能是包装 JdkDynamicAopProxy 实例的动态代理。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		// 获取JdkDynamicAopProxy
		JdkDynamicAopProxy otherProxy;
		if (other instanceof JdkDynamicAopProxy) {
			otherProxy = (JdkDynamicAopProxy) other;
		}
		else if (Proxy.isProxyClass(other.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy)) {
				return false;
			}
			otherProxy = (JdkDynamicAopProxy) ih;
		}
		else {
			// Not a valid comparison...
			// 没有有效的比较...
			return false;
		}

		// If we get here, otherProxy is the other AopProxy.
		// 如果我们到达这里，otherProxy 就是另一个 AopProxy。
		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
	}

	/**
	 * Proxy uses the hash code of the TargetSource.
	 */
	@Override
	public int hashCode() {
		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}

}
