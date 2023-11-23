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

package org.springframework.context.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.annotation.AliasFor;

/**
 * Annotation that marks a method as a listener for application events.
 *
 * 将方法标记为应用程序事件监听器的注解。
 *
 * <p>If an annotated method supports a single event type, the method may
 * declare a single parameter that reflects the event type to listen to.
 * If an annotated method supports multiple event types, this annotation
 * may refer to one or more supported event types using the {@code classes}
 * attribute. See the {@link #classes} javadoc for further details.
 *
 * 如果带注解的方法支持单个事件类型，则该方法可以声明反映要侦听的事件类型的单个参数。如果带注解的方法支持多个事件类型，则此注解可以使用 {@code classes} 属性引用一个或多个受支持的事件类型。
 * 有关详细信息，请参阅 {@link #classes} javadoc。
 *
 * <p>Events can be {@link ApplicationEvent} instances as well as arbitrary
 * objects.
 *
 * 事件可以是 {@link ApplicationEvent} 实例，也可以是任意对象。
 *
 * <p>Processing of {@code @EventListener} annotations is performed via
 * the internal {@link EventListenerMethodProcessor} bean which gets
 * registered automatically when using Java config or manually via the
 * {@code <context:annotation-config/>} or {@code <context:component-scan/>}
 * element when using XML config.
 *
 * {@code @EventListener} 注解的处理是通过内部 {@link EventListenerMethodProcessor} bean 执行的，该 bean 在使用 Java 配置时会自动注册，
 * 或者在使用 XML 配置时通过 {@code <context: annotation-config>} 或 {@code <context: component-scan>} 元素手动注册。
 *
 * <p>Annotated methods may have a non-{@code void} return type. When they
 * do, the result of the method invocation is sent as a new event. If the
 * return type is either an array or a collection, each element is sent
 * as a new individual event.
 *
 * 带注解的方法可能具有非 {@code void} 返回类型。当他们这样做时，方法调用的结果将作为新事件发送。如果返回类型是数组或集合，则每个元素都作为新的单个事件发送。
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * 此注释可用作元注释，以创建自定义组合注释。
 *
 * <h3>Exception Handling</h3>
 *
 * 异常处理
 *
 * <p>While it is possible for an event listener to declare that it
 * throws arbitrary exception types, any checked exceptions thrown
 * from an event listener will be wrapped in an
 * {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException}
 * since the event publisher can only handle runtime exceptions.
 *
 * 虽然事件监听器可以声明它抛出任意异常类型，但从事件监听器抛出的任何受检的异常都将包装在 {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException} 中，
 * 因为事件发布者只能处理运行时异常。
 *
 * <h3>Asynchronous Listeners</h3>
 *
 * 异步侦听器
 *
 * <p>If you want a particular listener to process events asynchronously, you
 * can use Spring's {@link org.springframework.scheduling.annotation.Async @Async}
 * support, but be aware of the following limitations when using asynchronous events.
 *
 * 如果您希望特定监听器异步处理事件，则可以使用 Spring 的 {@link org.springframework.scheduling.annotation.Async @Async} 支持，但在使用异步事件时请注意以下限制。
 *
 * <ul>
 * <li>If an asynchronous event listener throws an exception, it is not propagated
 * to the caller. See {@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
 * AsyncUncaughtExceptionHandler} for more details.</li>
 *
 * 如果异步事件监听器引发异常，则不会将其传播到调用方。有关详细信息，请参阅 {@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler AsyncUncaughtExceptionHandler}。
 *
 * <li>Asynchronous event listener methods cannot publish a subsequent event by returning a
 * value. If you need to publish another event as the result of the processing, inject an
 * {@link org.springframework.context.ApplicationEventPublisher ApplicationEventPublisher}
 * to publish the event manually.</li>
 *
 * 异步事件监听器方法无法通过返回值来发布后续事件。如果需要发布另一个事件作为处理的结果，
 * 请注入 {@link org.springframework.context.ApplicationEventPublisher ApplicationEventPublisher} 以手动发布事件。
 *
 * </ul>
 *
 * <h3>Ordering Listeners</h3>
 *
 * 对侦听器进行排序
 *
 * <p>It is also possible to define the order in which listeners for a
 * certain event are to be invoked. To do so, add Spring's common
 * {@link org.springframework.core.annotation.Order @Order} annotation
 * alongside this event listener annotation.
 *
 * 还可以定义调用特定事件的监听器的顺序。为此，请在此事件监听器注解旁边添加 Spring 的通用 {@link org.springframework.core.annotation.Order @Order} 注解。
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.2
 * @see EventListenerMethodProcessor
 * @see org.springframework.transaction.event.TransactionalEventListener
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {

	/**
	 * Alias for {@link #classes}.
	 */
	@AliasFor("classes")
	Class<?>[] value() default {};

	/**
	 * The event classes that this listener handles.
	 * <p>If this attribute is specified with a single value, the
	 * annotated method may optionally accept a single parameter.
	 * However, if this attribute is specified with multiple values,
	 * the annotated method must <em>not</em> declare any parameters.
	 */
	@AliasFor("value")
	Class<?>[] classes() default {};

	/**
	 * Spring Expression Language (SpEL) expression used for making the event
	 * handling conditional.
	 * <p>The event will be handled if the expression evaluates to boolean
	 * {@code true} or one of the following strings: {@code "true"}, {@code "on"},
	 * {@code "yes"}, or {@code "1"}.
	 * <p>The default expression is {@code ""}, meaning the event is always handled.
	 * <p>The SpEL expression will be evaluated against a dedicated context that
	 * provides the following metadata:
	 * <ul>
	 * <li>{@code #root.event} or {@code event} for references to the
	 * {@link ApplicationEvent}</li>
	 * <li>{@code #root.args} or {@code args} for references to the method
	 * arguments array</li>
	 * <li>Method arguments can be accessed by index. For example, the first
	 * argument can be accessed via {@code #root.args[0]}, {@code args[0]},
	 * {@code #a0}, or {@code #p0}.</li>
	 * <li>Method arguments can be accessed by name (with a preceding hash tag)
	 * if parameter names are available in the compiled byte code.</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * An optional identifier for the listener, defaulting to the fully-qualified
	 * signature of the declaring method (e.g. "mypackage.MyClass.myMethod()").
	 * @since 5.3.5
	 * @see SmartApplicationListener#getListenerId()
	 * @see ApplicationEventMulticaster#removeApplicationListeners(Predicate)
	 */
	String id() default "";

}
