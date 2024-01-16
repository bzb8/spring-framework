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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Provider;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.core.OrderComparator;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.log.LogMessage;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.CompositeIterator;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Spring's default implementation of the {@link ConfigurableListableBeanFactory}
 * and {@link BeanDefinitionRegistry} interfaces: a full-fledged bean factory
 * based on bean definition metadata, extensible through post-processors.
 *
 * Spring 的 {@link ConfigurableListableBeanFactory} 和 {@link BeanDefinitionRegistry} 接口的默认实现：
 * 一个基于 Bean 定义元数据的成熟 Bean 工厂，可通过后处理器进行扩展。
 *
 * <p>Typical usage is registering all bean definitions first (possibly read
 * from a bean definition file), before accessing beans. Bean lookup by name
 * is therefore an inexpensive operation in a local bean definition table,
 * operating on pre-resolved bean definition metadata objects.
 * 典型的用法是首先注册所有的Bean定义（可能是从Bean定义文件中读取），然后再访问Bean。
 * 因此，在本地的Bean定义表中按名称查找Bean是一种低成本的操作，它操作的是预先解析的Bean定义元数据对象
 *
 * <p>Note that readers for specific bean definition formats are typically
 * implemented separately rather than as bean factory subclasses: see for example
 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
 * 请注意，针对特定Bean定义格式的读取器通常是单独实现的，而不是作为Bean工厂子类实现的：
 * 例如，可以参考{@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}。
 *
 * <p>For an alternative implementation of the
 * {@link org.springframework.beans.factory.ListableBeanFactory} interface,
 * have a look at {@link StaticListableBeanFactory}, which manages existing
 * bean instances rather than creating new ones based on bean definitions.
 * 对于{@link org.springframework.beans.factory.ListableBeanFactory}接口的另一种实现，请查看{@link StaticListableBeanFactory}，
 * 它管理现有的Bean实例，而不是基于Bean定义创建新的实例。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see #registerBeanDefinition
 * @see #addBeanPostProcessor
 * @see #getBean
 * @see #resolveDependency
 * @since 16 April 2001
 */
@SuppressWarnings("serial")
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {

	/**
	 * javax.inject.Provider class 对象
	 */
	@Nullable
	private static Class<?> javaxInjectProviderClass;

	static {
		try {
			// 使用当前类的类加载器加载javax.inject.Provider的class对象
			javaxInjectProviderClass =
					ClassUtils.forName("javax.inject.Provider", DefaultListableBeanFactory.class.getClassLoader());
		} catch (ClassNotFoundException ex) {
			// JSR-330 API not available - Provider interface simply not supported then.
			javaxInjectProviderClass = null;
		}
	}


	/**
	 * Map from serialized id to factory instance.
	 * 反序列化的ID -> 工厂实例
	 */
	private static final Map<String, Reference<DefaultListableBeanFactory>> serializableFactories =
			new ConcurrentHashMap<>(8);

	/**
	 * Optional id for this factory, for serialization purposes.
	 * 此工厂的可选 ID，用于序列化目的。
	 */
	@Nullable
	private String serializationId;

	/**
	 * Whether to allow re-registration of a different definition with the same name.
	 * 是否允许重新注册具有相同名称的不同定义
	 */
	private boolean allowBeanDefinitionOverriding = true;

	/**
	 * Whether to allow eager class loading even for lazy-init beans.
	 * 是否允许预先加载类，即使对于惰性初始化 Bean 也是如此
	 */
	private boolean allowEagerClassLoading = true;

	/**
	 * Optional OrderComparator for dependency Lists and arrays.
	 * 依赖项列表和数组的可选 OrderComparator。
	 */
	@Nullable
	private Comparator<Object> dependencyComparator;

	/**
	 * Resolver to use for checking if a bean definition is an autowire candidate.
	 * 用于检查 bean 定义是否为自动装配候选者的解析器
	 * ContextAnnotationAutowireCandidateResolver
	 */
	private AutowireCandidateResolver autowireCandidateResolver = SimpleAutowireCandidateResolver.INSTANCE;

	/**
	 * Map from dependency type to corresponding autowired value.
	 * 依赖项类型 -> 相应的自动装配值
	 * 存放着手动显式注册的依赖项类型 -> 相应的自动装配值的缓存
	 * 手动显示注册指直接调用{@link #registerResolvableDependency(Class, Object)}
	 */
	private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

	/**
	 * Map of bean definition objects, keyed by bean name.
	 * BeanName -> BeanDefinition
	 */
	private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

	/**
	 * Map from bean name to merged BeanDefinitionHolder.
	 * bean name -> merged BeanDefinitionHolder
	 */
	private final Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders = new ConcurrentHashMap<>(256);

	/**
	 * Map of singleton and non-singleton bean names, keyed by dependency type.
	 * 单例和非单例Bean名称的映射，按依赖项类型进行键控
	 * Class -> BeanName数组
	 */
	private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

	/**
	 * Map of singleton-only bean names, keyed by dependency type.
	 * 仅依赖单例的bean名称的映射，按依赖项类型进行键控
	 * Class -> BeanName数组
	 */
	private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

	/**
	 * List of bean definition names, in registration order.
	 * Bean 定义名称列表（按注册顺序排列）
	 */
	private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

	/**
	 * List of names of manually registered singletons, in registration order.
	 * 手动注册的单例名称列表，按注册顺序排序。
	 */
	//
	private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

	/**
	 * Cached array of bean definition names in case of frozen configuration.
	 * 在冻结配置的情况下缓存 bean 定义名称数组。
	 * 缓存的BeanDefinition名称数组，以防配置被冻结
	 */
	@Nullable
	private volatile String[] frozenBeanDefinitionNames;

	/**
	 * Whether bean definition metadata may be cached for all beans.
	 * 是否可以为所有 bean 缓存 bean 定义元数据。
	 * 冻结配置的标记
	 */
	private volatile boolean configurationFrozen;


	/**
	 * Create a new DefaultListableBeanFactory.
	 * 指定默认的初始化策略，instantiationStrategy = CglibSubclassingInstantiationStrategy
	 */
	public DefaultListableBeanFactory() {
		super();
	}

	/**
	 * Create a new DefaultListableBeanFactory with the given parent.
	 * 使用给定的父级Bean工厂创建一个新的DefaultListableBeanFactory
	 *
	 * @param parentBeanFactory the parent BeanFactory
	 */
	public DefaultListableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}


	/**
	 * Specify an id for serialization purposes, allowing this BeanFactory to be
	 * deserialized from this id back into the BeanFactory object, if needed.
	 * 指定一个ID以进行序列化，如果需要的话，允许该BeanFactory从该ID反序列化为BeanFactory对象
	 * 更新并保存序列化ID与当前工厂实例的映射关系
	 */
	public void setSerializationId(@Nullable String serializationId) {
		if (serializationId != null) {
			// 使用弱类型引用包装当前Bean工厂，再将映射关系添加到serializableFactories中
			serializableFactories.put(serializationId, new WeakReference<>(this));
		} else if (this.serializationId != null) {
			// 如果当前bean工厂的序列化ID为null
			// 从serializableFactories中移除该serializationId对应的映射关系
			serializableFactories.remove(this.serializationId);
		}
		this.serializationId = serializationId;
	}

	/**
	 * Return an id for serialization purposes, if specified, allowing this BeanFactory
	 * to be deserialized from this id back into the BeanFactory object, if needed.
	 * 返回一个用于序列化的 id（如果指定），如果需要，允许将此 BeanFactory 从此 ID 反序列化回 BeanFactory 对象。
	 *
	 * @since 4.1.2
	 */
	@Nullable
	public String getSerializationId() {
		return this.serializationId;
	}

	/**
	 * Set whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * If not, an exception will be thrown. This also applies to overriding aliases.
	 * <p>Default is "true".
	 * 设置是否允许通过使用相同名称注册不同的定义来覆盖bean定义，自动替换原来的定义。如果不允许，则会抛出异常。这也适用于覆盖别名。
	 * 默认值为"true"。
	 *
	 * @see #registerBeanDefinition
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * Return whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * 返回是否允许通过注册具有相同名称的不同定义来覆盖 bean 定义，自动替换前者。
	 *
	 * @since 4.1.2
	 */
	public boolean isAllowBeanDefinitionOverriding() {
		return this.allowBeanDefinitionOverriding;
	}

	/**
	 * Set whether the factory is allowed to eagerly load bean classes
	 * even for bean definitions that are marked as "lazy-init".
	 * <p>Default is "true". Turn this flag off to suppress class loading
	 * for lazy-init beans unless such a bean is explicitly requested.
	 * In particular, by-type lookups will then simply ignore bean definitions
	 * without resolved class name, instead of loading the bean classes on
	 * demand just to perform a type check.
	 * 设置工厂是否允许对标记为"lazy-init"的bean定义进行急切加载。
	 * 默认值为"true"。关闭此标志以在显式请求该bean之前，抑制对延迟初始化bean的类加载。
	 * 特别是，按类型查找将简单地忽略没有已解析类名的bean定义，而不是根据需要加载bean类来执行类型检查。
	 *
	 * @see AbstractBeanDefinition#setLazyInit
	 */
	public void setAllowEagerClassLoading(boolean allowEagerClassLoading) {
		this.allowEagerClassLoading = allowEagerClassLoading;
	}

	/**
	 * Return whether the factory is allowed to eagerly load bean classes
	 * even for bean definitions that are marked as "lazy-init".
	 * <p>
	 * 返回是否允许工厂急切地加载 Bean 类，即使对于标记为“lazy-init”的 Bean 定义也是如此。
	 *
	 * @since 4.1.2
	 */
	public boolean isAllowEagerClassLoading() {
		return this.allowEagerClassLoading;
	}

	/**
	 * Set a {@link java.util.Comparator} for dependency Lists and arrays.
	 *
	 * @see org.springframework.core.OrderComparator
	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
	 * @since 4.0
	 */
	public void setDependencyComparator(@Nullable Comparator<Object> dependencyComparator) {
		this.dependencyComparator = dependencyComparator;
	}

	/**
	 * Return the dependency comparator for this BeanFactory (may be {@code null}).
	 * 返回此 BeanFactory 的依赖项比较器（可能是 {@code null}）。
	 *
	 * @since 4.0
	 */
	@Nullable
	public Comparator<Object> getDependencyComparator() {
		return this.dependencyComparator;
	}

	/**
	 * Set a custom autowire candidate resolver for this BeanFactory to use
	 * when deciding whether a bean definition should be considered as a
	 * candidate for autowiring.
	 * 为了在决定是否将bean定义视为自动装配候选时，为该BeanFactory设置一个自定义的自动装配候选解析器。
	 */
	public void setAutowireCandidateResolver(AutowireCandidateResolver autowireCandidateResolver) {
		Assert.notNull(autowireCandidateResolver, "AutowireCandidateResolver must not be null");
		if (autowireCandidateResolver instanceof BeanFactoryAware) {
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(this);
					return null;
				}, getAccessControlContext());
			} else {
				((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(this);
			}
		}
		this.autowireCandidateResolver = autowireCandidateResolver;
	}

	/**
	 * Return the autowire candidate resolver for this BeanFactory (never {@code null}).
	 *
	 * 返回此 BeanFactory 的自动装配候选解析器（绝不是 {@code null}）。
	 * ContextAnnotationAutowireCandidateResolver
	 */
	public AutowireCandidateResolver getAutowireCandidateResolver() {
		return this.autowireCandidateResolver;
	}


	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		super.copyConfigurationFrom(otherFactory);
		if (otherFactory instanceof DefaultListableBeanFactory) {
			DefaultListableBeanFactory otherListableFactory = (DefaultListableBeanFactory) otherFactory;
			this.allowBeanDefinitionOverriding = otherListableFactory.allowBeanDefinitionOverriding;
			this.allowEagerClassLoading = otherListableFactory.allowEagerClassLoading;
			this.dependencyComparator = otherListableFactory.dependencyComparator;
			// A clone of the AutowireCandidateResolver since it is potentially BeanFactoryAware
			setAutowireCandidateResolver(otherListableFactory.getAutowireCandidateResolver().cloneIfNecessary());
			// Make resolvable dependencies (e.g. ResourceLoader) available here as well
			this.resolvableDependencies.putAll(otherListableFactory.resolvableDependencies);
		}
	}


	//---------------------------------------------------------------------
	// Implementation of remaining BeanFactory methods
	//---------------------------------------------------------------------

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBean(requiredType, (Object[]) null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
		Assert.notNull(requiredType, "Required type must not be null");
		Object resolved = resolveBean(ResolvableType.forRawClass(requiredType), args, false);
		if (resolved == null) {
			throw new NoSuchBeanDefinitionException(requiredType);
		}
		return (T) resolved;
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		Assert.notNull(requiredType, "Required type must not be null");
		return getBeanProvider(ResolvableType.forRawClass(requiredType), true);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		return getBeanProvider(requiredType, true);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return this.beanDefinitionMap.containsKey(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.beanDefinitionMap.size();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		String[] frozenNames = this.frozenBeanDefinitionNames;
		if (frozenNames != null) {
			return frozenNames.clone();
		} else {
			return StringUtils.toStringArray(this.beanDefinitionNames);
		}
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		Assert.notNull(requiredType, "Required type must not be null");
		return getBeanProvider(ResolvableType.forRawClass(requiredType), allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		return new BeanObjectProvider<T>() {
			@Override
			public T getObject() throws BeansException {
				T resolved = resolveBean(requiredType, null, false);
				if (resolved == null) {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
				return resolved;
			}

			@Override
			public T getObject(Object... args) throws BeansException {
				T resolved = resolveBean(requiredType, args, false);
				if (resolved == null) {
					throw new NoSuchBeanDefinitionException(requiredType);
				}
				return resolved;
			}

			@Override
			@Nullable
			public T getIfAvailable() throws BeansException {
				try {
					return resolveBean(requiredType, null, false);
				} catch (ScopeNotActiveException ex) {
					// Ignore resolved bean in non-active scope
					return null;
				}
			}

			@Override
			public void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
				T dependency = getIfAvailable();
				if (dependency != null) {
					try {
						dependencyConsumer.accept(dependency);
					} catch (ScopeNotActiveException ex) {
						// Ignore resolved bean in non-active scope, even on scoped proxy invocation
					}
				}
			}

			@Override
			@Nullable
			public T getIfUnique() throws BeansException {
				try {
					return resolveBean(requiredType, null, true);
				} catch (ScopeNotActiveException ex) {
					// Ignore resolved bean in non-active scope
					return null;
				}
			}

			@Override
			public void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
				T dependency = getIfUnique();
				if (dependency != null) {
					try {
						dependencyConsumer.accept(dependency);
					} catch (ScopeNotActiveException ex) {
						// Ignore resolved bean in non-active scope, even on scoped proxy invocation
					}
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			public Stream<T> stream() {
				return Arrays.stream(getBeanNamesForTypedStream(requiredType, allowEagerInit))
						.map(name -> (T) getBean(name))
						.filter(bean -> !(bean instanceof NullBean));
			}

			@SuppressWarnings("unchecked")
			@Override
			public Stream<T> orderedStream() {
				String[] beanNames = getBeanNamesForTypedStream(requiredType, allowEagerInit);
				if (beanNames.length == 0) {
					return Stream.empty();
				}
				Map<String, T> matchingBeans = CollectionUtils.newLinkedHashMap(beanNames.length);
				for (String beanName : beanNames) {
					Object beanInstance = getBean(beanName);
					if (!(beanInstance instanceof NullBean)) {
						matchingBeans.put(beanName, (T) beanInstance);
					}
				}
				Stream<T> stream = matchingBeans.values().stream();
				return stream.sorted(adaptOrderComparator(matchingBeans));
			}
		};
	}

	@Nullable
	private <T> T resolveBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {
		NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
		if (namedBean != null) {
			return namedBean.getBeanInstance();
		}
		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof DefaultListableBeanFactory) {
			return ((DefaultListableBeanFactory) parent).resolveBean(requiredType, args, nonUniqueAsNull);
		} else if (parent != null) {
			ObjectProvider<T> parentProvider = parent.getBeanProvider(requiredType);
			if (args != null) {
				return parentProvider.getObject(args);
			} else {
				return (nonUniqueAsNull ? parentProvider.getIfUnique() : parentProvider.getIfAvailable());
			}
		}
		return null;
	}

	private String[] getBeanNamesForTypedStream(ResolvableType requiredType, boolean allowEagerInit) {
		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, requiredType, true, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		return getBeanNamesForType(type, true, true);
	}

	/**
	 * 获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的名称:
	 * <ol>
	 *  <li>从type中解析出Class对象【变量 resolved 】</li>
	 *  <li>如果resolved不为null 且 没有包含泛型参数,调用 {@link #getBeanNamesForType(Class, boolean, boolean)} 来
	 *  获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的名称并返回出去</li>
	 *  <li>否则,调用 {@link #doGetBeanNamesForType(ResolvableType, boolean, boolean)} 来
	 *  获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的名称并返回出去</li>
	 * </ol>
	 * @param type the generically typed class or interface to match -- 要匹配的通用类型的类或接口
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * -- 是否也包括原型或作用域bean或仅包含单例（也适用于FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * -- 是否初始化 lazy-init单例和由FactoryBeans创建的对象(或通过带有'factory-bean'引用的
	 * 工厂创建方法）创建类型检查。注意，需要饿汉式初始化FactoryBeans以确定他们的类型：因此
	 * 请注意，为此标记传递'true'将初始化FactoryBean和'factory-bean'引用
	 * @return 匹配给定对象类型（包含子类）的bean（或由FactoryBean创建的对象）的名称；如果没有，则 返回一个空数组。
	 */
	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		// 从type中解析出Class对象
		Class<?> resolved = type.resolve();
		// 如果resolved不为null 且 没有包含泛型参数
		if (resolved != null && !type.hasGenerics()) {
			return getBeanNamesForType(resolved, includeNonSingletons, allowEagerInit);
		} else {
			return doGetBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		}
		// getBeanNamesForType方法虽然也会调用doGetBeanNamesForType去解析有泛型的情况，但是getBeanNamesForType
		// 优先读取缓存，在能确定其匹配类型的情况下(即没有泛型)的情况下，getBeanNamesForType方法的性能优于
		// doGetBeanNamesForType方法。
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {

		return getBeanNamesForType(type, true, true);
	}
	/**
	 * 获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的名称
	 * <ol>
	 *   <li>如果该工厂的配置冻结了 或者 要匹配的类型或接口为null 或者 不允许初始化FactoryBean和'factory-bean'引用,
	 *    就调用 doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit)来
	 *    获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的名称,然后返回出去
	 *   </li>
	 *   <li>定义一个Class-String[]的Map缓存，如果包含是否包含非单例，就引用单例和非单例Bean名称的映射Map【{@link #allBeanNamesByType}】；否则
	 *   引用仅依赖单例的bean名称的映射Map【{@link #singletonBeanNamesByType}】</li>
	 *   <li>从缓存中获取type对应的BeanName数组 【变量 resolvedBeanNames】,如果获取成功就返回出去</li>
	 *   <li>调用doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit)来
	 *   获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的名称,然后返回出去
	 *   </li>
	 *   <li>获取工厂的类加载器，如果该类加载器加载过type就将type与属于type的Bean类的所有Bean名称的映射关系添加到cache中</li>
	 *   <li>返回属于type的Bean类的所有Bean名称</li>
	 * </ol>
	 * @param type 要匹配的通用类型的类或接口
	 * @param includeNonSingletons 是否包含非单例
	 * @param allowEagerInit 是否初始化FactoryBean和'factory-bean'引用
	 * @return 匹配给定对象类型（包含子类）的bean（或由FactoryBean创建的对象）的名称；如果没有，则 返回一个空数组。
	 * @see #doGetBeanNamesForType(ResolvableType, boolean, boolean)
	 */
	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		// 如果该工厂的配置没有冻结了 或者 要匹配的类型或接口为null 或者 不允许初始化FactoryBean和'factory-bean'引用
		if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
			// 获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的名称
			return doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
		}

		// 定义一个Class-String[]的Map缓存，如果包含是否包含非单例，就引用单例和非单例Bean名称的映射Map；
		// 否则 引用仅依赖单例的bean名称的映射Map
		Map<Class<?>, String[]> cache =
				(includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
		String[] resolvedBeanNames = cache.get(type);
		if (resolvedBeanNames != null) {
			return resolvedBeanNames;
		}
		// 获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的名称
		resolvedBeanNames = doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
		// 获取工厂的类加载器，如果该类加载器加载过type
		if (ClassUtils.isCacheSafe(type, getBeanClassLoader())) {
			cache.put(type, resolvedBeanNames);
		}
		// 返回属于type的Bean类的所有Bean名称
		return resolvedBeanNames;
	}

	/**
	 * 获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的名称
	 * <ol>
	 *   <li>定义一个用于存储的匹配的bean名的ArrayList集合 【变量 result】</li>
	 *   <li><b>先从工厂收集到的所有BeanDefinition中查找：</b>
	 *    <ol>
	 *      <li>遍历工厂的BeanDefinition缓存集合【beanDefinitionNames】,元素为 beanName：
	 *       <ol>
	 *         <li>如果beanName不是别名:
	 *           <ol>
	 *             <li>获取beanName对应的合并后BootDefinition对象 【变量 mbd】</li>
	 *             <li>【这里的判断很多，主要就是判断mbd是否完整】如果mbd不是抽象类 且 (允许早期初始化 或者 mbd有指定bean类 或者  mbd没有设置延迟初始化
	 *             或者工厂配置了允许急切加载Bean类，即使对于标记为'lazy-init'的bean定义
	 *             也是如此【allowEagerClassLoading】) 且 mdb配置的FactoryBean名不需要急切初始化以确定其类型 ：
	 *				 <ol>
	 *				   <li>定义一个是否是FactoryBean类型标记【变量 isFactoryBean】：判断beanName和mbd所指的bean是否已定义为FactoryBean</li>
	 *				   <li>获取mbd的BeanDefinitionHolder对象 【变量 dbd】</li>
	 *				   <li>初始化匹配已找到标记为未找到【false,变量matchFound】</li>
	 *				   <li>定义允许FactoryBean初始化标记【变量 allowFactoryBeanInit】：只要允许饿汉式初始化 或者 beanName已经在单例对象的高速缓存Map集合
	 *				   【singletonObjects】有所属对象</li>
	 *				   <li>定义是非延时装饰标记【变量 isNonLazyDecorated】：dbd获取成功 且 mbd没有设置延时初始化</li>
	 *				   <li>如果不是FactoryBean类型，且(包含非单例 或者 beanName, mbd, dbd所指向的bean是单例),就将beanName的Bean类型是
	 *				   否与type匹配的结果赋值给matchFound</li>
	 *				   <li>如果是FactoryBean类型:
	 *				    <ol>
	 *				      <li>如果包含非单例 或者 mdb没有配置延时 或者 (允许FactoryBean初始化 且  beanName, mbd, dbd所指向的bean是单例),
	 *				      就将beanName的Bean类型是否与type匹配的结果赋值给matchFound</li>
	 *				      <li>如果不匹配， 将beanName改成解引用名，再来一次匹配：将beanName的Bean类型是否与type匹配的结果赋值给matchFound
	 *				      (此时的beanName是解引用名)</li>
	 *				    </ol>
	 *				   </li>
	 *				   <li>如果匹配成功,将beanName添加到result中</li>
	 *				 </ol>
	 *             </li>
	 *             <li>捕捉BeanFactory无法加载给定bean的指定类时引发的异常 和 当BeanFactory遇到无效的bean定义时引发的异常:
	 *              <ol>
	 *                <li>如果是允许马上初始化，重新抛出异常</li>
	 *                <li>构建日志消息对象，并打印跟踪日志：如果ex是CannotLoadBeanClassException，描述忽略Bean 'beanName'的Bean类加载失败；
	 *                否则描述忽略bean定义'beanName'中无法解析的元数据</li>
	 *                <li>将要注册的异常对象添加到 抑制异常列表【DefaultSingletonBeanRegistry#suppressedExceptions】中</li>
	 *              </ol>
	 *             </li>
	 *           </ol>
	 *         </li>
	 *       </ol>
	 *      </li>
	 *    </ol>
	 *   </li>
	 *   <li><b>从工厂收集到的手动注册的单例对象中查找：</b>
	 *    <ol>
	 *      <li>遍历 手动注册单例的名称列表【manualSingletonNames】：
	 *       <ol>
	 *         <li>如果beanName所指的对象属于FactoryBean实例：
	 *          <ol>
	 *           <li>如果(包含非单例 或者 beanName所指的对象是单例) 且 beanName对应的Bean类型与type匹配,就将beanName
	 *           添加到result中，然后 continue</li>
	 *           <li>否则，将beanName改成FactoryBean解引用名</li>
	 *          </ol>
	 *         </li>
	 *         <li>如果beanName对应的Bean类型与type匹配（此时的beanName有可能是FactoryBeany解引用名），就将beanName添加
	 *         到result中</li>
	 *         <li>捕捉没有此类bean定义异常,打印跟踪消息：无法检查名称为'beanName'的手动注册的单例</li>
	 *       </ol>
	 *      </li>
	 *    </ol>
	 *   </li>
	 *   <li>将result转换成Stirng数组返回出去</li>
	 * </ol>
	 * @param type 要匹配的泛型类型的类或接口
	 * @param includeNonSingletons 是否包含非单例
	 * @param allowEagerInit 是否初始化FactoryBean和'factory-bean'引用
	 */
	private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		// 定义一个用于存储的匹配的bean名的ArrayList集合
		List<String> result = new ArrayList<>();

		// Check all bean definitions.
		// 检查所有 Bean 定义。
		for (String beanName : this.beanDefinitionNames) {
			// Only consider bean as eligible if the bean name is not defined as alias for some other bean.
			// 如果未将bean名称定义为其他bean的别名，则将bean视为可选。
			// 如果beanName不是别名
			if (!isAlias(beanName)) {
				try {
					// 获取beanName对应的合并后BootDefinition对象
					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
					// Only check bean definition if it is complete.
					// 仅检查 bean 定义是否完整。
					// 如果mbd不是抽象
					// 且 (允许饿汉式初始化
					// 或者 mbd指定了bean类 或者 mbd没有设置延迟初始化
					// 或者 允许急切加载Bean类，即使对于标记为'lazy-init'的bean定义)
					// 且 mdb配置的FactoryBean名不需要急切初始化以确定其类型
					if (!mbd.isAbstract() && (allowEagerInit ||
							(mbd.hasBeanClass() || !mbd.isLazyInit() || isAllowEagerClassLoading()) &&
									!requiresEagerInitForType(mbd.getFactoryBeanName()))) {
						// 是否是FactoryBean类型标记：判断beanName和mbd所指的bean是否已定义为FactoryBean
						boolean isFactoryBean = isFactoryBean(beanName, mbd);
						// 获取mbd修饰的目标定义，
						// BeanDefinitionHolder:具有名称和别名的BeanDefinition的持有人
						BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
						// 初始化匹配已找到标记为未找到
						boolean matchFound = false;
						// 允许FactoryBean初始化标记：只要参数允许饿汉式初始化 或者 beanName已经在单例对象的高速缓存Map集合有所属对象
						boolean allowFactoryBeanInit = (allowEagerInit || containsSingleton(beanName));
						// 是否延时装饰标记：mdb有配置BeanDefinitionHolder 且 mbd没有设置延时初始化
						boolean isNonLazyDecorated = (dbd != null && !mbd.isLazyInit());
						// 如果不是FactoryBean类型
						if (!isFactoryBean) {
							// 如果包含非单例 或者 beanName, mbd, dbd所指向的bean是单例
							if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
								// 将beanName的Bean类型是否与type匹配的结果赋值给matchFound
								matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
							}
						} else { //如果是FactoryBean类型
							// 如果包含非单例 或者 mdb没有配置延时 或者 (允许FactoryBean初始化 且  beanName, mbd, dbd所指向的bean是单例)
							if (includeNonSingletons || isNonLazyDecorated ||
									(allowFactoryBeanInit && isSingleton(beanName, mbd, dbd))) {
								matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
							}
							if (!matchFound) {
								// In case of FactoryBean, try to match FactoryBean instance itself next.
								// 如果是FactoryBean，请尝试接下来匹配FactoryBean实例本身
								// 将beanName改成解引用名
								beanName = FACTORY_BEAN_PREFIX + beanName;
								if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
									matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
								}
							}
						}
						// 如果匹配成功
						if (matchFound) {
							result.add(beanName);
						}
					}
				} catch (CannotLoadBeanClassException | BeanDefinitionStoreException ex) {
					if (allowEagerInit) {
						throw ex;
					}
					// Probably a placeholder: let's ignore it for type matching purposes.
					LogMessage message = (ex instanceof CannotLoadBeanClassException ?
							LogMessage.format("Ignoring bean class loading failure for bean '%s'", beanName) :
							LogMessage.format("Ignoring unresolvable metadata in bean definition '%s'", beanName));
					logger.trace(message, ex);
					// Register exception, in case the bean was accidentally unresolvable.
					onSuppressedException(ex);
				} catch (NoSuchBeanDefinitionException ex) {
					// Bean definition got removed while we were iterating -> ignore.
				}
			}
		}

		// Check manually registered singletons too.
		// 还要检查手动注册的单例。
		// 遍历手动注册单例的名称列表
		for (String beanName : this.manualSingletonNames) {
			try {
				// In case of FactoryBean, match object created by FactoryBean.
				// 对于FactoryBean，请匹配FactoryBean创建的对象
				// 如果beanName所指的对象属于FactoryBean实例
				if (isFactoryBean(beanName)) {
					// 如果(包含非单例 或者 beanName所指的对象是单例) 且 beanName对应的Bean类型与type匹配
					if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
						result.add(beanName);
						// Match found for this bean: do not match FactoryBean itself anymore.
						// 为此bean找到匹配项：不再匹配FactoryBean本身
						continue;
					}
					// In case of FactoryBean, try to match FactoryBean itself next.
					// 如果是FactoryBean,请尝试接下来匹配FactoryBean本身
					beanName = FACTORY_BEAN_PREFIX + beanName;
				}
				// Match raw bean instance (might be raw FactoryBean).
				// 匹配原始 Bean 实例（可能是原始 FactoryBean）。
				// 如果beanName对应的Bean类型与type匹配（此时的beanName有可能是FactoryBeany解引用名）
				if (isTypeMatch(beanName, type)) {
					result.add(beanName);
				}
			} catch (NoSuchBeanDefinitionException ex) {
				// Shouldn't happen - probably a result of circular reference resolution...
				logger.trace(LogMessage.format(
						"Failed to check manually registered singleton with name '%s'", beanName), ex);
			}
		}

		// 将result转换成Stirng数组返回出去
		return StringUtils.toStringArray(result);
	}

	private boolean isSingleton(String beanName, RootBeanDefinition mbd, @Nullable BeanDefinitionHolder dbd) {
		return (dbd != null ? mbd.isSingleton() : isSingleton(beanName));
	}

	/**
	 * Check whether the specified bean would need to be eagerly initialized
	 * in order to determine its type.
	 * 检查是否需要急切地初始化指定的 Bean 以确定其类型。
	 *
	 * @param factoryBeanName a factory-bean reference that the bean definition
	 *                        defines a factory method for
	 *                        -- 一个工厂 Bean 引用，Bean 定义为其定义了工厂方法
	 * @return whether eager initialization is necessary
	 */
	private boolean requiresEagerInitForType(@Nullable String factoryBeanName) {
		return (factoryBeanName != null && isFactoryBean(factoryBeanName) && !containsSingleton(factoryBeanName));
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getBeansOfType(
			@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {

		String[] beanNames = getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		Map<String, T> result = CollectionUtils.newLinkedHashMap(beanNames.length);
		for (String beanName : beanNames) {
			try {
				Object beanInstance = getBean(beanName);
				if (!(beanInstance instanceof NullBean)) {
					result.put(beanName, (T) beanInstance);
				}
			} catch (BeanCreationException ex) {
				Throwable rootCause = ex.getMostSpecificCause();
				if (rootCause instanceof BeanCurrentlyInCreationException) {
					BeanCreationException bce = (BeanCreationException) rootCause;
					String exBeanName = bce.getBeanName();
					if (exBeanName != null && isCurrentlyInCreation(exBeanName)) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring match to currently created bean '" + exBeanName + "': " +
									ex.getMessage());
						}
						onSuppressedException(ex);
						// Ignore: indicates a circular reference when autowiring constructors.
						// We want to find matches other than the currently created bean itself.
						continue;
					}
				}
				throw ex;
			}
		}
		return result;
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		List<String> result = new ArrayList<>();
		for (String beanName : this.beanDefinitionNames) {
			BeanDefinition bd = this.beanDefinitionMap.get(beanName);
			if (bd != null && !bd.isAbstract() && findAnnotationOnBean(beanName, annotationType) != null) {
				result.add(beanName);
			}
		}
		for (String beanName : this.manualSingletonNames) {
			if (!result.contains(beanName) && findAnnotationOnBean(beanName, annotationType) != null) {
				result.add(beanName);
			}
		}
		return StringUtils.toStringArray(result);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
		String[] beanNames = getBeanNamesForAnnotation(annotationType);
		Map<String, Object> result = CollectionUtils.newLinkedHashMap(beanNames.length);
		for (String beanName : beanNames) {
			Object beanInstance = getBean(beanName);
			if (!(beanInstance instanceof NullBean)) {
				result.put(beanName, beanInstance);
			}
		}
		return result;
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		return findAnnotationOnBean(beanName, annotationType, true);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		return findMergedAnnotationOnBean(beanName, annotationType, allowFactoryBeanInit)
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	private <A extends Annotation> MergedAnnotation<A> findMergedAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) {

		Class<?> beanType = getType(beanName, allowFactoryBeanInit);
		if (beanType != null) {
			MergedAnnotation<A> annotation =
					MergedAnnotations.from(beanType, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
			if (annotation.isPresent()) {
				return annotation;
			}
		}
		if (containsBeanDefinition(beanName)) {
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			// Check raw bean class, e.g. in case of a proxy.
			if (bd.hasBeanClass()) {
				Class<?> beanClass = bd.getBeanClass();
				if (beanClass != beanType) {
					MergedAnnotation<A> annotation =
							MergedAnnotations.from(beanClass, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
					if (annotation.isPresent()) {
						return annotation;
					}
				}
			}
			// Check annotations declared on factory method, if any.
			Method factoryMethod = bd.getResolvedFactoryMethod();
			if (factoryMethod != null) {
				MergedAnnotation<A> annotation =
						MergedAnnotations.from(factoryMethod, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
				if (annotation.isPresent()) {
					return annotation;
				}
			}
		}
		return MergedAnnotation.missing();
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue) {
		Assert.notNull(dependencyType, "Dependency type must not be null");
		if (autowiredValue != null) {
			if (!(autowiredValue instanceof ObjectFactory || dependencyType.isInstance(autowiredValue))) {
				throw new IllegalArgumentException("Value [" + autowiredValue +
						"] does not implement specified dependency type [" + dependencyType.getName() + "]");
			}
			// 缓存要注册的依赖项类型 和 相应自动注入值的关系
			this.resolvableDependencies.put(dependencyType, autowiredValue);
		}
	}

	/**
	 *
	 * @param beanName the name of the bean to check 属性的beanName
	 * @param descriptor the descriptor of the dependency to resolve 属性描述符
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */

	@Override
	public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException {

		return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
	}

	/**
	 * Determine whether the specified bean definition qualifies as an autowire candidate,
	 * to be injected into other beans which declare a dependency of matching type.
	 *
	 * 确定指定的 bean 定义是否有资格作为自动装配候选者，以注入到声明匹配类型的依赖项的其他 bean 中。
	 *
	 * @param beanName   the name of the bean definition to check
	 * @param descriptor the descriptor of the dependency to resolve
	 * @param resolver   the AutowireCandidateResolver to use for the actual resolution algorithm
	 * @return whether the bean should be considered as autowire candidate
	 */
	protected boolean isAutowireCandidate(
			String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
			throws NoSuchBeanDefinitionException {

		String bdName = BeanFactoryUtils.transformedBeanName(beanName);
		// 确定beanDefinitionName的合并后RootBeanDefinition是否符合自动装配候选条件，以注入到声明匹配类型依赖项的其他bean中
		// 并将结果返回出去
		if (containsBeanDefinition(bdName)) {
			return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(bdName), descriptor, resolver);
		} else if (containsSingleton(beanName)) {
			return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
		}

		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof DefaultListableBeanFactory) {
			// No bean definition found in this factory -> delegate to parent.
			return ((DefaultListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor, resolver);
		} else if (parent instanceof ConfigurableListableBeanFactory) {
			// If no DefaultListableBeanFactory, can't pass the resolver along.
			return ((ConfigurableListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor);
		} else {
			return true;
		}
	}

	/**
	 * Determine whether the specified bean definition qualifies as an autowire candidate,
	 * to be injected into other beans which declare a dependency of matching type.
	 *
	 * 确定指定的 Bean 定义是否有资格作为 autowire 候选项，以注入到声明匹配类型的依赖关系的其他 Bean 中。
	 *
	 * @param beanName   the name of the bean definition to check
	 * @param mbd        the merged bean definition to check
	 * @param descriptor the descriptor of the dependency to resolve
	 * @param resolver   the AutowireCandidateResolver to use for the actual resolution algorithm
	 * @return whether the bean should be considered as autowire candidate
	 */
	protected boolean isAutowireCandidate(String beanName, RootBeanDefinition mbd,
										  DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {

		String bdName = BeanFactoryUtils.transformedBeanName(beanName);
		// 为mdb解析出对应的bean class对象
		resolveBeanClass(mbd, bdName);
		// 如果mbd指明了引用非重载方法的工厂方法名称 且 mbd还没有缓存用于自省的唯一工厂方法候选
		if (mbd.isFactoryMethodUnique && mbd.factoryMethodToIntrospect == null) {
			new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
		}
		BeanDefinitionHolder holder = (beanName.equals(bdName) ?
				this.mergedBeanDefinitionHolders.computeIfAbsent(beanName,
						key -> new BeanDefinitionHolder(mbd, beanName, getAliases(bdName))) :
				new BeanDefinitionHolder(mbd, beanName, getAliases(bdName)));
		return resolver.isAutowireCandidate(holder, descriptor);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		BeanDefinition bd = this.beanDefinitionMap.get(beanName);
		if (bd == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("No bean named '" + beanName + "' found in " + this);
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}
		return bd;
	}

	@Override
	public Iterator<String> getBeanNamesIterator() {
		CompositeIterator<String> iterator = new CompositeIterator<>();
		iterator.add(this.beanDefinitionNames.iterator());
		iterator.add(this.manualSingletonNames.iterator());
		return iterator;
	}

	@Override
	protected void clearMergedBeanDefinition(String beanName) {
		super.clearMergedBeanDefinition(beanName);
		this.mergedBeanDefinitionHolders.remove(beanName);
	}

	@Override
	public void clearMetadataCache() {
		super.clearMetadataCache();
		this.mergedBeanDefinitionHolders.clear();
		clearByTypeCache();
	}

	@Override
	public void freezeConfiguration() {
		this.configurationFrozen = true;
		this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
	}

	@Override
	public boolean isConfigurationFrozen() {
		return this.configurationFrozen;
	}

	/**
	 * Considers all beans as eligible for metadata caching
	 * if the factory's configuration has been marked as frozen.
	 *
	 * @see #freezeConfiguration()
	 */
	@Override
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return (this.configurationFrozen || super.isBeanEligibleForMetadataCaching(beanName));
	}

	/**
	 * 在之后的内容你可能会频繁的见到“MergedBeanDefinition”这个词，因此这边先稍微讲解一下，有助于你更好的理解。
	 * MergedBeanDefinition：这个词其实不是一个官方词，但是很接近，该词主要是用来表示“合并的bean定义”，因为每次都写“合并的bean定义”有点太绕口，
	 * 因此我在之后的注释或解析中或统一使用MergedBeanDefinition来表示“合并的bean定义”。
	 * 之所以称之为“合并的”，是因为存在“子定义”和“父定义”的情况。对于一个bean定义来说，可能存在以下几种情况：
	 * (1)该BeanDefinition存在“父定义”：首先使用“父定义”的参数构建一个RootBeanDefinition，然后再使用该BeanDefinition的参数来进行覆盖。
	 * (2)该BeanDefinition不存在“父定义”，并且该BeanDefinition的类型是RootBeanDefinition：直接返回该RootBeanDefinition的一个克隆。
	 * (3)该BeanDefinition不存在“父定义”，但是该BeanDefinition的类型不是RootBeanDefinition：使用该BeanDefinition的参数构建一个RootBeanDefinition。
	 * 之所以区分出2和3，是因为通常BeanDefinition在之前加载到BeanFactory中的时候，通常是被封装成GenericBeanDefinition或ScannedGenericBeanDefinition，
	 * 但是从这边之后bean的后续流程处理都是针对RootBeanDefinition，因此在这边会统一将BeanDefinition转换成RootBeanDefinition。
	 * 在我们日常使用的过程中，通常会是上面的第3种情况。如果我们使用XML配置来注册bean，则该bean定义会被封装成：GenericBeanDefinition；
	 * 如果我们使用注解的方式来注册bean，也就是<context:component-scan/>+@Compoment，则该bean定义会被封装成ScannedGenericBeanDefinition。
	 */
	@Override
	public void preInstantiateSingletons() throws BeansException {
		if (logger.isTraceEnabled()) {
			logger.trace("Pre-instantiating singletons in " + this);
		}

		// Iterate over a copy to allow for init methods which in turn register new bean definitions.
		// While this may not be part of the regular factory bootstrap, it does otherwise work fine.
		// 创建BeanDefinitionNames的副本BeanNames用于后续的遍历，以允许init等方法注册新的bean定义.
		List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

		// Trigger initialization of all non-lazy singleton beans...
		// 遍历所有的beanNames，触发所有非懒加载单例bean的初始化，即：创建所有的单实例Bean
		for (String beanName : beanNames) {
			// 获取beanName对应的MergedBeanDefinition.
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			// 如果bd对应的Bean实例满足：(不是抽象类 && 是单例 && 不是懒加载)
			if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
				// 判断是否FactoryBean, 一般情况下只是创建FactoryBean对象，除非isEagerInit = true，则会调用它的getObject()方法
				if (isFactoryBean(beanName)) {
					// 通过beanName获取FactoryBean的实例，factoryBean的名称是："&" + beanName
					Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
					if (bean instanceof FactoryBean) {
						FactoryBean<?> factory = (FactoryBean<?>) bean;
						boolean isEagerInit;
						if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
							isEagerInit = AccessController.doPrivileged(
									(PrivilegedAction<Boolean>) ((SmartFactoryBean<?>) factory)::isEagerInit,
									getAccessControlContext());
						} else {
							isEagerInit = (factory instanceof SmartFactoryBean &&
									((SmartFactoryBean<?>) factory).isEagerInit());
						}
						// 急切的初始化
						if (isEagerInit) {
							getBean(beanName);
						}
					}
				} else {
					// 如果BeanName对应的Bean实例不是FactoryBean，则通过BeanName去获取Bean实例.
					getBean(beanName);
				}
			}
		}

		// Trigger post-initialization callback for all applicable beans...
		// 触发适用的所有bean的后初始化回调函数。
		/**
		 * 上一步for循环中已经创建完了所有的单实例Bean，这个for循环中，会拿出所有的单实例Bean，
		 *   然后遍历，判断单实例bean是否实现了SmartInitializingSingleton接口，如果实现了该接口，
		 *   则调用单实例Bean的afterSingletonsInstantiated方法
		 */
		for (String beanName : beanNames) {
			Object singletonInstance = getSingleton(beanName);
			if (singletonInstance instanceof SmartInitializingSingleton) {
				StartupStep smartInitialize = getApplicationStartup().start("spring.beans.smart-initialize")
						.tag("beanName", beanName);
				SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
						smartSingleton.afterSingletonsInstantiated();
						return null;
					}, getAccessControlContext());
				} else {
					/**
					 * 如果实现了SmartInitializingSingleton接口，则会调用afterSingletonInstantiated方法
					 *   例如@EventListener注解的实现原理，就是利用EventListenerMethodProcessor后置处理器完成的，
					 *   而在EventListenerMethodProcessor中就是实现了SmartInitializingSingleton接口
					 */
					smartSingleton.afterSingletonsInstantiated();
				}
				smartInitialize.end();
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry interface
	//---------------------------------------------------------------------

	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");

		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
				((AbstractBeanDefinition) beanDefinition).validate();
			} catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}

		BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
		if (existingDefinition != null) {
			if (!isAllowBeanDefinitionOverriding()) {
				throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
			} else if (existingDefinition.getRole() < beanDefinition.getRole()) {
				// e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
				// 例如是 ROLE_APPLICATION，现在用 ROLE_SUPPORT 或 ROLE_INFRASTRUCTURE 覆盖
				if (logger.isInfoEnabled()) {
					logger.info("Overriding user-defined bean definition for bean '" + beanName +
							"' with a framework-generated bean definition: replacing [" +
							existingDefinition + "] with [" + beanDefinition + "]");
				}
			} else if (!beanDefinition.equals(existingDefinition)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Overriding bean definition for bean '" + beanName +
							"' with a different definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace("Overriding bean definition for bean '" + beanName +
							"' with an equivalent definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			this.beanDefinitionMap.put(beanName, beanDefinition);
		} else {
			if (hasBeanCreationStarted()) {
				// Cannot modify startup-time collection elements anymore (for stable iteration)
				synchronized (this.beanDefinitionMap) {
					this.beanDefinitionMap.put(beanName, beanDefinition);
					List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
					updatedDefinitions.addAll(this.beanDefinitionNames);
					updatedDefinitions.add(beanName);
					this.beanDefinitionNames = updatedDefinitions;
					removeManualSingletonName(beanName);
				}
			} else {
				// Still in startup registration phase 仍处于启动注册阶段
				this.beanDefinitionMap.put(beanName, beanDefinition);
				this.beanDefinitionNames.add(beanName);
				removeManualSingletonName(beanName);
			}
			this.frozenBeanDefinitionNames = null;
		}

		if (existingDefinition != null || containsSingleton(beanName)) {
			resetBeanDefinition(beanName);
		} else if (isConfigurationFrozen()) {
			clearByTypeCache();
		}
	}

	@Override
	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		Assert.hasText(beanName, "'beanName' must not be empty");

		BeanDefinition bd = this.beanDefinitionMap.remove(beanName);
		if (bd == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("No bean named '" + beanName + "' found in " + this);
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}

		if (hasBeanCreationStarted()) {
			// Cannot modify startup-time collection elements anymore (for stable iteration)
			synchronized (this.beanDefinitionMap) {
				List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames);
				updatedDefinitions.remove(beanName);
				this.beanDefinitionNames = updatedDefinitions;
			}
		} else {
			// Still in startup registration phase
			this.beanDefinitionNames.remove(beanName);
		}
		this.frozenBeanDefinitionNames = null;

		resetBeanDefinition(beanName);
	}

	/**
	 * Reset all bean definition caches for the given bean,
	 * including the caches of beans that are derived from it.
	 * <p>Called after an existing bean definition has been replaced or removed,
	 * triggering {@link #clearMergedBeanDefinition}, {@link #destroySingleton}
	 * and {@link MergedBeanDefinitionPostProcessor#resetBeanDefinition} on the
	 * given bean and on all bean definitions that have the given bean as parent.
	 * <p>
	 * 重置给定 bean 的所有 bean 定义缓存，包括从其派生的 bean 的缓存。
	 * 在替换或删除现有 bean 定义后调用，触发给定 bean 以及以给定 bean 作为父级的所有 bean 定义上的 {@link #clearMergedBeanDefinition}、
	 * {@link #destroySingleton} 和 {@link MergedBeanDefinitionPostProcessor#resetBeanDefinition}。
	 *
	 * @param beanName the name of the bean to reset
	 * @see #registerBeanDefinition
	 * @see #removeBeanDefinition
	 */
	protected void resetBeanDefinition(String beanName) {
		// Remove the merged bean definition for the given bean, if already created.
		// 删除给定 bean 的合并 bean 定义（如果已创建）。
		clearMergedBeanDefinition(beanName);

		// Remove corresponding bean from singleton cache, if any. Shouldn't usually
		// be necessary, rather just meant for overriding a context's default beans
		// (e.g. the default StaticMessageSource in a StaticApplicationContext).
		// 从单例缓存中删除相应的 bean（如果有）。通常不是必需的，而只是用于覆盖上下文的默认 bean（例如 StaticApplicationContext 中的默认 StaticMessageSource）。
		destroySingleton(beanName);

		// Notify all post-processors that the specified bean definition has been reset.
		// 通知所有后处理器指定的 bean 定义已重置。
		for (MergedBeanDefinitionPostProcessor processor : getBeanPostProcessorCache().mergedDefinition) {
			processor.resetBeanDefinition(beanName);
		}

		// Reset all bean definitions that have the given bean as parent (recursively).
		// 重置以给定 bean 作为父级的所有 bean 定义（递归地）。
		for (String bdName : this.beanDefinitionNames) {
			if (!beanName.equals(bdName)) {
				BeanDefinition bd = this.beanDefinitionMap.get(bdName);
				// Ensure bd is non-null due to potential concurrent modification of beanDefinitionMap.
				// 确保 bd 不为 null，因为可能会同时修改 beanDefinitionMap。
				if (bd != null && beanName.equals(bd.getParentName())) {
					resetBeanDefinition(bdName);
				}
			}
		}
	}

	/**
	 * Only allows alias overriding if bean definition overriding is allowed.
	 */
	@Override
	protected boolean allowAliasOverriding() {
		return isAllowBeanDefinitionOverriding();
	}

	/**
	 * Also checks for an alias overriding a bean definition of the same name.
	 */
	@Override
	protected void checkForAliasCircle(String name, String alias) {
		super.checkForAliasCircle(name, alias);
		if (!isAllowBeanDefinitionOverriding() && containsBeanDefinition(alias)) {
			throw new IllegalStateException("Cannot register alias '" + alias +
					"' for name '" + name + "': Alias would override bean definition '" + alias + "'");
		}
	}

	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		super.registerSingleton(beanName, singletonObject);
		// 加入manualSingletonNames中
		updateManualSingletonNames(set -> set.add(beanName), set -> !this.beanDefinitionMap.containsKey(beanName));
		clearByTypeCache();
	}

	@Override
	public void destroySingletons() {
		super.destroySingletons();
		updateManualSingletonNames(Set::clear, set -> !set.isEmpty());
		clearByTypeCache();
	}

	@Override
	public void destroySingleton(String beanName) {
		super.destroySingleton(beanName);
		removeManualSingletonName(beanName);
		clearByTypeCache();
	}

	private void removeManualSingletonName(String beanName) {
		updateManualSingletonNames(set -> set.remove(beanName), set -> set.contains(beanName));
	}

	/**
	 * Update the factory's internal set of manual singleton names. 更新工厂的内部手动单例名称集。
	 *
	 * @param action    the modification action 修改操作
	 * @param condition a precondition for the modification action
	 *                  (if this condition does not apply, the action can be skipped) 修改操作的前提条件（如果此条件不适用，则可以跳过该操作）
	 */
	private void updateManualSingletonNames(Consumer<Set<String>> action, Predicate<Set<String>> condition) {
		if (hasBeanCreationStarted()) {
			// Cannot modify startup-time collection elements anymore (for stable iteration)
			// 无法再修改启动时集合元素（为了稳定迭代）
			synchronized (this.beanDefinitionMap) {
				if (condition.test(this.manualSingletonNames)) {
					Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
					action.accept(updatedSingletons);
					this.manualSingletonNames = updatedSingletons;
				}
			}
		} else {
			// Still in startup registration phase
			if (condition.test(this.manualSingletonNames)) {
				action.accept(this.manualSingletonNames);
			}
		}
	}

	/**
	 * Remove any assumptions about by-type mappings.
	 */
	private void clearByTypeCache() {
		this.allBeanNamesByType.clear();
		this.singletonBeanNamesByType.clear();
	}


	//---------------------------------------------------------------------
	// Dependency resolution functionality
	//---------------------------------------------------------------------

	@Override
	public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException {
		Assert.notNull(requiredType, "Required type must not be null");
		NamedBeanHolder<T> namedBean = resolveNamedBean(ResolvableType.forRawClass(requiredType), null, false);
		if (namedBean != null) {
			return namedBean;
		}
		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof AutowireCapableBeanFactory) {
			return ((AutowireCapableBeanFactory) parent).resolveNamedBean(requiredType);
		}
		throw new NoSuchBeanDefinitionException(requiredType);
	}

	/**
	 * 解析与给定对象类型唯一匹配的bean实例，包括其bean名:
	 * <ol>
	 *  <li>如果requiredType为null，抛出异常</li>
	 *  <li>获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的候选Bean名(也包括Prototype级别的Bean对象,并
	 *  允许初始化lazy-init单例和由FactoryBeans创建的对象)【变量 candidateNames】</li>
	 *  <li>如果candidateNames不止一个:
	 *   <ol>
	 *    <li>定义一个用于保存可自动注入Bean名的集合，初始化长度为后续Bean名数组的长度【变量 autowireCandidates】</li>
	 *    <li>遍历候选Bean名称,元素为beanName:
	 *     <ol>
	 *      <li>如果本地工厂中BeanDefinition对象映射【beanDefinitionMap】中不存在beanName该键名 或者
	 *      该beanName所对应的BeanDefinition对象指定了该Bean对象可以自动注入:就将bean名添加到autowireCandidates中</li>
	 *     </ol>
	 *    </li>
	 *    <li>如果autowireCandidates不为空,将autowireCandidates转换成数组重新赋值给candidateNames</li>
	 *   </ol>
	 *  </li>
	 *  <li>如果candidateNames只有一个:
	 *   <ol>
	 *    <li>获取这唯一一个后续bean名【变量 beanName】</li>
	 *    <li>将beanName，何其对应的Bean对象一起封装到NameBeanHolder对象中，然后返回出去</li>
	 *   </ol>
	 *  </li>
	 *  <li>如果candidateNames不止一个:
	 *   <ol>
	 *    <li>定义一个用于存储候选Bean名和后续Bean对象/后续Bean类型的Map【变量 candidates】</li>
	 *    <li>遍历candidateNames,元素为beanName:
	 *     <ol>
	 *      <li>如果beanName在该BeanFactory的单例对象的高速缓存Map集合【DefaultListableBeanFactory.singletonObjects】中
	 *      且 没有生成Bean对象所需的构造函数参数:
	 *       <ol>
	 *        <li>获取beanName的Bean对象【变量 beanInstance】</li>
	 *        <li>将beanName和bean对象添加到candidates中(如果bean对象是NullBean实例，value则为null)</li>
	 *       </ol>
	 *      </li>
	 *      <li>否则，可以认为是Prototype级别的Bean对象:将beanName和beanName所对应的Bean Class对象添加到
	 *      candidates中</li>
	 *     </ol>
	 *    </li>
	 *    <li>在candidates中确定primary候选Bean名【变量 candidateName】</li>
	 *    <li>如果没有primary候选Bean名,获取candidates中具有Priority注解最高优先级的候选Bean名重新赋值给candidateName</li>
	 *    <li>如果candidateName不为null:
	 *     <ol>
	 *      <li>从candidates中获取candidateName对应的Bean对象【变量 beanInstance】</li>
	 *      <li>如果beanInstance为null 或者 benaInstance是Class对象:根据candidateName,requiredType的Class对象，
	 *      args获取对应Bean对象</li>
	 *      <li>将beanName，和beanInstance一起封装到NameBeanHolder对象中，然后返回出去</li>
	 *     </ol>
	 *    </li>
	 *    <li>如果没有设置，或者设置遇到非唯一Bean对象情况下直接抛出异常的时候:抛出 非唯一BenaDefinition异常</li>
	 *   </ol>
	 *  </li>
	 *  <li>在没有候选Bean对象的情况下，返回null</li>
	 * </ol>
	 * @param requiredType 键入bean必需匹配；可以是接口或超类
	 * @param args 用于构造requiredType所对应的Bean对象的参数，一般是指生成Bean对象所需的构造函数参数
	 * @param nonUniqueAsNull 遇到非唯一Bean对象情况下，如果为true将直接返回null，否则抛出异常
	 * @param <T> Bean对象的类型
	 * @return Bean名称加Bean实例的封装类
	 * @throws BeansException 如果无法创建该bean
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private <T> NamedBeanHolder<T> resolveNamedBean(
			ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) throws BeansException {

		Assert.notNull(requiredType, "Required type must not be null");
		// 获取匹配type（包含子类）的bean（或由FactoryBean创建的对象）的候选Bean名称(也包括Prototype级别的Bean对象,并
		// 	允许初始化lazy-init单例和由FactoryBeans创建的对象)
		String[] candidateNames = getBeanNamesForType(requiredType);

		// 如果候选Bean名不止一个
		if (candidateNames.length > 1) {
			// 定义一个用于保存可自动注入Bean名的集合，初始化长度为候选Bean名数组的长度
			List<String> autowireCandidates = new ArrayList<>(candidateNames.length);
			for (String beanName : candidateNames) {
				// 如果本地工厂中BeanDefinition对象映射【beanDefinitionMap】中不存在beanName该键名 或者
				// 	该beanName所对应的BeanDefinition对象指定了该Bean对象可以自动注入
				if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
					autowireCandidates.add(beanName);
				}
			}
			if (!autowireCandidates.isEmpty()) {
				// 将autowireCandidates转换成数组重新赋值给candidateNames
				candidateNames = StringUtils.toStringArray(autowireCandidates);
			}
		}

		// 如果candidateNames只有一个
		if (candidateNames.length == 1) {
			// 获取这唯一一个候选bean名
			// 将beanName，和其对应的Bean对象一起封装到NameBeanHolder对象中，然后返回出去
			return resolveNamedBean(candidateNames[0], requiredType, args);
		} else if (candidateNames.length > 1) { // 如果candidateNames不止一个
			// 定义一个用于存储候选Bean名和候选Bean对象/后续Bean类型的Map
			Map<String, Object> candidates = CollectionUtils.newLinkedHashMap(candidateNames.length);
			for (String beanName : candidateNames) {
				// 如果beanName在该BeanFactory的单例对象的高速缓存Map集合【DefaultListableBeanFactory.singletonObjects】中
				// 且 没有生成Bean对象所需的构造函数参数
				if (containsSingleton(beanName) && args == null) {
					// 获取beanName的Bean对象
					Object beanInstance = getBean(beanName);
					// 将beanName和bean对象添加到candidates中(如果bean对象是NullBean实例，value则为null)
					candidates.put(beanName, (beanInstance instanceof NullBean ? null : beanInstance));
				} else { // 否则，可以认为是Prototype级别的Bean对象
					// 将beanName和beanName所对应的Bean Class对象 添加到candidates中
					candidates.put(beanName, getType(beanName));
				}
			}
			// 在candidates中确定primary候选Bean名
			String candidateName = determinePrimaryCandidate(candidates, requiredType.toClass());
			// 如果没有primary候选Bean名
			if (candidateName == null) {
				// 获取candidates中具有Priority注解最高优先级的候选Bean名重新赋值给candidateName
				candidateName = determineHighestPriorityCandidate(candidates, requiredType.toClass());
			}
			if (candidateName != null) {
				// 从candidates中获取candidateName对应的Bean对象
				Object beanInstance = candidates.get(candidateName);
				if (beanInstance == null) {
					return null;
				}
				// benaInstance是Class对象
				if (beanInstance instanceof Class) {
					// 根据candidateName,requiredType的Class对象，args获取对应Bean对象
					return resolveNamedBean(candidateName, requiredType, args);
				}
				// 将beanName，和beanInstance一起封装到NameBeanHolder对象中，然后返回出去
				return new NamedBeanHolder<>(candidateName, (T) beanInstance);
			}
			if (!nonUniqueAsNull) {
				throw new NoUniqueBeanDefinitionException(requiredType, candidates.keySet());
			}
		}

		return null;
	}

	@Nullable
	private <T> NamedBeanHolder<T> resolveNamedBean(
			String beanName, ResolvableType requiredType, @Nullable Object[] args) throws BeansException {

		Object bean = getBean(beanName, null, args);
		if (bean instanceof NullBean) {
			return null;
		}
		return new NamedBeanHolder<T>(beanName, adaptBeanInstance(beanName, bean, requiredType.toClass()));
	}

	/**
	 * 根据descriptor的依赖类型解析出与descriptor所包装的对象匹配的候选Bean对象:
	 * <ol>
	 *  <li>获取工厂的参数名发现器，设置到descriptor中。使得descriptor初始化基础方法参数的参数名发现。</li>
	 *  <li>【<b>当descriptor的依赖类型是Optional时</b>】:
	 *   <ol>
	 *    <li>如果descriptor的依赖类型为Optional类,创建Optional类型的符合descriptor要求的候选Bean对象并返回
	 *    出去</li>
	 *   </ol>
	 *  </li>
	 *  <li>【<b>当decriptord的依赖类型是ObjectFactory或者是ObjectProvider</b>】:
	 *   <ol>
	 *    <li>如果decriptord的依赖类型是ObjectFactory或者是ObjectProvider,新建一个
	 *    DependencyObjectProvider的实例并返回出去</li>
	 *   </ol>
	 *  </li>
	 *  <li>【<b>当decriptord的依赖类型是javax.inject.Provider</b>】:
	 *   <ol>
	 *    <li>如果依赖类型是javax.inject.Provider类,新建一个专门用于构建
	 *    javax.inject.Provider对象的工厂来构建创建Jse330Provider对象</li>
	 *   </ol>
	 *  </li>
	 *  <li>【<b>当descriptor需要延迟加载时</b>】:
	 *   <ol>
	 *    <li>尝试获取延迟加载代理对象【变量 result】</li>
	 *   </ol>
	 *  </li>
	 *  <li>【<b>当现在就需要得到候选Bean对象时</b>】:
	 *   <ol>
	 *    <li>如果result为null，即表示现在需要得到候选Bean对象,解析出与descriptor所包装的对象匹配
	 *    的候选Bean对象</li>
	 *   </ol>
	 *  </li>
	 *  <li>将与descriptor所包装的对象匹配的候选Bean对象【result】返回出去</li>
	 * </ol>
	 * @param descriptor the descriptor for the dependency (field/method/constructor)
	 *                   -- 依赖项的描述符(字段/方法/构造函数)
	 * @param requestingBeanName the name of the bean which declares the given dependency
	 *                           -- 声明给定依赖项的bean名,即需要Field/MethodParamter所对应的bean对象来构建的Bean对象的Bean名
	 * @param autowiredBeanNames a Set that all names of autowired beans (used for
	 * resolving the given dependency) are supposed to be added to
	 *     一个集合，所有自动装配的bean名(用于解决给定依赖关系)都应添加.即自动注入匹配成功的候选Bean名集合。
	 *                              【当autowiredBeanNames不为null，会将所找到的所有候选Bean对象添加到该集合中,以供调用方使用】
	 * @param typeConverter the TypeConverter to use for populating arrays and collections
	 *    -- 用于填充数组和集合的TypeConverter
	 * @return  解析的对象；如果找不到，则为null
	 * @throws BeansException 如果依赖项解析由于任何其他原因而失败
	 */
	@Override
	@Nullable
	public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
									@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
		// 获取工厂的参数名发现器，设置到descriptor中。使得descriptor初始化基础方法参数的参数名发现。此时，该方法实际上
		// 并没有尝试检索参数名称；它仅允许发现在应用程序调用getDependencyName时发生
		descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
		// 如果descriptor的依赖类型为Optional类
		if (Optional.class == descriptor.getDependencyType()) {
			return createOptionalDependency(descriptor, requestingBeanName);
		} else if (ObjectFactory.class == descriptor.getDependencyType() ||
				ObjectProvider.class == descriptor.getDependencyType()) {
			return new DependencyObjectProvider(descriptor, requestingBeanName);
		} else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
			return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
		} else {
			//
			Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
					descriptor, requestingBeanName);
			if (result == null) {
				result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
			}
			return result;
		}
	}

	/**
	 * 解析出与descriptor所包装的对象匹配的候选Bean对象
	 * <ol>
	 *  <li>设置新得当前切入点对象，得到旧的当前切入点对象【变量 previousInjectionPoint】</li>
	 *  <li>【<b>尝试使用descriptor的快捷方法得到最佳候选Bean对象</b>】:
	 *   <ol>
	 *    <li>获取针对该工厂的这种依赖关系的快捷解析最佳候选Bean对象【变量 shortcut】</li>
	 *    <li>如果shortcut不为null，返回该shortcut</li>
	 *   </ol>
	 *  </li>
	 *  <li>获取descriptor的依赖类型【变量 type】</li>
	 *  <li>【<b>尝试使用descriptor的默认值作为最佳候选Bean对象</b>】:
	 *   <ol>
	 *    <li>使用此BeanFactory的自动装配候选解析器获取descriptor的默认值【变量 value】</li>
	 *    <li>如果value不为null:
	 *     <ol>
	 *      <li>如果value是String类型:
	 *       <ol>
	 *        <li>解析嵌套的值(如果value是表达式会解析出该表达式的值)【变量 strVal】</li>
	 *        <li>获取beanName的合并后RootBeanDefinition</li>
	 *        <li>让value引用评估bd中包含的value,如果strVal是可解析表达式，会对其进行解析.</li>
	 *       </ol>
	 *      </li>
	 *      <li>如果没有传入typeConverter,则引用工厂的类型转换器【变量 converter】</li>
	 *      <li>将value转换为type的实例对象并返回出去</li>
	 *      <li>捕捉 不支持操作异常:
	 *       <ol>
	 *        <li>如果descriptor有包装成员属性,根据descriptor包装的成员属性来将值转换为type然后返回出去</li>
	 *        <li>否则，根据descriptor包装的方法参数对象来将值转换为type然后返回出去</li>
	 *       </ol>
	 *      </li>
	 *     </ol>
	 *    </li>
	 *   </ol>
	 *  </li>
	 *  <li>【<b>尝试针对desciptor所包装的对象类型是[stream,数组,Collection类型且对象类型是接口,Map]的情况，
	 *  进行解析与依赖类型匹配的候选Bean对象</b>】:
	 *   <ol>
	 *    <li>针对desciptor所包装的对象类型是[stream,数组,Collection类型且对象类型是接口,Map]的情况，进行解析与依赖类型匹配的 候选Bean对象，
	 *    并将其封装成相应的依赖类型对象【{@link #resolveMultipleBeans(DependencyDescriptor, String, Set, TypeConverter)}】</li>
	 *    <li>如果multpleBeans不为null,将multipleBeans返回出去</li>
	 *   </ol>
	 *  </li>
	 *  <li>【<b>尝试与type匹配的唯一候选bean对象</b>】:
	 *   <ol>
	 *    <li>查找与type匹配的候选bean对象,构建成Map，key=bean名,val=Bean对象【变量 matchingBeans】</li>
	 *    <li>如果没有候选bean对象:
	 *     <ol>
	 *      <li>如果descriptor需要注入,抛出NoSuchBeanDefinitionException或BeanNotOfRequiredTypeException以解决不可
	 *      解决的依赖关系</li>
	 *      <li>返回null，表示么有找到候选Bean对象</li>
	 *     </ol>
	 *    </li>
	 *    <li>定义用于存储唯一的候选Bean名变量【变量 autowiredBeanName】</li>
	 *    <li>定义用于存储唯一的候选Bean对象变量【变量 instanceCandidate】</li>
	 *    <li>如果候选Bean对象Map不止有一个:
	 *     <ol>
	 *      <li>让autowiredBeanName引用candidates中可以自动注入的最佳候选Bean名称</li>
	 *      <li>如果autowiredBeanName为null:
	 *       <ol>
	 *        <li>如果descriptor需要注入 或者 type不是数组/集合类型，让descriptor尝试选择其中一个实例，默认实现是
	 *        抛出NoUniqueBeanDefinitionException.</li>
	 *        <li>返回null，表示找不到最佳候选Bean对象</li>
	 *       </ol>
	 *      </li>
	 *      <li>让instanceCandidate引用autowiredBeanName对应的候选Bean对象</li>
	 *     </ol>
	 *    </li>
	 *    <li>否则，获取machingBeans唯一的元素【变量 entry】:
	 *     <ol>
	 *       <li>让autowireBeanName引用该entry的候选bean名</li>
	 *       <li>让instanceCandidate引用该entry的候选bean对象</li>
	 *     </ol>
	 *    </li>
	 *    <li>如果候选bean名不为null，将autowiredBeanName添加到autowiredBeanNames中</li>
	 *    <li>如果instanceCandidate是Class实例,让instanceCandidate引用 descriptor对autowiredBeanName解析
	 *    为该工厂的Bean实例</li>
	 *    <li>定义一个result变量，用于存储最佳候选Bean对象</li>
	 *    <li>如果reuslt是NullBean的实例:
	 *     <ol>
	 *       <li>如果descriptor需要注入,抛出NoSuchBeanDefinitionException或BeanNotOfRequiredTypeException
	 *       以解决不可 解决的依赖关系</li>
	 *       <li>返回null，表示找不到最佳候选Bean对象</li>
	 *     </ol>
	 *    </li>
	 *    <li>如果result不是type的实例,抛出Bean不是必需类型异常</li>
	 *    <li>返回最佳候选Bean对象【result】</li>
	 *   </ol>
	 *  </li>
	 *  <li>【finally】设置上一个切入点对象</li>
	 * </ol>
	 * @param descriptor 依赖项的描述符(字段/方法/构造函数)
	 * @param beanName 要依赖的Bean名,即需要Field/MethodParamter所对应的bean对象来构建的Bean对象的Bean名称
	 * @param autowiredBeanNames 一个集合，所有自动装配的bean名(用于解决给定依赖关系)都应添加.即自动注入匹配成功的候选Bean名集合。
	 *                             【当autowiredBeanNames不为null，会将所找到的所有候选Bean对象添加到该集合中,以供调用方使用】
	 * @param typeConverter 用于填充数组和集合的TypeConverter
	 * @return 解析的对象；如果找不到，则为null
	 * @throws BeansException 如果依赖项解析由于任何其他原因而失败
	 */
	@Nullable
	public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
									  @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

		// 设置新得当前切入点对象，得到旧的当前切入点对象
		InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
		try {
			// 尝试使用descriptor的快捷方法得到最近候选Bean对象
			// resolveShortcut：解决针对给定工厂的这种依赖关系的快捷方式，例如，考虑一些预先解析的信息
			// 尝试调用该工厂解决这种依赖关系的快捷方式来获取beanName对应的bean对象,默认返回null
			// 获取针对该工厂的这种依赖关系的快捷解析最佳候选Bean对象
			Object shortcut = descriptor.resolveShortcut(this);
			// 如果shortcut不为null，返回该shortcut
			if (shortcut != null) {
				return shortcut;
			}
			// BService
			// 获取descriptor的依赖类型
			Class<?> type = descriptor.getDependencyType();
			// 解析@Value注解的value属性，并转换为相应的类型，返回
			Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
			if (value != null) {
				if (value instanceof String) {
					// 解析嵌套的值(如果value是表达式会解析出该表达式的值)
					String strVal = resolveEmbeddedValue((String) value);
					BeanDefinition bd = (beanName != null && containsBean(beanName) ?
							getMergedBeanDefinition(beanName) : null);
					// 解析bean定义中的表达式
					value = evaluateBeanDefinitionString(strVal, bd);
				}
				// 如果没有传入typeConverter,则引用工厂的类型转换器
				TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
				try {
					// 将value转换为type的实例对象
					return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
				} catch (UnsupportedOperationException ex) {
					// A custom TypeConverter which does not support TypeDescriptor resolution...
					// 不支持 TypeDescriptor 解析的自定义 TypeConverter...
					return (descriptor.getField() != null ?
							// 根据包装的成员属性来将值转换为所需的类型
							converter.convertIfNecessary(value, type, descriptor.getField()) :
							// 根据包装的方法参数对象来将值转换为所需的类型
							converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
				}
			}

			// 针对desciptor所包装的对象类型是[stream,数组,Collection类型且对象类型是接口,Map]的情况，进行解析与依赖类型匹配的 候选Bean对象，
			// 并将其封装成相应的依赖类型对象
			Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
			if (multipleBeans != null) {
				return multipleBeans;
			}
			// 尝试与type匹配的唯一候选bean对象
			// 查找与type匹配的候选bean对象,构建成Map，key=bean名,val=Bean对象
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
			// 如果没有候选bean对象
			if (matchingBeans.isEmpty()) {
				// 如果descriptor需要注入
				if (isRequired(descriptor)) {
					// 抛出NoSuchBeanDefinitionException或BeanNotOfRequiredTypeException以解决不可 解决的依赖关系
					raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
				}
				return null;
			}

			// 定义用于存储唯一的候选Bean名称变量
			String autowiredBeanName;
			// 定义用于存储唯一的候选Bean对象变量
			Object instanceCandidate;

			// 如果候选Bean对象Map不止有一个
			if (matchingBeans.size() > 1) {
				// 确定candidates中可以自动注入的最佳候选Bean名称
				autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
				if (autowiredBeanName == null) {
					// descriptor需要注入 或者 type不是数组/集合类型
					if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
						//让descriptor尝试选择其中一个实例，默认实现是抛出NoUniqueBeanDefinitionException.
						return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
					} else {
						// In case of an optional Collection/Map, silently ignore a non-unique case:
						// possibly it was meant to be an empty collection of multiple regular beans
						// (before 4.3 in particular when we didn't even look for collection beans).
						// 在可选的 Collection/Map 的情况下，静默忽略非唯一情况：
						// 可能是它被设计为多个常规 bean 的空集合
						// （特别是在 4.3 之前，我们甚至没有寻找集合 bean）
						return null;
					}
				}
				instanceCandidate = matchingBeans.get(autowiredBeanName);
			} else {
				// We have exactly one match.
				Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
				autowiredBeanName = entry.getKey();
				instanceCandidate = entry.getValue();
			}

			if (autowiredBeanNames != null) {
				autowiredBeanNames.add(autowiredBeanName);
			}

			// 如果instanceCandidate是Class实例
			if (instanceCandidate instanceof Class) {
				// 让instanceCandidate引用 descriptor对autowiredBeanName解析为该工厂的Bean实例
				instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
			}
			Object result = instanceCandidate;
			if (result instanceof NullBean) {
				if (isRequired(descriptor)) {
					raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
				}
				result = null;
			}
			if (!ClassUtils.isAssignableValue(type, result)) {
				throw new BeanNotOfRequiredTypeException(autowiredBeanName, type, instanceCandidate.getClass());
			}
			return result;
		} finally {
			ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
		}
	}

	/**
	 * 针对desciptor所包装的对象类型是[stream,数组,Collection类型且对象类型是接口,Map]的情况，进行解析与依赖类型匹配的
	 * 候选Bean对象，并将其封装成相应的依赖类型对象
	 * <ol>
	 *  <li>获取包装的参数/字段的声明的(非通用)类型【变量 type】</li>
	 *  <li>【<b>当decriptor所包装的对象是Stream类型</b>】:
	 *   <ol>
	 *    <li>如果描述符是Stream依赖项描述符:
	 *     <ol>
	 *      <li>查找与valueType匹配的候选bean对象;构建成Map，key=bean名,val=Bean对象【变量 matchingBeans】</li>
	 *      <li>自动注入匹配成功的候选Bean名集合不为null,将所有的自动注入匹配成功的候选Bean名添加到autowiredBeanNames</li>
	 *      <li>取出除Bean对象为NullBean以外的所有候选Bean名称的Bean对象【变量 stream】</li>
	 *      <li>如果decriptor需要排序,根据matchingBean构建排序比较器，交由steam进行排序</li>
	 *      <li>返回已排好序且已存放除Bean对象为NullBean以外的所有候选Bean名称的Bean对象的stream对象【变量 stream】</li>
	 *     </ol>
	 *    </li>
	 *   </ol>
	 *  </li>
	 *  <li>【<b>当decriptor所包装的对象是数组类型</b>】：
	 *   <ol>
	 *    <li>如果依赖类型是数组类型:
	 *     <ol>
	 *      <li>获取type的元素Class对象【变量 componentType】</li>
	 *      <li>获取decriptor包装的参数/字段所构建出来的ResolvableType对象【变量 resolvableType】</li>
	 *      <li>让resolvableType解析出的对应的数组Class对象，如果解析失败，就引用type【变量 resolvedArrayType】</li>
	 *      <li>如果resolvedArrayType与type不是同一个Class对象,componentType就引用resolvableType解析处理的元素Class对象</li>
	 *      <li>如果没有元素Class对象，就返回null，表示获取不到候选bean对象</li>
	 *      <li>查找与valueType匹配的候选bean对象;构建成Map，key=bean名,val=Bean对象【变量 matchingBeans】</li>
	 *      <li>如果没有候选Bean对象,返回null，表示获取不到候选bean对象</li>
	 *      <li>自动注入匹配成功的候选Bean名集合不为null,将所有的自动注入匹配成功的候选Bean名添加到autowiredBeanNames</li>
	 *      <li>如果有传入类型转换器就引用传入的类型转换器，否则获取此BeanFactory使用的类型转换器</li>
	 *      <li>将所有候选Bean对象转换为resolvedArrayType类型【变量 result】</li>
	 *      <li>如果result是数组实例:
	 *       <ol>
	 *        <li>构建依赖比较器,用于对matchingBean的所有bean对象进行优先级排序【变量 comparator】</li>
	 *        <li>如果比较器不为null,使用comparator对result数组进行排序</li>
	 *       </ol>
	 *      </li>
	 *      <li>返回该候选对象数组【result】</li>
	 *     </ol>
	 *    </li>
	 *   </ol>
	 *  </li>
	 *  <li>【<b>如果依赖类型属于Collection类型 且 依赖类型是否接口</b>】:
	 *   <ol>
	 *    <li>将descoptor所包装的参数/字段构建出来的ResolvableType对象解析成Collectionl类型，然后解析出其
	 *    泛型参数的Class对象【变量 elementType】</li>
	 *    <li>如果元素类型为null,返回null，表示获取不到候选bean对象</li>
	 *    <li>查找与valueType匹配的候选bean对象;构建成Map，key=bean名,val=Bean对象【变量 matchingBeans】</li>
	 *    <li>如果没有候选bean对象，返回null，表示获取不到候选bean对象</li>
	 *    <li>自动注入匹配成功的候选Bean名集合不为null,将所有的自动注入匹配成功的候选Bean名添加到autowiredBeanNames</li>
	 *    <li>如果有传入类型转换器就引用传入的类型转换器，否则获取此BeanFactory使用的类型转换器</li>
	 *    <li>将所有候选Bean对象转换为resolvedArrayType类型【变量 result】</li>
	 *    <li>如果result是List实例:
	 *     <ol>
	 *      <li>构建依赖比较器,用于对matchingBean的所有bean对象进行优先级排序【变量 comparator】</li>
	 *      <li>如果比较器不为null,使用comparator对result数组进行排序</li>
	 *     </ol>
	 *    </li>
	 *    <li>返回该候选对象数组【result】</li>
	 *   </ol>
	 *  </li>
	 *  <li>【<b>如果依赖类型是Map类型</b>】：
	 *   <ol>
	 *    <li>将descoptor所包装的参数/字段构建出来的ResolvableType对象解析成Map类型【变量 mapType】</li>
	 *    <li>解析出第1个泛型参数的Class对象,即key的Class对象【变量 keyType】</li>
	 *    <li>如果keyType不是String类型,返回null，表示获取不到候选bean对象</li>
	 *    <li>解析出第2个泛型参数的Class对象,即value的Class对象【变量 valueType】</li>
	 *    <li>如果keyType为null，即解析不出value的Class对象或者是根本没有value的Class对象,
	 *    返回null，表示获取不到候选bean对象</li>
	 *    <li>查找与valueType匹配的候选bean对象;构建成Map，key=bean名,val=Bean对象【变量 matchingBeans】</li>
	 *    <li>如果没有候选bean对象,返回null，表示获取不到候选bean对象</li>
	 *    <li>自动注入匹配成功的候选Bean名集合不为null,将所有的自动注入匹配成功的候选Bean名添加到autowiredBeanNames</li>
	 *    <li>返回候选的Bean对象Map【matchingBeans】</li>
	 *   </ol>
	 *  </li>
	 * </ol>
	 * @param descriptor 依赖项的描述符(字段/方法/构造函数)
	 * @param beanName 声明给定依赖项的bean名
	 * @param autowiredBeanNames 一个集合，所有自动装配的bean名(用于解决给定依赖关系)都应添加.即自动注入匹配成功的候选Bean名集合。
	 *                           【当autowiredBeanNames不为null，会将所找到的所有候选Bean对象添加到该集合中,以供调用方使用】
	 * @param typeConverter 用于填充数组和集合的TypeConverter
	 * @return 由候选Bean对象组成对象，该对象与descriptor的依赖类型相同;如果descriptor的依赖类型不是
	 * [stream,数组,Collection类型且对象类型是接口,Map],又或者解析不出相应的依赖类型，又或者拿不到候选Bean对象都会导致返回null
	 */
	@Nullable
	private Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName,
										@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

		// 获取包装的参数/字段的声明的(非通用)类型
		Class<?> type = descriptor.getDependencyType();

		// 如果描述符是Stream依赖项描述符
		if (descriptor instanceof StreamDependencyDescriptor) {
			// 查找与valueType匹配的候选bean对象;构建成Map，key=bean名,val=Bean对象
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
			// 自动注入匹配成功的候选Bean名集合不为null
			if (autowiredBeanNames != null) {
				// 将所有的自动注入匹配成功的候选Bean名添加到autowiredBeanNames
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			// 取出除Bean对象为NullBean以外的所有候选Bean名称的Bean对象
			Stream<Object> stream = matchingBeans.keySet().stream()
					// 将name解析为该Bean工厂的Bean实例
					.map(name -> descriptor.resolveCandidate(name, type, this))
					.filter(bean -> !(bean instanceof NullBean));
			if (((StreamDependencyDescriptor) descriptor).isOrdered()) {
				stream = stream.sorted(adaptOrderComparator(matchingBeans));
			}
			return stream;
		} else if (type.isArray()) {
			// 如果依赖类型是数组类型
			// 获取type的元素Class对象
			Class<?> componentType = type.getComponentType();
			// 获取decriptor包装的参数/字段所构建出来的ResolvableType对象
			ResolvableType resolvableType = descriptor.getResolvableType();
			// 让resolvableType解析出的对应的数组Class对象，如果解析失败，就引用type
			Class<?> resolvedArrayType = resolvableType.resolve(type);
			// 如果resolvedArrayType与type不是同一个Class对象
			if (resolvedArrayType != type) {
				// componentType就引用resolvableType解析处理的元素Class对象
				componentType = resolvableType.getComponentType().resolve();
			}
			// 如果没有元素Class对象，就返回null，表示获取不到候选bean对象
			if (componentType == null) {
				return null;
			}
			// MultiElemetDesciptor:具有嵌套元素的多元素声明的依赖描述符，表示集合/数组依赖
			// 查找与valueType匹配的候选bean对象;构建成Map，key=bean名,val=Bean对象
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType,
					new MultiElementDescriptor(descriptor));
			if (matchingBeans.isEmpty()) {
				return null;
			}
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
			// 将所有候选Bean对象转换为resolvedArrayType类型
			Object result = converter.convertIfNecessary(matchingBeans.values(), resolvedArrayType);
			if (result instanceof Object[]) {
				Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
				if (comparator != null) {
					Arrays.sort((Object[]) result, comparator);
				}
			}
			return result;
		} else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
			// 如果依赖类型属于Collection类型 且 依赖类型是否接口
			// 将descoptor所包装的参数/字段构建出来的ResolvableType对象解析成Collectionl类型，然后
			// 解析出其泛型参数的Class对象
			Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
			if (elementType == null) {
				return null;
			}
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType,
					new MultiElementDescriptor(descriptor));
			if (matchingBeans.isEmpty()) {
				return null;
			}
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
			Object result = converter.convertIfNecessary(matchingBeans.values(), type);
			if (result instanceof List) {
				if (((List<?>) result).size() > 1) {
					Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
					if (comparator != null) {
						((List<?>) result).sort(comparator);
					}
				}
			}
			return result;
		} else if (Map.class == type) {
			ResolvableType mapType = descriptor.getResolvableType().asMap();
			// 解析出第1个泛型参数的Class对象,即key的Class对象
			Class<?> keyType = mapType.resolveGeneric(0);
			//如果keyType不是String类型
			if (String.class != keyType) {
				return null;
			}
			// 解析出第2个泛型参数的Class对象,即value的Class对象
			Class<?> valueType = mapType.resolveGeneric(1);
			if (valueType == null) {
				return null;
			}
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType,
					new MultiElementDescriptor(descriptor));
			if (matchingBeans.isEmpty()) {
				return null;
			}
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			return matchingBeans;
		} else {
			return null;
		}
	}

	private boolean isRequired(DependencyDescriptor descriptor) {
		return getAutowireCandidateResolver().isRequired(descriptor);
	}

	private boolean indicatesMultipleBeans(Class<?> type) {
		return (type.isArray() || (type.isInterface() &&
				(Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
	}

	/**
	 * 构建依赖比较器,用于对matchingBean的所有bean对象进行优先级排序
	 * <ol>
	 *  <li>获取此BeanFactory的依赖关系比较器【变量 comparator】</li>
	 *  <li>如果comparator是OrderComparator实例,创建工厂感知排序源提供者实例
	 *  【FactoryAwareOrderSourceProvider】并让comparator引用它,然后返回出去</li>
	 *  <li>返回此BeanFactory的依赖关系比较器【comparator】</li>
	 * </ol>
	 * @param matchingBeans 要排序的Bean对象Map，key=Bean名,value=Bean对象
	 * @return 依赖比较器,可能是 {@link OrderComparator} 实例
	 */
	@Nullable
	private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
		// 获取此BeanFactory的依赖关系比较器
		Comparator<Object> comparator = getDependencyComparator();
		if (comparator instanceof OrderComparator) {
			// 创建工厂感知排序源提供者实例【FactoryAwareOrderSourceProvider】并让comparator引用它,然后返回出去
			return ((OrderComparator) comparator).withSourceProvider(
					createFactoryAwareOrderSourceProvider(matchingBeans));
		} else {
			return comparator;
		}
	}

	/**
	 * 构建排序比较器,用于对matchingBean的所有bean对象进行优先级排序
	 * <ol>
	 *  <li>获取该工厂的依赖关系比较器【变量 dependencyComparator】，SpringBoot默认
	 *  使用【{@link org.springframework.core.annotation.AnnotationAwareOrderComparator}】</li>
	 *  <li>如果dependencyComparator是OrderComparator的实例,就让comparator引用该实例，
	 *  否则使用OrderComparator的默认实例【变量 comparator】</li>
	 *  <li>创建工厂感知排序源提供者实例【FactoryAwareOrderSourceProvider】,并让comparator引用它</li>
	 *  <li>返回比较器【comparator】</li>
	 * </ol>
	 * @param matchingBeans 要排序的Bean对象Map，key=Bean名,value=Bean对象
	 * @return 排序比较器，一定是 {@link OrderComparator} 实例
	 */
	private Comparator<Object> adaptOrderComparator(Map<String, ?> matchingBeans) {
		// 获取该工厂的依赖关系比较器，SpringBoot默认使用 AnnotationAwareOrderComparator
		Comparator<Object> dependencyComparator = getDependencyComparator();
		OrderComparator comparator = (dependencyComparator instanceof OrderComparator ?
				(OrderComparator) dependencyComparator : OrderComparator.INSTANCE);
		return comparator.withSourceProvider(createFactoryAwareOrderSourceProvider(matchingBeans));
	}

	private OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
		IdentityHashMap<Object, String> instancesToBeanNames = new IdentityHashMap<>();
		beans.forEach((beanName, instance) -> instancesToBeanNames.put(instance, beanName));
		return new FactoryAwareOrderSourceProvider(instancesToBeanNames);
	}

	/**
	 * Find bean instances that match the required type.
	 * Called during autowiring for the specified bean.
	 *
	 * 查找与所需类型匹配的 Bean 实例。在指定 Bean 的自动注入期间调用。
	 *
	 * @param beanName     the name of the bean that is about to be wired
	 * @param requiredType the actual type of bean to look for
	 *                     (may be an array component type or collection element type)
	 *
	 * 要查找的实际 Bean 类型（可能是 Array 组件类型或 Collection 元素类型）
	 *
	 * @param descriptor   the descriptor of the dependency to resolve -- 要解析的依赖项的描述符
	 * @return a Map of candidate names and candidate instances that match
	 * the required type (never {@code null}) -- 与所需类型匹配的候选名称和候选实例的映射（绝不是 {@code null}）
	 * @throws BeansException in case of errors
	 * @see #autowireByType
	 * @see #autowireConstructor
	 */
	/**
	 * <p>查找与type匹配的候选bean对象,构建成Map，key=bean名,val=Bean对象【在自动装配指定bean期间调用】:
	 *  <ol>
	 *   <li>获取requiredType的所有bean名,包括父级工厂中定义的名称【变量 candidateNames】</li>
	 *   <li>定义用于保存匹配requiredType的bean名和其实例对象的Map，即匹配成功的候选Map【变量 result】</li>
	 *   <li>【<b>从 存放着手动显示注册的依赖项类型-相应的自动装配值的缓存【{@link #resolvableDependencies}】中匹配候选</b>】:
	 *    <ol>
	 *     <li>遍历resolvableDependencies,元素classObjectEntry:
	 *      <ol>
	 *       <li>取出依赖项类型【变量 autowiringType】</li>
	 *       <li>如果autowiringType是属于requiredType的实例:
	 *        <ol>
	 *         <li>取出autowiringType对应的实例对象【变量 autowiringValue】</li>
	 *         <li>根据requiredType解析autowiringValue,并针对autowiringValue是ObjectFactory的情况进行解析,将解析出来的值
	 *         重新赋值给autowiringValue</li>
	 *         <li>如果autowiringValue是requiredType类型,就根据autowiringValue构建出唯一
	 *         ID与autowiringValue绑定到result中,然后跳槽循环</li>
	 *        </ol>
	 *       </li>
	 *      </ol>
	 *     </li>
	 *    </ol>
	 *   </li>
	 *   <li>【<b>常规匹配候选(beanDefinition是否允许依赖注入,泛型类型是否匹配,限定符注解
	 *   /限定符信息是否匹配)</b>】:
	 *    <ol>
	 *     <li>遍历candidateNames,元素candidate:
	 *      <ol>
	 *       <li>如果beanName与candidateName所对应的Bean对象不是同一个且candidate可以自动注入,
	 *       添加一个条目在result中:一个bean实例(如果可用)或仅一个已解析的类型</li>
	 *      </ol>
	 *     </li>
	 *    </ol>
	 *   </li>
	 *   <li>【<b>找不到候选时，就采用将回退模式(在回退模式下，候选Bean具有无法解析的泛型 || 候选Bean的Class
	 *   对象是Properties类对象时，都允许成为该描述符的可自动注入对象)尽可能的匹配到候选，一般情况
	 *   下不会出现回退情况,除非代码非常糟糕</b>】:
	 *    <ol>
	 *     <li>如果result为空:
	 *      <ol>
	 *       <li>requiredType是否是数组/集合类型的标记【变量multiple】</li>
	 *       <li>获取desciptord的一个旨在用于回退匹配变体【遍历 fallbackDescriptor】</li>
	 *       <li>【<b>先尝试匹配候选bean名符合允许回退匹配的依赖描述符的自动依赖条件且(依赖类型不是集合/数组
	 *       或者描述符指定限定符)的候选Bean对象</b>】:
	 *        <ol>
	 *         <li>遍历candidateNames,元素candidate:
	 *          <ol>
	 *           <li>如果beanName与candidateName所对应的Bean对象不是同一个 且 candidate可以自动注入 且
	 *           (type不是数组/集合类型或者 desciptor有@Qualifier注解或qualifier标准修饰),
	 *           就添加一个条目在result中:一个bean实例(如果可用)或仅一个已解析的类型</li>
	 *          </ol>
	 *         </li>
	 *        </ol>
	 *       </li>
	 *       <li>【<b>降低匹配精度:满足下面条件即可</b>】
	 *        <ul>
	 *         <li>除beanName符合描述符依赖类型不是数组/集合</li>
	 *         <li>如果beanName与candidateName所对应的Bean对象不是同一个</li>
	 *         <li>(descriptor不是集合依赖或者beanName与candidate不相同) </li>
	 *         <li>候选bean名符合允许回退匹配的依赖描述符的自动依赖条件 </li>
	 *        </ul>
	 *        <ol>
	 *         <li>如果result为空且requiredType不是数组/集合类型或者
	 *          <ol>
	 *           <li>遍历candidateNames,元素candidate:
	 *            <ol>
	 *             <li>如果beanName与candidateName所对应的Bean对象不是同一个 且 (descriptor不是
	 *             MultiElementDescriptor实例(即集合依赖)或者beanName不等于candidate)
	 *             且 candidate可以自动注入,添加一个条目在result中:一个bean实例(如果可用)
	 *             或仅一个已解析的类型</li>
	 *            </ol>
	 *           </li>
	 *          </ol>
	 *         </li>
	 *        </ol>
	 *       </li>
	 *      </ol>
	 *     </li>
	 *    </ol>
	 *   </li>
	 *   <li>返回匹配成功的后续Bean对象【result】</li>
	 *  </ol>
	 * </p>
	 * Find bean instances that match the required type.
	 * Called during autowiring for the specified bean.
	 * <p>查找与所需类型匹配的bean实例。在自动装配指定bean期间调用</p>
	 * @param beanName the name of the bean that is about to be wired
	 *                 -- 即将被连线的bean名，要依赖的bean名(不是指desciptor的所包装的Field/MethodParamater的依赖类型的bean名，
	 *                 			而是指需要Field/MethodParamter所对应的bean对象来构建的Bean对象的Bean名)
	 * @param requiredType the actual type of bean to look for
	 * (may be an array component type or collection element type)
	 *                     -- 要查找的bean的实际类型(可以是数组组件或集合元素类型),descriptor的依赖类型
	 * @param descriptor the descriptor of the dependency to resolve
	 *                   -- 要解析的依赖项的描述符
	 * @return a Map of candidate names and candidate instances that match
	 * the required type (never {@code null})
	 * -- 匹配所需类型的候选名称和候选实例的映射(从不为null)
	 * @throws BeansException in case of errors -- 如果有错误
	 * @see #autowireByType
	 * @see #autowireConstructor
	 */
	protected Map<String, Object> findAutowireCandidates(
			@Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {

		// 获取指定类型的BeanNames作为候选Bean
		String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				this, requiredType, true, descriptor.isEager());
		Map<String, Object> result = CollectionUtils.newLinkedHashMap(candidateNames.length);
		// 存放着手动显式注册的依赖项类型-相应的自动装配值的缓存
		// 遍历 从依赖项类型映射到相应的自动装配值缓存
		for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
			Class<?> autowiringType = classObjectEntry.getKey();
			// 如果autowiringType是属于requiredType的实例
			if (autowiringType.isAssignableFrom(requiredType)) {
				Object autowiringValue = classObjectEntry.getValue();
				// 根据requiredType解析autowiringValue,并针对autowiringValue是ObjectFactory的情况进行解析
				autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
				// 如果autowiringValue是requiredType类型
				if (requiredType.isInstance(autowiringValue)) {
					// 对象的标识，对象值
					// 根据autowiringValue构建出唯一ID与autowiringValue绑定到result中
					result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
					break;
				}
			}
		}
		// 常规匹配候选
		// 遍历candidateNames
		for (String candidate : candidateNames) {
			// 如果beanName与candidateName所对应的Bean对象不是同一个 且 candidate可以自动注入
			if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
				// 添加一个条目在result中:一个bean实例(如果可用)或仅一个已解析的类型
				addCandidateEntry(result, candidate, descriptor, requiredType);
			}
		}
		// 找不到候选时，就采用将回退模式尽可能的匹配到候选，一般情况下不会出现回退情况,除非代码非常糟糕
		// result为空
		if (result.isEmpty()) {
			// requiredType是否是数组/集合类型的标记
			boolean multiple = indicatesMultipleBeans(requiredType);
			// Consider fallback matches if the first pass failed to find anything...
			// 如果第一遍未找到任何内容，请考虑进行回退匹配
			// 在允许回退的情况下，候选Bean具有无法解析的泛型 || 候选Bean的Class对象是Properties类对象时，
			//   都允许成为 该描述符的可自动注入对象
			// 获取desciptord的一个旨在用于回退匹配变体
			DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
			// 先尝试匹配候选bean名符合允许回退匹配的依赖描述符的自动依赖条件且(依赖类型不是集合/数组或者描述符指定限定符)的候选Bean对象
			// 遍历candidateNames
			for (String candidate : candidateNames) {
				if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) &&
						(!multiple || getAutowireCandidateResolver().hasQualifier(descriptor))) {
					addCandidateEntry(result, candidate, descriptor, requiredType);
				}
			}
			if (result.isEmpty() && !multiple) {
				// Consider self references as a final pass...
				// but in the case of a dependency collection, not the very same bean itself.
				for (String candidate : candidateNames) {
					if (isSelfReference(beanName, candidate) &&
							(!(descriptor instanceof MultiElementDescriptor) || !beanName.equals(candidate)) &&
							isAutowireCandidate(candidate, fallbackDescriptor)) {
						addCandidateEntry(result, candidate, descriptor, requiredType);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Add an entry to the candidate map: a bean instance if available or just the resolved
	 * type, preventing early bean initialization ahead of primary candidate selection.
	 * <p>
	 * 在候选映射中添加一个条目：Bean 实例（如果可用）或仅解析的类型，从而防止在选择主候选对象之前提前初始化 Bean。
	 */
	/**
	 * <p>
	 *  在候选映射中添加一个条目:一个bean实例(如果可用)或仅一个已解析的类型，以防止在选择主要
	 *  候选对象之前太早初始化bean:
	 *  <ol>
	 *   <li>如果desciprtor是MultiElementDescriptor的实例【集合类型依赖】:
	 *    <ol>
	 *     <li>获取candidateName的该工厂的Bean实例【变量 beanInstance】</li>
	 *     <li>如果beanInstance不是NullBean实例,将candidateName和其对应的实例绑定到candidates中</li>
	 *    </ol>
	 *   </li>
	 *   <li>如果beanName是在该BeanFactory的单例对象的高速缓存Map集合中 或者 (descriptor是SteamDependencyDesciptor实例【Stream类型依赖】且
	 *   该实例有排序标记):
	 *    <ol>
	 *     <li>获取candidateName的该工厂的Bean实例【变量 beanInstance】</li>
	 *     <li>如果beanInstance是NullBean实例,会将candidateName和null绑定到candidates中；否则将candidateName和其对应的实例绑定到candidates中</li>
	 *    </ol>
	 *   </li>
	 *   <li>否则(一般就是指candidatName所对应的bean不是单例):将candidateName和其对应的Class对象绑定到candidates中</li>
	 *  </ol>
	 * </p>
	 * Add an entry to the candidate map: a bean instance if available or just the resolved
	 * type, preventing early bean initialization ahead of primary candidate selection.
	 * <p>在候选映射中添加一个条目:一个bean实例(如果可用)或仅一个已解析的类型，以防止在选择主要
	 * 候选对象之前太早初始化bean</p>
	 */
	private void addCandidateEntry(Map<String, Object> candidates, String candidateName,
								   DependencyDescriptor descriptor, Class<?> requiredType) {
		// MultiElementDesciptor:具有嵌套元素的多元素声明的依赖描述符，表示集合/数组依赖
		// 如果desciprtor是MultiElementDescriptor的实例
		if (descriptor instanceof MultiElementDescriptor) {
			// 获取candidateName的该工厂的Bean实例
			Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
			if (!(beanInstance instanceof NullBean)) {
				// 将candidateName和其对应的实例绑定到candidates中
				candidates.put(candidateName, beanInstance);
			}
		} else if (containsSingleton(candidateName) || (descriptor instanceof StreamDependencyDescriptor &&
				((StreamDependencyDescriptor) descriptor).isOrdered())) {
			// StreamDependencyDescriptor:用于访问多个元素的流依赖项描述符标记，即属性依赖是 stream类型
			// 如果beanName是在该BeanFactory的单例对象的高速缓存Map集合中 或者 (descriptor是SteamDependencyDesciptor实例 且 该实例有排序标记)
			Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
			// 如果beanInstance是NullBean实例,会将candidateName和null绑定到candidates中；否则将candidateName和其对应的实例绑定到candidates中
			candidates.put(candidateName, (beanInstance instanceof NullBean ? null : beanInstance));
		} else {
			// candidateName所对应的bean不是单例
			// 将candidateName和其对应的Class对象绑定到candidates中
			candidates.put(candidateName, getType(candidateName));
		}
	}

	/**
	 * Determine the autowire candidate in the given set of beans.
	 * <p>Looks for {@code @Primary} and {@code @Priority} (in that order).
	 * 确定给定 bean 集中的自动装配候选者。
	 * 查找 @Primary 和 @Priority（按此顺序）。
	 *
	 * @param candidates a Map of candidate names and candidate instances
	 *                   that match the required type, as returned by {@link #findAutowireCandidates}
	 *                   -- 一个 Map 的候选名称和候选实例, 匹配所需类型，如 {@link #findAutowireCandidates} 返回的
	 * @param descriptor the target dependency to match against -- 要匹配的目标依赖项
	 * @return the name of the autowire candidate, or {@code null} if none found
	 * 自动装配候选者的名称，如果未找到则返回 {@code null}
	 */
	@Nullable
	protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
		// 获取descriptor的依赖类型
		Class<?> requiredType = descriptor.getDependencyType();
		// 在candidates中确定primary候选Bean名称
		String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
		if (primaryCandidate != null) {
			return primaryCandidate;
		}
		// 确定candidates中具有Priority注解最高优先级的候选Bean名称
		String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
		if (priorityCandidate != null) {
			return priorityCandidate;
		}
		// Fallback -- 回退
		// 遍历候选Bean对象Map
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			String candidateName = entry.getKey();
			Object beanInstance = entry.getValue();
			// 如果(beanInstance不为null 且 存放着手动显示注册的依赖项类型-相应的自动装配值的缓存 中包含该bean对象)
			// 或者 descriptor包装的参数/字段的名称与candidateName或此candidateName的BeanDefinition中存储的别名匹配
			if ((beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) ||
					matchesBeanName(candidateName, descriptor.getDependencyName())) {
				return candidateName;
			}
		}
		return null;
	}

	/**
	 * Determine the primary candidate in the given set of beans.
	 *
	 * @param candidates   a Map of candidate names and candidate instances
	 *                     (or candidate classes if not created yet) that match the required type
	 * @param requiredType the target dependency type to match against
	 * @return the name of the primary candidate, or {@code null} if none found
	 * @see #isPrimary(String, Object)
	 */
	/**
	 * <p>在candidates中确定primary候选Bean名:
	 *  <ol>
	 *   <li>定义用于存储primary bean名的变量【变量 primaryBeanName】</li>
	 *   <li>遍历候选bean对象Map:
	 *    <ol>
	 *     <li>获取元素的候选bean名【变量 candidateBeanName】</li>
	 *     <li>获取元素的候选bean对象【变量 beanInstance】</li>
	 *     <li>如果candidateBeanName的beanDefinition已标记为primary bean:
	 *      <ol>
	 *       <li>如果primaryBeanName不为null
	 *        <ol>
	 *         <li>本地工厂中BeanDefinition对象映射【beanDefinitionMap】中是否存在
	 *         candidateBeanName该键名，将结果赋值给【变量 candidateLocal】</li>
	 *         <li>本地工厂中BeanDefinition对象映射【beanDefinitionMap】中是否存在
	 *         primaryBeanName该键名，将结果赋值给【变量 primaryLocal】</li>
	 *         <li>如果candiateLocalh和primaryLocal都为true,表示candidateBeanName和primaryBeanName
	 *         都存在于本地工厂中BeanDefinition对象映射【beanDefinitionMap】中，就抛出没有唯一BeanDefinition异常：
	 *         在候选Bean对象Map中发现一个以上的"primary" bean
	 *         </li>
	 *         <li>如果只是candidateLocal为true，表示只是candidateBeanName存在于本地工厂中BeanDefinition对
	 *         象映射【beanDefinitionMap】中,就让primaryBeanName引用candidateBeanName</li>
	 *        </ol>
	 *       </li>
	 *       <li>否则，让primaryBeanName直接引用candidateBeanName</li>
	 *      </ol>
	 *     </li>
	 *    </ol>
	 *   </li>
	 *   <li>返回primary bean名【primaryBeanName】</li>
	 *  </ol>
	 * </p>
	 * Determine the primary candidate in the given set of beans.
	 * <p>在给定的bean集中确定primary候选对象</p>
	 * @param candidates a Map of candidate names and candidate instances
	 * (or candidate classes if not created yet) that match the required type
	 *   -- 匹配所需类型的候选名称和候选实例(或候选类，如果尚未创建,即PROTOTYPE级别的Bean) 的映射
	 * @param requiredType the target dependency type to match against
	 *                     -- 要匹配的目标依赖类型
	 * @return the name of the primary candidate, or {@code null} if none found
	 *   -- primary候选Bean名，如果找不到则为null
	 * @see #isPrimary(String, Object)
	 * 获取candidates中标记了primary的BeanName
	 */
	@Nullable
	protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
		String primaryBeanName = null;
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			String candidateBeanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (isPrimary(candidateBeanName, beanInstance)) {
				if (primaryBeanName != null) {
					// 本地工厂中BeanDefinition对象映射【beanDefinitionMap】中是否存在candidateBeanName该键名，将结果
					// 赋值给candidateLocal
					boolean candidateLocal = containsBeanDefinition(candidateBeanName);
					// 本地工厂中BeanDefinition对象映射【beanDefinitionMap】中是否存在primaryBeanName该键名，将结果
					// 赋值给primaryLocal
					boolean primaryLocal = containsBeanDefinition(primaryBeanName);
					if (candidateLocal && primaryLocal) {
						throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
								"more than one 'primary' bean found among candidates: " + candidates.keySet());
					} else if (candidateLocal) {
						primaryBeanName = candidateBeanName;
					}
				} else {
					primaryBeanName = candidateBeanName;
				}
			}
		}
		return primaryBeanName;
	}

	/**
	 * Determine the candidate with the highest priority in the given set of beans.
	 * <p>Based on {@code @javax.annotation.Priority}. As defined by the related
	 * {@link org.springframework.core.Ordered} interface, the lowest value has
	 * the highest priority.
	 *
	 * @param candidates   a Map of candidate names and candidate instances
	 *                     (or candidate classes if not created yet) that match the required type
	 * @param requiredType the target dependency type to match against
	 * @return the name of the candidate with the highest priority,
	 * or {@code null} if none found
	 * @see #getPriority(Object)
	 */
	/**
	 * <p>确定candidates中具有Priority注解最高优先级的候选Bean名：
	 *  <ol>
	 *   <li>定义用于保存最高优先级bean名的变量【变量 highestPriorityBeanName】</li>
	 *   <li>定义用于保存最搞优先级值的变量【变量 highestPriority】</li>
	 *   <li>遍历候选bean对象Map:
	 *    <ol>
	 *     <li>获取元素的候选bean名【变量 candidateBeanName】</li>
	 *     <li>获取元素的候选bean对象【变量 beanInstance】</li>
	 *     <li>如果beanInstance不为null
	 *      <ol>
	 *       <li>获取Priority注解为beanInstance分配的优先级值【变量 candidatePriority】</li>
	 *       <li>如果候选Bean对象的优先级值不为null:
	 *        <ol>
	 *         <li>如果最高优先级bean名不为null:
	 *          <ol>
	 *           <li>如果候选优先级值与最高优先级级值相同,抛出没有唯一BeanDefinition异常【{@link NoUniqueBeanDefinitionException}】</li>
	 *           <li>如果后续优先级值小于最高优先级值：
	 *            <ol>
	 *             <li>让highestPriorityBeanName引用candidateBeanName</li>
	 *             <li>让highestPriority引用candidatePriority</li>
	 *            </ol>
	 *           </li>
	 *          </ol>
	 *         </li>
	 *        </ol>
	 *       </li>
	 *       <li>否则：
	 *        <ol>
	 *         <li>让highestPriorityBeanName引用candidateBeanName</li>
	 *         <li>让highestPriority引用candidatePriority</li>
	 *        </ol>
	 *       </li>
	 *      </ol>
	 *     </li>
	 *    </ol>
	 *   </li>
	 *   <li>返回最高优先级bean名【highestPriorityBeanName】</li>
	 *  </ol>
	 * </p>
	 * Determine the candidate with the highest priority in the given set of beans.
	 * <p>确定给定bean组中具有最高优先级的候选对象</p>
	 * <p>Based on {@code @javax.annotation.Priority}. As defined by the related
	 * {@link org.springframework.core.Ordered} interface, the lowest value has
	 * the highest priority.
	 * <p>基于{@code javax.annotation.Priority}.如相关org.springframwwork.core.Ordered接口
	 * 定义，最低优先级值具有最高优先级</p>
	 * @param candidates a Map of candidate names and candidate instances
	 * (or candidate classes if not created yet) that match the required type
	 *  -- 匹配所需类型的候选名称和候选实例(后候选类，如果尚未创建,即PROTOTYPE级别的Bean)的映射
	 * @param requiredType the target dependency type to match against
	 *                     -- 要匹配的目标依赖项类型
	 * @return the name of the candidate with the highest priority,
	 * or {@code null} if none found
	 *  -- 优先级最高候选Bean名；如果找不到，则为null
	 * @see #getPriority(Object)
	 */
	@Nullable
	protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
		// 定义用于保存最高优先级bean名的变量
		String highestPriorityBeanName = null;
		// 定义用于保存最搞优先级值的变量
		Integer highestPriority = null;
		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
			String candidateBeanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (beanInstance != null) {
				// 获取Priority注解为beanInstance分配的优先级值
				Integer candidatePriority = getPriority(beanInstance);
				if (candidatePriority != null) {
					if (highestPriorityBeanName != null) {
						if (candidatePriority.equals(highestPriority)) {
							throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
									"Multiple beans found with the same priority ('" + highestPriority +
											"') among candidates: " + candidates.keySet());
						} else if (candidatePriority < highestPriority) {
							highestPriorityBeanName = candidateBeanName;
							highestPriority = candidatePriority;
						}
					} else {
						highestPriorityBeanName = candidateBeanName;
						highestPriority = candidatePriority;
					}
				}
			}
		}
		return highestPriorityBeanName;
	}

	/**
	 * Return whether the bean definition for the given bean name has been
	 * marked as a primary bean.
	 * <p>返回给定bean名的beanDefinition是否已标记为primary bean</p>
	 * <ol>
	 *  <li>去除beanName开头的'&'字符,获取name最终的规范名称【最终别名或者是全类名】【变量 transformedBeanName】</li>
	 *  <li>如果Beand定义对象映射【beanDefinitionMap】中存在beanName该键名:
	 *   <ol>
	 *    <li>获取beanName合并后的本地RootBeanDefintiond,以判断是否为自动装配的 primary 候选对
	 *    象并将结果返回出去</li>
	 *   </ol>
	 *  </li>
	 *  <li>获取父工厂【变量 parent】</li>
	 *  <li>如果父工厂为DefaultListableBeanFactory,则使用父工厂递归该方法进行判断transformedBeanName
	 *  的beanDefinition是否已标记为primary bean并将结果返回出去。</li>
	 * </ol>
	 * @param beanName the name of the bean -- bean名
	 * @param beanInstance the corresponding bean instance (can be null)
	 *                      -- 相应的bean实例(可以为null)
	 * @return whether the given bean qualifies as primary
	 *   -- 给定的bean是否符合primary
	 *
	 * 校验bean定义是否有primary标记
	 */
	protected boolean isPrimary(String beanName, Object beanInstance) {
		String transformedBeanName = transformedBeanName(beanName);
		if (containsBeanDefinition(transformedBeanName)) {
			// 获取beanName合并后的本地RootBeanDefintiond,以判断是否为自动装配的 primary 候选对象
			// 并将结果返回出去
			return getMergedLocalBeanDefinition(transformedBeanName).isPrimary();
		}
		BeanFactory parent = getParentBeanFactory();
		return (parent instanceof DefaultListableBeanFactory &&
				((DefaultListableBeanFactory) parent).isPrimary(transformedBeanName, beanInstance));
	}

	/**
	 * Return the priority assigned for the given bean instance by
	 * the {@code javax.annotation.Priority} annotation.
	 * <p>The default implementation delegates to the specified
	 * {@link #setDependencyComparator dependency comparator}, checking its
	 * {@link OrderComparator#getPriority method} if it is an extension of
	 * Spring's common {@link OrderComparator} - typically, an
	 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator}.
	 * If no such comparator is present, this implementation returns {@code null}.
	 *
	 * @param beanInstance the bean instance to check (can be {@code null})
	 * @return the priority assigned to that bean or {@code null} if none is set
	 */
	@Nullable
	protected Integer getPriority(Object beanInstance) {
		Comparator<Object> comparator = getDependencyComparator();
		if (comparator instanceof OrderComparator) {
			return ((OrderComparator) comparator).getPriority(beanInstance);
		}
		return null;
	}

	/**
	 * Determine whether the given candidate name matches the bean name or the aliases
	 * stored in this bean definition.
	 */
	protected boolean matchesBeanName(String beanName, @Nullable String candidateName) {
		return (candidateName != null &&
				(candidateName.equals(beanName) || ObjectUtils.containsElement(getAliases(beanName), candidateName)));
	}

	/**
	 * Determine whether the given beanName/candidateName pair indicates a self reference,
	 * i.e. whether the candidate points back to the original bean or to a factory method
	 * on the original bean.
	 *
	 * 确定给定的 beanName/candidateName 对是否表示自引用，即候选项是指向原始 Bean 还是指向原始 Bean 上的工厂方法。
	 */
	/**

	 *
	 * Determine whether the given beanName/candidateName pair indicates a self reference,
	 * i.e. whether the candidate points back to the original bean or to a factory method
	 * on the original bean.
	 *
	 * <p>确定给定beanName/candidateName Pair 是否表示自引用,即候选对象是指向原始bean
	 * 或者指向原始bean的工厂方法</p>
	 *
	 * <p>可以理解为beanName与candidateName所对应的Bean对象是不是同一个</p>
	 * <p>自引用：beanName和candidateName是否都是指向同一个Bean对象，至少beanName所指bean对象是candidateName的合并后
	 * RootBeanDefinition对象里的FactoryBean对象</p>
	 */
	private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
		return (beanName != null && candidateName != null &&
				(beanName.equals(candidateName) || (containsBeanDefinition(candidateName) &&
						beanName.equals(getMergedLocalBeanDefinition(candidateName).getFactoryBeanName()))));
	}

	/**
	 * Raise a NoSuchBeanDefinitionException or BeanNotOfRequiredTypeException
	 * for an unresolvable dependency.
	 * 为无法解析的依赖项引发 NoSuchBeanDefinitionException 或 BeanNotOfRequiredTypeException。
	 */
	private void raiseNoMatchingBeanFound(
			Class<?> type, ResolvableType resolvableType, DependencyDescriptor descriptor) throws BeansException {

		checkBeanNotOfRequiredType(type, descriptor);

		throw new NoSuchBeanDefinitionException(resolvableType,
				"expected at least 1 bean which qualifies as autowire candidate. " +
						"Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
	}

	/**
	 * Raise a BeanNotOfRequiredTypeException for an unresolvable dependency, if applicable,
	 * i.e. if the target type of the bean would match but an exposed proxy doesn't.
	 * 如果适用，则为无法解析的依赖项引发 BeanNotOfRequiredTypeException，即如果 bean 的目标类型匹配但暴露的代理不匹配。
	 */
	/**
	 * 检查Bean是否属于Type类型，如果不是抛出BeanNotOfRequiredTypeException
	 * <ol>
	 *  <li>遍历Bean定义名称列表【{@link #beanDefinitionNames}】【变量 beanName】:
	 *   <ol>
	 *    <li>获取beanName对应的合并后BeanDefinition【变量 mbd】</li>
	 *    <li>获取mbd的目标类型【变量 targetType】</li>
	 *    <li>如果目标类型不为null，且 目标类型属于descriptor的依赖类型 且 mbd可以自动注入到descriptor
	 *    所包装的field/methodParam中:
	 *     <ol>
	 *      <li>获取以beanName注册的(原始)单例Bean对象【变量 beanInstance】</li>
	 *      <li>如果beanInstance不为null且不是NUllBean.class，则获取beanInstance的Class对象；否则预测
	 *      beanName,mbd所提供的信息的最终bean类型</li>
	 *      <li>如果beanType不为null且beanType不是descriptord的依赖类型,抛出：
	 *      Bean不是必需的类型异常【{@link BeanNotOfRequiredTypeException}】</li>
	 *     </ol>
	 *    </li>
	 *   </ol>
	 *  </li>
	 *  <li>获取父工厂</li>
	 *  <li>如果父工厂是DefaultListableBeanFactory的实例，递归调用父工厂的该方法进行检查</li>
	 * </ol>
	 * Raise a BeanNotOfRequiredTypeException for an unresolvable dependency, if applicable,
	 * i.e. if the target type of the bean would match but an exposed proxy doesn't.
	 * <p>抛出BeanNotOfRequiredTypeException以解决不可解决的依赖关系(如果适用),即，如果Bean的
	 * 目标类型匹配，但公开的代理不匹配。</p>
	 * @param type 	descriptor的依赖类型
	 * @param descriptor  descriptor
	 */
	private void checkBeanNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
		for (String beanName : this.beanDefinitionNames) {
			try {
				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				Class<?> targetType = mbd.getTargetType();
				//如果目标类型不为null，且 目标类型属于descriptor的依赖类型 且 mbd可以自动注入到descriptor所包装的field/methodParam中
				if (targetType != null && type.isAssignableFrom(targetType) &&
						isAutowireCandidate(beanName, mbd, descriptor, getAutowireCandidateResolver())) {
					// Probably a proxy interfering with target type match -> throw meaningful exception.
					// 可能是干扰目标类型匹配的代理->抛出有意义的异常
					// 获取以beanName注册的(原始)单例Bean对象
					Object beanInstance = getSingleton(beanName, false);
					Class<?> beanType = (beanInstance != null && beanInstance.getClass() != NullBean.class ?
							beanInstance.getClass() : predictBeanType(beanName, mbd));
					if (beanType != null && !type.isAssignableFrom(beanType)) {
						throw new BeanNotOfRequiredTypeException(beanName, type, beanType);
					}
				}
			} catch (NoSuchBeanDefinitionException ex) {
				// Bean definition got removed while we were iterating -> ignore.
			}
		}

		BeanFactory parent = getParentBeanFactory();
		if (parent instanceof DefaultListableBeanFactory) {
			((DefaultListableBeanFactory) parent).checkBeanNotOfRequiredType(type, descriptor);
		}
	}

	/**
	 * Create an {@link Optional} wrapper for the specified dependency.
	 * 为指定的依赖项创建一个 {@link Optional} 包装器。
	 */
	/**
	 * <p>创建Optional类型的符合descriptor要求的候选Bean对象:
	 *  <ol>
	 *   <li>新建一个NestedDependencyDescriptor实例，,该实例不要求一定要得到候选Bean对象，且可根据arg构建候选Bean对象且
	 *   	可根据arg构建候选Bean对象(仅在Bean是{@link #SCOPE_PROTOTYPE}时)。【变量 descriptorToUse】</li>
	 *   <li>解析出与descriptor所包装的对象匹配的后续Bean对象【变量 result】</li>
	 *   <li>如果result是Optional的实例,就将其强转为Optional后返回出去；否则将result包装到Optional对象中再返回出去</li>
	 *  </ol>
	 * </p>
	 * Create an {@link Optional} wrapper for the specified dependency.
	 * <p>为指定的依赖关系创建一个{@link Optional}包装器</p>
	 *
	 * @param descriptor   依赖项的描述符(字段/方法/构造函数)
	 * @param beanName  要依赖的Bean名,即需要Field/MethodParamter所对应的bean对象来构建的Bean对象的Bean名
	 * @param args 创建候选Bean对象所需的构造函数参数(仅在Bean是{@link #SCOPE_PROTOTYPE}时)
	 * @return Optional类型的符合descriptor要求的候选Bean对象,不会为null
	 */
	private Optional<?> createOptionalDependency(
			DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {

		DependencyDescriptor descriptorToUse = new NestedDependencyDescriptor(descriptor) {
			@Override
			public boolean isRequired() {
				return false;
			}

			@Override
			public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
				return (!ObjectUtils.isEmpty(args) ? beanFactory.getBean(beanName, args) :
						super.resolveCandidate(beanName, requiredType, beanFactory));
			}
		};
		Object result = doResolveDependency(descriptorToUse, beanName, null, null);
		return (result instanceof Optional ? (Optional<?>) result : Optional.ofNullable(result));
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(ObjectUtils.identityToString(this));
		sb.append(": defining beans [");
		sb.append(StringUtils.collectionToCommaDelimitedString(this.beanDefinitionNames));
		sb.append("]; ");
		BeanFactory parent = getParentBeanFactory();
		if (parent == null) {
			sb.append("root of factory hierarchy");
		} else {
			sb.append("parent: ").append(ObjectUtils.identityToString(parent));
		}
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		throw new NotSerializableException("DefaultListableBeanFactory itself is not deserializable - " +
				"just a SerializedBeanFactoryReference is");
	}

	protected Object writeReplace() throws ObjectStreamException {
		if (this.serializationId != null) {
			return new SerializedBeanFactoryReference(this.serializationId);
		} else {
			throw new NotSerializableException("DefaultListableBeanFactory has no serialization id");
		}
	}


	/**
	 * Minimal id reference to the factory.
	 * Resolved to the actual factory instance on deserialization.
	 */
	private static class SerializedBeanFactoryReference implements Serializable {

		private final String id;

		public SerializedBeanFactoryReference(String id) {
			this.id = id;
		}

		private Object readResolve() {
			Reference<?> ref = serializableFactories.get(this.id);
			if (ref != null) {
				Object result = ref.get();
				if (result != null) {
					return result;
				}
			}
			// Lenient fallback: dummy factory in case of original factory not found...
			DefaultListableBeanFactory dummyFactory = new DefaultListableBeanFactory();
			dummyFactory.serializationId = this.id;
			return dummyFactory;
		}
	}


	/**
	 * A dependency descriptor marker for nested elements.
	 */
	private static class NestedDependencyDescriptor extends DependencyDescriptor {

		public NestedDependencyDescriptor(DependencyDescriptor original) {
			super(original);
			increaseNestingLevel();
		}
	}


	/**
	 * A dependency descriptor for a multi-element declaration with nested elements.
	 * 具有嵌套元素的多元素声明的依赖关系描述符。
	 */
	private static class MultiElementDescriptor extends NestedDependencyDescriptor {

		public MultiElementDescriptor(DependencyDescriptor original) {
			super(original);
		}
	}


	/**
	 * A dependency descriptor marker for stream access to multiple elements.
	 */
	private static class StreamDependencyDescriptor extends DependencyDescriptor {

		private final boolean ordered;

		public StreamDependencyDescriptor(DependencyDescriptor original, boolean ordered) {
			super(original);
			this.ordered = ordered;
		}

		public boolean isOrdered() {
			return this.ordered;
		}
	}


	private interface BeanObjectProvider<T> extends ObjectProvider<T>, Serializable {
	}


	/**
	 * Serializable ObjectFactory/ObjectProvider for lazy resolution of a dependency.
	 */
	private class DependencyObjectProvider implements BeanObjectProvider<Object> {

		private final DependencyDescriptor descriptor;

		private final boolean optional;

		@Nullable
		private final String beanName;

		public DependencyObjectProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
			this.descriptor = new NestedDependencyDescriptor(descriptor);
			this.optional = (this.descriptor.getDependencyType() == Optional.class);
			this.beanName = beanName;
		}

		@Override
		public Object getObject() throws BeansException {
			if (this.optional) {
				return createOptionalDependency(this.descriptor, this.beanName);
			} else {
				Object result = doResolveDependency(this.descriptor, this.beanName, null, null);
				if (result == null) {
					throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
				}
				return result;
			}
		}

		@Override
		public Object getObject(final Object... args) throws BeansException {
			if (this.optional) {
				return createOptionalDependency(this.descriptor, this.beanName, args);
			} else {
				DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
					@Override
					public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
						return beanFactory.getBean(beanName, args);
					}
				};
				Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
				if (result == null) {
					throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
				}
				return result;
			}
		}

		@Override
		@Nullable
		public Object getIfAvailable() throws BeansException {
			try {
				if (this.optional) {
					return createOptionalDependency(this.descriptor, this.beanName);
				} else {
					DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
						@Override
						public boolean isRequired() {
							return false;
						}
					};
					return doResolveDependency(descriptorToUse, this.beanName, null, null);
				}
			} catch (ScopeNotActiveException ex) {
				// Ignore resolved bean in non-active scope
				return null;
			}
		}

		@Override
		public void ifAvailable(Consumer<Object> dependencyConsumer) throws BeansException {
			Object dependency = getIfAvailable();
			if (dependency != null) {
				try {
					dependencyConsumer.accept(dependency);
				} catch (ScopeNotActiveException ex) {
					// Ignore resolved bean in non-active scope, even on scoped proxy invocation
				}
			}
		}

		@Override
		@Nullable
		public Object getIfUnique() throws BeansException {
			DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
				@Override
				public boolean isRequired() {
					return false;
				}

				@Override
				@Nullable
				public Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingBeans) {
					return null;
				}
			};
			try {
				if (this.optional) {
					return createOptionalDependency(descriptorToUse, this.beanName);
				} else {
					return doResolveDependency(descriptorToUse, this.beanName, null, null);
				}
			} catch (ScopeNotActiveException ex) {
				// Ignore resolved bean in non-active scope
				return null;
			}
		}

		@Override
		public void ifUnique(Consumer<Object> dependencyConsumer) throws BeansException {
			Object dependency = getIfUnique();
			if (dependency != null) {
				try {
					dependencyConsumer.accept(dependency);
				} catch (ScopeNotActiveException ex) {
					// Ignore resolved bean in non-active scope, even on scoped proxy invocation
				}
			}
		}

		@Nullable
		protected Object getValue() throws BeansException {
			if (this.optional) {
				return createOptionalDependency(this.descriptor, this.beanName);
			} else {
				return doResolveDependency(this.descriptor, this.beanName, null, null);
			}
		}

		@Override
		public Stream<Object> stream() {
			return resolveStream(false);
		}

		@Override
		public Stream<Object> orderedStream() {
			return resolveStream(true);
		}

		@SuppressWarnings("unchecked")
		private Stream<Object> resolveStream(boolean ordered) {
			DependencyDescriptor descriptorToUse = new StreamDependencyDescriptor(this.descriptor, ordered);
			Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
			return (result instanceof Stream ? (Stream<Object>) result : Stream.of(result));
		}
	}


	/**
	 * Separate inner class for avoiding a hard dependency on the {@code javax.inject} API.
	 * Actual {@code javax.inject.Provider} implementation is nested here in order to make it
	 * invisible for Graal's introspection of DefaultListableBeanFactory's nested classes.
	 */
	private class Jsr330Factory implements Serializable {

		public Object createDependencyProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
			return new Jsr330Provider(descriptor, beanName);
		}

		private class Jsr330Provider extends DependencyObjectProvider implements Provider<Object> {

			public Jsr330Provider(DependencyDescriptor descriptor, @Nullable String beanName) {
				super(descriptor, beanName);
			}

			@Override
			@Nullable
			public Object get() throws BeansException {
				return getValue();
			}
		}
	}


	/**
	 * An {@link org.springframework.core.OrderComparator.OrderSourceProvider} implementation
	 * that is aware of the bean metadata of the instances to sort.
	 * <p>Lookup for the method factory of an instance to sort, if any, and let the
	 * comparator retrieve the {@link org.springframework.core.annotation.Order}
	 * value defined on it. This essentially allows for the following construct:
	 */
	private class FactoryAwareOrderSourceProvider implements OrderComparator.OrderSourceProvider {

		private final Map<Object, String> instancesToBeanNames;

		public FactoryAwareOrderSourceProvider(Map<Object, String> instancesToBeanNames) {
			this.instancesToBeanNames = instancesToBeanNames;
		}

		@Override
		@Nullable
		public Object getOrderSource(Object obj) {
			String beanName = this.instancesToBeanNames.get(obj);
			if (beanName == null) {
				return null;
			}
			try {
				RootBeanDefinition beanDefinition = (RootBeanDefinition) getMergedBeanDefinition(beanName);
				List<Object> sources = new ArrayList<>(2);
				Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
				if (factoryMethod != null) {
					sources.add(factoryMethod);
				}
				Class<?> targetType = beanDefinition.getTargetType();
				if (targetType != null && targetType != obj.getClass()) {
					sources.add(targetType);
				}
				return sources.toArray();
			} catch (NoSuchBeanDefinitionException ex) {
				return null;
			}
		}
	}

}
