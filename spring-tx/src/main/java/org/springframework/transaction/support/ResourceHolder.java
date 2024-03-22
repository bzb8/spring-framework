/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.transaction.support;

/**
 * Generic interface to be implemented by resource holders.
 * Allows Spring's transaction infrastructure to introspect
 * and reset the holder when necessary.
 * 资源持有者接口，需由资源持有者实现。
 * 允许Spring的事务基础设施进行反射，并在必要时重置持有者。
 * 这个接口主要用于管理资源的持有和释放，特别是在事务环境中。
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see ResourceHolderSupport
 * @see ResourceHolderSynchronization
 */
public interface ResourceHolder {

	/**
	 * Reset the transactional state of this holder.
	 */
	void reset();

	/**
	 * Notify this holder that it has been unbound from transaction synchronization.
	 */
	void unbound();

	/**
	 * Determine whether this holder is considered as 'void',
	 * i.e. as a leftover from a previous thread.
	 * 判断当前持有者是否被视为“空”，
	 * 即，作为之前线程的遗留物
	 */
	boolean isVoid();

}
