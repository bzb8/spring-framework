/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.util;

/**
 * A strategy for handling errors. This is especially useful for handling
 * errors that occur during asynchronous execution of tasks that have been
 * submitted to a TaskScheduler. In such cases, it may not be possible to
 * throw the error to the original caller.
 * --
 * 处理错误的策略。这对于处理在异步执行已提交到 TaskScheduler 的任务期间发生的错误特别有用。在这种情况下，可能无法将错误抛出给原始调用方。
 *
 * @author Mark Fisher
 * @since 3.0
 */
@FunctionalInterface
public interface ErrorHandler {

	/**
	 * Handle the given error, possibly rethrowing it as a fatal exception.
	 */
	void handleError(Throwable t);

}
