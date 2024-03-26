/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionDefinition;

/**
 * Enumeration that represents transaction propagation behaviors for use
 * with the {@link Transactional} annotation, corresponding to the
 * {@link TransactionDefinition} interface.
 * 事务传播行为枚举，定义了在不同情况下方法执行所需的事务传播策略。
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.2
 */
public enum Propagation {

	/**
	 * Support a current transaction, create a new one if none exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * <p>This is the default setting of a transaction annotation.
	 * 支持当前事务，如果不存在则创建一个新事务。
	 * 类似于EJB的同名事务属性。
	 * <p>这是事务注解的默认设置。
	 */
	REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),

	/**
	 * Support a current transaction, execute non-transactionally if none exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * <p>Note: For transaction managers with transaction synchronization,
	 * {@code SUPPORTS} is slightly different from no transaction at all,
	 * as it defines a transaction scope that synchronization will apply for.
	 * As a consequence, the same resources (JDBC Connection, Hibernate Session, etc)
	 * will be shared for the entire specified scope. Note that this depends on
	 * the actual synchronization configuration of the transaction manager.
	 * 支持当前事务，如果不存在则非事务执行。
	 * 类似于EJB的同名事务属性。
	 * <p>注意：对于具有事务同步的事务管理器，{@code SUPPORTS}与没有事务略有不同，
	 * 因为它定义了同步将适用的事务范围。因此，相同的资源（JDBC连接、Hibernate会话等）
	 * 将适用于整个指定范围。这取决于事务管理器的实际同步配置。
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
	 *
	 */
	SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS),

	/**
	 * Support a current transaction, throw an exception if none exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * 支持当前事务，如果不存在则抛出异常。
	 * 类似于EJB的同名事务属性。
	 * 强制的
	 */
	MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY),

	/**
	 * Create a new transaction, and suspend the current transaction if one exists.
	 * Analogous to the EJB transaction attribute of the same name.
	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
	 * on all transaction managers. This in particular applies to
	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
	 * which requires the {@code javax.transaction.TransactionManager} to be
	 * made available to it (which is server-specific in standard Java EE).
	 * 创建一个新事务，并暂停当前存在的事务。
	 * 类似于EJB的同名事务属性。
	 * <p><b>注意：</b>实际的事务暂停不会自动在所有事务管理器上工作。
	 * 这特别是针对{@link org.springframework.transaction.jta.JtaTransactionManager}的情况，
	 * 它需要{@code javax.transaction.TransactionManager}（在标准Java EE中是服务器特定的）。
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),

	/**
	 * Execute non-transactionally, suspend the current transaction if one exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
	 * on all transaction managers. This in particular applies to
	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
	 * which requires the {@code javax.transaction.TransactionManager} to be
	 * made available to it (which is server-specific in standard Java EE).
	 * 非事务执行，如果存在当前事务则暂停它。
	 *  类似于EJB的同名事务属性。
	 *  <p><b>注意：</b>实际的事务暂停不会自动在所有事务管理器上工作。
	 *  这特别是针对{@link org.springframework.transaction.jta.JtaTransactionManager}的情况，
	 *  它需要{@code javax.transaction.TransactionManager}（在标准Java EE中是服务器特定的）。
	 *
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	NOT_SUPPORTED(TransactionDefinition.PROPAGATION_NOT_SUPPORTED),

	/**
	 * Execute non-transactionally, throw an exception if a transaction exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * 非事务执行，如果存在事务则抛出异常。
	 * 类似于EJB的同名事务属性。
	 */
	NEVER(TransactionDefinition.PROPAGATION_NEVER),

	/**
	 * Execute within a nested transaction if a current transaction exists,
	 * behave like {@code REQUIRED} otherwise. There is no analogous feature in EJB.
	 * <p>Note: Actual creation of a nested transaction will only work on specific
	 * transaction managers. Out of the box, this only applies to the JDBC
	 * DataSourceTransactionManager. Some JTA providers might support nested
	 * transactions as well.
	 * 如果存在当前事务，则在其中执行嵌套事务，否则行为类似于{@code REQUIRED}。
	 * 在EJB中没有相应的特性。
	 * <p>注意：实际创建嵌套事务仅在特定的事务管理器中工作。开箱即用，这仅适用于
	 * JDBC {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}。
	 * 一些JTA提供者也可能支持嵌套事务。
	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
	 */
	NESTED(TransactionDefinition.PROPAGATION_NESTED);


	private final int value;


	Propagation(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}

}
