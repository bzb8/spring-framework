/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.annotation;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceRef;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jndi.support.SimpleJndiBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that supports common Java annotations out of the box, in particular the JSR-250
 * annotations in the {@code javax.annotation} package. These common Java
 * annotations are supported in many Java EE 5 technologies (e.g. JSF 1.2),
 * as well as in Java 6's JAX-WS.
 * --
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} 实现，支持开箱即用的常见 Java 注解，
 * 特别是 {@code javax.annotation} 包中的 JSR-250 注释。
 * 许多 Java EE 5 技术（例如 JSF 1.2）以及 Java 6 的 JAX-WS 都支持这些常见的 Java 注释。
 *
 * <p>This post-processor includes support for the {@link javax.annotation.PostConstruct}
 * and {@link javax.annotation.PreDestroy} annotations - as init annotation
 * and destroy annotation, respectively - through inheriting from
 * {@link InitDestroyAnnotationBeanPostProcessor} with pre-configured annotation types.
 * --
 * 该后置处理器支持{@link javax.annotation.PostConstruct}和{@link javax.annotation.PreDestroy}注解，
 * 分别作为初始化注解和销毁注解，通过继承{@link InitDestroyAnnotationBeanPostProcessor}并预配置注解类型来实现相应的功能。
 *
 * <p>The central element is the {@link javax.annotation.Resource} annotation
 * for annotation-driven injection of named beans, by default from the containing
 * Spring BeanFactory, with only {@code mappedName} references resolved in JNDI.
 * The {@link #setAlwaysUseJndiLookup "alwaysUseJndiLookup" flag} enforces JNDI lookups
 * equivalent to standard Java EE 5 resource injection for {@code name} references
 * and default names as well. The target beans can be simple POJOs, with no special
 * requirements other than the type having to match.
 * --
 * 该注解的中心元素是{@link javax.annotation.Resource}注解，用于注解驱动的命名bean的注入，默认情况下从包含的Spring BeanFactory中注入，
 * 仅在JNDI中解析{@code mappedName}引用。{@link #setAlwaysUseJndiLookup "alwaysUseJndiLookup"标志}强制执行JNDI查找，
 * 相当于标准Java EE 5资源注入{@code name}引用和默认名称。目标bean可以是简单的POJO，除了类型必须匹配之外，没有其他特殊要求。
 *
 * <p>The JAX-WS {@link javax.xml.ws.WebServiceRef} annotation is supported too,
 * analogous to {@link javax.annotation.Resource} but with the capability of creating
 * specific JAX-WS service endpoints. This may either point to an explicitly defined
 * resource by name or operate on a locally specified JAX-WS service class. Finally,
 * this post-processor also supports the EJB 3 {@link javax.ejb.EJB} annotation,
 * analogous to {@link javax.annotation.Resource} as well, with the capability to
 * specify both a local bean name and a global JNDI name for fallback retrieval.
 * The target beans can be plain POJOs as well as EJB 3 Session Beans in this case.
 * -
 * JAX-WS {@link javax.xml.ws.WebServiceRef} 注解也受支持，类似于 {@link javax.annotation.Resource}，但能够创建特定的 JAX-WS 服务端点。
 * 这可以指向按名称显式定义的资源，也可以对本地指定的 JAX-WS 服务类进行操作。最后，这个后处理器还支持 EJB 3 {@link javax.ejb.EJB} 注解，
 * 类似于 {@link javax.annotation.Resource}，能够指定本地 Bean 名称和全局 JNDI 名称以进行回退检索。
 * 在这种情况下，目标 Bean 可以是普通 POJO，也可以是 EJB 3 会话 Bean。
 *
 * <p>The common annotations supported by this post-processor are available in
 * Java 6 (JDK 1.6) as well as in Java EE 5/6 (which provides a standalone jar for
 * its common annotations as well, allowing for use in any based application).
 * --
 * 此后处理器支持的通用注解在 Java 6 （JDK 1.6） 和 Java EE 5/6 中可用（Java EE 5/6 也为其通用注解提供了一个独立的 jar，允许在任何基于的应用程序中使用）。
 *
 * <p>For default usage, resolving resource names as Spring bean names,
 * simply define the following in your application context:
 * --
 * 对于默认用法，将资源名称解析为 Spring bean 名称，只需在应用程序上下文中定义以下内容：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor"/&gt;</pre>
 *
 * For direct JNDI access, resolving resource names as JNDI resource references
 * within the Java EE application's "java:comp/env/" namespace, use the following:
 *
 * 对于直接 JNDI 访问，将资源名称解析为 Java EE 应用程序的 “java:comp/env/” 名称空间中的 JNDI 资源引用，请使用以下命令：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="alwaysUseJndiLookup" value="true"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * {@code mappedName} references will always be resolved in JNDI,
 * allowing for global JNDI names (including "java:" prefix) as well. The
 * "alwaysUseJndiLookup" flag just affects {@code name} references and
 * default names (inferred from the field name / property name).
 *
 * {@code mappedName} 引用将始终在 JNDI 中解析，也允许全局 JNDI 名称（包括 “java:” 前缀）。“alwaysUseJndiLookup”标志仅影响 {@code name} 引用和默认名称（从字段名称属性名称推断）。
 *
 * <p><b>NOTE:</b> A default CommonAnnotationBeanPostProcessor will be registered
 * by the "context:annotation-config" and "context:component-scan" XML tags.
 * Remove or turn off the default annotation configuration there if you intend
 * to specify a custom CommonAnnotationBeanPostProcessor bean definition!
 * <p><b>NOTE:</b> Annotation injection will be performed <i>before</i> XML injection; thus
 * the latter configuration will override the former for properties wired through
 * both approaches.
 *
 * 注意：默认的 CommonAnnotationBeanPostProcessor 将由 “context：annotation-config” 和 “context：component-scan” XML 标记注册。
 * 如果您打算指定自定义 CommonAnnotationBeanPostProcessor bean 定义，请删除或关闭那里的默认注解配置！
 * 注意：注解注入将在  XML 注入之前执行;因此，对于通过这两种方法连接的属性，后一种配置将覆盖前一种配置。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see #setAlwaysUseJndiLookup
 * @see #setResourceFactory
 * @see org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
 */
@SuppressWarnings("serial")
public class CommonAnnotationBeanPostProcessor extends InitDestroyAnnotationBeanPostProcessor
		implements InstantiationAwareBeanPostProcessor, BeanFactoryAware, Serializable {

	// Defensive reference to JNDI API for JDK 9+ (optional java.naming module)
	private static final boolean jndiPresent = ClassUtils.isPresent(
			"javax.naming.InitialContext", CommonAnnotationBeanPostProcessor.class.getClassLoader());

	/**
	 * Resource、javax.xml.ws.WebServiceRef、javax.ejb.EJB 注解
	 */
	private static final Set<Class<? extends Annotation>> resourceAnnotationTypes = new LinkedHashSet<>(4);

	@Nullable
	private static final Class<? extends Annotation> webServiceRefClass;

	@Nullable
	private static final Class<? extends Annotation> ejbClass;

	static {
		resourceAnnotationTypes.add(Resource.class);

		webServiceRefClass = loadAnnotationType("javax.xml.ws.WebServiceRef");
		if (webServiceRefClass != null) {
			resourceAnnotationTypes.add(webServiceRefClass);
		}

		ejbClass = loadAnnotationType("javax.ejb.EJB");
		if (ejbClass != null) {
			resourceAnnotationTypes.add(ejbClass);
		}
	}

	/**
	 * 解析Resource注解应忽略的类型
	 */
	private final Set<String> ignoredResourceTypes = new HashSet<>(1);

	private boolean fallbackToDefaultTypeMatch = true;

	private boolean alwaysUseJndiLookup = false;

	@Nullable
	private transient BeanFactory jndiFactory;

	@Nullable
	private transient BeanFactory resourceFactory;

	@Nullable
	private transient BeanFactory beanFactory;

	@Nullable
	private transient StringValueResolver embeddedValueResolver;

	/**
	 * 缓存 bean名称或bean class name -> 查找bean class 类和父类中 字段和方法上的@Resource注解，并解析为ResourceElement，存入list的首部
	 * bean -> 它对应的@Resource注解
	 */
	private final transient Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);


	/**
	 * Create a new CommonAnnotationBeanPostProcessor,
	 * with the init and destroy annotation types set to
	 * {@link javax.annotation.PostConstruct} and {@link javax.annotation.PreDestroy},
	 * respectively.
	 * 创建一个新的 CommonAnnotationBeanPostProcessor，其中 init 和 destroy 注释类型分别设置为 @PostConstruct 和 @PreDestroy。
	 */
	public CommonAnnotationBeanPostProcessor() {
		setOrder(Ordered.LOWEST_PRECEDENCE - 3);
		setInitAnnotationType(PostConstruct.class);
		setDestroyAnnotationType(PreDestroy.class);
		ignoreResourceType("javax.xml.ws.WebServiceContext");

		// java.naming module present on JDK 9+?
		if (jndiPresent) {
			this.jndiFactory = new SimpleJndiBeanFactory();
		}
	}


	/**
	 * Ignore the given resource type when resolving {@code @Resource}
	 * annotations.
	 * <p>By default, the {@code javax.xml.ws.WebServiceContext} interface
	 * will be ignored, since it will be resolved by the JAX-WS runtime.
	 * --
	 * 解析 @Resource 注解时忽略给定的资源类型。
	 * 缺省情况下，该 javax.xml.ws.WebServiceContext 接口将被忽略，因为它将由 JAX-WS 运行时解析
	 *
	 * @param resourceType the resource type to ignore -- 要忽略的资源类型
	 */
	public void ignoreResourceType(String resourceType) {
		Assert.notNull(resourceType, "Ignored resource type must not be null");
		this.ignoredResourceTypes.add(resourceType);
	}

	/**
	 * Set whether to allow a fallback to a type match if no explicit name has been
	 * specified. The default name (i.e. the field name or bean property name) will
	 * still be checked first; if a bean of that name exists, it will be taken.
	 * However, if no bean of that name exists, a by-type resolution of the
	 * dependency will be attempted if this flag is "true".
	 * <p>Default is "true". Switch this flag to "false" in order to enforce a
	 * by-name lookup in all cases, throwing an exception in case of no name match.
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#resolveDependency
	 */
	public void setFallbackToDefaultTypeMatch(boolean fallbackToDefaultTypeMatch) {
		this.fallbackToDefaultTypeMatch = fallbackToDefaultTypeMatch;
	}

	/**
	 * Set whether to always use JNDI lookups equivalent to standard Java EE 5 resource
	 * injection, <b>even for {@code name} attributes and default names</b>.
	 * <p>Default is "false": Resource names are used for Spring bean lookups in the
	 * containing BeanFactory; only {@code mappedName} attributes point directly
	 * into JNDI. Switch this flag to "true" for enforcing Java EE style JNDI lookups
	 * in any case, even for {@code name} attributes and default names.
	 * @see #setJndiFactory
	 * @see #setResourceFactory
	 */
	public void setAlwaysUseJndiLookup(boolean alwaysUseJndiLookup) {
		this.alwaysUseJndiLookup = alwaysUseJndiLookup;
	}

	/**
	 * Specify the factory for objects to be injected into {@code @Resource} /
	 * {@code @WebServiceRef} / {@code @EJB} annotated fields and setter methods,
	 * <b>for {@code mappedName} attributes that point directly into JNDI</b>.
	 * This factory will also be used if "alwaysUseJndiLookup" is set to "true" in order
	 * to enforce JNDI lookups even for {@code name} attributes and default names.
	 * <p>The default is a {@link org.springframework.jndi.support.SimpleJndiBeanFactory}
	 * for JNDI lookup behavior equivalent to standard Java EE 5 resource injection.
	 * @see #setResourceFactory
	 * @see #setAlwaysUseJndiLookup
	 */
	public void setJndiFactory(BeanFactory jndiFactory) {
		Assert.notNull(jndiFactory, "BeanFactory must not be null");
		this.jndiFactory = jndiFactory;
	}

	/**
	 * Specify the factory for objects to be injected into {@code @Resource} /
	 * {@code @WebServiceRef} / {@code @EJB} annotated fields and setter methods,
	 * <b>for {@code name} attributes and default names</b>.
	 * <p>The default is the BeanFactory that this post-processor is defined in,
	 * if any, looking up resource names as Spring bean names. Specify the resource
	 * factory explicitly for programmatic usage of this post-processor.
	 * <p>Specifying Spring's {@link org.springframework.jndi.support.SimpleJndiBeanFactory}
	 * leads to JNDI lookup behavior equivalent to standard Java EE 5 resource injection,
	 * even for {@code name} attributes and default names. This is the same behavior
	 * that the "alwaysUseJndiLookup" flag enables.
	 * @see #setAlwaysUseJndiLookup
	 */
	public void setResourceFactory(BeanFactory resourceFactory) {
		Assert.notNull(resourceFactory, "BeanFactory must not be null");
		this.resourceFactory = resourceFactory;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		if (this.resourceFactory == null) {
			this.resourceFactory = beanFactory;
		}
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		// 查找本类和父类、接口中的所有方法上标记了@PostConstruct(添加到列表的头部)和@PreDestroy注解的方法(添加到列表的尾部)。
		super.postProcessMergedBeanDefinition(beanDefinition, beanType, beanName);
		// 查找本类和父类中 字段和方法上的@Resource注解，并解析为ResourceElement，存入list的首部
		InjectionMetadata metadata = findResourceMetadata(beanName, beanType, null);
		// 去重
		metadata.checkConfigMembers(beanDefinition);
	}

	@Override
	public void resetBeanDefinition(String beanName) {
		this.injectionMetadataCache.remove(beanName);
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) {
		return true;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		// 查找@Resource注解
		InjectionMetadata metadata = findResourceMetadata(beanName, bean.getClass(), pvs);
		try {
			// 注入解析的bean
			metadata.inject(bean, beanName, pvs);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
		}
		return pvs;
	}

	@Deprecated
	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		return postProcessProperties(pvs, bean, beanName);
	}


	private InjectionMetadata findResourceMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
		// Fall back to class name as cache key, for backwards compatibility with custom callers.
		// 回退到类名作为缓存键，以便与自定义调用方向后兼容。
		String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
		// Quick check on the concurrent map first, with minimal locking.
		InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
			synchronized (this.injectionMetadataCache) {
				metadata = this.injectionMetadataCache.get(cacheKey);
				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
					if (metadata != null) {
						metadata.clear(pvs);
					}
					metadata = buildResourceMetadata(clazz);
					this.injectionMetadataCache.put(cacheKey, metadata);
				}
			}
		}
		return metadata;
	}

	private InjectionMetadata buildResourceMetadata(Class<?> clazz) {
		if (!AnnotationUtils.isCandidateClass(clazz, resourceAnnotationTypes)) {
			return InjectionMetadata.EMPTY;
		}

		List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
		Class<?> targetClass = clazz;

		// 查找本类和父类中 字段和方法上的@Resource注解，并解析为ResourceElement，存入list的首部
		do {
			final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

			// 本类声明的所有字段数组
			ReflectionUtils.doWithLocalFields(targetClass, field -> {
				if (webServiceRefClass != null && field.isAnnotationPresent(webServiceRefClass)) {
					if (Modifier.isStatic(field.getModifiers())) {
						throw new IllegalStateException("@WebServiceRef annotation is not supported on static fields");
					}
					currElements.add(new WebServiceRefElement(field, field, null));
				}
				else if (ejbClass != null && field.isAnnotationPresent(ejbClass)) {
					if (Modifier.isStatic(field.getModifiers())) {
						throw new IllegalStateException("@EJB annotation is not supported on static fields");
					}
					currElements.add(new EjbRefElement(field, field, null));
				}
				// 解析@Resource注解为ResourceElement
				else if (field.isAnnotationPresent(Resource.class)) {
					if (Modifier.isStatic(field.getModifiers())) {
						throw new IllegalStateException("@Resource annotation is not supported on static fields");
					}
					if (!this.ignoredResourceTypes.contains(field.getType().getName())) {
						currElements.add(new ResourceElement(field, field, null));
					}
				}
			});

			// 解析本类的方法和接口上的默认方法
			ReflectionUtils.doWithLocalMethods(targetClass, method -> {
				// 查找桥接方法的原始方法
				Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
				// 返回值和参数都相同的话返回true
				if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
					return;
				}
				// 找到子类重写的方法 == 当前方法
				if (method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
					if (webServiceRefClass != null && bridgedMethod.isAnnotationPresent(webServiceRefClass)) {
						if (Modifier.isStatic(method.getModifiers())) {
							throw new IllegalStateException("@WebServiceRef annotation is not supported on static methods");
						}
						if (method.getParameterCount() != 1) {
							throw new IllegalStateException("@WebServiceRef annotation requires a single-arg method: " + method);
						}
						PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
						currElements.add(new WebServiceRefElement(method, bridgedMethod, pd));
					}
					else if (ejbClass != null && bridgedMethod.isAnnotationPresent(ejbClass)) {
						if (Modifier.isStatic(method.getModifiers())) {
							throw new IllegalStateException("@EJB annotation is not supported on static methods");
						}
						if (method.getParameterCount() != 1) {
							throw new IllegalStateException("@EJB annotation requires a single-arg method: " + method);
						}
						PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
						currElements.add(new EjbRefElement(method, bridgedMethod, pd));
					}
					else if (bridgedMethod.isAnnotationPresent(Resource.class)) {
						if (Modifier.isStatic(method.getModifiers())) {
							throw new IllegalStateException("@Resource annotation is not supported on static methods");
						}
						Class<?>[] paramTypes = method.getParameterTypes();
						// 必须只有一个方法参数
						if (paramTypes.length != 1) {
							throw new IllegalStateException("@Resource annotation requires a single-arg method: " + method);
						}
						if (!this.ignoredResourceTypes.contains(paramTypes[0].getName())) {
							PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
							currElements.add(new ResourceElement(method, bridgedMethod, pd));
						}
					}
				}
			});

			elements.addAll(0, currElements);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		// 组装为InjectionMetadata
		return InjectionMetadata.forElements(elements, clazz);
	}

	/**
	 * Obtain a lazily resolving resource proxy for the given name and type,
	 * delegating to {@link #getResource} on demand once a method call comes in.
	 * @param element the descriptor for the annotated field/method
	 * @param requestingBeanName the name of the requesting bean
	 * @return the resource object (never {@code null})
	 * @since 4.2
	 * @see #getResource
	 * @see Lazy
	 */
	protected Object buildLazyResourceProxy(final LookupElement element, final @Nullable String requestingBeanName) {
		TargetSource ts = new TargetSource() {
			@Override
			public Class<?> getTargetClass() {
				return element.lookupType;
			}
			@Override
			public boolean isStatic() {
				return false;
			}
			@Override
			public Object getTarget() {
				return getResource(element, requestingBeanName);
			}
			@Override
			public void releaseTarget(Object target) {
			}
		};

		ProxyFactory pf = new ProxyFactory();
		pf.setTargetSource(ts);
		if (element.lookupType.isInterface()) {
			pf.addInterface(element.lookupType);
		}
		ClassLoader classLoader = (this.beanFactory instanceof ConfigurableBeanFactory ?
				((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() : null);
		return pf.getProxy(classLoader);
	}

	/**
	 * Obtain the resource object for the given name and type.
	 * -- 获取给定名称和类型的资源对象。
	 * @param element the descriptor for the annotated field/method
	 * @param requestingBeanName the name of the requesting bean
	 * @return the resource object (never {@code null})
	 * @throws NoSuchBeanDefinitionException if no corresponding target resource found
	 */
	protected Object getResource(LookupElement element, @Nullable String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		// JNDI lookup to perform?
		String jndiName = null;
		if (StringUtils.hasLength(element.mappedName)) {
			jndiName = element.mappedName;
		}
		else if (this.alwaysUseJndiLookup) {
			jndiName = element.name;
		}
		if (jndiName != null) {
			if (this.jndiFactory == null) {
				throw new NoSuchBeanDefinitionException(element.lookupType,
						"No JNDI factory configured - specify the 'jndiFactory' property");
			}
			return this.jndiFactory.getBean(jndiName, element.lookupType);
		}

		// Regular resource autowiring
		if (this.resourceFactory == null) {
			throw new NoSuchBeanDefinitionException(element.lookupType,
					"No resource factory configured - specify the 'resourceFactory' property");
		}
		return autowireResource(this.resourceFactory, element, requestingBeanName);
	}

	/**
	 * Obtain a resource object for the given name and type through autowiring
	 * based on the given factory.
	 * -- 根据给定的工厂，通过自动注入获取给定名称和类型的资源对象。
	 *
	 * @param factory the factory to autowire against
	 * @param element the descriptor for the annotated field/method
	 *                   -- 字段或方法的描述符 （ResourceElement(field, field, null)、ResourceElement(method, bridgedMethod, pd)）
	 * @param requestingBeanName the name of the requesting bean
	 * @return the resource object (never {@code null})
	 * @throws NoSuchBeanDefinitionException if no corresponding target resource found
	 */
	protected Object autowireResource(BeanFactory factory, LookupElement element, @Nullable String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		Object resource;
		Set<String> autowiredBeanNames;
		String name = element.name;

		// 根据bean名字或bean名字类型解析工厂中的依赖bean对象
		if (factory instanceof AutowireCapableBeanFactory) {
			AutowireCapableBeanFactory beanFactory = (AutowireCapableBeanFactory) factory;
			DependencyDescriptor descriptor = element.getDependencyDescriptor();
			if (this.fallbackToDefaultTypeMatch && element.isDefaultName && !factory.containsBean(name)) {
				autowiredBeanNames = new LinkedHashSet<>();
				// 解析依赖的bean对象
				resource = beanFactory.resolveDependency(descriptor, requestingBeanName, autowiredBeanNames, null);
				if (resource == null) {
					throw new NoSuchBeanDefinitionException(element.getLookupType(), "No resolvable resource object");
				}
			}
			else {
				resource = beanFactory.resolveBeanByName(name, descriptor);
				autowiredBeanNames = Collections.singleton(name);
			}
		}
		else {
			resource = factory.getBean(name, element.lookupType);
			autowiredBeanNames = Collections.singleton(name);
		}

		// 注册依赖缓存
		if (factory instanceof ConfigurableBeanFactory) {
			ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) factory;
			for (String autowiredBeanName : autowiredBeanNames) {
				if (requestingBeanName != null && beanFactory.containsBean(autowiredBeanName)) {
					beanFactory.registerDependentBean(autowiredBeanName, requestingBeanName);
				}
			}
		}

		return resource;
	}

	/**
	 * 加载name注解，并返回它的Class
	 * @param name
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private static Class<? extends Annotation> loadAnnotationType(String name) {
		try {
			return (Class<? extends Annotation>)
					ClassUtils.forName(name, CommonAnnotationBeanPostProcessor.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			return null;
		}
	}


	/**
	 * Class representing generic injection information about an annotated field
	 * or setter method, supporting @Resource and related annotations.
	 * --
	 * 该类表示有关注解字段或setter方法的通用注入信息，支持@Resource和相关注解。
	 */
	protected abstract static class LookupElement extends InjectionMetadata.InjectedElement {

		// resourceName 或 “”
		// 属性名
		protected String name = "";

		/**
		 * resource.name() 是否为空
		 */
		protected boolean isDefaultName = false;

		/**
		 * resourceType
		 */
		protected Class<?> lookupType = Object.class;

		@Nullable
		protected String mappedName;

		public LookupElement(Member member, @Nullable PropertyDescriptor pd) {
			super(member, pd);
		}

		/**
		 * Return the resource name for the lookup.
		 */
		public final String getName() {
			return this.name;
		}

		/**
		 * Return the desired type for the lookup.
		 */
		public final Class<?> getLookupType() {
			return this.lookupType;
		}

		/**
		 * Build a DependencyDescriptor for the underlying field/method.
		 */
		public final DependencyDescriptor getDependencyDescriptor() {
			if (this.isField) {
				return new LookupDependencyDescriptor((Field) this.member, this.lookupType);
			}
			else {
				return new LookupDependencyDescriptor((Method) this.member, this.lookupType);
			}
		}
	}


	/**
	 * Class representing injection information about an annotated field
	 * or setter method, supporting the @Resource annotation.
	 * --
	 * 该类表示有关注解字段或setter方法的通用注入信息，支持@Resource和相关注解。
	 */
	private class ResourceElement extends LookupElement {

		/**
		 * 是否延迟查找
		 */
		private final boolean lazyLookup;

		public ResourceElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
			super(member, pd);
			Resource resource = ae.getAnnotation(Resource.class);
			String resourceName = resource.name();
			Class<?> resourceType = resource.type();

			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			// 解析资源名称
			else if (embeddedValueResolver != null) {
				resourceName = embeddedValueResolver.resolveStringValue(resourceName);
			}
			if (Object.class != resourceType) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.name = (resourceName != null ? resourceName : "");
			this.lookupType = resourceType;
			String lookupValue = resource.lookup();
			this.mappedName = (StringUtils.hasLength(lookupValue) ? lookupValue : resource.mappedName());
			// 字段上是否有@Lazy注解
			// 字段上是否有@Lazy注解
			Lazy lazy = ae.getAnnotation(Lazy.class);
			this.lazyLookup = (lazy != null && lazy.value());
		}

		@Override
		protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
			return (this.lazyLookup ? buildLazyResourceProxy(this, requestingBeanName) :
					getResource(this, requestingBeanName));
		}
	}


	/**
	 * Class representing injection information about an annotated field
	 * or setter method, supporting the @WebServiceRef annotation.
	 * --
	 * 表示有关带注解的字段或 setter 方法的注入信息的类，支持 @WebServiceRef 注解。
	 */
	private class WebServiceRefElement extends LookupElement {

		private final Class<?> elementType;

		private final String wsdlLocation;

		public WebServiceRefElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
			super(member, pd);
			WebServiceRef resource = ae.getAnnotation(WebServiceRef.class);
			String resourceName = resource.name();
			Class<?> resourceType = resource.type();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				// 从set方法中获取属性名
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			if (Object.class != resourceType) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.name = resourceName;
			this.elementType = resourceType;
			if (Service.class.isAssignableFrom(resourceType)) {
				this.lookupType = resourceType;
			}
			else {
				this.lookupType = resource.value();
			}
			this.mappedName = resource.mappedName();
			this.wsdlLocation = resource.wsdlLocation();
		}

		@Override
		protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
			Service service;
			try {
				service = (Service) getResource(this, requestingBeanName);
			}
			catch (NoSuchBeanDefinitionException notFound) {
				// Service to be created through generated class.
				if (Service.class == this.lookupType) {
					throw new IllegalStateException("No resource with name '" + this.name + "' found in context, " +
							"and no specific JAX-WS Service subclass specified. The typical solution is to either specify " +
							"a LocalJaxWsServiceFactoryBean with the given name or to specify the (generated) Service " +
							"subclass as @WebServiceRef(...) value.");
				}
				if (StringUtils.hasLength(this.wsdlLocation)) {
					try {
						Constructor<?> ctor = this.lookupType.getConstructor(URL.class, QName.class);
						WebServiceClient clientAnn = this.lookupType.getAnnotation(WebServiceClient.class);
						if (clientAnn == null) {
							throw new IllegalStateException("JAX-WS Service class [" + this.lookupType.getName() +
									"] does not carry a WebServiceClient annotation");
						}
						service = (Service) BeanUtils.instantiateClass(ctor,
								new URL(this.wsdlLocation), new QName(clientAnn.targetNamespace(), clientAnn.name()));
					}
					catch (NoSuchMethodException ex) {
						throw new IllegalStateException("JAX-WS Service class [" + this.lookupType.getName() +
								"] does not have a (URL, QName) constructor. Cannot apply specified WSDL location [" +
								this.wsdlLocation + "].");
					}
					catch (MalformedURLException ex) {
						throw new IllegalArgumentException(
								"Specified WSDL location [" + this.wsdlLocation + "] isn't a valid URL");
					}
				}
				else {
					service = (Service) BeanUtils.instantiateClass(this.lookupType);
				}
			}
			return service.getPort(this.elementType);
		}
	}


	/**
	 * Class representing injection information about an annotated field
	 * or setter method, supporting the @EJB annotation.
	 */
	private class EjbRefElement extends LookupElement {

		private final String beanName;

		public EjbRefElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
			super(member, pd);
			EJB resource = ae.getAnnotation(EJB.class);
			String resourceBeanName = resource.beanName();
			String resourceName = resource.name();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			Class<?> resourceType = resource.beanInterface();
			if (Object.class != resourceType) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.beanName = resourceBeanName;
			this.name = resourceName;
			this.lookupType = resourceType;
			this.mappedName = resource.mappedName();
		}

		@Override
		protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
			if (StringUtils.hasLength(this.beanName)) {
				if (beanFactory != null && beanFactory.containsBean(this.beanName)) {
					// Local match found for explicitly specified local bean name.
					Object bean = beanFactory.getBean(this.beanName, this.lookupType);
					if (requestingBeanName != null && beanFactory instanceof ConfigurableBeanFactory) {
						((ConfigurableBeanFactory) beanFactory).registerDependentBean(this.beanName, requestingBeanName);
					}
					return bean;
				}
				else if (this.isDefaultName && !StringUtils.hasLength(this.mappedName)) {
					throw new NoSuchBeanDefinitionException(this.beanName,
							"Cannot resolve 'beanName' in local BeanFactory. Consider specifying a general 'name' value instead.");
				}
			}
			// JNDI name lookup - may still go to a local BeanFactory.
			return getResource(this, requestingBeanName);
		}
	}


	/**
	 * Extension of the DependencyDescriptor class,
	 * overriding the dependency type with the specified resource type.
	 */
	private static class LookupDependencyDescriptor extends DependencyDescriptor {

		private final Class<?> lookupType;

		public LookupDependencyDescriptor(Field field, Class<?> lookupType) {
			super(field, true);
			this.lookupType = lookupType;
		}

		public LookupDependencyDescriptor(Method method, Class<?> lookupType) {
			super(new MethodParameter(method, 0), true);
			this.lookupType = lookupType;
		}

		@Override
		public Class<?> getDependencyType() {
			return this.lookupType;
		}
	}

}
