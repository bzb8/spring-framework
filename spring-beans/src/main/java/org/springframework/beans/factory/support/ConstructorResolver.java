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

import java.beans.ConstructorProperties;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Delegate for resolving constructors and factory methods.
 *
 * 用于解析构造函数和工厂方法的委托。
 *
 * Performs constructor resolution through argument matching.
 *
 * 通过参数匹配执行构造函数解析。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 2.0
 * @see #autowireConstructor
 * @see #instantiateUsingFactoryMethod
 * @see AbstractAutowireCapableBeanFactory
 */
class ConstructorResolver {

	private static final Object[] EMPTY_ARGS = new Object[0];

	private static final NamedThreadLocal<InjectionPoint> currentInjectionPoint =
			new NamedThreadLocal<>("Current injection point");


	private final AbstractAutowireCapableBeanFactory beanFactory;

	private final Log logger;


	/**
	 * Create a new ConstructorResolver for the given factory and instantiation strategy.
	 * @param beanFactory the BeanFactory to work with
	 */
	public ConstructorResolver(AbstractAutowireCapableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.logger = beanFactory.getLogger();
	}


	/**
	 * "autowire constructor" (with constructor arguments by type) behavior.
	 * Also applied if explicit constructor argument values are specified,
	 * matching all remaining arguments with beans from the bean factory.
	 * <p>This corresponds to constructor injection: In this mode, a Spring
	 * bean factory is able to host components that expect constructor-based
	 * dependency resolution.
	 *
	 * “AutoWire 构造函数”（按类型使用构造函数参数）行为。如果指定了显式构造函数参数值，则也适用，将所有剩余参数与 Bean 工厂中的 Bean 进行匹配。
	 * 这对应于构造函数注入：在这种模式下，Spring Bean 工厂能够托管需要基于构造函数的依赖项解析的组件。
	 *
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @param chosenCtors chosen candidate constructors (or {@code null} if none) 选定的候选构造函数（如果没有，则为 {@code null}）
	 * @param explicitArgs argument values passed in programmatically via the getBean method,
	 * or {@code null} if none (-> use constructor argument values from bean definition)
	 *
	 * 通过 getBean 方法以编程方式传入的参数值，如果没有，则为 {@code null}（-> 使用 Bean 定义中的构造函数参数值）
	 *
	 * @return a BeanWrapper for the new instance
	 */
	public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
			@Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {
		// 新建一个BeanWrapperImp实例，用于封装使用构造方法生成与beanName对应的Bean对象
		BeanWrapperImpl bw = new BeanWrapperImpl();
		// 初始化bw
		this.beanFactory.initBeanWrapper(bw);

		// 定义一个用于要使用的构造函数对象的Constructor对象
		Constructor<?> constructorToUse = null;
		// 声明一个用于存储不同形式的参数值的ArgumentsHolder，默认为null
		ArgumentsHolder argsHolderToUse = null;
		// 定义一个用于要使用的参数值数组
		Object[] argsToUse = null;

		if (explicitArgs != null) {
			// 如果explicitArgs不为null，让argsToUse引用explicitArgs
			argsToUse = explicitArgs;
		}
		else {
			// 声明一个要解析的参数值数组，默认为null
			Object[] argsToResolve = null;
			synchronized (mbd.constructorArgumentLock) {
				// 指定constructorToUse引用mbd已解析的构造函数或工厂方法对象
				constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse != null && mbd.constructorArgumentsResolved) {
					// Found a cached constructor...
					// 找到了缓存的构造方法
					// 指定argsToUse引用mbd完全解析的构造函数参数值
					argsToUse = mbd.resolvedConstructorArguments;
					if (argsToUse == null) {
						// 指定argsToResolve引用mbd部分准备好的构造函数参数值
						argsToResolve = mbd.preparedConstructorArguments;
					}
				}
			}
			// 如果argsToResolve不为null,即表示mbd还没有完全解析的构造函数参数值
			if (argsToResolve != null) {
				// 解析缓存在mbd中准备好的参数值,允许在没有此类BeanDefintion的时候回退
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve);
			}
		}

		// 如果constructorToUse为null或者argsToUser为null
		if (constructorToUse == null || argsToUse == null) {
			// Take specified constructors, if any.
			// 采用指定的构造函数（如果有）。
			Constructor<?>[] candidates = chosenCtors;
			if (candidates == null) {
				// 获取mbd的Bean类
				Class<?> beanClass = mbd.getBeanClass();
				try {
					// 如果mbd允许访问非公共构造函数和方法，获取BeanClass的所有声明构造函数;否则获取public的构造函数
					candidates = (mbd.isNonPublicAccessAllowed() ?
							beanClass.getDeclaredConstructors() : beanClass.getConstructors());
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Resolution of declared constructors on bean Class [" + beanClass.getName() +
							"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
				}
			}

			// 如果candidateList只有一个元素 且 没有传入构造函数值 且 mbd也没有构造函数参数值
			if (candidates.length == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
				Constructor<?> uniqueCandidate = candidates[0];
				if (uniqueCandidate.getParameterCount() == 0) {
					synchronized (mbd.constructorArgumentLock) {
						mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
						mbd.constructorArgumentsResolved = true;
						mbd.resolvedConstructorArguments = EMPTY_ARGS;
					}
					// 使用constructorToUse生成与beanName对应的Bean对象,并将该Bean对象保存到bw中
					bw.setBeanInstance(instantiate(beanName, mbd, uniqueCandidate, EMPTY_ARGS));
					return bw;
				}
			}

			// Need to resolve the constructor.
			// 需要解析构造函数。
			// 定义一个mbd是否支持使用构造函数进行自动注入的标记. 如果chosenCtos不为null或者mbd解析自动注入模式为自动注入可以满足的最
			// 贪婪的构造函数的常数(涉及解析适当的构造函数)就为true;否则为false
			boolean autowiring = (chosenCtors != null ||
					mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
			// 定义一个用于存放解析后的构造函数参数值的ConstructorArgumentValues对象
			ConstructorArgumentValues resolvedValues = null;

			int minNrOfArgs;
			if (explicitArgs != null) {
				minNrOfArgs = explicitArgs.length;
			}
			else {
				// 获取mbd的构造函数参数值
				ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
				resolvedValues = new ConstructorArgumentValues();
				// 将cargs解析后值保存到resolveValues中，并让minNrOfArgs引用解析后的最小(索引参数值数+泛型参数值数)
				minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
			}
			// AutowireUtils.sortConstructors:对给定的构造函数进行排序.优先取public级别的构造函数，然后取非public级别的构造函数；
			AutowireUtils.sortConstructors(candidates);
			// 定义一个最小类型差异权重，默认是Integer最大值
			int minTypeDiffWeight = Integer.MAX_VALUE;
			Set<Constructor<?>> ambiguousConstructors = null;
			Deque<UnsatisfiedDependencyException> causes = null;

			for (Constructor<?> candidate : candidates) {
				int parameterCount = candidate.getParameterCount();

				// 如果已经找到要使用的构造函数和要使用的构造函数参数值且要使用的构造函数参数值比要匹配的参数长度要多，即意味着找到
				// 最匹配的构造函数
				if (constructorToUse != null && argsToUse != null && argsToUse.length > parameterCount) {
					// Already found greedy constructor that can be satisfied ->
					// do not look any further, there are only less greedy constructors left.
					// 已经发现可以满足的贪婪构造函数->不要再看了，只剩下更少的贪婪构造函数了
					// 跳出遍历循环
					break;
				}
				if (parameterCount < minNrOfArgs) {
					continue;
				}

				// 定义一个封装参数数组的ArgumentsHolder对象
				ArgumentsHolder argsHolder;
				Class<?>[] paramTypes = candidate.getParameterTypes();
				if (resolvedValues != null) {
					try {
						// 获取candidate的ConstructorProperties注解的name属性值作为candidate的参数名
						String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, parameterCount);
						if (paramNames == null) {
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								paramNames = pnd.getParameterNames(candidate);
							}
						}
						// 将resolvedValues转换成一个封装着参数数组ArgumentsHolder实例，当candidate只有一个时，支持可在抛
						// 出没有此类BeanDefintion的异常返回null，而不抛出异常
						argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
								getUserDeclaredConstructor(candidate), autowiring, candidates.length == 1);
					}
					catch (UnsatisfiedDependencyException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
						}
						// Swallow and try next constructor.
						if (causes == null) {
							causes = new ArrayDeque<>(1);
						}
						causes.add(ex);
						continue;
					}
				}
				else {
					// Explicit arguments given -> arguments length must match exactly.
					// 给定的显示参数 -> 参数长度必须完全匹配
					if (parameterCount != explicitArgs.length) {
						continue;
					}
					// 实例化argsHolder，封装explicitArgs到argsHolder
					argsHolder = new ArgumentsHolder(explicitArgs);
				}

				//mbd支持的构造函数解析模式,默认使用宽松模式:
				// 1. 严格模式如果摸棱两可的构造函数在转换参数时都匹配，则抛出异常
				// 2. 宽松模式将使用"最接近类型匹配"的构造函数
				// 如果bd支持的构造函数解析模式时宽松模式,引用获取类型差异权重值，否则引用获取Assignabliity权重值
				int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
						argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
				// Choose this constructor if it represents the closest match.
				// 如果它表示最接近的匹配项，则选择此构造函数
				if (typeDiffWeight < minTypeDiffWeight) {
					constructorToUse = candidate;
					argsHolderToUse = argsHolder;
					argsToUse = argsHolder.arguments;
					minTypeDiffWeight = typeDiffWeight;
					ambiguousConstructors = null;
				}
				else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
					if (ambiguousConstructors == null) {
						ambiguousConstructors = new LinkedHashSet<>();
						ambiguousConstructors.add(constructorToUse);
					}
					ambiguousConstructors.add(candidate);
				}
			}

			if (constructorToUse == null) {
				if (causes != null) {
					UnsatisfiedDependencyException ex = causes.removeLast();
					for (Exception cause : causes) {
						this.beanFactory.onSuppressedException(cause);
					}
					throw ex;
				}
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Could not resolve matching constructor on bean class [" + mbd.getBeanClassName() + "] " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
			}
			else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Ambiguous constructor matches found on bean class [" + mbd.getBeanClassName() + "] " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
						ambiguousConstructors);
			}

			// 缓存
			if (explicitArgs == null && argsHolderToUse != null) {
				argsHolderToUse.storeCache(mbd, constructorToUse);
			}
		}

		Assert.state(argsToUse != null, "Unresolved constructor arguments");
		bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, argsToUse));
		return bw;
	}

	private Object instantiate(
			String beanName, RootBeanDefinition mbd, Constructor<?> constructorToUse, Object[] argsToUse) {

		try {
			InstantiationStrategy strategy = this.beanFactory.getInstantiationStrategy();
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedAction<Object>) () ->
						strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse),
						this.beanFactory.getAccessControlContext());
			}
			else {
				return strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse);
			}
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean instantiation via constructor failed", ex);
		}
	}

	/**
	 * Resolve the factory method in the specified bean definition, if possible.
	 * 如果可能，请在指定的 Bean 定义中解析工厂方法。
	 *
	 * {@link RootBeanDefinition#getResolvedFactoryMethod()} can be checked for the result.
	 * @param mbd the bean definition to check
	 */
	public void resolveFactoryMethodIfPossible(RootBeanDefinition mbd) {
		Class<?> factoryClass;
		boolean isStatic;
		if (mbd.getFactoryBeanName() != null) {
			factoryClass = this.beanFactory.getType(mbd.getFactoryBeanName());
			isStatic = false;
		}
		else {
			factoryClass = mbd.getBeanClass();
			isStatic = true;
		}
		Assert.state(factoryClass != null, "Unresolvable factory class");
		factoryClass = ClassUtils.getUserClass(factoryClass);

		Method[] candidates = getCandidateMethods(factoryClass, mbd);
		Method uniqueCandidate = null;
		for (Method candidate : candidates) {
			if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
				if (uniqueCandidate == null) {
					uniqueCandidate = candidate;
				}
				else if (isParamMismatch(uniqueCandidate, candidate)) {
					uniqueCandidate = null;
					break;
				}
			}
		}
		mbd.factoryMethodToIntrospect = uniqueCandidate;
	}

	private boolean isParamMismatch(Method uniqueCandidate, Method candidate) {
		int uniqueCandidateParameterCount = uniqueCandidate.getParameterCount();
		int candidateParameterCount = candidate.getParameterCount();
		return (uniqueCandidateParameterCount != candidateParameterCount ||
				!Arrays.equals(uniqueCandidate.getParameterTypes(), candidate.getParameterTypes()));
	}

	/**
	 * Retrieve all candidate methods for the given class, considering
	 * the {@link RootBeanDefinition#isNonPublicAccessAllowed()} flag.
	 * Called as the starting point for factory method determination.
	 * 检索给定类的所有候选方法，考虑到{@link RootBeanDefinition#isNonPublicAccessAllowed()}标志。作为确定工厂方法的起点调用。
	 */
	private Method[] getCandidateMethods(Class<?> factoryClass, RootBeanDefinition mbd) {
		if (System.getSecurityManager() != null) {
			return AccessController.doPrivileged((PrivilegedAction<Method[]>) () ->
					(mbd.isNonPublicAccessAllowed() ?
						ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods()));
		}
		else {
			return (mbd.isNonPublicAccessAllowed() ?
					// 获取本类或父类的所有方法，包括非public方法
					ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods());
		}
	}

	/**
	 * Instantiate the bean using a named factory method. The method may be static, if the
	 * bean definition parameter specifies a class, rather than a "factory-bean", or
	 * an instance variable on a factory object itself configured using Dependency Injection.
	 * <p>Implementation requires iterating over the static or instance methods with the
	 * name specified in the RootBeanDefinition (the method may be overloaded) and trying
	 * to match with the parameters. We don't have the types attached to constructor args,
	 * so trial and error is the only way to go here. The explicitArgs array may contain
	 * argument values passed in programmatically via the corresponding getBean method.
	 * --
	 * 使用命名的工厂方法实例化bean。如果bean定义参数指定的是类而不是"factory-bean"，
	 * 或者是通过依赖注入配置的工厂对象本身的实例变量，则该方法可以是静态的。
	 * 实现需要迭代具有RootBeanDefinition中指定的名称的静态或实例方法（方法可能是重载的），并尝试与参数匹配。
	 * 我们没有将类型附加到构造函数参数，所以这里只能通过试错的方式进行。explicitArgs数组可以包含通过相应的getBean方法以编程方式传递的参数值。
	 *
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @param explicitArgs argument values passed in programmatically via the getBean
	 * method, or {@code null} if none (-> use constructor argument values from bean definition)
	 * 通过 getBean 方法以编程方式传入的参数值，如果没有，则为 {@code null}（-> 使用 Bean 定义中的构造函数参数值）
	 *
	 * @return a BeanWrapper for the new instance
	 */
	public BeanWrapper instantiateUsingFactoryMethod(
			String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {

		BeanWrapperImpl bw = new BeanWrapperImpl();
		this.beanFactory.initBeanWrapper(bw);

		// 工厂对象
		Object factoryBean;
		// 定义一个用于存放工厂Bean对象的类对象的Class
		Class<?> factoryClass;
		// 定义一个表示是静态工厂方法的标记
		boolean isStatic;

		// 获取工厂bean名称，如果工厂bean名称不为空，表示以实例工厂的方式创建实例对象
		String factoryBeanName = mbd.getFactoryBeanName();
		// 解析factoryClass和isStatic
		if (factoryBeanName != null) {
			// 抛出 BeanDefinitionStoreException：factory-bean引用指向相同的beanDefinition
			if (factoryBeanName.equals(beanName)) {
				throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
						"factory-bean reference points back to the same bean definition"); // 工厂 bean 引用指向相同的 bean 定义
			}
			// 创建factoryBeanName bean
			factoryBean = this.beanFactory.getBean(factoryBeanName);
			// 已经创建了bean，则抛出异常
			if (mbd.isSingleton() && this.beanFactory.containsSingleton(beanName)) {
				throw new ImplicitlyAppearedSingletonException();
			}
			// bean 依赖factoryBean
			this.beanFactory.registerDependentBean(factoryBeanName, beanName);
			factoryClass = factoryBean.getClass();
			isStatic = false;
		}
		else {
			// It's a static factory method on the bean class.
			// 它是 Bean 类上的静态工厂方法。
			if (!mbd.hasBeanClass()) {
				throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
						"bean definition declares neither a bean class nor a factory-bean reference");
				// Bean 定义既不声明 Bean 类，也不声明工厂 Bean 引用
			}
			factoryBean = null;
			factoryClass = mbd.getBeanClass();
			isStatic = true;
		}

		// 2. 尝试从mbd的缓存属性中获取要使用的工厂方法，要使用的参数值数组
		// 声明最终选择的工厂方法，默认为null
		Method factoryMethodToUse = null;
		// 最终选择的ArgumentsHolder
		ArgumentsHolder argsHolderToUse = null;
		// 已经解析（依赖注入过的）的参数列表
		// 声明一个要使用的参数值数组,默认为null
		Object[] argsToUse = null;

		// 解析要使用的工厂方法参数值数组和工厂方法
		if (explicitArgs != null) {
			argsToUse = explicitArgs;
		}
		// 如果没有显式传递要使用的工厂方法参数
		else {
			// 声明一个待解析的参数值数组，默认为null
			Object[] argsToResolve = null;
			// 使用mbd的构造函数字段通用锁进行加锁，以保证线程安全
			synchronized (mbd.constructorArgumentLock) {
				// 指定factoryMethodToUser引用mbd已解析的构造函数或工厂方法对象
				factoryMethodToUse = (Method) mbd.resolvedConstructorOrFactoryMethod;
				// 如果factoryMethodToUser不为null 且 mbd已解析构造函数参数
				if (factoryMethodToUse != null && mbd.constructorArgumentsResolved) {
					// Found a cached factory method...
					// 找到缓存的工厂方法...
					// 指定argsToUse引用mbd完全解析的构造函数参数值
					argsToUse = mbd.resolvedConstructorArguments;
					if (argsToUse == null) {
						// argsToUse == null, 则指定argsToResolve引用mbd部分准备好的构造函数参数值
						argsToResolve = mbd.preparedConstructorArguments;
					}
				}
			}
			// 如果argsToResolve不为null,即表示mbd还没有完全解析的构造函数参数值
			if (argsToResolve != null) {
				// 解析缓存在mbd中准备好的参数值,允许在没有此类BeanDefintion的时候回退
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, factoryMethodToUse, argsToResolve);
			}
		}

		//3. 在没法从mbd的缓存属性中获取要使用的工厂方法，或者要使用的参数值数组时，尝试从候选工厂方法中获取要使用的工厂方法以及要使用的参数值数组
		// 如果factoryMethoToUse为null或者argsToUser为null
		if (factoryMethodToUse == null || argsToUse == null) {
			// 3.1 获取工厂类的所有候选工厂方法

			// Need to determine the factory method...
			// Try all methods with this name to see if they match the given arguments.
			// 需要确定工厂方法...
			// 尝试使用此名称的所有方法，看看它们是否与给定的参数匹配。
			// ClassUtils.getUserClass：如果clazz是CGLIB生成的子类，则返回该子类的父类，否则直接返回要检查的类
			factoryClass = ClassUtils.getUserClass(factoryClass);

			// 定义一个用于存储候选方法的集合
			List<Method> candidates = null;
			// 如果mbd所配置工厂方法时唯一
			if (mbd.isFactoryMethodUnique) {
				if (factoryMethodToUse == null) {
					// 如果factoryMethodToUse为null，获取mbd解析后的工厂方法对象
					factoryMethodToUse = mbd.getResolvedFactoryMethod();
				}
				if (factoryMethodToUse != null) {
					// 新建一个不可变，只能存一个对象的集合，将factoryMethodToUse添加进行，然后让candidateList引用该集合
					candidates = Collections.singletonList(factoryMethodToUse);
				}
			}
			// 如果candidateList为null
			if (candidates == null) {
				// 让candidateList引用一个新的ArrayList
				candidates = new ArrayList<>();
				// 获取factoryClass中的所有方法，可能包含私有、还包含父级的
				// 根据mbd的是否允许访问非公共构造函数和方法标记【RootBeanDefinition.isNonPublicAccessAllowed】来获取factoryClass的所有候选方法
				Method[] rawCandidates = getCandidateMethods(factoryClass, mbd);
				for (Method candidate : rawCandidates) {
					// 如果candidate的修饰符与isStatic一致 且 candidate有资格作为mdb的工厂方法
					if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
						candidates.add(candidate);
					}
				}
			}

			// 3.2 候选方法只有一个且没有构造函数时，就直接使用该候选方法生成与beanName对应的Bean对象封装到bw中返回出去

			// 如果candidateList只有一个元素 且 没有显式传入构造函数值 且 mbd也没有构造函数参数值
			if (candidates.size() == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
				Method uniqueCandidate = candidates.get(0);
				// 如果uniqueCandidate是不需要参数
				if (uniqueCandidate.getParameterCount() == 0) {
					// 让mbd缓存uniqueCandidate【{@link RootBeanDefinition#factoryMethodToIntrospect}】
					mbd.factoryMethodToIntrospect = uniqueCandidate;
					synchronized (mbd.constructorArgumentLock) {
						mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
						mbd.constructorArgumentsResolved = true;
						mbd.resolvedConstructorArguments = EMPTY_ARGS;
					}
					// 使用反射调用工厂方法，直接返回
					bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, uniqueCandidate, EMPTY_ARGS));
					return bw;
				}
			}

			// 3.3 筛选出最匹配的候选工厂方法，以及解析出去对应的工厂方法的参数值

			if (candidates.size() > 1) {  // explicitly skip immutable singletonList
				// 对candidates进行排序，首选公共方法和带有最多参数的'greedy'方法
				candidates.sort(AutowireUtils.EXECUTABLE_COMPARATOR);
			}

			// ConstructorArgumentValues：构造函数参数值的Holder,通常作为BeanDefinition的一部分,支持构造函数参数列表中特定索引的值

			// 定义一个用于存放解析后的构造函数参数值的ConstructorArgumentValues对象
			ConstructorArgumentValues resolvedValues = null;
			// 定义一个mbd是否支持使用构造函数进行自动注入的标记
			boolean autowiring = (mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
			// 定义一个最小类型差异权重，默认是Integer最大值
			int minTypeDiffWeight = Integer.MAX_VALUE;
			// 定义一个存储摸棱两可的工厂方法的Set集合,以用于抛出BeanCreationException时描述异常信息
			Set<Method> ambiguousFactoryMethods = null;

			// 定义一个最少参数数，默认为0
			int minNrOfArgs;
			if (explicitArgs != null) {
				// minNrOfArgs引用explitArgs的数组长度
				minNrOfArgs = explicitArgs.length;
			}
			else {
				// We don't have arguments passed in programmatically, so we need to resolve the
				// arguments specified in the constructor arguments held in the bean definition.
				// 我们没有以编程方式传入的参数，因此我们需要解析 bean 定义中保存的构造函数参数中指定的参数。

				// 如果mbd有构造函数参数值
				if (mbd.hasConstructorArgumentValues()) {
					ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
					// 对resolvedValues实例化
					resolvedValues = new ConstructorArgumentValues();
					// 将cargs解析后值保存到resolveValues中，并让minNrOfArgs引用解析后的最小(索引参数值数+泛型参数值数)
					minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
				}
				else {
					// 意味着mbd没有构造函数参数值时，将minNrOfArgs设为0
					minNrOfArgs = 0;
				}
			}

			// 定义一个用于UnsatisfiedDependencyException的列表
			Deque<UnsatisfiedDependencyException> causes = null;

			// 遍历candidates，元素名为candidate
			for (Method candidate : candidates) {
				// 获取candidate方法的参数数量
				int parameterCount = candidate.getParameterCount();

				// 如果paramTypes的数组长度大于等于minNrOfArgs
				if (parameterCount >= minNrOfArgs) {
					// 定义一个封装解析后的参数数组的ArgumentsHolder对象
					ArgumentsHolder argsHolder;

					// 当前候选方法的参数类型列表
					Class<?>[] paramTypes = candidate.getParameterTypes();
					if (explicitArgs != null) {
						// Explicit arguments given -> arguments length must match exactly.
						// 给定的显示参数->参数长度必须完全匹配
						// 如果paramTypes的长度与explicitArgsd额长度不相等
						if (paramTypes.length != explicitArgs.length) {
							// 跳过当次循环中剩下的步骤，执行下一次循环
							continue;
						}
						// 实例化argsHolder，封装explicitArgs到argsHolder
						argsHolder = new ArgumentsHolder(explicitArgs);
					}
					else {
						// Resolved constructor arguments: type conversion and/or autowiring necessary.
						// 已解析的构造函数参数:类型转换 and/or 自动注入时必须的
						try {
							// 当前候选方法的参数名称列表
							String[] paramNames = null;
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								// 通过pnd解析candidate的参数名
								paramNames = pnd.getParameterNames(candidate);
							}
							// 将resolvedValues转换成一个封装着参数数组ArgumentsHolder实例，当candidate只有一个时，支持可在抛
							// 出没有此类BeanDefintion的异常返回null，而不抛出异常
							argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw,
									paramTypes, paramNames, candidate, autowiring, candidates.size() == 1);
						}
						catch (UnsatisfiedDependencyException ex) {
							if (logger.isTraceEnabled()) {
								logger.trace("Ignoring factory method [" + candidate + "] of bean '" + beanName + "': " + ex);
							}
							// Swallow and try next overloaded factory method.
							if (causes == null) {
								causes = new ArrayDeque<>(1);
							}
							causes.add(ex);
							continue;
						}
					}

					//mbd支持的构造函数解析模式,默认使用宽松模式:
					// 1. 严格模式如果摸棱两可的构造函数在转换参数时都匹配，则抛出异常
					// 2. 宽松模式将使用"最接近类型匹配"的构造函数
					// 如果bd支持的构造函数解析模式时宽松模式,引用获取类型差异权重值，否则引用获取Assignabliity权重值
					int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
							argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
					// Choose this factory method if it represents the closest match.
					// 如果它表示最接近的匹配项，则选择此工厂方法
					// 如果typeDiffWeight小于minTypeDiffWeight
					if (typeDiffWeight < minTypeDiffWeight) {
						factoryMethodToUse = candidate;
						argsHolderToUse = argsHolder;
						argsToUse = argsHolder.arguments;
						minTypeDiffWeight = typeDiffWeight;
						ambiguousFactoryMethods = null;
					}
					// Find out about ambiguity: In case of the same type difference weight
					// for methods with the same number of parameters, collect such candidates
					// and eventually raise an ambiguity exception.
					// However, only perform that check in non-lenient constructor resolution mode,
					// and explicitly ignore overridden methods (with the same parameter signature).
					// 找出歧义:如果具有相同数量参数的方法具有相同的类型差异权重，则收集此类候选想并最终引发歧义异常。
					// 但是，仅在非宽松构造函数解析模式下执行该检查，并显示忽略的方法（具有相同的参数签名）
					// 如果factoryMethodToUse不为null 且 typeDiffWeight与minTypeDiffWeight相等
					// 	且 mbd指定了严格模式解析构造函数 且 paramTypes的数组长度与factoryMethodToUse的参数数组长度相等 且
					// paramTypes的数组元素与factoryMethodToUse的参数数组元素不相等
					else if (factoryMethodToUse != null && typeDiffWeight == minTypeDiffWeight &&
							!mbd.isLenientConstructorResolution() &&
							paramTypes.length == factoryMethodToUse.getParameterCount() &&
							!Arrays.equals(paramTypes, factoryMethodToUse.getParameterTypes())) {
						if (ambiguousFactoryMethods == null) {
							ambiguousFactoryMethods = new LinkedHashSet<>();
							ambiguousFactoryMethods.add(factoryMethodToUse);
						}
						ambiguousFactoryMethods.add(candidate);
					}
				}
			}

			// 3.4 整合无法筛选出候选方法 或者 无法解析出要使用的参数值的情况，抛出BeanCreationException并加以描述
			// 如果factoryMethodToUse为null或者argsToUse为null
			if (factoryMethodToUse == null || argsToUse == null) {
				if (causes != null) {
					UnsatisfiedDependencyException ex = causes.removeLast();
					for (Exception cause : causes) {
						this.beanFactory.onSuppressedException(cause);
					}
					throw ex;
				}
				List<String> argTypes = new ArrayList<>(minNrOfArgs);
				if (explicitArgs != null) {
					for (Object arg : explicitArgs) {
						argTypes.add(arg != null ? arg.getClass().getSimpleName() : "null");
					}
				}
				else if (resolvedValues != null) {
					Set<ValueHolder> valueHolders = new LinkedHashSet<>(resolvedValues.getArgumentCount());
					valueHolders.addAll(resolvedValues.getIndexedArgumentValues().values());
					valueHolders.addAll(resolvedValues.getGenericArgumentValues());
					for (ValueHolder value : valueHolders) {
						String argType = (value.getType() != null ? ClassUtils.getShortName(value.getType()) :
								(value.getValue() != null ? value.getValue().getClass().getSimpleName() : "null"));
						argTypes.add(argType);
					}
				}
				String argDesc = StringUtils.collectionToCommaDelimitedString(argTypes);
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"No matching factory method found on class [" + factoryClass.getName() + "]: " +
						(mbd.getFactoryBeanName() != null ?
							"factory bean '" + mbd.getFactoryBeanName() + "'; " : "") +
						"factory method '" + mbd.getFactoryMethodName() + "(" + argDesc + ")'. " +
						"Check that a method with the specified name " +
						(minNrOfArgs > 0 ? "and arguments " : "") +
						"exists and that it is " +
						(isStatic ? "static" : "non-static") + ".");
			}
			else if (void.class == factoryMethodToUse.getReturnType()) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Invalid factory method '" + mbd.getFactoryMethodName() + "' on class [" +
						factoryClass.getName() + "]: needs to have a non-void return type!");
			}
			else if (ambiguousFactoryMethods != null) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Ambiguous factory method matches found on class [" + factoryClass.getName() + "] " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
						ambiguousFactoryMethods);
			}

			if (explicitArgs == null && argsHolderToUse != null) {
				mbd.factoryMethodToIntrospect = factoryMethodToUse;
				argsHolderToUse.storeCache(mbd, factoryMethodToUse);
			}
		}

		// 使用反射调用工厂方法，并储存在ThreadLocal中
		bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, factoryMethodToUse, argsToUse));
		return bw;
	}

	private Object instantiate(String beanName, RootBeanDefinition mbd,
			@Nullable Object factoryBean, Method factoryMethod, Object[] args) {

		try {
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedAction<Object>) () ->
						this.beanFactory.getInstantiationStrategy().instantiate(
								mbd, beanName, this.beanFactory, factoryBean, factoryMethod, args),
						this.beanFactory.getAccessControlContext());
			}
			else {
				return this.beanFactory.getInstantiationStrategy().instantiate(
						mbd, beanName, this.beanFactory, factoryBean, factoryMethod, args);
			}
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean instantiation via factory method failed", ex);
		}
	}

	/**
	 * Resolve the constructor arguments for this bean into the resolvedValues object.
	 * This may involve looking up other beans.
	 * <p>This method is also used for handling invocations of static factory methods.
	 * 将此bean的构造函数参数解析为resolvedValues对象。这可能涉及查找其他bean。
	 * <p>此方法也用于处理静态工厂方法的调用。
	 * --
	 * 将此 bean 的构造函数参数解析为resolvedValues 对象。这可能涉及查找其他 bean。
	 * 此方法还用于处理静态工厂方法的调用。
	 */
	/**
	 * <p>将cargs解析后值保存到resolveValues中，并返回解析后的最小(索引参数值数+泛型参数值数)</p>
	 * Resolve the constructor arguments for this bean into the resolvedValues object.
	 * This may involve looking up other beans.
	 * <p>将此Bean构造函数参数解析为resolveValues对象。这可能涉及查找其他Bean</p>
	 * <p>This method is also used for handling invocations of static factory methods.
	 * <p>此方法还用于处理静态工厂方法的调用</p>
	 * <ol>
	 *  <li>获取Bean工厂的类型转换器【变量 customConverter】</li>
	 *  <li>定义一个TypeConverter对象，如果有customConverter，就引用customConverter;否则引用bw【变量 converter】</li>
	 *  <li>新建一个BeanDefinitionValueResolver对象【变量 valueResolver】</li>
	 *  <li>获取cargs的参数值数量和泛型参数值数量作为 最小(索引参数值数+泛型参数值数)【变量 minNrOfArgs】</li>
	 *  <li>【<b>解析索引参数值</b>】遍历cargs所封装的索引参数值的Map，元素为entry(key=参数值的参数索引,value=
	 *  ConstructorArgumentValues.ValueHolder对象):
	 *   <ol>
	 *    <li>获取参数值的参数索引【变量 index】</li>
	 *    <li>如果index小于0,抛出Bean创建异常</li>
	 *    <li>如果index大于minNrOfArgs,minNrOfArgs就为index+1</li>
	 *    <li>获取ConstructorArgumentValues.ValueHolder对象【变量 valueHolder】</li>
	 *    <li>如果valueHolder已经包含转换后的值,将index和valueHolder添加到resolvedValues所封装的索引参数值的Map中</li>
	 *    <li>否则:
	 *     <ol>
	 *      <li>使用valueResolver解析出valueHolder实例的构造函数参数值所封装的对象【变量 resolvedValue】</li>
	 *      <li>使用valueHolder所封装的type,name属性以及解析出来的resovledValue构造出一个ConstructorArgumentValues.ValueHolder对象</li>
	 *      <li>将valueHolder作为resolvedValueHolder的配置源对象设置到resolvedValueHolder中</li>
	 *      <li>将index和valueHolder添加到resolvedValues所封装的索引参数值的Map中</li>
	 *     </ol>
	 *    </li>
	 *   </ol>
	 *  </li>
	 *  <li>【<b>解析泛型参数值</b>】遍历cargs的泛型参数值的列表,元素为ConstructorArgumentValues.ValueHolder对象【变量 valueHolder】:
	 *   <ol>
	 *    <li>如果valueHolder已经包含转换后的值,将index和valueHolder添加到resolvedValues的泛型参数值的列表中</li>
	 *    <li>否则:
	 *     <ol>
	 *       <li>使用valueResolver解析出valueHolder实例的构造函数参数值所封装的对象</li>
	 *       <li>将valueHolder作为resolvedValueHolder的配置源对象设置到resolvedValueHolder中</li>
	 *       <li>将index和valueHolder添加到resolvedValues所封装的索引参数值的Map中</li>
	 *     </ol>
	 *    </li>
	 *   </ol>
	 *  </li>
	 *  <li>将minNrOfArgs【最小(索引参数值数+泛型参数值数)】返回出去</li>
	 * </ol>
	 * @param beanName bean名
	 * @param mbd beanName对于的合并后RootBeanDefinition
	 * @param bw Bean实例的包装对象
	 * @param cargs mbd的构造函数参数值Holder
	 * @param resolvedValues 解析后的构造函数参数值Holder
	 * @return 最小(索引参数值数+泛型参数值数)
	 */
	private int resolveConstructorArguments(String beanName, RootBeanDefinition mbd, BeanWrapper bw,
			ConstructorArgumentValues cargs, ConstructorArgumentValues resolvedValues) {

		TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
		TypeConverter converter = (customConverter != null ? customConverter : bw);
		// BeanDefinitionValueResolver:在bean工厂实现中使用Helper类，它将beanDefinition对象中包含的值解析为应用于
		// 目标bean实例的实际值
		// 新建一个BeanDefinitionValueResolver对象
		BeanDefinitionValueResolver valueResolver =
				new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);

        // ConstructorArgumentValues.getArgumentCount():返回此实例中保存的参数值的数量，同时计算索引参数值和泛型参数值
		// 获取cargs的参数值数量和泛型参数值数量作为 最小(索引参数值数+泛型参数值数)
		int minNrOfArgs = cargs.getArgumentCount();

		for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : cargs.getIndexedArgumentValues().entrySet()) {
			int index = entry.getKey();
			if (index < 0) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Invalid constructor argument index: " + index);
			}
			if (index + 1 > minNrOfArgs) {
				minNrOfArgs = index + 1;
			}
			ConstructorArgumentValues.ValueHolder valueHolder = entry.getValue();
			// 如果valueHolder已经包含转换后的值
			if (valueHolder.isConverted()) {
				resolvedValues.addIndexedArgumentValue(index, valueHolder);
			}
			else {
				// 使用valueResolver解析出valueHolder实例的构造函数参数值所封装的对象
				Object resolvedValue =
						valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
				// 使用valueHolder所封装的type,name属性以及解析出来的resovledValue构造出一个ConstructorArgumentValues.ValueHolder对象
				ConstructorArgumentValues.ValueHolder resolvedValueHolder =
						new ConstructorArgumentValues.ValueHolder(resolvedValue, valueHolder.getType(), valueHolder.getName());
				// 将valueHolder作为resolvedValueHolder的配置源对象设置到resolvedValueHolder中
				resolvedValueHolder.setSource(valueHolder);
				resolvedValues.addIndexedArgumentValue(index, resolvedValueHolder);
			}
		}

		for (ConstructorArgumentValues.ValueHolder valueHolder : cargs.getGenericArgumentValues()) {
			if (valueHolder.isConverted()) {
				resolvedValues.addGenericArgumentValue(valueHolder);
			}
			else {
				Object resolvedValue =
						valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
				ConstructorArgumentValues.ValueHolder resolvedValueHolder = new ConstructorArgumentValues.ValueHolder(
						resolvedValue, valueHolder.getType(), valueHolder.getName());
				// 设置数校源为原来的
				resolvedValueHolder.setSource(valueHolder);
				resolvedValues.addGenericArgumentValue(resolvedValueHolder);
			}
		}

        // 返回 最小(索引参数值数+泛型参数值数)
		return minNrOfArgs;
	}

	/**
	 * Create an array of arguments to invoke a constructor or factory method,
	 * given the resolved constructor argument values.
	 * <p>给定已解析的构造函数参数值，创建一个参数数组以调用构造函数或工厂方法</p>
	 * <ol>
	 *  <li>获取bean工厂的自定义的TypeConverter【变量 customConverter】</li>
	 *  <li>如果customeConverter不为null,converter就引用customeConverter，否则引用bw【变量 converter】</li>
	 *  <li>根据paramTypes的数组长度构建一个ArgumentsHolder实例,用于保存解析后的参数值【变量 args】</li>
	 *  <li>定义一个用于存储构造函数参数值Holder，以查找下一个任意泛型参数值时，忽略该集合的元素的HashSet,初始化长度为paramTypes的数组长度
	 *  【变量 usedValueHolders】</li>
	 *  <li>定义一个用于存储自动注入Bean名的LinkedHashSet【变量 autowiredBeanNames】</li>
	 *  <li>fori形式遍历paramType,索引为paramIndex:
	 *   <ol>
	 *    <li>获取paramTypes中第paramIndex个参数类型【变量 paramType】</li>
	 *    <li>如果paramNames不为null，就引用第paramIndex个参数名否则引用空字符串【变量 paramName】</li>
	 *    <li>定义一个用于存储与paramIndex对应的ConstructorArgumentValues.ValueHolder实例【变量 valueHolder】</li>
	 *    <li>如果resolvedValues不为null:
	 *     <ol>
	 *      <li>在resolvedValues中查找与paramIndex对应的参数值，或者按paramType一般匹配【变量 valueHolder】</li>
	 *      <li>如果valueHolder为null 且 (mbd不支持使用构造函数进行自动注入 或者 paramTypes数组长度与resolvedValues的
	 *      (索引参数值+泛型参数值)数量相等)</li>
	 *     </ol>
	 *    </li>
	 *    <li>如果valueHolder不为null:
	 *     <ol>
	 *      <li>将valueHolder添加到usedValueHolders中，以表示该valueHolder已经使用过，下次在resolvedValues中
	 *      获取下一个valueHolder时，不要返回同一个对象</li>
	 *      <li>从valueHolder中获取原始参数值【变量 originalValue】</li>
	 *      <li>定义一个用于存储转换后的参数值的Object对象【变量 convertedValue】</li>
	 *      <li>如果valueHolder已经包含转换后的值:
	 *       <ol>
	 *        <li>从valueHolder中获取转换后的参数值【变量 convertedValue】</li>
	 *        <li>将convertedValue保存到args的preparedArguments数组的paramIndex对应元素中</li>
	 *       </ol>
	 *      </li>
	 *      <li>否则:
	 *       <ol>
	 *        <li>将executable中paramIndex对应的参数封装成MethodParameter对象【变量 methodParam】</li>
	 *        <li>使用converter将originalValue转换为paramType类型</li>
	 *        <li>捕捉在转换类型时出现的类型不匹配异常,重新抛出不满足的依赖异常</li>
	 *        <li>获取valueHolder的源对象，一般是ValueHolder【变量 sourceHolder】</li>
	 *        <li>如果sourceHolder是ConstructorArgumentValues.ValueHolder实例:
	 *         <ol>
	 *          <li>将soureHolder转换为ConstructorArgumentValues.ValueHolder对象【变量 sourceValue】</li>
	 *          <li>将args的resolveNecessary该为true，表示args.preparedArguments需要解析</li>
	 *          <li>将sourceValue保存到args的preparedArguments数组的paramIndex对应元素中</li>
	 *         </ol>
	 *        </li>
	 *       </ol>
	 *      </li>
	 *      <li>将convertedValue保存到args的arguments数组的paramIndex对应元素中</li>
	 *      <li>将originalValue保存到args的rawArguments数组的paramIndex对应元素中</li>
	 *     </ol>
	 *    </li>
	 *    <li>否则:
	 *     <ol>
	 *      <li>将executable中paramIndex对应的参数封装成MethodParameter对象【变量 methodParam】</li>
	 *      <li>mbd不支持适用构造函数进行自动注入,抛出不满足依赖异常</li>
	 *      <li>解析应该自动装配的methodParam的Bean对象,使用autowiredBeanNames保存所找到的所有候选Bean对象【变量 autowiredArgument】</li>
	 *      <li>将autowiredArgument保存到args的rawArguments数组的paramIndex对应元素中</li>
	 *      <li>将autowiredArgument保存到args的arguments数组的paramIndex对应元素中</li>
	 *      <li>将autowiredArgumentMarker保存到args的arguments数组的paramIndex对应元素中</li>
	 *      <li>将args的resolveNecessary该为true，表示args.preparedArguments需要解析</li>
	 *      <li>捕捉解析应该自动装配的methodParam的Bean对象时出现的BeanException,重新抛出满足依赖异常，引用mbd的资源描述作为异常信息</li>
	 *     </ol>
	 *    </li>
	 *    <li>遍历 autowiredBeanNames，元素为autowiredBeanName:
	 *     <ol>
	 *      <li>注册beanName与dependentBeanNamed的依赖关系到beanFactory中</li>
	 *      <li>如果当前日志级别时debug,打印debug日志</li>
	 *     </ol>
	 *    </li>
	 *    <li>将args(保存着解析后的参数值的ArgumentsHolder对象)返回出去</li>
	 *   </ol>
	 *  </li>
	 * </ol>
	 * @param beanName bean名
	 * @param mbd beanName对应的合并后RootBeanDefinition
	 * @param resolvedValues 已经解析过的构造函数参数值Holder对象
	 * @param bw bean实例包装类
	 * @param paramTypes 候选方法的参数类型数组
	 * @param paramNames  候选方法的参数名数组
	 * @param executable 候选方法
	 * @param autowiring mbd是否支持使用构造函数进行自动注入的标记
	 * @param fallback 是否可在抛出NoSuchBeanDefinitionException返回null，而不抛出异常
	 */
	private ArgumentsHolder createArgumentArray(
			String beanName, RootBeanDefinition mbd, @Nullable ConstructorArgumentValues resolvedValues,
			BeanWrapper bw, Class<?>[] paramTypes, @Nullable String[] paramNames, Executable executable,
			boolean autowiring, boolean fallback) throws UnsatisfiedDependencyException {

		TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
		//如果customConverter不为null,converter就引用customConverter，否则引用bw
		TypeConverter converter = (customConverter != null ? customConverter : bw);

		// 根据paramTypes的数组长度构建一个ArgumentsHolder实例，用于保存解析后的参数值
		ArgumentsHolder args = new ArgumentsHolder(paramTypes.length);
		// 定义一个用于存储构造函数参数值Holder，以查找下一个任意泛型参数值时，忽略该集合的元素的HashSet,初始化长度为paramTypes的数组长度
		Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
		// 定义一个用于存储自动注入Bean名的LinkedHashSet
		Set<String> allAutowiredBeanNames = new LinkedHashSet<>(paramTypes.length * 2);

		for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) {
			// 获取paramTypes中第paramIndex个参数类型
			Class<?> paramType = paramTypes[paramIndex];
			// 如果paramNames不为null，就引用第paramIndex个参数名否则引用空字符串
			String paramName = (paramNames != null ? paramNames[paramIndex] : "");
			// Try to find matching constructor argument value, either indexed or generic.
			// 尝试找到匹配的构造函数参数值，无论是索引的还是泛型的
			// 定义一个用于存储与paramIndex对应的ConstructorArgumentValues.ValueHolder实例
			ConstructorArgumentValues.ValueHolder valueHolder = null;
			if (resolvedValues != null) {
				// 在resolvedValues中查找与paramIndex对应的参数值，或者按paramType一般匹配
				valueHolder = resolvedValues.getArgumentValue(paramIndex, paramType, paramName, usedValueHolders);
				// If we couldn't find a direct match and are not supposed to autowire,
				// let's try the next generic, untyped argument value as fallback:
				// it could match after type conversion (for example, String -> int).
				// 如果找不到直接匹配并且不希望自动装配，请尝试使用一个通用的，无类型的参数值作为后备：
				// 类型转换后可以匹配(例如String -> int)
				// 如果valueHolder为null 且 (mbd不支持使用构造函数进行自动注入 或者 paramTypes数组长度与resolvedValues的
				// (索引参数值+泛型参数值)数量相等)
				if (valueHolder == null && (!autowiring || paramTypes.length == resolvedValues.getArgumentCount())) {
					// 在resovledValues中查找任意，不按名称匹配参数值的下一个泛型参数值，而忽略usedValueHolders参数值
					valueHolder = resolvedValues.getGenericArgumentValue(null, null, usedValueHolders);
				}
			}
			// 如果valueHolder不为null
			if (valueHolder != null) {
				// We found a potential match - let's give it a try.
				// Do not consider the same value definition multiple times!
				// 我们找到了可能的匹配-让我们尝试一些。
				// 不要考虑相同的值定义
				// 将valueHolder添加到usedValueHolders中，以表示该valueHolder已经使用过，下次在resolvedValues中
				// 获取下一个valueHolder时，不要返回同一个对象
				usedValueHolders.add(valueHolder);
				// 从valueHolder中获取原始参数值
				Object originalValue = valueHolder.getValue();
				// 定义一个用于存储转换后的参数值的Object对象
				Object convertedValue;
				if (valueHolder.isConverted()) {
					convertedValue = valueHolder.getConvertedValue();
					args.preparedArguments[paramIndex] = convertedValue;
				}
				else {
					// 将executable中paramIndex对应的参数封装成MethodParameter对象
					MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex);
					try {
						// 使用converter将originalValue转换为paramType类型
						convertedValue = converter.convertIfNecessary(originalValue, paramType, methodParam);
					}
					catch (TypeMismatchException ex) {
						throw new UnsatisfiedDependencyException(
								mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
								"Could not convert argument value of type [" +
								ObjectUtils.nullSafeClassName(valueHolder.getValue()) +
								"] to required type [" + paramType.getName() + "]: " + ex.getMessage());
					}
					Object sourceHolder = valueHolder.getSource();
					if (sourceHolder instanceof ConstructorArgumentValues.ValueHolder) {
						Object sourceValue = ((ConstructorArgumentValues.ValueHolder) sourceHolder).getValue();
						// 将args的resolveNecessary该为true，表示args.preparedArguments需要解析
						args.resolveNecessary = true;
						args.preparedArguments[paramIndex] = sourceValue;
					}
				}
				args.arguments[paramIndex] = convertedValue;
				args.rawArguments[paramIndex] = originalValue;
			}
			else {
				MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex);
				// No explicit match found: we're either supposed to autowire or
				// have to fail creating an argument array for the given constructor.
				// 找不到明确的匹配项:我们要么自动装配，要么必须为给定的构造函数创建参数数组而失败
				// mbd不支持适用构造函数进行自动注入
				if (!autowiring) {
					throw new UnsatisfiedDependencyException(
							mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
							"Ambiguous argument values for parameter of type [" + paramType.getName() +
							"] - did you specify the correct bean references as arguments?");
				}
				try {
					ConstructorDependencyDescriptor desc = new ConstructorDependencyDescriptor(methodParam, true);
					Set<String> autowiredBeanNames = new LinkedHashSet<>(2);
					// 解析应该自动装配的methodParam的Bean对象,使用autowiredBeanNames保存所找到的所有候选Bean对象
					Object arg = resolveAutowiredArgument(
							desc, paramType, beanName, autowiredBeanNames, converter, fallback);
					if (arg != null) {
						setShortcutIfPossible(desc, paramType, autowiredBeanNames);
					}
					allAutowiredBeanNames.addAll(autowiredBeanNames);
					args.rawArguments[paramIndex] = arg;
					args.arguments[paramIndex] = arg;
					args.preparedArguments[paramIndex] = desc;
					args.resolveNecessary = true;
				}
				catch (BeansException ex) {
					throw new UnsatisfiedDependencyException(
							mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam), ex);
				}
			}
		}

		// beanName 依赖 allAutowiredBeanNames
		registerDependentBeans(executable, beanName, allAutowiredBeanNames);

		return args;
	}

	/**
	 * Resolve the prepared arguments stored in the given bean definition.
	 * 解析存储在给定 Bean 定义中的准备好的参数。
	 *
	 * @param executable 构造函数或工厂方法
	 * @param argsToResolve 缓存准备解析的构造函数的参数。表示还没有进行依赖注入的形参
	 */
	/**
	 * <p>解析缓存在mbd中准备好的参数值:
	 *  <ol>
	 *   <li>获取bean工厂的自定义的TypeConverter【变量 customConverter】</li>
	 *   <li>如果customeConverter不为null,converter就引用customeConverter，否则引用bw</li>
	 *   <li>新建一个BeanDefinitionValue解析器对象【变量 valueResolver】</li>
	 *   <li>从executable中获取其参数类型数组【变量 paramTypes】</li>
	 *   <li>定义一个解析后的参数值数组,长度argsToResolve的长度【变量 resolvedArgs】</li>
	 *   <li>遍历argsToResolvefori形式)：
	 *    <ol>
	 *     <li>获取argsToResolver的第argIndex个参数值【变量 argValue】</li>
	 *     <li>为executable的argIndex位置参数创建一个新的MethodParameter对象【变量 methodParam】</li>
	 *     <li><b>如果agrValue是自动装配的参数标记【{@link #autowiredArgumentMarker}】</b>:,解析出应该自动装配的methodParam
	 *     的Bean对象【{@link #resolveAutowiredArgument(MethodParameter, String, Set, TypeConverter, boolean)}】</li>
	 *     <li><b>如果argValue是BeanMetadataElement对象</b>:交由valueResolver解析出value所封装的对象【{@link BeanDefinitionValueResolver#resolveValueIfNecessary(Object, Object)}】</li>
	 *     <li><b>如果argValue是String对象</b>:评估benaDefinition中包含的argValue,如果argValue是可解析表达式，会对其进行解析，否
	 *     则得到的还是argValue【{@link AbstractBeanFactory#evaluateBeanDefinitionString(String, BeanDefinition)}】</li>
	 *     <li>获取第argIndex个的参数类型【变量 paramType】</li>
	 *     <li>将argValue转换为paramType类型对象并赋值给第i个resolvedArgs元素</li>
	 *     <li>捕捉转换类型时抛出的类型不匹配异常,抛出不满意的依赖异常</li>
	 *    </ol>
	 *   </li>
	 *   <li>返回解析后的参数值数组【resolvedArgs】</li>
	 *  </ol>
	 * </p>
	 * Resolve the prepared arguments stored in the given bean definition.
	 * <p>解析缓存在给定bean定义中的准备好的参数</p>
	 * @param beanName bean名
	 * @param mbd beanName对于的合并后RootBeanDefinition
	 * @param bw bean的包装类，此时bw还没有拿到bean
	 * @param executable mbd已解析的构造函数或工厂方法对象
	 * @param argsToResolve mbd部分准备好的构造函数参数值
	 * @param fallback 是否可在抛出NoSuchBeanDefinitionException返回null，而不抛出异常
	 */
	private Object[] resolvePreparedArguments(String beanName, RootBeanDefinition mbd, BeanWrapper bw,
			Executable executable, Object[] argsToResolve) {

		TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
		// 使用自定义的TypeConverter，自定义的TypeConverter = null的话使用bw
		TypeConverter converter = (customConverter != null ? customConverter : bw);
		// 用来解析bean定义包含的值的
		BeanDefinitionValueResolver valueResolver =
				new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);
		// 方法参数class数组
		Class<?>[] paramTypes = executable.getParameterTypes();

		// 定义一个解析后的参数值数组,长度argsToResolve的长度
		Object[] resolvedArgs = new Object[argsToResolve.length];
		for (int argIndex = 0; argIndex < argsToResolve.length; argIndex++) {
			// 待解析的参数值
			Object argValue = argsToResolve[argIndex];
			// 参数类型
			Class<?> paramType = paramTypes[argIndex];
			boolean convertNecessary = false;
			// 根据argValue的不同情况解析不同的依赖，如果必要进行转换为paramType类型
			if (argValue instanceof ConstructorDependencyDescriptor) {
				ConstructorDependencyDescriptor descriptor = (ConstructorDependencyDescriptor) argValue;
				try {
					// 解析应该自动注入的指定参数
					argValue = resolveAutowiredArgument(descriptor, paramType, beanName,
							null, converter, true);
				}
				catch (BeansException ex) {
					// Unexpected target bean mismatch for cached argument -> re-resolve
					// 缓存参数的目标 bean 意外不匹配 -> 重新解析
					Set<String> autowiredBeanNames = null;
					if (descriptor.hasShortcut()) {
						// Reset shortcut and try to re-resolve it in this thread...
						// 重置快捷方式并尝试在此线程中重新解决它。
						descriptor.setShortcut(null);
						autowiredBeanNames = new LinkedHashSet<>(2);
					}
					logger.debug("Failed to resolve cached argument", ex);
					argValue = resolveAutowiredArgument(descriptor, paramType, beanName,
							autowiredBeanNames, converter, true);
					if (autowiredBeanNames != null && !descriptor.hasShortcut()) {
						// We encountered as stale shortcut before, and the shortcut has
						// not been re-resolved by another thread in the meantime...
						if (argValue != null) {
							setShortcutIfPossible(descriptor, paramType, autowiredBeanNames);
						}
						registerDependentBeans(executable, beanName, autowiredBeanNames);
					}
				}
			}
			else if (argValue instanceof BeanMetadataElement) {
				argValue = valueResolver.resolveValueIfNecessary("constructor argument", argValue);
				convertNecessary = true;
			}
			else if (argValue instanceof String) {
				argValue = this.beanFactory.evaluateBeanDefinitionString((String) argValue, mbd);
				convertNecessary = true;
			}
			if (convertNecessary) {
				MethodParameter methodParam = MethodParameter.forExecutable(executable, argIndex);
				try {
					// 将argValue转换为paramType
					argValue = converter.convertIfNecessary(argValue, paramType, methodParam);
				}
				catch (TypeMismatchException ex) {
					throw new UnsatisfiedDependencyException(
							mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
							"Could not convert argument value of type [" + ObjectUtils.nullSafeClassName(argValue) +
							"] to required type [" + paramType.getName() + "]: " + ex.getMessage());
				}
			}
			resolvedArgs[argIndex] = argValue;
		}
		return resolvedArgs;
	}

	private Constructor<?> getUserDeclaredConstructor(Constructor<?> constructor) {
		Class<?> declaringClass = constructor.getDeclaringClass();
		Class<?> userClass = ClassUtils.getUserClass(declaringClass);
		if (userClass != declaringClass) {
			try {
				return userClass.getDeclaredConstructor(constructor.getParameterTypes());
			}
			catch (NoSuchMethodException ex) {
				// No equivalent constructor on user class (superclass)...
				// Let's proceed with the given constructor as we usually would.
			}
		}
		return constructor;
	}

	/**
	 * Resolve the specified argument which is supposed to be autowired.
	 * 解析应该自动注入的指定参数。
	 *
	 * @param descriptor 参数依赖描述符
	 * @param paramType 参数类型
	 * @param beanName
	 */
	@Nullable
	Object resolveAutowiredArgument(DependencyDescriptor descriptor, Class<?> paramType, String beanName,
			@Nullable Set<String> autowiredBeanNames, TypeConverter typeConverter, boolean fallback) {

		// 如果参数类型 = InjectionPoint，直接从NamedThreadLocal获取
		if (InjectionPoint.class.isAssignableFrom(paramType)) {
			InjectionPoint injectionPoint = currentInjectionPoint.get();
			if (injectionPoint == null) {
				throw new IllegalStateException("No current InjectionPoint available for " + descriptor);
			}
			return injectionPoint;
		}

		try {
			return this.beanFactory.resolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter);
		}
		catch (NoUniqueBeanDefinitionException ex) {
			throw ex;
		}
		catch (NoSuchBeanDefinitionException ex) {
			if (fallback) {
				// Single constructor or factory method -> let's return an empty array/collection
				// for e.g. a vararg or a non-null List/Set/Map parameter.
				if (paramType.isArray()) {
					return Array.newInstance(paramType.getComponentType(), 0);
				}
				else if (CollectionFactory.isApproximableCollectionType(paramType)) {
					return CollectionFactory.createCollection(paramType, 0);
				}
				else if (CollectionFactory.isApproximableMapType(paramType)) {
					return CollectionFactory.createMap(paramType, 0);
				}
			}
			throw ex;
		}
	}

	private void setShortcutIfPossible(
			ConstructorDependencyDescriptor descriptor, Class<?> paramType, Set<String> autowiredBeanNames) {

		if (autowiredBeanNames.size() == 1) {
			String autowiredBeanName = autowiredBeanNames.iterator().next();
			if (this.beanFactory.containsBean(autowiredBeanName) &&
					this.beanFactory.isTypeMatch(autowiredBeanName, paramType)) {
				descriptor.setShortcut(autowiredBeanName);
			}
		}
	}

	private void registerDependentBeans(
			Executable executable, String beanName, Set<String> autowiredBeanNames) {

		for (String autowiredBeanName : autowiredBeanNames) {
			this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
			if (logger.isDebugEnabled()) {
				logger.debug("Autowiring by type from bean name '" + beanName + "' via " +
						(executable instanceof Constructor ? "constructor" : "factory method") +
						" to bean named '" + autowiredBeanName + "'");
			}
		}
	}

	static InjectionPoint setCurrentInjectionPoint(@Nullable InjectionPoint injectionPoint) {
		InjectionPoint old = currentInjectionPoint.get();
		if (injectionPoint != null) {
			currentInjectionPoint.set(injectionPoint);
		}
		else {
			currentInjectionPoint.remove();
		}
		return old;
	}


	/**
	 * Private inner class for holding argument combinations.
	 * 用于保存参数组合的私有内部类。
	 */
	private static class ArgumentsHolder {
		/**
		 * 原始参数值数组
		 */
		public final Object[] rawArguments;

		/**
		 * 经过转换后参数值数组
		 */
		public final Object[] arguments;
		/**
		 * 准备好的参数值数组，保存着 由解析的自动装配参数替换的标记和源参数值
		 */
		public final Object[] preparedArguments;
		/**
		 * 需要解析的标记，默认为false
		 */
		public boolean resolveNecessary = false;

		public ArgumentsHolder(int size) {
			this.rawArguments = new Object[size];
			this.arguments = new Object[size];
			this.preparedArguments = new Object[size];
		}

		public ArgumentsHolder(Object[] args) {
			this.rawArguments = args;
			this.arguments = args;
			this.preparedArguments = args;
		}

		public int getTypeDifferenceWeight(Class<?>[] paramTypes) {
			// If valid arguments found, determine type difference weight.
			// Try type difference weight on both the converted arguments and
			// the raw arguments. If the raw weight is better, use it.
			// Decrease raw weight by 1024 to prefer it over equal converted weight.
			int typeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.arguments);
			int rawTypeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.rawArguments) - 1024;
			return Math.min(rawTypeDiffWeight, typeDiffWeight);
		}

		public int getAssignabilityWeight(Class<?>[] paramTypes) {
			for (int i = 0; i < paramTypes.length; i++) {
				if (!ClassUtils.isAssignableValue(paramTypes[i], this.arguments[i])) {
					return Integer.MAX_VALUE;
				}
			}
			for (int i = 0; i < paramTypes.length; i++) {
				if (!ClassUtils.isAssignableValue(paramTypes[i], this.rawArguments[i])) {
					return Integer.MAX_VALUE - 512;
				}
			}
			return Integer.MAX_VALUE - 1024;
		}

		public void storeCache(RootBeanDefinition mbd, Executable constructorOrFactoryMethod) {
			synchronized (mbd.constructorArgumentLock) {
				mbd.resolvedConstructorOrFactoryMethod = constructorOrFactoryMethod;
				mbd.constructorArgumentsResolved = true;
				if (this.resolveNecessary) {
					mbd.preparedConstructorArguments = this.preparedArguments;
				}
				else {
					mbd.resolvedConstructorArguments = this.arguments;
				}
			}
		}
	}


	/**
	 * Delegate for checking Java's {@link ConstructorProperties} annotation.
	 */
	private static class ConstructorPropertiesChecker {

		@Nullable
		public static String[] evaluate(Constructor<?> candidate, int paramCount) {
			ConstructorProperties cp = candidate.getAnnotation(ConstructorProperties.class);
			if (cp != null) {
				String[] names = cp.value();
				if (names.length != paramCount) {
					throw new IllegalStateException("Constructor annotated with @ConstructorProperties but not " +
							"corresponding to actual number of parameters (" + paramCount + "): " + candidate);
				}
				return names;
			}
			else {
				return null;
			}
		}
	}


	/**
	 * DependencyDescriptor marker for constructor arguments,
	 * for differentiating between a provided DependencyDescriptor instance
	 * and an internally built DependencyDescriptor for autowiring purposes.
	 *
	 * 构造函数参数的 DependencyDescriptor 标记，用于区分提供的 DependencyDescriptor 实例和
	 * 内部构建的 DependencyDescriptor 以进行自动注入。
	 */
	@SuppressWarnings("serial")
	private static class ConstructorDependencyDescriptor extends DependencyDescriptor {
		/**
		 * 自动注入的bean名称
		 */
		@Nullable
		private volatile String shortcut;

		public ConstructorDependencyDescriptor(MethodParameter methodParameter, boolean required) {
			super(methodParameter, required);
		}

		public void setShortcut(@Nullable String shortcut) {
			this.shortcut = shortcut;
		}

		public boolean hasShortcut() {
			return (this.shortcut != null);
		}

		@Override
		public Object resolveShortcut(BeanFactory beanFactory) {
			String shortcut = this.shortcut;
			return (shortcut != null ? beanFactory.getBean(shortcut, getDependencyType()) : null);
		}
	}

}
