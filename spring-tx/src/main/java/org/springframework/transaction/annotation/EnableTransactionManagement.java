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

package org.springframework.transaction.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Enables Spring's annotation-driven transaction management capability, similar to
 * the support found in Spring's {@code <tx:*>} XML namespace. To be used on
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * classes to configure traditional, imperative transaction management or
 * reactive transaction management.
 * <p>启用Spring的注解驱动的事务管理能力，类似于Spring的{@code <tx:*>} XML命名空间中的支持。该注解用于
 * {@link org.springframework.context.annotation.Configuration @Configuration} 类上，
 * 用于配置传统的、命令式的事务管理或反应式事务管理。
 *
 * <p>The following example demonstrates imperative transaction management
 * using a {@link org.springframework.transaction.PlatformTransactionManager
 * PlatformTransactionManager}. For reactive transaction management, configure a
 * {@link org.springframework.transaction.ReactiveTransactionManager
 * ReactiveTransactionManager} instead.
 * <p>以下示例演示了使用 PlatformTransactionManager 的命令式事务管理。对于反应式事务管理，请配置 ReactiveTransactionManager。</p>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTransactionManagement
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         // configure and return a class having &#064;Transactional methods
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // configure and return the necessary JDBC DataSource
 *     }
 *
 *     &#064;Bean
 *     public PlatformTransactionManager txManager() {
 *         return new DataSourceTransactionManager(dataSource());
 *     }
 * }</pre>
 *
 * <p>For reference, the example above can be compared to the following Spring XML
 * configuration:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;tx:annotation-driven/&gt;
 *
 *     &lt;bean id="fooRepository" class="com.foo.JdbcFooRepository"&gt;
 *         &lt;constructor-arg ref="dataSource"/&gt;
 *     &lt;/bean&gt;
 *
 *     &lt;bean id="dataSource" class="com.vendor.VendorDataSource"/&gt;
 *
 *     &lt;bean id="transactionManager" class="org.sfwk...DataSourceTransactionManager"&gt;
 *         &lt;constructor-arg ref="dataSource"/&gt;
 *     &lt;/bean&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * In both of the scenarios above, {@code @EnableTransactionManagement} and {@code
 * <tx:annotation-driven/>} are responsible for registering the necessary Spring
 * components that power annotation-driven transaction management, such as the
 * TransactionInterceptor and the proxy- or AspectJ-based advice that weaves the
 * interceptor into the call stack when {@code JdbcFooRepository}'s {@code @Transactional}
 * methods are invoked.
 * 在上述两种场景中，@EnableTransactionManagement 和 <tx:annotation-driven/> 负责注册必要的Spring组件，以支持注解驱动的事务管理，
 * 例如TransactionInterceptor和代理或AspectJ为基础的建议，当调用JdbcFooRepository的@Transactional方法时，这些组件会被编织进调用栈中。
 *
 * <p>A minor difference between the two examples lies in the naming of the {@code
 * TransactionManager} bean: In the {@code @Bean} case, the name is
 * <em>"txManager"</em> (per the name of the method); in the XML case, the name is
 * <em>"transactionManager"</em>. {@code <tx:annotation-driven/>} is hard-wired to
 * look for a bean named "transactionManager" by default, however
 * {@code @EnableTransactionManagement} is more flexible; it will fall back to a by-type
 * lookup for any {@code TransactionManager} bean in the container. Thus the name
 * can be "txManager", "transactionManager", or "tm": it simply does not matter.
 * 两个示例之间的一个小差异在于TransactionManager bean的命名：在@Bean情况下，名称为"txManager"（根据方法的名称）；
 * 在XML情况下，名称为"transactionManager"。虽然tx:annotation-driven/默认硬编码为查找名为"transactionManager"的bean，
 * 但@EnableTransactionManagement更具灵活性；它会回退到容器中任何TransactionManager bean的按类型查找。因此，
 * 名称可以是"txManager"、"transactionManager"或"tm"，这根本不重要。
 *
 * <p>For those that wish to establish a more direct relationship between
 * {@code @EnableTransactionManagement} and the exact transaction manager bean to be used,
 * the {@link TransactionManagementConfigurer} callback interface may be implemented -
 * notice the {@code implements} clause and the {@code @Override}-annotated method below:
 * 对于希望在@EnableTransactionManagement和将要使用的确切事务管理器bean之间建立更直接关系的人，
 * 可以实现TransactionManagementConfigurer回调接口——请注意以下代码中的实现条款和带有@Override注解的方法：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTransactionManagement
 * public class AppConfig implements TransactionManagementConfigurer {
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         // configure and return a class having &#064;Transactional methods
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // configure and return the necessary JDBC DataSource
 *     }
 *
 *     &#064;Bean
 *     public PlatformTransactionManager txManager() {
 *         return new DataSourceTransactionManager(dataSource());
 *     }
 *
 *     &#064;Override
 *     public PlatformTransactionManager annotationDrivenTransactionManager() {
 *         return txManager();
 *     }
 * }</pre>
 *
 * <p>This approach may be desirable simply because it is more explicit, or it may be
 * necessary in order to distinguish between two {@code TransactionManager} beans
 * present in the same container.  As the name suggests, the
 * {@code annotationDrivenTransactionManager()} will be the one used for processing
 * {@code @Transactional} methods. See {@link TransactionManagementConfigurer} Javadoc
 * for further details.
 * 这种做法可能只是因为更明确而被期望，或者可能是因为需要区分同一容器中两个TransactionManager bean而成为必要。
 * 如名称所示，annotationDrivenTransactionManager()将用于处理@Transactional方法。有关进一步详细信息，
 * 请参阅TransactionManagementConfigurer Javadoc。
 *
 * <p>The {@link #mode} attribute controls how advice is applied: If the mode is
 * {@link AdviceMode#PROXY} (the default), then the other attributes control the behavior
 * of the proxying. Please note that proxy mode allows for interception of calls through
 * the proxy only; local calls within the same class cannot get intercepted that way.
 * mode属性控制如何应用建议：如果模式为AdviceMode.PROXY（默认值），则其他属性将控制代理行为的。
 * 请注意，代理模式只允许通过代理进行调用的拦截；相同类内的本地调用不能以这种方式被拦截。
 *
 * <p>Note that if the {@linkplain #mode} is set to {@link AdviceMode#ASPECTJ}, then the
 * value of the {@link #proxyTargetClass} attribute will be ignored. Note also that in
 * this case the {@code spring-aspects} module JAR must be present on the classpath, with
 * compile-time weaving or load-time weaving applying the aspect to the affected classes.
 * There is no proxy involved in such a scenario; local calls will be intercepted as well.
 * 请注意，如果将模式设置为AdviceMode.ASPECTJ，则proxyTargetClass属性的值将被忽略。还请注意，在这种情况下，
 * 必须在类路径上存在spring-aspects模块JAR，并且编译时编织或加载时编织将方面应用到受影响的类中。
 * 在这种情况下没有代理参与；本地调用也将被拦截。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see TransactionManagementConfigurer
 * @see TransactionManagementConfigurationSelector
 * @see ProxyTransactionManagementConfiguration
 * @see org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TransactionManagementConfigurationSelector.class)
public @interface EnableTransactionManagement {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created ({@code true}) as
	 * opposed to standard Java interface-based proxies ({@code false}). The default is
	 * {@code false}. <strong>Applicable only if {@link #mode()} is set to
	 * {@link AdviceMode#PROXY}</strong>.
	 * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
	 * Spring-managed beans requiring proxying, not just those marked with
	 * {@code @Transactional}. For example, other beans marked with Spring's
	 * {@code @Async} annotation will be upgraded to subclass proxying at the same
	 * time. This approach has no negative impact in practice unless one is explicitly
	 * expecting one type of proxy vs another, e.g. in tests.
	 * 它的作用是指示是否创建基于子类的（CGLIB）代理（返回值为true），而不是标准的Java接口代理（返回值为false）。默认值为false。
	 * 注意，该属性仅在#mode()被设置为AdviceMode.PROXY时适用。另外，将此属性设置为true将会影响所有需要代理的Spring管理的bean，
	 * 而不仅仅是标记为@Transactional的bean。例如，其他标记为Spring的@Async注解的bean将同时被升级为子类代理。
	 * 在实践中，这种方法没有负面影响，除非明确期望一种代理类型而不是另一种
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate how transactional advice should be applied.
	 * <p><b>The default is {@link AdviceMode#PROXY}.</b>
	 * Please note that proxy mode allows for interception of calls through the proxy
	 * only. Local calls within the same class cannot get intercepted that way; an
	 * {@link Transactional} annotation on such a method within a local call will be
	 * ignored since Spring's interceptor does not even kick in for such a runtime
	 * scenario. For a more advanced mode of interception, consider switching this to
	 * {@link AdviceMode#ASPECTJ}.
	 * <p>该函数用于指示事务性建议应该如何应用。默认情况下，采用AdviceMode#PROXY模式。需要注意的是，代理模式只允许拦截通过代理的调用，
	 * 而在同一类内部的本地调用无法被拦截。如果在本地调用中使用Transactional注解，它将被忽略，因为Spring的拦截器在这种运行时场景下不会起作用。
	 * 如果需要更高级的拦截模式，可以考虑将此切换为AdviceMode#ASPECTJ
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * Indicate the ordering of the execution of the transaction advisor
	 * when multiple advices are applied at a specific joinpoint.
	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
	 * 当在特定连接点上应用多个通知时，指示事务顾问程序的执行顺序。 默认值是Ordered.LOWEST_PRECEDENCE
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
