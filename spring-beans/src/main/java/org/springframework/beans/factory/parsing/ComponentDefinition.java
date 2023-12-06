/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * Interface that describes the logical view of a set of {@link BeanDefinition BeanDefinitions}
 * and {@link BeanReference BeanReferences} as presented in some configuration context.
 *
 * 描述在某些配置上下文中呈现的一组 {@link BeanDefinition BeanDefinitions} 和 {@link BeanReference BeanReferences} 的逻辑视图的接口。
 *
 * <p>With the introduction of {@link org.springframework.beans.factory.xml.NamespaceHandler pluggable custom XML tags},
 * it is now possible for a single logical configuration entity, in this case an XML tag, to
 * create multiple {@link BeanDefinition BeanDefinitions} and {@link BeanReference RuntimeBeanReferences}
 * in order to provide more succinct configuration and greater convenience to end users. As such, it can
 * no longer be assumed that each configuration entity (e.g. XML tag) maps to one {@link BeanDefinition}.
 * For tool vendors and other users who wish to present visualization or support for configuring Spring
 * applications it is important that there is some mechanism in place to tie the {@link BeanDefinition BeanDefinitions}
 * in the {@link org.springframework.beans.factory.BeanFactory} back to the configuration data in a way
 * that has concrete meaning to the end user. As such, {@link org.springframework.beans.factory.xml.NamespaceHandler}
 * implementations are able to publish events in the form of a {@code ComponentDefinition} for each
 * logical entity being configured. Third parties can then {@link ReaderEventListener subscribe to these events},
 * allowing for a user-centric view of the bean metadata.
 *
 * 随着{@link org.springframework.beans.factory.xml.NamespaceHandler 可插入自定义XML标签}的引入，
 * 现在可以使用单个逻辑配置实体（在本例中为XML标签）创建多个{@link BeanDefinition BeanDefinition } 和
 * {@link BeanReference RuntimeBeanReferences} 以便为最终用户提供更简洁的配置和更大的便利。
 * 因此，不能再假设每个配置实体（例如 XML 标签）映射到一个 {@link BeanDefinition}。
 * 对于希望提供可视化或支持配置 Spring 应用程序的工具供应商和其他用户来说，有一些机制可以将 {@link BeanDefinition BeanDefinitions}
 * 绑定在 {@link org.springframework.beans.factory.BeanFactory 中以对最终用户具有具体意义的方式返回配置数据。
 * 因此，{@link org.springframework.beans.factory.xml.NamespaceHandler} 实现能够以 {@code ComponentDefinition} 的形式为每个正在配置的逻辑实体发布事件。
 * 然后，第三方可以{@link ReaderEventListener 订阅这些事件}，从而实现以用户为中心的 bean 元数据视图。
 *
 * <p>Each {@code ComponentDefinition} has a {@link #getSource source object} which is configuration-specific.
 * In the case of XML-based configuration this is typically the {@link org.w3c.dom.Node} which contains the user
 * supplied configuration information. In addition to this, each {@link BeanDefinition} enclosed in a
 * {@code ComponentDefinition} has its own {@link BeanDefinition#getSource() source object} which may point
 * to a different, more specific, set of configuration data. Beyond this, individual pieces of bean metadata such
 * as the {@link org.springframework.beans.PropertyValue PropertyValues} may also have a source object giving an
 * even greater level of detail. Source object extraction is handled through the
 * {@link SourceExtractor} which can be customized as required.
 *
 * 每个 {@code ComponentDefinition} 都有一个特定于配置的 {@link #getSource 源对象}。
 * 在基于 XML 的配置的情况下，这通常是包含用户提供的配置信息的 {@link org.w3c.dom.Node}。
 * 除此之外，包含在 {@code ComponentDefinition} 中的每个 {@link BeanDefinition} 都有自己的 {@link BeanDefinition#getSource() 源对象}，
 * 该对象可能指向一组不同的、更具体的配置数据。除此之外，各个 bean 元数据（例如 {@link org.springframework.beans.PropertyValue PropertyValues}）
 * 也可能有一个提供更高级别详细信息的源对象。源对象提取是通过 {@link SourceExtractor} 处理的，可以根据需要进行自定义。
 *
 * <p>Whilst direct access to important {@link BeanReference BeanReferences} is provided through
 * {@link #getBeanReferences}, tools may wish to inspect all {@link BeanDefinition BeanDefinitions} to gather
 * the full set of {@link BeanReference BeanReferences}. Implementations are required to provide
 * all {@link BeanReference BeanReferences} that are required to validate the configuration of the
 * overall logical entity as well as those required to provide full user visualization of the configuration.
 * It is expected that certain {@link BeanReference BeanReferences} will not be important to
 * validation or to the user view of the configuration and as such these may be omitted. A tool may wish to
 * display any additional {@link BeanReference BeanReferences} sourced through the supplied
 * {@link BeanDefinition BeanDefinitions} but this is not considered to be a typical case.
 *
 * 虽然通过 {@link #getBeanReferences} 提供对重要的 {@link BeanReference BeanReferences} 的直接访问，
 * 但工具可能希望检查所有 {@link BeanDefinition BeanDefinitions} 以收集完整的 {@link BeanReference BeanReferences} 集。
 * 实现需要提供验证整个逻辑实体的配置以及提供配置的完整用户可视化所需的所有{@link BeanReference BeanReferences}。
 * 预计某些{@link BeanReference BeanReferences}对于验证或配置的用户视图并不重要，因此可以省略它们。
 * 工具可能希望显示通过提供的 {@link BeanDefinition BeanDefinitions} 获取的任何其他 {@link BeanReference BeanReferences}，但这并不被视为典型情况。
 *
 * <p>Tools can determine the importance of contained {@link BeanDefinition BeanDefinitions} by checking the
 * {@link BeanDefinition#getRole role identifier}. The role is essentially a hint to the tool as to how
 * important the configuration provider believes a {@link BeanDefinition} is to the end user. It is expected
 * that tools will <strong>not</strong> display all {@link BeanDefinition BeanDefinitions} for a given
 * {@code ComponentDefinition} choosing instead to filter based on the role. Tools may choose to make
 * this filtering user configurable. Particular notice should be given to the
 * {@link BeanDefinition#ROLE_INFRASTRUCTURE INFRASTRUCTURE role identifier}. {@link BeanDefinition BeanDefinitions}
 * classified with this role are completely unimportant to the end user and are required only for
 * internal implementation reasons.
 *
 * 工具可以通过检查{@link BeanDefinition#getRole 角色标识符}来确定所包含的{@link BeanDefinition BeanDefinitions}的重要性。
 * 该角色本质上是向工具暗示配置提供者认为 {@link BeanDefinition} 对最终用户有多重要。预计工具将不会显示给定 {@code ComponentDefinition}
 * 的所有 {@link BeanDefinition BeanDefinitions}，而是选择根据角色进行过滤。工具可以选择使此过滤由用户配置。
 * 应特别注意 {@link BeanDefinition#ROLE_INFRASTRUCTURE INFRASTRUCTURE 角色标识符}。具有此角色的{@link BeanDefinition BeanDefinitions}对于最终用户来说完全不重要，
 * 仅出于内部实现原因才需要。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see AbstractComponentDefinition
 * @see CompositeComponentDefinition
 * @see BeanComponentDefinition
 * @see ReaderEventListener#componentRegistered(ComponentDefinition)
 */
public interface ComponentDefinition extends BeanMetadataElement {

	/**
	 * Get the user-visible name of this {@code ComponentDefinition}.
	 * <p>This should link back directly to the corresponding configuration data
	 * for this component in a given context.
	 */
	String getName();

	/**
	 * Return a friendly description of the described component.
	 * <p>Implementations are encouraged to return the same value from
	 * {@code toString()}.
	 */
	String getDescription();

	/**
	 * Return the {@link BeanDefinition BeanDefinitions} that were registered
	 * to form this {@code ComponentDefinition}.
	 * <p>It should be noted that a {@code ComponentDefinition} may well be related with
	 * other {@link BeanDefinition BeanDefinitions} via {@link BeanReference references},
	 * however these are <strong>not</strong> included as they may be not available immediately.
	 * Important {@link BeanReference BeanReferences} are available from {@link #getBeanReferences()}.
	 * @return the array of BeanDefinitions, or an empty array if none
	 */
	BeanDefinition[] getBeanDefinitions();

	/**
	 * Return the {@link BeanDefinition BeanDefinitions} that represent all relevant
	 * inner beans within this component.
	 * <p>Other inner beans may exist within the associated {@link BeanDefinition BeanDefinitions},
	 * however these are not considered to be needed for validation or for user visualization.
	 * @return the array of BeanDefinitions, or an empty array if none
	 */
	BeanDefinition[] getInnerBeanDefinitions();

	/**
	 * Return the set of {@link BeanReference BeanReferences} that are considered
	 * to be important to this {@code ComponentDefinition}.
	 * <p>Other {@link BeanReference BeanReferences} may exist within the associated
	 * {@link BeanDefinition BeanDefinitions}, however these are not considered
	 * to be needed for validation or for user visualization.
	 * @return the array of BeanReferences, or an empty array if none
	 */
	BeanReference[] getBeanReferences();

}
