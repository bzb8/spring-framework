/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * Common marker interface for before advice, such as {@link MethodBeforeAdvice}.
 *
 * <p>Spring supports only method before advice. Although this is unlikely to change,
 * this API is designed to allow field advice in future if desired.
 * --
 * 用于 before 建议的常用标记接口，例如 MethodBeforeAdvice.
 * Spring 只支持建议之前的方法。尽管这不太可能改变，但此 API 旨在允许将来根据需要提供字段建议。
 *
 * @author Rod Johnson
 * @see AfterAdvice
 */
public interface BeforeAdvice extends Advice {

}
