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

package org.springframework.context.support;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationStartupAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.core.ResolvableType;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Abstract implementation of the {@link org.springframework.context.ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * 这段代码是Spring框架中的ApplicationContext接口的抽象实现。它并不强制要求使用特定的存储方式来进行配置，只是实现了常见的上下文功能。
 * 它使用了模板方法设计模式，要求具体的子类来实现抽象方法。
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors},
 * and {@link org.springframework.context.ApplicationListener ApplicationListeners}
 * which are defined as beans in the context.
 *
 * 与普通的 BeanFactory 相比，ApplicationContext 应该检测其内部 Bean 工厂中定义的特殊 bean：
 * 因此，此类会自动注册 {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors}、
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors} 和
 * {@link org.springframework.context.ApplicationListener ApplicationListeners}，它们在上下文中定义为 bean。
 *
 * <p>A {@link org.springframework.context.MessageSource} may also be supplied
 * as a bean in the context, with the name "messageSource"; otherwise, message
 * resolution is delegated to the parent context. Furthermore, a multicaster
 * for application events can be supplied as an "applicationEventMulticaster" bean
 * of type {@link org.springframework.context.event.ApplicationEventMulticaster}
 * in the context; otherwise, a default multicaster of type
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} will be used.
 *
 * 在上下文中，还可以将{@link org.springframework.context.MessageSource}作为一个名为"messageSource"的bean提供；
 * 否则，消息解析将委托给父上下文。
 * 此外，应用程序事件的多播器可以在上下文中作为 {@link org.springframework.context.event.ApplicationEventMulticaster}
 * 类型的“applicationEventMulticaster”bean 提供；
 * 否则，将使用默认的{@link org.springframework.context.event.SimpleApplicationEventMulticaster}类型的多播器。
 *
 * <p>Implements resource loading by extending
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 * Consequently treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
 * method is overridden in a subclass.
 *
 * 通过扩展 {@link org.springframework.core.io.DefaultResourceLoader}来实现资源加载。
 * 因此，将非URL资源路径视为类路径资源（支持包含程序包路径的完整类路径资源名称，如
 * 'mypackage/myresource.dat'),除非 {@link #getResourceByPath}在子类中被覆盖
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext {

	/**
	 * Name of the MessageSource bean in the factory.
	 * If none is supplied, message resolution is delegated to the parent.
	 *
	 * MessageSource在工厂中的bean名称.如果未提供，则将消息解析位委托给父类.
	 *
	 * @see MessageSource
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * Name of the LifecycleProcessor bean in the factory.
	 * If none is supplied, a DefaultLifecycleProcessor is used.
	 * @see org.springframework.context.LifecycleProcessor
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 *
	 * LifecycleProcessor在工厂中的bean名称。如果未提供，就用DefaultLifecycleProcessor
	 */
	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

	/**
	 * Name of the ApplicationEventMulticaster bean in the factory.
	 * If none is supplied, a default SimpleApplicationEventMulticaster is used.
	 * @see org.springframework.context.event.ApplicationEventMulticaster
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 *
	 * ApplicationEventMulticaster在工厂中的bean名称。如果未提供，就用默认的SimpleApplicationEventMulticaster
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	/**
	 * Boolean flag controlled by a {@code spring.spel.ignore} system property that instructs Spring to
	 * ignore SpEL, i.e. to not initialize the SpEL infrastructure.
	 * <p>The default is "false".
	 *
	 * 这是一个布尔标志，由{@code spring.spel.ignore}系统属性控制，用于指示Spring是否忽略SpEL，即不初始化SpEL基础设施。
	 * 默认值为"false"。
	 */
	private static final boolean shouldIgnoreSpel = SpringProperties.getFlag("spring.spel.ignore");


	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		// 饿汉式加载ContextClosedEvent类，以避免出现奇怪的类加载问题
		// Weblogic8.1中应用程序关闭时发生的故障。（达斯汀·伍兹报道）
		ContextClosedEvent.class.getName();
	}


	/** Logger used by this class. Available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Unique id for this context, if any.
	 * 此上下文的唯一ID（如果有）
	 * ObjectUtils.identityToString:可得到(obj的全类名+'@'+obj的hashCode的十六进制字符串)
	 */

	private String id = ObjectUtils.identityToString(this);

	/** Display name. */
	private String displayName = ObjectUtils.identityToString(this);

	/** Parent context. */
	@Nullable
	private ApplicationContext parent;

	/** Environment used by this context. -- 此上下文使用的环境，SpringBoot默认使用StandardServletEnvironment */
	@Nullable
	private ConfigurableEnvironment environment;

	/** BeanFactoryPostProcessors to apply on refresh. */
	// BeanFactoryPostProcessors，以便在刷新时应用。
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

	/** System time in milliseconds when this context started. -- 此上下文启动时的系统时间（以毫秒为单位）。*/
	private long startupDate;

	/** Flag that indicates whether this context is currently active. -- 指示此上下文当前是否处于活动状态的标记，该变量是线程安全的 */
	private final AtomicBoolean active = new AtomicBoolean();

	/** Flag that indicates whether this context has been closed already. -- 指示此上下文是否已经关闭的标记，该变量是线程安全的*/
	private final AtomicBoolean closed = new AtomicBoolean();

	/** Synchronization monitor for the "refresh" and "destroy". -- 刷新和销毁的同步监视器 */
	private final Object startupShutdownMonitor = new Object();

	/** Reference to the JVM shutdown hook, if registered. -- 对 JVM 关闭钩子的引用（如果已注册）。*/
	@Nullable
	private Thread shutdownHook;

	/**
	 * ResourcePatternResolver used by this context.
	 * 默认得到的是PathMatchingResourcePatternResolver, 这个一个支持Ant-style位置模式的类
	 * Ant-styple: https://blog.csdn.net/my__sun_/article/details/76444435
	 *
	 */
	private ResourcePatternResolver resourcePatternResolver;

	/**
	 * LifecycleProcessor for managing the lifecycle of beans within this context.
	 * LifecycleProcessor，用于在此上下文中管理 Bean 的生命周期。
	 */
	@Nullable
	private LifecycleProcessor lifecycleProcessor;

	/** MessageSource we delegate our implementation of this interface to. -- 我们将此接口的实现委托给 MessageSource */
	// 我们将此接口的实现委托给 MessageSource。
	@Nullable
	private MessageSource messageSource;

	/** Helper class used in event publishing. -- 事件发布中使用的帮助程序类。*/
	@Nullable
	private ApplicationEventMulticaster applicationEventMulticaster;

	/** Application startup metrics. -- 应用程序启动指标 **/
	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	/**
	 * Statically specified listeners.
	 * 静态指定的侦听器。
	 */
	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

	/** Local listeners registered before refresh. -- 刷新前注册的本地监听器。*/
	@Nullable
	private Set<ApplicationListener<?>> earlyApplicationListeners;

	/**
	 * ApplicationEvents published before the multicaster setup.
	 * 在多播程序设置之前发布的ApplicationEvents
	 */
	@Nullable
	private Set<ApplicationEvent> earlyApplicationEvents;


	/**
	 * Create a new AbstractApplicationContext with no parent.
	 * 创建一个新的无父类的 AbstractApplicationContext
	 */
	public AbstractApplicationContext() {
		// 默认得到的是PathMatchingResourcePatternResolver, 这个一个支持Ant-style位置模式的类
		// Ant-style:https://blog.csdn.net/my__sun_/article/details/76444435
		this.resourcePatternResolver = getResourcePatternResolver();
	}

	/**
	 * Create a new AbstractApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	public AbstractApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * Set the unique id of this application context.
	 * <p>Default is the object id of the context instance, or the name
	 * of the context bean if the context is itself defined as a bean.
	 *
	 * 设置此应用程序上下文的唯一 ID。
	 * 缺省值是上下文实例的对象 ID，如果上下文本身定义为 Bean，则为上下文 Bean 的名称。
	 *
	 * @param id the unique id of the context
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	/**
	 * Set a friendly name for this context.
	 * Typically done during initialization of concrete context implementations.
	 * <p>Default is the object id of the context instance.
	 *
	 * 为此上下文设置友好名称。通常在具体上下文实现的初始化期间完成。
	 * 默认值为上下文实例的对象 ID
	 */
	public void setDisplayName(String displayName) {
		Assert.hasLength(displayName, "Display name must not be empty");
		this.displayName = displayName;
	}

	/**
	 * Return a friendly name for this context.
	 * @return a display name for this context (never {@code null})
	 */
	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * (that is, this context is the root of the context hierarchy).
	 */
	@Override
	@Nullable
	public ApplicationContext getParent() {
		return this.parent;
	}

	/**
	 * Set the {@code Environment} for this application context.
	 * <p>Default value is determined by {@link #createEnvironment()}. Replacing the
	 * default with this method is one option but configuration through {@link
	 * #getEnvironment()} should also be considered. In either case, such modifications
	 * should be performed <em>before</em> {@link #refresh()}.
	 *
	 * Environment设置此应用程序上下文。
	 * 缺省值由 createEnvironment()确定。将默认值替换为此方法是一种选择，但也应考虑配置通过getEnvironment()。
	 * 无论哪种情况，此类修改都应在 之前refresh()执行
	 *
	 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * Return the {@code Environment} for this application context in configurable
	 * form, allowing for further customization.
	 * 以可配置的形式返回此应用程序上下文的 {@code Environment}，允许进一步自定义。
	 *
	 * <p>If none specified, a default environment will be initialized via
	 * 如果未指定，则默认环境将通过{@link #createEnvironment()}进行初始化。
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Create and return a new {@link StandardEnvironment}.
	 * 创建并返回一个新的{@link StandardEnvironment}
	 *
	 * <p>Subclasses may override this method in order to supply
	 * a custom {@link ConfigurableEnvironment} implementation.
	 * 子类可以重新此方法，以提供一个自定义{@link ConfigurableEnvironment}实现
	 */
	protected ConfigurableEnvironment createEnvironment() {
		// StandardEnvironment:使用与"标准"(即非网络)应用程序的Environment实现,
		// 除了ConfigurableEnvironment的常用功能(例如属性解析和配置文件相关的操作)之外，
		// 此实现还配置了两个默认的属性源，将按以下顺序进行搜索:
		// 1. system properties 系统属性
		// 2. system environment variables 系统环境属性
		return new StandardEnvironment();
	}

	/**
	 * Return this context's internal bean factory as AutowireCapableBeanFactory,
	 * if already available.
	 * 如果已可用，则将此上下文的内部 Bean 工厂作为 AutowireCapableBeanFactory 返回。
	 *
	 * @see #getBeanFactory()
	 */
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * Return the timestamp (ms) when this context was first loaded.
	 * 返回首次加载此上下文时的时间戳（毫秒）。
	 */
	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

	/**
	 * Publish the given event to all listeners.
	 * 将给定事件发布到所有监听器。
	 *
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * 注意: 监听器在MessageSource之后被初始化，以便能够在监听器实现中访问它。
	 * 因此，MessageSource实现不能发布事件
	 *
	 * @param event the event to publish (may be application-specific or a
	 * standard framework event)  -- 要发布的事件(可能是特定于应用程序的事件或标准框架事件)
	 */
	@Override
	public void publishEvent(ApplicationEvent event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 */
	@Override
	public void publishEvent(Object event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * 将给定事件发布到所有监听器
	 *
	 * 将 event 多播到所有适合的监听器。如果 event 不是 ApplicationEvent 实例，会将其封装成 PayloadApplicationEvent 对象再进行多播。
	 * 当上下文的多播器没有初始化时，会将event暂时保存起来，待多播器初始化后才将缓存的event进行多播
	 *
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent}) - 要发布的事件(可以是 ApplicationEvent 或要转换为 PayloadApplicationEvent的有效负载对象)
	 * @param eventType the resolved event type, if known -- 已解析的事件类型(如果已知)
	 * @since 4.2
	 */
	protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
		Assert.notNull(event, "Event must not be null");

		// Decorate event as an ApplicationEvent if necessary
		// 如有必要，将事件装饰为 ApplicationEvent
		ApplicationEvent applicationEvent;
		if (event instanceof ApplicationEvent) {
			applicationEvent = (ApplicationEvent) event;
		}
		else {
			applicationEvent = new PayloadApplicationEvent<>(this, event);
			if (eventType == null) {
				// 将 applicationEvent 转换为 PayloadApplicationEvent 对象，引用其 ResolvableType 对象
				eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
			}
		}

		// Multicast right now if possible - or lazily once the multicaster is initialized
		// 如果可能的话，立即进行组播 - 或者在组播器初始化后延迟
		// earlyApplicationEvents：在多播程序设置之前发布的ApplicationEvent
		// 如果 earlyApplicationEvents 不为 null，这种情况只在上下文的多播器还没有初始化的情况下才会成立， 会将 applicationEvent
		// 添加到 earlyApplicationEvents 保存起来，待多播器初始化后才继续进行多播到适当的监听器
		if (this.earlyApplicationEvents != null) {
			this.earlyApplicationEvents.add(applicationEvent);
		}
		else {
			// 多播applicationEvent到适当的监听器
			getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
		}

		// Publish event via parent context as well...
		if (this.parent != null) {
			if (this.parent instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
			}
			else {
				this.parent.publishEvent(event);
			}
		}
	}

	/**
	 * Return the internal ApplicationEventMulticaster used by the context.
	 *
	 * 返回上下文使用的内部 ApplicationEventMulticaster。
	 *
	 * @return the internal ApplicationEventMulticaster (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
					"call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		Assert.notNull(applicationStartup, "ApplicationStartup must not be null");
		this.applicationStartup = applicationStartup;
	}

	@Override
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	/**
	 * Return the internal LifecycleProcessor used by the context.
	 * @return the internal LifecycleProcessor (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
		if (this.lifecycleProcessor == null) {
			throw new IllegalStateException("LifecycleProcessor not initialized - " +
					"call 'refresh' before invoking lifecycle methods via the context: " + this);
		}
		return this.lifecycleProcessor;
	}

	/**
	 * Return the ResourcePatternResolver to use for resolving location patterns
	 * into Resource instances. Default is a
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
	 * supporting Ant-style location patterns.
	 *
	 * 返回用于将位置模式解析为 Resource 实例的 ResourcePatternResolver。
	 * 默认值为 {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}，支持 Ant 样式的位置模式。
	 *
	 * <p>Can be overridden in subclasses, for extended resolution strategies,
	 * for example in a web environment.
	 *
	 * 可以在子类中重写，用于扩展解析策略，例如在 Web 环境中。
	 *
	 * <p><b>Do not call this when needing to resolve a location pattern.</b>
	 * Call the context's {@code getResources} method instead, which
	 * will delegate to the ResourcePatternResolver.
	 *
	 * 在需要解析位置模式时不要调用此方法，而是调用上下文的{@code getResources}方法,该方法将委托给ResourcePatternResolver
	 *
	 * @return the ResourcePatternResolver for this context
	 * @see #getResources
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * Set the parent of this application context.
	 * 设置应用程序上下文的父级
	 *
	 * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
	 * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
	 * this (child) application context environment if the parent is non-{@code null} and
	 * its environment is an instance of {@link ConfigurableEnvironment}.
	 * 如果父级引用程序上下文不为null且它的environment是{@link ConfigurableEnvironment}的实例，将会和子级的应用程序上下文进行合并
	 *
	 * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
	 */
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		this.parent = parent;
		if (parent != null) {
			Environment parentEnvironment = parent.getEnvironment();
			if (parentEnvironment instanceof ConfigurableEnvironment) {
				// 将给定的父环境的活动配置文件，默认配置文件和属性源追加到此(子)环境各自集合中
				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
			}
		}
	}

	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
		this.beanFactoryPostProcessors.add(postProcessor);
	}

	/**
	 * Return the list of BeanFactoryPostProcessors that will get applied
	 * to the internal BeanFactory.
	 *
	 * 返回将应用于内部 BeanFactory 的 BeanFactoryPostProcessors 列表。
	 */
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
	}

	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		Assert.notNull(listener, "ApplicationListener must not be null");
		if (this.applicationEventMulticaster != null) {
			this.applicationEventMulticaster.addApplicationListener(listener);
		}
		this.applicationListeners.add(listener);
	}

	/**
	 * Return the list of statically specified ApplicationListeners.
	 * 返回静态指定的 ApplicationListener 的列表。
	 */
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.applicationListeners;
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// 标定刷新步骤开始了
			StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");

			// Prepare this context for refreshing.
			// 准备此上下文以进行刷新。
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			// 告诉子类刷新内部bean工厂
			// 注册BeanDefinition
			// 对此上下文的基础bean工厂进行实际刷新，关闭前一个bean工厂（如果有），
			// 并为上下文生命周期的下一个阶段初始化一个新的bean工厂, 并使用XmlBeanDefinitionReader
			// 将bean定义加载给bean工厂中
			// DefaultListableBeanFactory
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				// 允许在上下文子类中对 bean 工厂进行后处理。
				postProcessBeanFactory(beanFactory);

				StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");

				// Invoke factory processors registered as beans in the context.
				// 调用在上下文中注册为bean的工厂处理器
				// 实例化并调用所有注册BeanFactoryPostProcess bean，并遵从显式顺序（如果给定的话）
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				// 注册拦截Bean创建的 BeanPostProcessor
				// 实例化和注册所有 BeanPostProcessor 类型的Bean对象,如果给定的话，遵循显示顺序.
				// 必须在应用程序Bean的任何实例化之前调用
				registerBeanPostProcessors(beanFactory);
				beanPostProcess.end();

				// Initialize message source for this context.
				// 为此上下文初始化消息源
				// 从当前BeanFactory中获取名为 'messageSource' 的 MessageSource 类型的Bean对象,如果没有找到，就创建 一个
				// DelegatingMessageSource 对象作为该上下文的 messageSource 属性值,并将该上下文的 messageSource 注册到 该
				// 上下文的BeanFactory中。
				// 默认对该上下文的messageSource设置父级messageSource,父级messageSource为父级上下文的messageSoure属性对象
				// 或者父级上下文本身
				initMessageSource();

				// Initialize event multicaster for this context.
				// 为此上下文初始化事件多播器。管理applicationListener, 实际的事件发布者
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				// 初始化特定上下文子类中的其他特殊Bean
				onRefresh();

				// Check for listener beans and register them.
				// 检查监听器Bean并注册它们
				// 注册静态指定的监听器和beanFactory内的监听器Bean对象【不会导致监听器Bean对象实例化，仅记录其Bean名】到当前上
				// 下文 的多播器，然后发布在多播程序设置之前发布的ApplicationEvent集到各个监听器中
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				// 实例化所有剩余的(非延迟初始化)的单例
				// 完成这个上下文BeanFactory的初始化，初始化所有剩余的 单例(非延迟初始化) Bean对象
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				// 最后一步：发布相应的事件。
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				destroyBeans();

				// Reset 'active' flag.
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				// 重置 Spring 核心中的常见内省缓存，因为我们可能不再需要单例 bean 的元数据......
				resetCommonCaches();
				// 标定刷新结束了
				contextRefresh.end();
			}
		}
	}

	/**
	 * Prepare this context for refreshing, setting its startup date and
	 * active flag as well as performing any initialization of property sources.
	 * 准备该上下文以进行刷新，设置其启动日期和activie标记以及执行属性源的任何初始化。
	 *
	 *  <p>ApplicationContext刷新前的准备工作：
	 * 		<ol>
	 * 		  <li>设置上下文启动时的系统时间，startupDate</li>
	 * 		  <li>设置上下文为不关闭状态，closed</li>
	 * 		  <li>设置上下文为活动状态，active</li>
	 * 		  <li>根据日志级别打印日志,提示准备刷新ApplicationContext</li>
	 * 		  <li>创建Environment对象，默认情况下Environment对象是StandardEnvironment对象</li>
	 * 		  <li>提供一个钩子方法，交由子类替换任何存根属性源。默认情况下子类GenericWebApplicationContext，
	 * 		  通过该方法设置sources中的servletContextInitParams和servletConfigInitParams属性，以供更方便的
	 * 		  形式获取SerlvetContext。</li>
	 * 		  <li>初始化用于存储已注册本地侦听器的LinkedHashSet集合，earlyApplicationListeners</li>
	 * 		  <li>初始化用于收集早期的ApplicationEvents的LinkeHashSet集合，earlyApplicationEvents</li>
	 * 		</ol>
	 * 	</p>
	 */
	protected void prepareRefresh() {
		// Switch to active.
		// 设置上下文启动时的系统时间
		this.startupDate = System.currentTimeMillis();
		this.closed.set(false);
		this.active.set(true);

		if (logger.isDebugEnabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Refreshing " + this);
			}
			else {
				logger.debug("Refreshing " + getDisplayName());
			}
		}

		// Initialize any placeholder property sources in the context environment.
		// 初始化上下文环境中的任何占位符属性源
		// 这个一个钩子方法，用于交由子类替换任何存根属性源
		initPropertySources();

		// Validate that all properties marked as required are resolvable:
		// see ConfigurablePropertyResolver#setRequiredProperties
		// 验证所有标记为必须的属性都是可解析的，请参见
		// ConfigurablePropertyResolver#setRequiredProperties方法，
		// 该方法的默认实现是AbstractPropertyResolver类
		// 只是验证要必须要有的属性不会为null，验证不通过会抛出MissingRequiredPropertiesException
		// SpringBoot默认情况下不存在必要的属性
		getEnvironment().validateRequiredProperties();

		// Store pre-refresh ApplicationListeners...
		// 存储预刷新的ApplicationListeners
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		}
		else {
			// Reset local application listeners to pre-refresh state. 将本地应用程序侦听器重置为刷新前状态。
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// Allow for the collection of early ApplicationEvents,
		// to be published once the multicaster is available...
		// 允许收集早期的ApplicationEvents，一旦多播器可用就发布
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}

	/**
	 * <p>Replace any stub property sources with actual instances.
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
	 */
	protected void initPropertySources() {
		// For subclasses: do nothing by default.
	}

	/**
	 * Tell the subclass to refresh the internal bean factory.
	 *
	 * 告诉子类刷新内部 Bean 工厂。
	 * @return the fresh BeanFactory instance -- 新的 BeanFactory 实例
	 * @see #refreshBeanFactory()
	 * @see #getBeanFactory()
	 */
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		// 调用子类实现方法执行实际的配置加载
		refreshBeanFactory();
		// 返回子类提供的Bean工厂
		return getBeanFactory();
	}

	/**
	 * Configure the factory's standard context characteristics,
	 * such as the context's ClassLoader and post-processors.
	 * 配置工厂的标准上下文特征，例如上下文的ClassLoader和后处理器。
	 *
	 * @param beanFactory the BeanFactory to configure
	 * <p>
	 * 对bean工厂做准备工作：
	 * 1. 设置beanFactory的bean的类加载器，通常是当前线程的上下文类加载器,AppClassLoader
	 * 2. 为bean定义值中的表达式指定解析策略，这里设置的是标准的Spring EL表达式解析器,StandardBeanExpressionResolver
	 * 3. 添加属性编辑器到bean工厂,ResourceEditorRegistrar
	 * 4. 为bean工厂添加 为实现了某种Aware接口的bean设置ApplicationContext中相应的属性的BeanPostProcessor，
	 *    ApplicationContextAwareProcessor
	 * 5. 忽略EnvironmentAware,EmbeddedValueResolverAware,ResourceLoaderAware,ApplicationEventPublisherAware,MessageSourceAware
	 *    ApplicationContextAware依赖接口进行自动装配
	 * 6. 添加自动装配对象：BeanFactory，ResourceLoader，ApplicationEventPublisher，ApplicationContext
	 * 7. 如果该bean工厂包含具有给定名称的BeanDefinition对象或外部注册的singleton实例，
	 *    会添加用于回调LoadTimeWeaverAware#setLoadTimeWeaver(LoadTimeWeaver) 方法的BeanPostProcessor，
	 *    以及设置一个ContextTypeMatchClassLoader的临时类加载器用于对每个调用loadClass的类【除指定排除类外】，
	 *    都会新建一个 ContextOverridingClassLoader类加载器进行加载，并对每个类的字节缓存起来，
	 *    以便在父类ClassLoader中拾取 最近加载的类
	 * 8. 注册environment,systemProperties,systemEnvironment单例对象到该工厂
	 * </p>
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// Tell the internal bean factory to use the context's class loader etc.
		// 告诉内部bean工厂使用上下文的类加载器等
		// 设置beanFactory的bean的类加载器，通常是当前线程的上下文类加载器
		beanFactory.setBeanClassLoader(getClassLoader());
		if (!shouldIgnoreSpel) {
			// 为bean定义值中的表达式指定解析策略，这里设置的是标准的Spring EL表达式解析器
			// StandardBeanExpressionResolver:用于解析SpEL表达式的解析器
			beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		}
		// 添加属性编辑器到bean工厂,ResourceEditorRegistrar：属性编辑器的注册器，里面默认提供了一下简单通用常用类型的属性编辑器
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		// Configure the bean factory with context callbacks. 使用上下文回调配置 bean 工厂。
		// 使用上下文回调配置Bean工厂
		// 为bean工厂添加ApplicationContextAwareProcessor
		// ApplicationContextAwareProcessor:一个Spring内部工具，它实现了接口BeanPostProcessor,用于向实现了如下某种Aware接口
		// 的bean设置ApplicationContext中相应的属性:EnvironmentAware,EmbeddedValueResolverAware,ResourceLoaderAware
		// ApplicationEventPublisherAware,MessageSourceAware,ApplicationContextAware,
		// 如果bean实现了以上接口中的某几个，那么这些接口方法调用的先后顺序就是上面接口排列的先后顺序。
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		// 下面的ignoreDependencyInterface是用来设置bean工厂中需要忽略的接口
		// 可以通过实现EnvironmentAware接口来获取到当前的环境信息Environment。
		// 如果将EnvironmentAware接口添加到ignoreDependencyInterface中，则在使用的地方通过@Autowired将会无法正常注入
		// 而是需要通过setEnvironment方法进行注入，下面的其他接口都类似.
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		// 可以通过实现EmbeddedValueResolverAware接口来获取String类型值的解析器
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		// 资源加载器，例如使用：@Autowired ResourceLoaderAware aware; 将不会被注入
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		// 事件发布器
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		// 消息资源
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationStartupAware.class);

		// BeanFactory interface not registered as resolvable type in a plain factory.
		// BeanFactory 接口未在普通工厂中注册为可解析类型。
		// MessageSource registered (and found for autowiring) as a bean.
		// MessageSource 已注册（并为自动装配而找到）为 bean。
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		// Register early post-processor for detecting inner beans as ApplicationListeners.
		// 注册早期的后处理器以将内部bean检测为ApplicationListeners
		// ApplicationListenerDetector:该BeanPostProcessor检测那些实现了接口ApplicationListener的bean，在它们创建时初始化之后，
		// 将它们添加到应用上下文的事件多播器上；并在这些ApplicationListener bean销毁之前，
		// 将它们从应用上下文的事件多播器上移除。
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// Detect a LoadTimeWeaver and prepare for weaving, if found.
		// 如果发现LoadTimeWeaver,请准备编织
		if (!NativeDetector.inNativeImage() && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			// LoadTimeWeaverAwareProcessor只是用实现了LoadTimeWeaverAware接口的bean
			// 回调LoadTimeWeaverAware.setLoadTimeWeaver(LoadTimeWeaver)方法
			// 参考博客：https://blog.csdn.net/weixin_34112208/article/details/89750308
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// Set a temporary ClassLoader for type matching.
			// 设置一个临时的ClassLoader以进行匹配
			// ContextTypeMatchClassLoader:该类加载器破坏了双亲委派机制，除指定排除类外，
			// 对每个调用loadClass的类，都会新建一个ContextOverridingClassLoader类加载器进行
			// 加载，并对每个类的字节缓存起来，以便在父类ClassLoader中拾取 最近加载的类型
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// Register default environment beans.
		// 注册默认的环境bean
		// 如果bean工厂中没有包含名为'environment'的bean,(忽略在祖先上下文定义的bean)
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
		if (!beanFactory.containsLocalBean(APPLICATION_STARTUP_BEAN_NAME)) {
			beanFactory.registerSingleton(APPLICATION_STARTUP_BEAN_NAME, getApplicationStartup());
		}
	}

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. The initial definition resources will have been loaded but no
	 * post-processors will have run and no derived bean definitions will have been
	 * registered, and most importantly, no beans will have been instantiated yet.
	 * <p>This template method allows for registering special BeanPostProcessors
	 * etc in certain AbstractApplicationContext subclasses.
	 * 您可以在应用程序上下文的标准初始化之后修改其内部的Bean工厂。初始定义资源已经加载，但尚未运行任何后处理器，
	 * 也没有注册任何派生的Bean定义，最重要的是，还没有实例化任何Bean。
	 * 这个模板方法允许在某些AbstractApplicationContext子类中注册特殊的BeanPostProcessors等。
	 *
	 * @param beanFactory the bean factory used by the application context
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	}

	/**
	 * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before singleton instantiation.
	 *
	 * 在单例实例化之前，实例化和调用所有已注册的BeanFactoryPostProcessor bean，如果给定了明确的顺序，则要遵守该顺序。
	 * 必须在单例实例化之前调用。
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// 回调 BeanFactoryPostPorcessors 和 beanFactory的所有BeanFactoryPostProcessor对象的回调方法
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
		// (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)

		// 检测 LoadTimeWeaver 并准备编织（如果在此期间找到）（例如，通过 ConfigurationClassPostProcessor 注册的 @Bean 方法）
		// 如果bean工厂没有设置临时类加载器 且 'loadTimeWeaver' 不存在于bean工厂中
		// 下面的代码其实跟prepareBeanFactory方法的标记1的代码一样，应该是因为该方法的上一个步是供给子类重写的钩子方法，
		// Spring开发人担心经过上一步的处理后，会导致'loadTimeWeaver'被玩坏了，所有才
		if (!NativeDetector.inNativeImage() && beanFactory.getTempClassLoader() == null &&
				beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}

	/**
	 * Instantiate and register all BeanPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before any instantiation of application beans.
	 *
	 * 在实例化任何应用程序bean之前，实例化和注册所有的BeanPostProcessor bean，如果有明确的顺序，则要遵守该顺序。
	 * 需要在实例化任何应用程序bean之前调用此方法。
	 */
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
	}

	/**
	 * Initialize the MessageSource.
	 * Use parent's if none defined in this context.
	 *
	 * 初始化 MessageSource。如果在此上下文中未定义，请使用父级。
	 * MessageSource: 用于解析消息的策略界面，支持此类消息参数化和国际化
	 */
	protected void initMessageSource() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			// 使 MessageSource 知道父 MessageSource。
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// Only set parent context as parent MessageSource if no parent MessageSource
					// registered already.

					// 如果还没有注册 父级 MessageSource，只能将父级上下文设置父级 MessageSource
					// 获取父级上下文的 MessageSource 对象，将其设置 hms 的 父级 MessagoueSource 对象
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Using MessageSource [" + this.messageSource + "]");
			}
		}
		else {
			// Use empty MessageSource to be able to accept getMessage calls.
			// 使用 空的 MessageSource 来能够接收 getMessage 调用
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			this.messageSource = dms;
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * Initialize the ApplicationEventMulticaster.
	 * Uses SimpleApplicationEventMulticaster if none defined in the context.
	 *
	 * 初始化 ApplicationEventMulticaster。如果上下文中未定义任何内容，则使用 SimpleApplicationEventMulticaster。
	 *
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	protected void initApplicationEventMulticaster() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			this.applicationEventMulticaster =
					beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		}
		else {
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
						"[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * Initialize the LifecycleProcessor.
	 * Uses DefaultLifecycleProcessor if none defined in the context.
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 *
	 * 初始化生命周期处理器。如果上下文中未定义，则使用 DefaultLifecycleProcessor。
	 */
	protected void initLifecycleProcessor() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
			this.lifecycleProcessor =
					beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
			}
		}
		else {
			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
			defaultProcessor.setBeanFactory(beanFactory);
			this.lifecycleProcessor = defaultProcessor;
			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " +
						"[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * Template method which can be overridden to add context-specific refresh work.
	 * Called on initialization of special beans, before instantiation of singletons.
	 * <p>This implementation is empty.
	 *
	 * 模板方法，可以重写该方法以添加特定上下文的刷新工作。在实例化单例对象之前，对特殊Bean进行初始化时调用。
	 *
	 * @throws BeansException in case of errors
	 * @see #refresh()
	 */
	protected void onRefresh() throws BeansException {
		// For subclasses: do nothing by default.
	}

	/**
	 * Add beans that implement ApplicationListener as listeners.
	 * Doesn't affect other listeners, which can be added without being beans.
	 * 将实现ApplicationListener接口的bean添加为监听器。不影响其他listeners，无需成为bean即可添加。
	 */
	protected void registerListeners() {
		// Register statically specified listeners first.
		// 首先注册静态指定的侦听器。
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let post-processors apply to them!
		// 不要在这里初始化 FactoryBeans：我们需要让所有常规 bean 保持未初始化状态，以便让后处理器应用到它们
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}

		// Publish early application events now that we finally have a multicaster...
		// 现在我们终于有了一个多播器，发布早期的应用程序事件......
		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
		this.earlyApplicationEvents = null;
		if (!CollectionUtils.isEmpty(earlyEventsToProcess)) {
			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
				getApplicationEventMulticaster().multicastEvent(earlyEvent);
			}
		}
	}

	/**
	 * Finish the initialization of this context's bean factory,
	 * initializing all remaining singleton beans.
	 *
	 * 完成此上下文的 Bean 工厂的初始化，初始化所有剩余的单例 Bean。
	 *
	 */
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// Initialize conversion service for this context.
		// 为此上下文初始化转换服务。

		// 该beanFactory包含'conversionService'的BeanDefinition对象或外部注册的singleton实例 &&
		// 如果在beanFactory中 'conversionService' 与 ConversionService.class 匹配
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			// 从beanFactory中获取名为 'conversionService' 的 ConversionService 类型的Bean对象 作为 beanFactory的转换属性值
			// 	服务，也作为JavaBeans PropertyEditor的替代方案
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// Register a default embedded value resolver if no BeanFactoryPostProcessor
		// (such as a PropertySourcesPlaceholderConfigurer bean) registered any before:
		// at this point, primarily for resolution in annotation attribute values.
		// 如果以前没有BeanPostProcessor(比如 PropertyPlaceholderConfigurer Bean)注册过任何Bean，
		// 那么注册一个默认的内嵌值解析器：此时，主要是为了解析注解属性值
		// 如果当前beanFactory没有字符串解析器，默认是有一个 PropertyPlaceHolderConfigurer 解析器的
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
		}

		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
		// 尽早初始化 LoadTimeWeaverAware Bean，以便尽早注册它们的转换器
		//获取与BeanPostProcessor.class（包括子类）匹配的bean名称，如果是FactoryBeans会根据beanDefinition 或getObjectType的值判断
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// Stop using the temporary ClassLoader for type matching.
		// 停止使用临时类加载器进行类型匹配。
		beanFactory.setTempClassLoader(null);

		// Allow for caching all bean definition metadata, not expecting further changes.
		// 允许缓存所有 bean 定义元数据，而不期望进一步更改。
		beanFactory.freezeConfiguration();

		// Instantiate all remaining (non-lazy-init) singletons.
		// 实例化所有剩余的（非惰性初始化）单例
		// 确保所有非延迟初始化单例化都已实例化，同时也要考虑FactoryBean。
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * Finish the refresh of this context, invoking the LifecycleProcessor's
	 * onRefresh() method and publishing the
	 * {@link org.springframework.context.event.ContextRefreshedEvent}.
	 *
	 * 完成此上下文的刷新，调用 LifecycleProcessor 的 onRefresh（） 方法并发布 {@link org.springframework.context.event.ContextRefreshedEvent}。
	 *
	 */
	@SuppressWarnings("deprecation")
	protected void finishRefresh() {
		// Clear context-level resource caches (such as ASM metadata from scanning).
		// 清楚上下文级别的资源缓存(如扫描的ASM元数据)
		// 清空在资源加载器中的所有资源缓存。
		clearResourceCaches();

		// Initialize lifecycle processor for this context.
		// 为此上下文初始化生命周期处理器。
		initLifecycleProcessor();

		// Propagate refresh to lifecycle processor first.
		// 首先将刷新传播到生命周期处理器
		// 上下文刷新的通知，例如自动启动的组件
		getLifecycleProcessor().onRefresh();

		// Publish the final event.
		// 发布最终事件
		// 新建 ContextRefreshedEvent 事件对象，将其发布到所有监听器。
		publishEvent(new ContextRefreshedEvent(this));

		// Participate in LiveBeansView MBean, if active.
		// 参与 LiveBeansView MBean，如果是活动的
		// LiveBeansView:Sping 用于支持 JMX 服务的类，详情请参考：https://www.jianshu.com/p/fa4e88f95631
		// 注册 当前上下文 到 LiveBeansView，以支持 JMX 服务
		if (!NativeDetector.inNativeImage()) {
			LiveBeansView.registerApplicationContext(this);
		}
	}

	/**
	 * Cancel this context's refresh attempt, resetting the {@code active} flag
	 * after an exception got thrown.
	 * @param ex the exception that led to the cancellation
	 */
	protected void cancelRefresh(BeansException ex) {
		this.active.set(false);
	}

	/**
	 * Reset Spring's common reflection metadata caches, in particular the
	 * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
	 * and {@link CachedIntrospectionResults} caches.
	 * @since 4.2
	 * @see ReflectionUtils#clearCache()
	 * @see AnnotationUtils#clearCache()
	 * @see ResolvableType#clearCache()
	 * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
	 */
	protected void resetCommonCaches() {
		ReflectionUtils.clearCache();
		AnnotationUtils.clearCache();
		ResolvableType.clearCache();
		CachedIntrospectionResults.clearClassLoader(getClassLoader());
	}


	/**
	 * Register a shutdown hook {@linkplain Thread#getName() named}
	 * {@code SpringContextShutdownHook} with the JVM runtime, closing this
	 * context on JVM shutdown unless it has already been closed at that time.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * @see Runtime#addShutdownHook
	 * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
	 * @see #close()
	 * @see #doClose()
	 */
	@Override
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// No shutdown hook registered yet.
			this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
				@Override
				public void run() {
					synchronized (startupShutdownMonitor) {
						doClose();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
	}

	/**
	 * Callback for destruction of this instance, originally attached
	 * to a {@code DisposableBean} implementation (not anymore in 5.0).
	 * <p>The {@link #close()} method is the native way to shut down
	 * an ApplicationContext, which this method simply delegates to.
	 * @deprecated as of Spring Framework 5.0, in favor of {@link #close()}
	 */
	@Deprecated
	public void destroy() {
		close();
	}

	/**
	 * Close this application context, destroying all beans in its bean factory.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
	 * @see #doClose()
	 * @see #registerShutdownHook()
	 */
	@Override
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose();
			// If we registered a JVM shutdown hook, we don't need it anymore now:
			// We've already explicitly closed the context.
			if (this.shutdownHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				}
				catch (IllegalStateException ex) {
					// ignore - VM is already shutting down
				}
			}
		}
	}

	/**
	 * Actually performs context closing: publishes a ContextClosedEvent and
	 * destroys the singletons in the bean factory of this application context.
	 * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see #destroyBeans()
	 * @see #close()
	 * @see #registerShutdownHook()
	 */
	@SuppressWarnings("deprecation")
	protected void doClose() {
		// Check whether an actual close attempt is necessary...
		if (this.active.get() && this.closed.compareAndSet(false, true)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing " + this);
			}

			if (!NativeDetector.inNativeImage()) {
				LiveBeansView.unregisterApplicationContext(this);
			}

			try {
				// Publish shutdown event.
				publishEvent(new ContextClosedEvent(this));
			}
			catch (Throwable ex) {
				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}

			// Stop all Lifecycle beans, to avoid delays during individual destruction.
			if (this.lifecycleProcessor != null) {
				try {
					this.lifecycleProcessor.onClose();
				}
				catch (Throwable ex) {
					logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
				}
			}

			// Destroy all cached singletons in the context's BeanFactory.
			destroyBeans();

			// Close the state of this context itself.
			closeBeanFactory();

			// Let subclasses do some final clean-up if they wish...
			onClose();

			// Reset common introspection caches to avoid class reference leaks.
			resetCommonCaches();

			// Reset local application listeners to pre-refresh state.
			if (this.earlyApplicationListeners != null) {
				this.applicationListeners.clear();
				this.applicationListeners.addAll(this.earlyApplicationListeners);
			}

			// Switch to inactive.
			this.active.set(false);
		}
	}

	/**
	 * Template method for destroying all beans that this context manages.
	 * The default implementation destroy all cached singletons in this context,
	 * invoking {@code DisposableBean.destroy()} and/or the specified
	 * "destroy-method".
	 * 用于销毁此上下文管理的所有 Bean 的模板方法。默认实现会销毁此上下文中所有缓存的单例，调用 {@code DisposableBean.destroy（）} 和/或指定的“destroy-method”。
	 * <p>Can be overridden to add context-specific bean destruction steps
	 * right before or right after standard singleton destruction,
	 * while the context's BeanFactory is still active.
	 * 可以重写以在标准单例销毁之前或之后添加特定于上下文的 Bean 销毁步骤，而上下文的 BeanFactory 仍处于活动状态。
	 * @see #getBeanFactory()
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
	 */
	protected void destroyBeans() {
		getBeanFactory().destroySingletons();
	}

	/**
	 * Template method which can be overridden to add context-specific shutdown work.
	 * The default implementation is empty.
	 * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
	 * this context's BeanFactory has been closed. If custom shutdown logic
	 * needs to execute while the BeanFactory is still active, override
	 * the {@link #destroyBeans()} method instead.
	 */
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	@Override
	public boolean isActive() {
		return this.active.get();
	}

	/**
	 * Assert that this context's BeanFactory is currently active,
	 * throwing an {@link IllegalStateException} if it isn't.
	 * <p>Invoked by all {@link BeanFactory} delegation methods that depend
	 * on an active context, i.e. in particular all bean accessor methods.
	 * <p>The default implementation checks the {@link #isActive() 'active'} status
	 * of this context overall. May be overridden for more specific checks, or for a
	 * no-op if {@link #getBeanFactory()} itself throws an exception in such a case.
	 */
	protected void assertBeanFactoryActive() {
		if (!this.active.get()) {
			if (this.closed.get()) {
				throw new IllegalStateException(getDisplayName() + " has been closed already");
			}
			else {
				throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, requiredType);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, args);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType, args);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isPrototype(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name, allowFactoryBeanInit);
	}

	@Override
	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType, allowFactoryBeanInit);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	@Override
	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * Return the internal bean factory of the parent context if it implements
	 * ConfigurableApplicationContext; else, return the parent context itself.
	 * 如果父上下文实现了 ConfigurableApplicationContext，则返回父上下文的内部 Bean 工厂;否则，返回父上下文本身。
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	@Nullable
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------

	@Override
	public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * Return the internal MessageSource used by the context.
	 * @return the internal MessageSource (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " +
					"call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	/**
	 * Return the internal message source of the parent context if it is an
	 * AbstractApplicationContext too; else, return the parent context itself.
	 */
	@Nullable
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext ?
				((AbstractApplicationContext) getParent()).messageSource : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	//---------------------------------------------------------------------
	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	@Override
	public void start() {
		getLifecycleProcessor().start();
		publishEvent(new ContextStartedEvent(this));
	}

	@Override
	public void stop() {
		getLifecycleProcessor().stop();
		publishEvent(new ContextStoppedEvent(this));
	}

	@Override
	public boolean isRunning() {
		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Subclasses must implement this method to perform the actual configuration load.
	 * The method is invoked by {@link #refresh()} before any other initialization work.
	 * 子类必须实现此方法才能执行实际的配置加载。在进行任何其他初始化化工作之前，该方法由{@link #refresh()}调用。
	 *
	 * <p>A subclass will either create a new bean factory and hold a reference to it,
	 * or return a single BeanFactory instance that it holds. In the latter case, it will
	 * usually throw an IllegalStateException if refreshing the context more than once.
	 * 子类创建一个新的bean工厂并保留对其的引用，或者返回它持有的单个BeanFactory实例。
	 * 在后一种情况下，如果多次刷新上下文，通常会抛出IllegalStateException
	 *
	 * @throws BeansException if initialization of the bean factory failed
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * Subclasses must implement this method to release their internal bean factory.
	 * This method gets invoked by {@link #close()} after all other shutdown work.
	 * <p>Should never throw an exception but rather log shutdown failures.
	 */
	protected abstract void closeBeanFactory();

	/**
	 * Subclasses must return their internal bean factory here. They should implement the
	 * lookup efficiently, so that it can be called repeatedly without a performance penalty.
	 *
	 * 子类必须在此返回其内部bean工厂。它们应该有效地实现查找，以便可以重复调用它而不影响性能
	 *
	 * <p>Note: Subclasses should check whether the context is still active before
	 * returning the internal bean factory. The internal factory should generally be
	 * considered unavailable once the context has been closed.
	 * 注意：子类应在返回内部bean工厂之前检验上下文是否仍处于活动状态。一旦关闭上下文，通常应将内部工厂视为不可用
	 *
	 * @return this application context's internal bean factory (never {@code null}) -- 此应用程序上下文的内部bean工厂（永远不为{@code null})
	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
	 * (usually if {@link #refresh()} has never been called) or if the context has been
	 * closed already
	 * 如果上下文尚未拥有内部bean工厂（通常是从未调用过{@link #refresh()}),或上下文已经关闭
	 *
	 * @see #refreshBeanFactory()
	 * @see #closeBeanFactory()
	 */
	@Override
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


	/**
	 * Return information about this context.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getDisplayName());
		sb.append(", started on ").append(new Date(getStartupDate()));
		ApplicationContext parent = getParent();
		if (parent != null) {
			sb.append(", parent: ").append(parent.getDisplayName());
		}
		return sb.toString();
	}

}
