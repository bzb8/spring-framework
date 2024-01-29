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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 * 委托AbstractApplicationContext进行后置处理器处理
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		// 警告：虽然看起来可以轻松重构此方法的主体以避免使用多个循环和多个列表，但使用多个列表和多次传递处理器名称是有意的。
		// 我们必须确保履行优先订购和订购处理器的合同。具体来说，我们不能导致处理器被实例化（通过 getBean() 调用）或以错误的顺序在 ApplicationContext 中注册。
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// 在提交更改此方法的拉取请求 (PR) 之前，请查看所有涉及 PostProcessorRegistrationDelegate 更改的被拒绝 PR 的列表，
		// 以确保您的提案不会导致重大更改：https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		// 如果有的话，首先调用 BeanDefinitionRegistryPostProcessors;。
		// 存储已经处理完的BeanPostPorcessor对象Bean名称
		Set<String> processedBeans = new HashSet<>();

		// 处理实现了BeanDefinitionRegistry接口的处理工厂类

		// 如果bean工厂是BeanDefinitionRegistry的实例。
		if (beanFactory instanceof BeanDefinitionRegistry) { // true
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			// 常规后处理器
			// beanFactoryPostProcessors中非BeanDefinitionRegistryPostProcessor类型的BeanFactoryPostProcessor对象的集合
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// 存放BeanFactory中BeanDefinitionRegistryPostProcessor对象集合
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			// 对beanFactoryPostProcessors进行分类: List<BeanFactoryPostProcessor>和List<BeanDefinitionRegistryPostProcessor>
			// 并应用BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法

			//遍历添加进来的BeanFactouryPostProcessor对象，即通过
			// 	ConfigurableApplicationContext#addBeanFactoryPostProcessor方式添加
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor = (BeanDefinitionRegistryPostProcessor) postProcessor;
					// 回调BeanDefinitionRegistryPostProcessor接口方法，传入当前BeanDefinitionRegistry对象，
					// 使得registryProcessor可以通过registry来注册BeanDefinition对象
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 将registryProcessor添加到BeanDefinition注册器后置处理器集合中
					registryProcessors.add(registryProcessor);
				}
				else {
					// 将postProcessor添加到BeanFactory后置处理器集合中
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			// 不要在这里初始化FactoryBeans：我们需要保留所有未初始化的常规bean。以便
			// 让bean工厂后置处理器对其应用！将实现 PriorityOrdered,Ordered和其余的
			// beanDefinitionRegistryPostProcessor分开

			// 当前BeanDefinition注册器的后置处理器集合
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.

			// 首先，调用实现PriorityOrder的BeanFactoryRegistryPostProcessors
			// 从bean工厂中获取BeanDefinitionRegistryPostProcessor的类名，包括原型或作用域bean或仅包含单例，
			// 并不初始化FactoryBean和'factory-bean'引用

			// 首先，调用实现PriorityOrder的BeanFactoryRegistryPostProcessors
			// 从bean工厂中获取BeanDefinitionRegistryPostProcessor的类名，包括原型作用域bean或仅包含单例，
			// 并不初始化FactoryBean和'factory-bean'引用
			String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				// 如果在beanFactory中ppName与PriorityOrdered匹配
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					// 查找并实例化BeanDefinitionRegistryPostProcessor
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			// 调用 currentRegistryProcessors 的每个 BeanDefinitionRegistryPostProcessor 对象的 postProcessBeanDefinitionRegistry 方法，
			// 以使得每个BeanDefinitionRegistryPostProcessor 对象都可以往 registry 注册 BeanDefinition 对象
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			// 清空currentRegistryProcessors
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			// 接下来，调用实现 Ordered 的 BeanDefinitionRegistryPostProcessors 对象
			// 重新从bean工厂中获取BeanDefinitionRegistryPostProcessor的类名，包括原型或作用域bean或仅包含单例，
			// 并不初始化FactoryBean和'factory-bean'引用
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 最后，调用所有其他 BeanDefinitionRegistryPostProcessor 直到不再出现。
			// 定义反复地做的标记
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			// 现在，调用迄今为止处理的所有处理器的 postProcessBeanFactory 回调。
			// 回调 registryProcessors 的每个BeanFactoryPostProcessor对象 的 postProcessBeanFactory 方法，使得每个BeanFactoryPostProcessor对象
			// 都可以对 beanFactory进行调整
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			// 调用向上下文实例注册的工厂处理器。
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// 处理实现了BeanFactoryPostProcessor接口的处理工厂类

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		// 不要在这里初始化 FactoryBeans：我们需要让所有常规 bean 保持未初始化状态，以便让 bean 工厂后处理器应用到它们！
		// org.springframework.context.annotation.internalConfigurationAnnotationProcessor
		// org.springframework.context.event.internalEventListenerProcessor
		// 重新从bean工厂中获取BeanFactoryPostProcessor类的Bean名，包括原型或作用域bean或仅包含单例，
		// 并不初始化FactoryBean和'factory-bean'引用
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		// 在实现 PriorityOrdered、Ordered 和其他 BeanFactoryPostProcessors 之间分开。
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
				// 跳过 - 已在上面的第一阶段处理
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		// 清除缓存的合并 Bean 定义，因为后处理器可能修改了原始元数据，例如替换值中的占位符......
		// 清除缓存的合并BeanDefinition，因为后处理器可能修改了原始元数据，例如替换值中的占位符
		beanFactory.clearMetadataCache();
	}

	/**
	 * 注册 BeanPostProcessors 到 beanFactory 中
	 * <ol>
	 *  <li>先注册一个 BeanPostProcessorChecker 类型的 BeanPostProcessor 到 beanFactory 中 </li>
	 *  <li>获取beanFactory中所有 非MergedBeanDefinitionPostProcessor类型的 BeanPostProcessor类型的Bean对象，按
	 *  PriorityOrdered -> Ordered -> 普通的顺序 注册到BeanFactory中</li>
	 *  <li>获取beanFactory中所有MergedBeanDefinitionPostProcessor类型的 BeanPostProcessor类型的Bean对象，
	 *  按 PriorityOrdered -> Ordered -> 普通的顺序 注册到BeanFactory中</li>
	 *  <li>新建一个ApplicationListernerDeteletor类型的BeanPostProcessor，将其注册到beanFactory</li>
	 * </ol>
	 * @param beanFactory bean工厂
	 * @param applicationContext 应用程序上下文
	 */
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		// 警告：尽管此方法的主体似乎可以很容易地重构以避免使用多个循环和多个列表，但使用多个列表和多次传递处理器名称是有意为之的。
		// 我们必须确保遵守 PriorityOrdered 和 Ordered 处理器的合同。具体来说，我们不能导致处理器被实例化（通过 getBean（） 调用）或以错误的顺序在 ApplicationContext 中注册。
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22
		// 在提交拉取请求 （PR） 以更改此方法之前，请查看涉及对 PostProcessorRegistrationDelegate 的更改的所有被拒绝的 PR 列表，以确保您的提案不会导致中断性变更：

		// 获取beanFactory中与BeanPostProcessor.class（包括子类）匹配的bean名称，如果是FactoryBeans会根据beanDefinition或
		// getObjectType的值判断
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.

		// 注册BeanPostProcessChecker,当Bean在BeanPostProcessor实例化过程中创建时，即当一个Bean不适合
		// 被所有BeanPostProcessor处理时，记录一个信息消息
		// 计算BeanProcessor目标数量：beanFactory注册的BeanPostProcessor数量+1+beanFactory中与BeanPostProcessor.class
		// 	（包括子类）匹配的bean名称数量
		// 这里的 +1 是指 BeanPostProcessorChecker
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		// 将实现 PriorityOrdered、Ordered 和其他的 BeanPostProcessor 分开。
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// 存放 MergedBeanDefinitionPostProcessor 对象 的列表
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 缓存给定的BeanPostProcessors，存入给定的beanPostProcessors属性中
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		// 最后，重新注册所有内部 BeanPostProcessor。
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		// 重新注册用于检测内部 bean 的后处理器作为 ApplicationListener，将其移动到处理器链的末尾（用于获取代理等）。
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	/**
	 * 按照Order接口的order值从小到大排序
	 * 对 postProcessors 进行排序，优先使用beanFactory的DependencyComparator，获取不到就使用OrderComparator.INSTANCE;
	 * @param postProcessors
	 * @param beanFactory
	 */
	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		// 定义要用于排序的比较器
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			// 获取beanfatroy的依赖关系比较器
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			// OrderComparator.INSTANCE:有序对象的比较实现，按顺序值升序或优先级降序排序,优先级由上往下：
			// 1.PriorityOrderd对象
			// 2.一些Order对象
			// 3.无顺序对象
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 * 调用给定的 BeanDefinitionRegistryPostProcessor bean。
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry, ApplicationStartup applicationStartup) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanDefinitionRegistry(registry);
			postProcessBeanDefRegistry.end();
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanFactory(beanFactory);
			postProcessBeanFactory.end();
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		if (beanFactory instanceof AbstractBeanFactory) {
			// Bulk addition is more efficient against our CopyOnWriteArrayList there
			// 批量添加对我们的 CopyOnWriteArrayList 更有效
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		}
		else {
			for (BeanPostProcessor postProcessor : postProcessors) {
				beanFactory.addBeanPostProcessor(postProcessor);
			}
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 * 注册 BeanPostProcessorChecker，在 BeanPostProcessor 实例化期间创建 bean 时（即，当 bean 不符合所有 BeanPostProcessor 处理条件时）记录一条信息消息。
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			// 如果 bean不是BeanPostProcessor实例 && beanName 不是 完全内部使用 && beanFactory当前注册的BeanPostProcessor
			// 数量小于 BeanPostProcessor目标数量
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					// 打印 信息 日志：BeanPostProcessorChecker检查到xxx不适合所有的BeanPostProcessor来处理。即，
					// 	存在出现“自动注入”不合适或无效的信息
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
