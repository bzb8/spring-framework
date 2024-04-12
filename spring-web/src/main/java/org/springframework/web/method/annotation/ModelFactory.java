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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Assist with initialization of the {@link Model} before controller method
 * invocation and with updates to it after the invocation.
 *
 * <p>On initialization the model is populated with attributes temporarily stored
 * in the session and through the invocation of {@code @ModelAttribute} methods.
 *
 * <p>On update model attributes are synchronized with the session and also
 * {@link BindingResult} attributes are added if missing.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ModelFactory {

	private static final Log logger = LogFactory.getLog(ModelFactory.class);

	private final List<ModelMethod> modelMethods = new ArrayList<>();

	// ServletRequestDataBinderFactory
	private final WebDataBinderFactory dataBinderFactory;

	private final SessionAttributesHandler sessionAttributesHandler;

	/**
	 * Create a new instance with the given {@code @ModelAttribute} methods.
	 * 使用给定的{@code @ModelAttribute}方法创建一个新的实例。
	 * @param handlerMethods 要调用的{@code @ModelAttribute}方法列表
	 * @param binderFactory 用于准备{@link BindingResult}属性的工厂
	 * @param attributeHandler 用于访问会话属性的处理器
	 */
	public ModelFactory(@Nullable List<InvocableHandlerMethod> handlerMethods,
						WebDataBinderFactory binderFactory, SessionAttributesHandler attributeHandler) {

		// 如果handlerMethods不为空，则遍历handlerMethods列表，并为每个方法创建一个新的ModelMethod实例，然后添加到modelMethods列表中
		if (handlerMethods != null) {
			for (InvocableHandlerMethod handlerMethod : handlerMethods) {
				this.modelMethods.add(new ModelMethod(handlerMethod));
			}
		}
		this.dataBinderFactory = binderFactory; // 设置数据绑定器工厂
		this.sessionAttributesHandler = attributeHandler; // 设置会话属性处理器
	}



	/**
	 * Populate the model in the following order:
	 * <ol>
	 * <li>Retrieve "known" session attributes listed as {@code @SessionAttributes}.
	 * <li>Invoke {@code @ModelAttribute} methods
	 * <li>Find {@code @ModelAttribute} method arguments also listed as
	 * {@code @SessionAttributes} and ensure they're present in the model raising
	 * an exception if necessary.
	 * </ol>
	 * 按以下顺序填充模型：
	 * <ol>
	 * <li>检索作为{@code @SessionAttributes}列出的“已知”会话属性。
	 * <li>调用{@code @ModelAttribute}方法。
	 * <li>查找作为{@code @SessionAttributes}列出的{@code @ModelAttribute}方法参数，并确保它们存在于模型中，如果必要，会引发异常。
	 * </ol>
	 * @param request the current request
	 * @param container a container with the model to be initialized
	 *                  用于初始化模型的容器
	 * @param handlerMethod the method for which the model is initialized
	 *                      为其中初始化模型的方法（ServletInvocableHandlerMethod）
	 * @throws Exception may arise from {@code @ModelAttribute} methods
	 */
	public void initModel(NativeWebRequest request, ModelAndViewContainer container, HandlerMethod handlerMethod)
			throws Exception {

		// 从会话中检索属性，并将其合并到模型容器中
		Map<String, ?> sessionAttributes = this.sessionAttributesHandler.retrieveAttributes(request);
		container.mergeAttributes(sessionAttributes);
		// 调用@ModelAttribute方法以进一步初始化模型
		invokeModelAttributeMethods(request, container);

		// 遍历所有作为会话属性的方法参数，确保它们已在模型中
		for (String name : findSessionAttributeArguments(handlerMethod)) {
			if (!container.containsAttribute(name)) {
				// 尝试从会话中检索属性值，如果不存在，则抛出异常
				Object value = this.sessionAttributesHandler.retrieveAttribute(request, name);
				if (value == null) {
					throw new HttpSessionRequiredException("Expected session attribute '" + name + "'", name);
				}
				container.addAttribute(name, value);
			}
		}
	}

	/**
	 * Invoke model attribute methods to populate the model.
	 * Attributes are added only if not already present in the model.
	 */
	/**
	 * 调用模型属性方法以填充模型。
	 * 只有当模型中尚未存在这些属性时，才会将它们添加到模型中。
	 *
	 * @param request 本地Web请求，用于获取请求相关信息。
	 * @param container 视图和模型容器，用于存储和管理模型数据及视图相关信息。
	 * @throws Exception 如果方法调用或数据绑定过程中发生错误，则抛出异常。
	 */
	private void invokeModelAttributeMethods(NativeWebRequest request, ModelAndViewContainer container)
			throws Exception {

		while (!this.modelMethods.isEmpty()) {
			// 获取下一个要处理的模型属性方法
			InvocableHandlerMethod modelMethod = getNextModelMethod(container).getHandlerMethod();
			ModelAttribute ann = modelMethod.getMethodAnnotation(ModelAttribute.class);
			// 确保方法上有ModelAttribute注解
			Assert.state(ann != null, "No ModelAttribute annotation");
			// 如果模型中已存在该属性，则检查是否禁止绑定
			if (container.containsAttribute(ann.name())) {
				if (!ann.binding()) {
					container.setBindingDisabled(ann.name());
				}
				continue;
			}

			// 调用模型属性方法，并处理返回值
			Object returnValue = modelMethod.invokeForRequest(request, container);
			// 如果方法返回void，且@ModelAttribute注解中指定了名称，则记录调试信息
			if (modelMethod.isVoid()) {
				if (StringUtils.hasText(ann.value())) {
					if (logger.isDebugEnabled()) {
						logger.debug("Name in @ModelAttribute is ignored because method returns void: " +
								modelMethod.getShortLogMessage());
					}
				}
				continue;
			}

			// 根据返回值或方法返回类型确定属性名称
			String returnValueName = getNameForReturnValue(returnValue, modelMethod.getReturnType());
			// 如果指定禁止绑定，则设置该属性为不可绑定
			if (!ann.binding()) {
				container.setBindingDisabled(returnValueName);
			}
			// 如果模型中不存在该属性，则将其添加到模型中
			if (!container.containsAttribute(returnValueName)) {
				container.addAttribute(returnValueName, returnValue);
			}
		}
	}

	private ModelMethod getNextModelMethod(ModelAndViewContainer container) {
		for (ModelMethod modelMethod : this.modelMethods) {
			if (modelMethod.checkDependencies(container)) {
				this.modelMethods.remove(modelMethod);
				return modelMethod;
			}
		}
		ModelMethod modelMethod = this.modelMethods.get(0);
		this.modelMethods.remove(modelMethod);
		return modelMethod;
	}

	/**
	 * Find {@code @ModelAttribute} arguments also listed as {@code @SessionAttributes}.
	 */
	private List<String> findSessionAttributeArguments(HandlerMethod handlerMethod) {
		List<String> result = new ArrayList<>();
		for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
			if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
				String name = getNameForParameter(parameter);
				Class<?> paramType = parameter.getParameterType();
				if (this.sessionAttributesHandler.isHandlerSessionAttribute(name, paramType)) {
					result.add(name);
				}
			}
		}
		return result;
	}

	/**
	 * Promote model attributes listed as {@code @SessionAttributes} to the session.
	 * Add {@link BindingResult} attributes where necessary.
	 * 将模型属性列表中标记为{@code @SessionAttributes}的属性提升至会话中。
	 * 在必要时添加{@link BindingResult}属性。
	 * @param request the current request
	 * @param container contains the model to update
	 *                  包含需要更新的模型的容器
	 * @throws Exception if creating BindingResult attributes fails
	 */
	public void updateModel(NativeWebRequest request, ModelAndViewContainer container) throws Exception {
		ModelMap defaultModel = container.getDefaultModel();
		// 如果会话状态完成，则清理会话属性
		if (container.getSessionStatus().isComplete()){
			this.sessionAttributesHandler.cleanupAttributes(request);
		}
		else {
			// 否则，将模型属性存储到会话中
			this.sessionAttributesHandler.storeAttributes(request, defaultModel);
		}
		// 如果请求未被处理且容器中的模型为默认模型，则更新BindingResult
		if (!container.isRequestHandled() && container.getModel() == defaultModel) {
			updateBindingResult(request, defaultModel);
		}
	}

	/**
	 * Add {@link BindingResult} attributes to the model for attributes that require it.
	 */
	private void updateBindingResult(NativeWebRequest request, ModelMap model) throws Exception {
		List<String> keyNames = new ArrayList<>(model.keySet());
		for (String name : keyNames) {
			Object value = model.get(name);
			if (value != null && isBindingCandidate(name, value)) {
				String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;
				if (!model.containsAttribute(bindingResultKey)) {
					WebDataBinder dataBinder = this.dataBinderFactory.createBinder(request, value, name);
					model.put(bindingResultKey, dataBinder.getBindingResult());
				}
			}
		}
	}

	/**
	 * Whether the given attribute requires a {@link BindingResult} in the model.
	 */
	private boolean isBindingCandidate(String attributeName, Object value) {
		if (attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
			return false;
		}

		if (this.sessionAttributesHandler.isHandlerSessionAttribute(attributeName, value.getClass())) {
			return true;
		}

		return (!value.getClass().isArray() && !(value instanceof Collection) &&
				!(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
	}


	/**
	 * Derive the model attribute name for the given method parameter based on
	 * a {@code @ModelAttribute} parameter annotation (if present) or falling
	 * back on parameter type based conventions.
	 * @param parameter a descriptor for the method parameter
	 * @return the derived name
	 * @see Conventions#getVariableNameForParameter(MethodParameter)
	 */
	/**
	 * 根据给定方法参数的{@code @ModelAttribute}参数注解（如果存在）或基于参数类型的传统约定，推导出模型属性名称。
	 * @param parameter 描述方法参数的对象
	 * @return 推导出的名称
	 * @see Conventions#getVariableNameForParameter(MethodParameter)
	 */
	public static String getNameForParameter(MethodParameter parameter) {
		// 尝试获取方法参数上的ModelAttribute注解，并从中获取名称
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		String name = (ann != null ? ann.value() : null);
		// 如果注解名称非空，则使用注解名称；否则，使用传统约定来获取变量名
		return (StringUtils.hasText(name) ? name : Conventions.getVariableNameForParameter(parameter));
	}


	/**
	 * Derive the model attribute name for the given return value. Results will be
	 * based on:
	 * <ol>
	 * <li>the method {@code ModelAttribute} annotation value
	 * <li>the declared return type if it is more specific than {@code Object}
	 * <li>the actual return value type
	 * </ol>
	 * @param returnValue the value returned from a method invocation
	 * @param returnType a descriptor for the return type of the method
	 * @return the derived name (never {@code null} or empty String)
	 */
	public static String getNameForReturnValue(@Nullable Object returnValue, MethodParameter returnType) {
		ModelAttribute ann = returnType.getMethodAnnotation(ModelAttribute.class);
		if (ann != null && StringUtils.hasText(ann.value())) {
			return ann.value();
		}
		else {
			Method method = returnType.getMethod();
			Assert.state(method != null, "No handler method");
			Class<?> containingClass = returnType.getContainingClass();
			Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
			return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
		}
	}


	private static class ModelMethod {

		private final InvocableHandlerMethod handlerMethod;
		// 模型方法依赖的属性名称，根据方法参数和@ModelAttribute注解获取
		private final Set<String> dependencies = new HashSet<>();

		public ModelMethod(InvocableHandlerMethod handlerMethod) {
			this.handlerMethod = handlerMethod;
			for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
				if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
					this.dependencies.add(getNameForParameter(parameter));
				}
			}
		}

		public InvocableHandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		public boolean checkDependencies(ModelAndViewContainer mavContainer) {
			for (String name : this.dependencies) {
				if (!mavContainer.containsAttribute(name)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public String toString() {
			return this.handlerMethod.getMethod().toGenericString();
		}
	}

}
