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

package org.springframework.beans.factory.support;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Adapter that implements the {@link DisposableBean} and {@link Runnable}
 * interfaces performing various destruction steps on a given bean instance:
 * <ul>
 * <li>DestructionAwareBeanPostProcessors;
 * <li>the bean implementing DisposableBean itself;
 * <li>a custom destroy method specified on the bean definition.
 * </ul>
 * --
 * 实现 DisposableBean 和 Runnable 接口的适配器，用于在给定的 Bean 实例上执行各种销毁步骤：
 * DestructionAwareBeanPostProcessors;
 * 实现 DisposableBean 本身的 bean;
 * 在 Bean 定义上指定的自定义销毁方法。
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 2.0
 * @see AbstractBeanFactory
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
 * @see AbstractBeanDefinition#getDestroyMethodName()
 */
@SuppressWarnings("serial")
class DisposableBeanAdapter implements DisposableBean, Runnable, Serializable {

	private static final String DESTROY_METHOD_NAME = "destroy";

	private static final String CLOSE_METHOD_NAME = "close";

	private static final String SHUTDOWN_METHOD_NAME = "shutdown";


	private static final Log logger = LogFactory.getLog(DisposableBeanAdapter.class);

	private final Object bean;

	private final String beanName;

	private final boolean nonPublicAccessAllowed;

	/**
	 * 标记该bean是否DisposableBean
	 */
	private final boolean invokeDisposableBean;

	/**
	 * bean instanceof AutoCloseable && CLOSE_METHOD_NAME.equals(destroyMethodName)
	 */
	private boolean invokeAutoCloseable;

	@Nullable
	private String destroyMethodName;

	@Nullable
	private transient Method destroyMethod;

	/**
	 * 过滤requiresDestruction == false之后的DestructionAwareBeanPostProcessor列表
	 */
	@Nullable
	private final List<DestructionAwareBeanPostProcessor> beanPostProcessors;

	@Nullable
	private final AccessControlContext acc;


	/**
	 * Create a new DisposableBeanAdapter for the given bean.
	 * --
	 * 为给定的 Bean 创建一个新的 DisposableBeanAdapter。
	 *
	 * @param bean the bean instance (never {@code null})
	 * @param beanName the name of the bean
	 * @param beanDefinition the merged bean definition
	 * @param postProcessors the List of BeanPostProcessors
	 * (potentially DestructionAwareBeanPostProcessor), if any
	 */
	public DisposableBeanAdapter(Object bean, String beanName, RootBeanDefinition beanDefinition,
			List<DestructionAwareBeanPostProcessor> postProcessors, @Nullable AccessControlContext acc) {

		Assert.notNull(bean, "Disposable bean must not be null");
		this.bean = bean;
		this.beanName = beanName;
		this.nonPublicAccessAllowed = beanDefinition.isNonPublicAccessAllowed();
		// bean实现了DisposableBean接口 && beanDefinition没有外部管理的destroy()方法
		this.invokeDisposableBean = (bean instanceof DisposableBean &&
				!beanDefinition.hasAnyExternallyManagedDestroyMethod(DESTROY_METHOD_NAME));

		String destroyMethodName = inferDestroyMethodIfNecessary(bean, beanDefinition);
		// bean具有销毁方法 && 它不是实现的DisposableBean接口 && 该销毁方法不是外部管理的方法
		if (destroyMethodName != null &&
				!(this.invokeDisposableBean && DESTROY_METHOD_NAME.equals(destroyMethodName)) &&
				!beanDefinition.hasAnyExternallyManagedDestroyMethod(destroyMethodName)) {

			// bean是否实现了AutoCloseable接口
			this.invokeAutoCloseable = (bean instanceof AutoCloseable && CLOSE_METHOD_NAME.equals(destroyMethodName));
			if (!this.invokeAutoCloseable) {
				this.destroyMethodName = destroyMethodName;
				// 查找销毁方法
				Method destroyMethod = determineDestroyMethod(destroyMethodName);
				if (destroyMethod == null) {
					if (beanDefinition.isEnforceDestroyMethod()) {
						throw new BeanDefinitionValidationException("Could not find a destroy method named '" +
								destroyMethodName + "' on bean with name '" + beanName + "'");
					}
				}
				else {
					// 如果销毁方法的参数数量大于1或==1并且参数不是boolean类型的话就抛出异常
					if (destroyMethod.getParameterCount() > 0) {
						Class<?>[] paramTypes = destroyMethod.getParameterTypes();
						if (paramTypes.length > 1) {
							throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
									beanName + "' has more than one parameter - not supported as destroy method");
						}
						else if (paramTypes.length == 1 && boolean.class != paramTypes[0]) {
							throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
									beanName + "' has a non-boolean parameter - not supported as destroy method");
						}
					}
					destroyMethod = ClassUtils.getInterfaceMethodIfPossible(destroyMethod, bean.getClass());
				}
				this.destroyMethod = destroyMethod;
			}
		}

		this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
		this.acc = acc;
	}

	/**
	 * Create a new DisposableBeanAdapter for the given bean.
	 * @param bean the bean instance (never {@code null})
	 * @param postProcessors the List of BeanPostProcessors
	 * (potentially DestructionAwareBeanPostProcessor), if any
	 */
	public DisposableBeanAdapter(
			Object bean, List<DestructionAwareBeanPostProcessor> postProcessors, AccessControlContext acc) {

		Assert.notNull(bean, "Disposable bean must not be null");
		this.bean = bean;
		this.beanName = bean.getClass().getName();
		this.nonPublicAccessAllowed = true;
		this.invokeDisposableBean = (this.bean instanceof DisposableBean);
		this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
		this.acc = acc;
	}

	/**
	 * Create a new DisposableBeanAdapter for the given bean.
	 */
	private DisposableBeanAdapter(Object bean, String beanName, boolean nonPublicAccessAllowed,
			boolean invokeDisposableBean, boolean invokeAutoCloseable, @Nullable String destroyMethodName,
			@Nullable List<DestructionAwareBeanPostProcessor> postProcessors) {

		this.bean = bean;
		this.beanName = beanName;
		this.nonPublicAccessAllowed = nonPublicAccessAllowed;
		this.invokeDisposableBean = invokeDisposableBean;
		this.invokeAutoCloseable = invokeAutoCloseable;
		this.destroyMethodName = destroyMethodName;
		this.beanPostProcessors = postProcessors;
		this.acc = null;
	}


	@Override
	public void run() {
		destroy();
	}

	@Override
	public void destroy() {
		// 在销毁bean之前执行的回调
		if (!CollectionUtils.isEmpty(this.beanPostProcessors)) {
			for (DestructionAwareBeanPostProcessor processor : this.beanPostProcessors) {
				processor.postProcessBeforeDestruction(this.bean, this.beanName);
			}
		}

		// 调用DisposableBean的destroy()方法
		if (this.invokeDisposableBean) {
			if (logger.isTraceEnabled()) {
				logger.trace("Invoking destroy() on bean with name '" + this.beanName + "'");
			}
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						((DisposableBean) this.bean).destroy();
						return null;
					}, this.acc);
				}
				else {
					((DisposableBean) this.bean).destroy();
				}
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					String msg = "Invocation of destroy method failed on bean with name '" + this.beanName + "'";
					if (logger.isDebugEnabled()) {
						// Log at warn level like below but add the exception stacktrace only with debug level
						logger.warn(msg, ex);
					}
					else {
						logger.warn(msg + ": " + ex);
					}
				}
			}
		}

		// 如果bean实现了AutoCloseable解决，则调用它的close()方法
		if (this.invokeAutoCloseable) {
			if (logger.isTraceEnabled()) {
				logger.trace("Invoking close() on bean with name '" + this.beanName + "'");
			}
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						((AutoCloseable) this.bean).close();
						return null;
					}, this.acc);
				}
				else {
					((AutoCloseable) this.bean).close();
				}
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					String msg = "Invocation of close method failed on bean with name '" + this.beanName + "'";
					if (logger.isDebugEnabled()) {
						// Log at warn level like below but add the exception stacktrace only with debug level
						logger.warn(msg, ex);
					}
					else {
						logger.warn(msg + ": " + ex);
					}
				}
			}
		}
		// 调用自定义的销毁方法
		else if (this.destroyMethod != null) {
			invokeCustomDestroyMethod(this.destroyMethod);
		}
		else if (this.destroyMethodName != null) {
			Method destroyMethod = determineDestroyMethod(this.destroyMethodName);
			if (destroyMethod != null) {
				invokeCustomDestroyMethod(ClassUtils.getInterfaceMethodIfPossible(destroyMethod, this.bean.getClass()));
			}
		}
	}


	@Nullable
	private Method determineDestroyMethod(String name) {
		try {
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedAction<Method>) () -> findDestroyMethod(name));
			}
			else {
				return findDestroyMethod(name);
			}
		}
		catch (IllegalArgumentException ex) {
			throw new BeanDefinitionValidationException("Could not find unique destroy method on bean with name '" +
					this.beanName + ": " + ex.getMessage());
		}
	}

	@Nullable
	private Method findDestroyMethod(String name) {
		return (this.nonPublicAccessAllowed ?
				BeanUtils.findMethodWithMinimalParameters(this.bean.getClass(), name) :
				BeanUtils.findMethodWithMinimalParameters(this.bean.getClass().getMethods(), name));
	}

	/**
	 * Invoke the specified custom destroy method on the given bean.
	 * <p>This implementation invokes a no-arg method if found, else checking
	 * for a method with a single boolean argument (passing in "true",
	 * assuming a "force" parameter), else logging an error.
	 * --
	 * 在给定的 Bean 上调用指定的自定义销毁方法。
	 * 如果找到，此实现将调用无参数方法，否则检查具有单个布尔参数的方法（传入“true”，假设为“force”参数），否则记录错误。
	 */
	private void invokeCustomDestroyMethod(final Method destroyMethod) {
		// 销毁方法的参数数量
		int paramCount = destroyMethod.getParameterCount();
		final Object[] args = new Object[paramCount];
		// 如果只有一个参数的话，将参数的值设置为true
		if (paramCount == 1) {
			args[0] = Boolean.TRUE;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Invoking custom destroy method '" + this.destroyMethodName +
					"' on bean with name '" + this.beanName + "'");
		}
		// 通过反射调用该方法
		try {
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(destroyMethod);
					return null;
				});
				try {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
						destroyMethod.invoke(this.bean, args), this.acc);
				}
				catch (PrivilegedActionException pax) {
					throw (InvocationTargetException) pax.getException();
				}
			}
			else {
				ReflectionUtils.makeAccessible(destroyMethod);
				destroyMethod.invoke(this.bean, args);
			}
		}
		catch (InvocationTargetException ex) {
			if (logger.isWarnEnabled()) {
				String msg = "Custom destroy method '" + this.destroyMethodName + "' on bean with name '" +
						this.beanName + "' threw an exception";
				if (logger.isDebugEnabled()) {
					// Log at warn level like below but add the exception stacktrace only with debug level
					logger.warn(msg, ex.getTargetException());
				}
				else {
					logger.warn(msg + ": " + ex.getTargetException());
				}
			}
		}
		catch (Throwable ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to invoke custom destroy method '" + this.destroyMethodName +
						"' on bean with name '" + this.beanName + "'", ex);
			}
		}
	}


	/**
	 * Serializes a copy of the state of this class,
	 * filtering out non-serializable BeanPostProcessors.
	 */
	protected Object writeReplace() {
		List<DestructionAwareBeanPostProcessor> serializablePostProcessors = null;
		if (this.beanPostProcessors != null) {
			serializablePostProcessors = new ArrayList<>();
			for (DestructionAwareBeanPostProcessor postProcessor : this.beanPostProcessors) {
				if (postProcessor instanceof Serializable) {
					serializablePostProcessors.add(postProcessor);
				}
			}
		}
		return new DisposableBeanAdapter(
				this.bean, this.beanName, this.nonPublicAccessAllowed, this.invokeDisposableBean,
				this.invokeAutoCloseable, this.destroyMethodName, serializablePostProcessors);
	}


	/**
	 * Check whether the given bean has any kind of destroy method to call.
	 * @param bean the bean instance
	 * @param beanDefinition the corresponding bean definition
	 */
	public static boolean hasDestroyMethod(Object bean, RootBeanDefinition beanDefinition) {
		// bean属于DisposableBean || 具有close或shutdown方法
		return (bean instanceof DisposableBean || inferDestroyMethodIfNecessary(bean, beanDefinition) != null);
	}


	/**
	 * If the current value of the given beanDefinition's "destroyMethodName" property is
	 * {@link AbstractBeanDefinition#INFER_METHOD}, then attempt to infer a destroy method.
	 * Candidate methods are currently limited to public, no-arg methods named "close" or
	 * "shutdown" (whether declared locally or inherited). The given BeanDefinition's
	 * "destroyMethodName" is updated to be null if no such method is found, otherwise set
	 * to the name of the inferred method. This constant serves as the default for the
	 * {@code @Bean#destroyMethod} attribute and the value of the constant may also be
	 * used in XML within the {@code <bean destroy-method="">} or {@code
	 * <beans default-destroy-method="">} attributes.
	 * <p>Also processes the {@link java.io.Closeable} and {@link java.lang.AutoCloseable}
	 * interfaces, reflectively calling the "close" method on implementing beans as well.
	 * --
	 * 如果给定 beanDefinition 的 “destroyMethodName” 属性的当前值为 AbstractBeanDefinition.INFER_METHOD，
	 * 则尝试推断 destroy 方法。候选方法目前仅限于名为“close”或“shutdown”的公共无参数方法（无论是本地声明的还是继承的）。
	 * 如果未找到此类方法，则给定 BeanDefinition 的“destroyMethodName”将更新为 null，否则设置为推断方法的名称。
	 * 此常量用作属性的 @Bean#destroyMethod 默认值，常量的值也可以在 XML 中的 <bean destroy-method=""> or
	 * <beans default-destroy-method=""> 属性中使用。
	 * 还处理 java.io.Closeable 和 AutoCloseable 接口，在实现 bean 时反射性地调用“close”方法。
	 */
	@Nullable
	private static String inferDestroyMethodIfNecessary(Object bean, RootBeanDefinition beanDefinition) {
		// 先从缓存中获取
		String destroyMethodName = beanDefinition.resolvedDestroyMethodName;
		if (destroyMethodName == null) {
			// bean定义的destroyMethodName
			destroyMethodName = beanDefinition.getDestroyMethodName();
			// bean是AutoCloseable的实例
			boolean autoCloseable = (bean instanceof AutoCloseable);
			// destroyMethodName == (inferred) ||
			if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName) ||
					(destroyMethodName == null && autoCloseable)) {
				// Only perform destroy method inference in case of the bean
				// not explicitly implementing the DisposableBean interface
				// 仅在 Bean 未显式实现 DisposableBean 接口的情况下执行 destroy 方法推理
				destroyMethodName = null;
				// bean没有实现DisposableBean接口
				if (!(bean instanceof DisposableBean)) {
					if (autoCloseable) {
						// 推断destroyMethodName = close
						destroyMethodName = CLOSE_METHOD_NAME;
					}
					else {
						try {
							// 从bean类中获取close方法
							destroyMethodName = bean.getClass().getMethod(CLOSE_METHOD_NAME).getName();
						}
						catch (NoSuchMethodException ex) {
							try {
								// 没有close方法则获取shutdown()方法
								destroyMethodName = bean.getClass().getMethod(SHUTDOWN_METHOD_NAME).getName();
							}
							catch (NoSuchMethodException ex2) {
								// no candidate destroy method found
							}
						}
					}
				}
			}
			// 缓存bean的销毁方法
			beanDefinition.resolvedDestroyMethodName = (destroyMethodName != null ? destroyMethodName : "");
		}
		return (StringUtils.hasLength(destroyMethodName) ? destroyMethodName : null);
	}

	/**
	 * Check whether the given bean has destruction-aware post-processors applying to it.
	 * --
	 * 检查给定的 Bean 是否应用了可感知destruction的后处理器。
	 *
	 * @param bean the bean instance
	 * @param postProcessors the post-processor candidates
	 */
	public static boolean hasApplicableProcessors(Object bean, List<DestructionAwareBeanPostProcessor> postProcessors) {
		if (!CollectionUtils.isEmpty(postProcessors)) {
			for (DestructionAwareBeanPostProcessor processor : postProcessors) {
				if (processor.requiresDestruction(bean)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Search for all DestructionAwareBeanPostProcessors in the List.
	 * @param processors the List to search
	 * @return the filtered List of DestructionAwareBeanPostProcessors
	 */
	@Nullable
	private static List<DestructionAwareBeanPostProcessor> filterPostProcessors(
			List<DestructionAwareBeanPostProcessor> processors, Object bean) {

		List<DestructionAwareBeanPostProcessor> filteredPostProcessors = null;
		if (!CollectionUtils.isEmpty(processors)) {
			filteredPostProcessors = new ArrayList<>(processors.size());
			for (DestructionAwareBeanPostProcessor processor : processors) {
				if (processor.requiresDestruction(bean)) {
					filteredPostProcessors.add(processor);
				}
			}
		}
		return filteredPostProcessors;
	}

}
