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

package org.springframework.web.servlet.mvc.support;

import java.util.Collection;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.web.servlet.FlashMap;

/**
 * A specialization of the {@link Model} interface that controllers can use to
 * select attributes for a redirect scenario. Since the intent of adding
 * redirect attributes is very explicit --  i.e. to be used for a redirect URL,
 * attribute values may be formatted as Strings and stored that way to make
 * them eligible to be appended to the query string or expanded as URI
 * variables in {@code org.springframework.web.servlet.view.RedirectView}.
 * Model 接口的一个特殊化版本，控制器可以使用它来为重定向场景选择属性。由于添加重定向属性的意图非常明确——即用于重定向 URL，
 * 因此属性值可以被格式化为字符串并以这种方式存储，以便使其有资格被追加到查询字符串或在 org.springframework.web.servlet.view.RedirectView
 * 中扩展为 URI 变量。
 *
 * <p>This interface also provides a way to add flash attributes. For a
 * general overview of flash attributes see {@link FlashMap}. You can use
 * {@link RedirectAttributes} to store flash attributes and they will be
 * automatically propagated to the "output" FlashMap of the current request.
 * <p>此接口还提供了一种添加闪现（flash）属性的方法。有关闪现属性的一般概述，请参见 {@link FlashMap}。您可以使用 {@link RedirectAttributes}
 * 存储闪现属性，它们将自动传播到当前请求的“输出”闪现映射（FlashMap）中。
 *
 * <p>Example usage in an {@code @Controller}:
 * <p>在 {@code @Controller} 中的示例用法：
 * <pre class="code">
 * &#064;RequestMapping(value = "/accounts", method = RequestMethod.POST)
 * public String handle(Account account, BindingResult result, RedirectAttributes redirectAttrs) {
 *   if (result.hasErrors()) {
 *     return "accounts/new";
 *   }
 *   // Save account ...
 *   redirectAttrs.addAttribute("id", account.getId()).addFlashAttribute("message", "Account created!");
 *   return "redirect:/accounts/{id}";
 * }
 * </pre>
 *
 * <p>A RedirectAttributes model is empty when the method is called and is never
 * used unless the method returns a redirect view name or a RedirectView.
 * <p>当调用方法时，`RedirectAttributes` 模型为空，并且除非方法返回重定向视图名称或 `RedirectView`，否则该模型永远不会被使用。
 *
 * <p>After the redirect, flash attributes are automatically added to the model
 * of the controller that serves the target URL.
 * <p>重定向后，闪现属性会自动添加到服务于目标 URL 的控制器的模型中
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public interface RedirectAttributes extends Model {

	@Override
	RedirectAttributes addAttribute(String attributeName, @Nullable Object attributeValue);

	@Override
	RedirectAttributes addAttribute(Object attributeValue);

	@Override
	RedirectAttributes addAllAttributes(Collection<?> attributeValues);

	@Override
	RedirectAttributes mergeAttributes(Map<String, ?> attributes);

	/**
	 * Add the given flash attribute.
	 * @param attributeName the attribute name; never {@code null}
	 * @param attributeValue the attribute value; may be {@code null}
	 */
	RedirectAttributes addFlashAttribute(String attributeName, @Nullable Object attributeValue);

	/**
	 * Add the given flash storage using a
	 * {@link org.springframework.core.Conventions#getVariableName generated name}.
	 * @param attributeValue the flash attribute value; never {@code null}
	 */
	RedirectAttributes addFlashAttribute(Object attributeValue);

	/**
	 * Return the attributes candidate for flash storage or an empty Map.
	 */
	Map<String, ?> getFlashAttributes();
}
