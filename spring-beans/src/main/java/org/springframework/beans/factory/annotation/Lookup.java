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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that indicates 'lookup' methods, to be overridden by the container
 * to redirect them back to the {@link org.springframework.beans.factory.BeanFactory}
 * for a {@code getBean} call. This is essentially an annotation-based version of the
 * XML {@code lookup-method} attribute, resulting in the same runtime arrangement.
 *
 * 指示“lookup”方法的注释，由容器覆盖以将它们重定向回{@code getBean}调用的{@link org.springframework.beans.factory.BeanFactory}。
 * 这实质上是 XML {@code lookup-method} 属性的基于注解的版本，从而产生相同的运行时安排。
 *
 * <p>The resolution of the target bean can either be based on the return type
 * ({@code getBean(Class)}) or on a suggested bean name ({@code getBean(String)}),
 * in both cases passing the method's arguments to the {@code getBean} call
 * for applying them as target factory method arguments or constructor arguments.
 *
 * 目标 Bean 的解析可以基于返回类型 （{@code getBean(Class)） 或建议的 Bean 名称 （{@code getBean(String)}），在这两种情况下，都将方法的参数传递给 {@code getBean} 调用，
 * 以便将它们应用为目标工厂方法参数或构造函数参数。
 *
 * <p>Such lookup methods can have default (stub) implementations that will simply
 * get replaced by the container, or they can be declared as abstract - for the
 * container to fill them in at runtime. In both cases, the container will generate
 * runtime subclasses of the method's containing class via CGLIB, which is why such
 * lookup methods can only work on beans that the container instantiates through
 * regular constructors: i.e. lookup methods cannot get replaced on beans returned
 * from factory methods where we cannot dynamically provide a subclass for them.
 *
 * 此类查找方法可以具有默认（存根）实现，这些实现将简单地被容器替换，或者它们可以声明为抽象 - 以便容器在运行时填充它们。
 * 在这两种情况下，容器都会通过 CGLIB 生成方法包含类的运行时子类，这就是为什么这种查找方法只能在容器通过常规构造函数实例化的 bean 上工作的原因：
 * 即，在从工厂方法返回的 bean 上，查找方法不能被替换，因为我们无法为它们动态提供子类。
 *
 * <p><b>Recommendations for typical Spring configuration scenarios:</b>
 * When a concrete class may be needed in certain scenarios, consider providing stub
 * implementations of your lookup methods. And please remember that lookup methods
 * won't work on beans returned from {@code @Bean} methods in configuration classes;
 * you'll have to resort to {@code @Inject Provider<TargetBean>} or the like instead.
 *
 * 针对典型 Spring 配置方案的建议：当在某些情况下可能需要具体类时，请考虑提供查找方法的存根实现。
 * 请记住，查找方法不适用于从配置类中的 {@code @Bean} 方法返回的 bean;您将不得不求助于 {@code @Inject Provider<TargetBean>} 或类似方法。
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class, Object...)
 * @see org.springframework.beans.factory.BeanFactory#getBean(String, Object...)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lookup {

	/**
	 * This annotation attribute may suggest a target bean name to look up.
	 * If not specified, the target bean will be resolved based on the
	 * annotated method's return type declaration.
	 */
	String value() default "";

}
