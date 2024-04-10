/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.context.request;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Abstract support class for RequestAttributes implementations,
 * offering a request completion mechanism for request-specific destruction
 * callbacks and for updating accessed session attributes.
 * <p>为RequestAttributes实现提供抽象支持类，提供一种请求完成机制，用于请求特定的销毁回调和更新访问过的会话属性。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #requestCompleted()
 */
public abstract class AbstractRequestAttributes implements RequestAttributes {

	/** Map from attribute name String to destruction callback Runnable. */
	/** 从属性名称字符串到销毁回调Runnable的映射。 */
	protected final Map<String, Runnable> requestDestructionCallbacks = new LinkedHashMap<>(8);

	private volatile boolean requestActive = true;


	/**
	 * Signal that the request has been completed.
	 * <p>Executes all request destruction callbacks and updates the
	 * session attributes that have been accessed during request processing.
	 * 信号表明请求已完成。
	 * <p>执行所有请求销毁回调，并更新在请求处理期间访问过的会话属性。
	 */
	public void requestCompleted() {
		executeRequestDestructionCallbacks();
		updateAccessedSessionAttributes();
		this.requestActive = false;
	}

	/**
	 * Determine whether the original request is still active.
	 * 确定原始请求是否仍处于活动状态
	 * @see #requestCompleted()
	 */
	protected final boolean isRequestActive() {
		return this.requestActive;
	}

	/**
	 * Register the given callback as to be executed after request completion.
	 * @param name the name of the attribute to register the callback for
	 * @param callback the callback to be executed for destruction
	 */
	protected final void registerRequestDestructionCallback(String name, Runnable callback) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(callback, "Callback must not be null");
		synchronized (this.requestDestructionCallbacks) {
			this.requestDestructionCallbacks.put(name, callback);
		}
	}

	/**
	 * Remove the request destruction callback for the specified attribute, if any.
	 * @param name the name of the attribute to remove the callback for
	 */
	protected final void removeRequestDestructionCallback(String name) {
		Assert.notNull(name, "Name must not be null");
		synchronized (this.requestDestructionCallbacks) {
			this.requestDestructionCallbacks.remove(name);
		}
	}

	/**
	 * Execute all callbacks that have been registered for execution
	 * after request completion.
	 * 执行所有在请求完成后注册的回调函数。
	 * 该方法会同步执行所有注册的回调，并在执行完毕后清空回调列表。
	 */
	private void executeRequestDestructionCallbacks() {
		// 同步锁定请求销毁回调列表，确保线程安全
		synchronized (this.requestDestructionCallbacks) {
			// 遍历并执行所有注册的回调函数
			for (Runnable runnable : this.requestDestructionCallbacks.values()) {
				runnable.run();
			}
			// 执行完毕后清空回调列表，准备下一次使用
			this.requestDestructionCallbacks.clear();
		}
	}

	/**
	 * Update all session attributes that have been accessed during request processing,
	 * to expose their potentially updated state to the underlying session manager.
	 * 更新在请求处理过程中访问过的所有会话属性，以向底层会话管理器暴露它们可能更新的状态。
	 */
	protected abstract void updateAccessedSessionAttributes();

}
