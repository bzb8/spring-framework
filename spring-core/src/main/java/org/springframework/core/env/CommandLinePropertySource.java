/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.env;

import java.util.Collection;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for {@link PropertySource} implementations backed by command line
 * arguments. The parameterized type {@code T} represents the underlying source of command
 * line options. This may be as simple as a String array in the case of
 * {@link SimpleCommandLinePropertySource}, or specific to a particular API such as JOpt's
 * {@code OptionSet} in the case of {@link JOptCommandLinePropertySource}.
 * <p>抽象基类，为命令行参数支持的{@link PropertySource}实现提供背书。泛型类型{@code T}代表命令行选项的底层源。
 * 这可以是一个简单的{@link SimpleCommandLinePropertySource}情况下的字符串数组，也可以是特定于API的，如JOpt的{@code OptionSet}。
 *
 * <h3>Purpose and General Usage</h3>
 * <h3>目的和一般用法</h3>
 *
 * For use in standalone Spring-based applications, i.e. those that are bootstrapped via
 * a traditional {@code main} method accepting a {@code String[]} of arguments from the
 * command line. In many cases, processing command-line arguments directly within the
 * {@code main} method may be sufficient, but in other cases, it may be desirable to
 * inject arguments as values into Spring beans. It is this latter set of cases in which
 * a {@code CommandLinePropertySource} becomes useful. A {@code CommandLinePropertySource}
 * will typically be added to the {@link Environment} of the Spring
 * {@code ApplicationContext}, at which point all command line arguments become available
 * through the {@link Environment#getProperty(String)} family of methods. For example:
 * <p>用于独立的Spring基础应用程序，即通过传统的{@code main}方法启动，该方法接受命令行参数{@code String[]}。
 * 在许多情况下，在{@code main}方法内直接处理命令行参数可能就足够了，但在其他情况下，可能希望将参数作为值注入Spring bean。
 * 在这种情况下，使用{@code CommandLinePropertySource}就很有用了。{@code CommandLinePropertySource}通常会被添加到Spring
 * {@code ApplicationContext}的{@link Environment}中，在这种情况下，所有命令行参数都通过{@link Environment#getProperty(String)}
 * 系列方法可用。例如：
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     CommandLinePropertySource clps = ...;
 *     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 *     ctx.getEnvironment().getPropertySources().addFirst(clps);
 *     ctx.register(AppConfig.class);
 *     ctx.refresh();
 * }</pre>
 *
 * With the bootstrap logic above, the {@code AppConfig} class may {@code @Inject} the
 * Spring {@code Environment} and query it directly for properties:
 * <p>通过上面的引导逻辑，{@code AppConfig}类可以{@code @Inject} Spring的{@code Environment}并直接查询属性：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Inject Environment env;
 *
 *     &#064;Bean
 *     public void DataSource dataSource() {
 *         MyVendorDataSource dataSource = new MyVendorDataSource();
 *         dataSource.setHostname(env.getProperty("db.hostname", "localhost"));
 *         dataSource.setUsername(env.getRequiredProperty("db.username"));
 *         dataSource.setPassword(env.getRequiredProperty("db.password"));
 *         // ...
 *         return dataSource;
 *     }
 * }</pre>
 *
 * Because the {@code CommandLinePropertySource} was added to the {@code Environment}'s
 * set of {@link MutablePropertySources} using the {@code #addFirst} method, it has
 * the highest search precedence, meaning that while "db.hostname" and other properties
 * may exist in other property sources such as the system environment variables, it will
 * be chosen from the command line property source first. This is a reasonable approach
 * given that arguments specified on the command line are naturally more specific than
 * those specified as environment variables.
 * <p>由于使用{@code #addFirst}方法将{@code CommandLinePropertySource}添加到{@code Environment}的{@link MutablePropertySources}中，
 * 它具有最高的搜索优先级，这意味着虽然"db.hostname"和其他属性可能存在于其他属性源中，如系统环境变量，但会优先从命令行属性源中选择。
 * 这是一种合理的方法，因为命令行指定的参数自然比环境变量更具体。
 *
 * <p>As an alternative to injecting the {@code Environment}, Spring's {@code @Value}
 * annotation may be used to inject these properties, given that a {@link
 * PropertySourcesPropertyResolver} bean has been registered, either directly or through
 * using the {@code <context:property-placeholder>} element. For example:
 * <p>作为注入{@code Environment}的替代方案，可以使用Spring的{@code @Value}注解注入这些属性，条件是必须注册一个{@link
 * PropertySourcesPropertyResolver} bean，或者通过使用{@code <context:property-placeholder>}元素。</p>
 *
 * <pre class="code">
 * &#064;Component
 * public class MyComponent {
 *
 *     &#064;Value("my.property:defaultVal")
 *     private String myProperty;
 *
 *     public void getMyProperty() {
 *         return this.myProperty;
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>Working with option arguments</h3>
 * <h3>处理选项参数</h3>
 *
 * <p>Individual command line arguments are represented as properties through the usual
 * {@link PropertySource#getProperty(String)} and
 * {@link PropertySource#containsProperty(String)} methods. For example, given the
 * following command line:
 * <p>各个命令行参数通过通常的{@link PropertySource#getProperty(String)}和
 * {@link PropertySource#containsProperty(String)}方法表示为属性。例如，给定以下命令行：</p>
 *
 * <pre class="code">--o1=v1 --o2</pre>
 *
 * 'o1' and 'o2' are treated as "option arguments", and the following assertions would
 * evaluate true:
 * <p>'o1'和'o2'被视为"选项参数"，以下断言将为真：
 *
 * <pre class="code">
 * CommandLinePropertySource&lt;?&gt; ps = ...
 * assert ps.containsProperty("o1") == true;
 * assert ps.containsProperty("o2") == true;
 * assert ps.containsProperty("o3") == false;
 * assert ps.getProperty("o1").equals("v1");
 * assert ps.getProperty("o2").equals("");
 * assert ps.getProperty("o3") == null;
 * </pre>
 *
 * Note that the 'o2' option has no argument, but {@code getProperty("o2")} resolves to
 * empty string ({@code ""}) as opposed to {@code null}, while {@code getProperty("o3")}
 * resolves to {@code null} because it was not specified. This behavior is consistent with
 * the general contract to be followed by all {@code PropertySource} implementations.
 * <p>请注意，“o2”选项没有参数，但 {@code getProperty（“o2”）} 解析为空字符串 （{@code “”}） 而不是 {@code null}，
 * 而 {@code getProperty（“o3”）} 解析为 {@code null}，因为它未指定。此行为与所有 {@code PropertySource} 实现要遵循的一般协定一致。
 *
 * <p>Note also that while "--" was used in the examples above to denote an option
 * argument, this syntax may vary across individual command line argument libraries. For
 * example, a JOpt- or Commons CLI-based implementation may allow for single dash ("-")
 * "short" option arguments, etc.
 * <p>注意，尽管在例子中使用"--"来表示选项参数，但具体实现可能允许使用单个"-"作为选项前缀（如JOpt或Commons CLI）。
 *
 * <h3>Working with non-option arguments</h3>
 * <h3>处理非选项参数</h3>
 *
 * <p>Non-option arguments are also supported through this abstraction. Any arguments
 * supplied without an option-style prefix such as "-" or "--" are considered "non-option
 * arguments" and available through the special {@linkplain
 * #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME "nonOptionArgs"} property.  If multiple
 * non-option arguments are specified, the value of this property will be a
 * comma-delimited string containing all the arguments. This approach ensures a simple
 * and consistent return type (String) for all properties from a {@code
 * CommandLinePropertySource} and at the same time lends itself to conversion when used
 * in conjunction with the Spring {@link Environment} and its built-in {@code
 * ConversionService}. Consider the following example:
 * <p>支持非选项参数。任何未指定选项前缀（如"-"或"--"）的参数都视为"非选项参数"，并通过特殊属性{@linkplain
 *  #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME "nonOptionArgs"}提供。如果指定了多个非选项参数，则该属性的值是这些参数的逗号分隔字符串。
 * 这种方法确保了从{@code CommandLinePropertySource}返回的所有属性的简单和一致的返回类型（String），同时又不妨碍与Spring
 * {@code Environment}及其内置{@code ConversionService}的使用。考虑以下示例：</p>
 *
 * <pre class="code">--o1=v1 --o2=v2 /path/to/file1 /path/to/file2</pre>
 *
 * In this example, "o1" and "o2" would be considered "option arguments", while the two
 * filesystem paths qualify as "non-option arguments".  As such, the following assertions
 * will evaluate true:
 * <p>在这个例子中，"o1"和"o2"被认为是"选项参数"，而两个文件路径被视为"非选项参数"。因此，以下断言将为真：
 *
 * <pre class="code">
 * CommandLinePropertySource&lt;?&gt; ps = ...
 * assert ps.containsProperty("o1") == true;
 * assert ps.containsProperty("o2") == true;
 * assert ps.containsProperty("nonOptionArgs") == true;
 * assert ps.getProperty("o1").equals("v1");
 * assert ps.getProperty("o2").equals("v2");
 * assert ps.getProperty("nonOptionArgs").equals("/path/to/file1,/path/to/file2");
 * </pre>
 *
 * <p>As mentioned above, when used in conjunction with the Spring {@code Environment}
 * abstraction, this comma-delimited string may easily be converted to a String array or
 * list:
 * <p>如上例所示，通过与Spring {@code Environment}的配合使用，可以轻松地将这个逗号分隔的字符串转换为字符串数组或列表：</p>
 *
 * <pre class="code">
 * Environment env = applicationContext.getEnvironment();
 * String[] nonOptionArgs = env.getProperty("nonOptionArgs", String[].class);
 * assert nonOptionArgs[0].equals("/path/to/file1");
 * assert nonOptionArgs[1].equals("/path/to/file2");
 * </pre>
 *
 * <p>The name of the special "non-option arguments" property may be customized through
 * the {@link #setNonOptionArgsPropertyName(String)} method. Doing so is recommended as
 * it gives proper semantic value to non-option arguments. For example, if filesystem
 * paths are being specified as non-option arguments, it is likely preferable to refer to
 * these as something like "file.locations" than the default of "nonOptionArgs":
 * <p><p>可以通过 {@link #setNonOptionArgsPropertyName(String)} 方法自定义特殊“non-option arguments”属性的名称。
 * 建议这样做，因为它为非选项参数提供了适当的语义值。
 * 例如，如果文件系统路径被指定为非选项参数，则最好将这些路径称为“file.locations”之类的内容，而不是默认的“nonOptionArgs”：
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     CommandLinePropertySource clps = ...;
 *     clps.setNonOptionArgsPropertyName("file.locations");
 *
 *     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 *     ctx.getEnvironment().getPropertySources().addFirst(clps);
 *     ctx.register(AppConfig.class);
 *     ctx.refresh();
 * }</pre>
 *
 * <h3>Limitations</h3>
 *
 * This abstraction is not intended to expose the full power of underlying command line
 * parsing APIs such as JOpt or Commons CLI. Its intent is rather just the opposite: to
 * provide the simplest possible abstraction for accessing command line arguments
 * <em>after</em> they have been parsed. So the typical case will involve fully configuring
 * the underlying command line parsing API, parsing the {@code String[]} of arguments
 * coming into the main method, and then simply providing the parsing results to an
 * implementation of {@code CommandLinePropertySource}. At that point, all arguments can
 * be considered either 'option' or 'non-option' arguments and as described above can be
 * accessed through the normal {@code PropertySource} and {@code Environment} APIs.
 * <h3>限制</h3>
 *
 * <p>此抽象旨在不暴露底层命令行解析API（如JOpt或Commons CLI）的全部功能。
 * 其目的是相反的：提供访问命令行参数的最简单可能抽象，<em>在</em>它们被解析之后。
 * 因此，典型用例将包括完全配置底层命令行解析API，解析进入{@code main}方法的{@code String[]}参数，
 * 然后简单地将解析结果提供给{@code CommandLinePropertySource}的实现。</p>
 *
 * @author Chris Beams
 * @since 3.1
 * @param <T> the source type
 * @see PropertySource
 * @see SimpleCommandLinePropertySource
 * @see JOptCommandLinePropertySource
 */
public abstract class CommandLinePropertySource<T> extends EnumerablePropertySource<T> {

	/**
	 * The default name given to {@link CommandLinePropertySource} instances: {@value}.
	 * {@link CommandLinePropertySource}实例的默认名称：{@value}。
	 */
	public static final String COMMAND_LINE_PROPERTY_SOURCE_NAME = "commandLineArgs";

	/**
	 * The default name of the property representing non-option arguments: {@value}.
	 * 默认的非选项参数属性名称：{@value}。
	 */
	public static final String DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME = "nonOptionArgs";


	private String nonOptionArgsPropertyName = DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME;


	/**
	 * Create a new {@code CommandLinePropertySource} having the default name
	 * {@value #COMMAND_LINE_PROPERTY_SOURCE_NAME} and backed by the given source object.
	 * 使用默认名称{@value #COMMAND_LINE_PROPERTY_SOURCE_NAME}和给定源对象创建一个新的{@code CommandLinePropertySource}实例。
	 */
	public CommandLinePropertySource(T source) {
		super(COMMAND_LINE_PROPERTY_SOURCE_NAME, source);
	}

	/**
	 * Create a new {@link CommandLinePropertySource} having the given name
	 * and backed by the given source object.
	 * 使用给定名称和源对象创建一个新的{@link CommandLinePropertySource}实例。
	 */
	public CommandLinePropertySource(String name, T source) {
		super(name, source);
	}


	/**
	 * Specify the name of the special "non-option arguments" property.
	 * The default is {@value #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME}.
	 * 指定特殊"非选项参数"属性的名称。
	 * 默认为{@value #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME}。
	 */
	public void setNonOptionArgsPropertyName(String nonOptionArgsPropertyName) {
		this.nonOptionArgsPropertyName = nonOptionArgsPropertyName;
	}

	/**
	 * This implementation first checks to see if the name specified is the special
	 * {@linkplain #setNonOptionArgsPropertyName(String) "non-option arguments" property},
	 * and if so delegates to the abstract {@link #getNonOptionArgs()} method
	 * checking to see whether it returns an empty collection. Otherwise, delegates to and
	 * returns the value of the abstract {@link #containsOption(String)} method.
	 * 此实现首先检查指定的名称是否为特殊"非选项参数"属性，
	 * 如果是，则委托给抽象方法{@link #getNonOptionArgs()}检查是否返回一个空集合。
	 * 否则，将委托并返回抽象方法{@link #containsOption(String)}的值。
	 */
	@Override
	public final boolean containsProperty(String name) {
		// 如果指定的名称是特殊"非选项参数"属性，则委托给抽象方法getNonOptionArgs()检查是否返回一个空集合。
		if (this.nonOptionArgsPropertyName.equals(name)) {
			return !getNonOptionArgs().isEmpty();
		}
		return this.containsOption(name);
	}

	/**
	 * This implementation first checks to see if the name specified is the special
	 * {@linkplain #setNonOptionArgsPropertyName(String) "non-option arguments" property},
	 * and if so delegates to the abstract {@link #getNonOptionArgs()} method. If so
	 * and the collection of non-option arguments is empty, this method returns {@code
	 * null}. If not empty, it returns a comma-separated String of all non-option
	 * arguments. Otherwise, delegates to and returns the result of the abstract {@link
	 * #getOptionValues(String)} method.
	 * 此实现首先检查指定的名称是否为特殊"非选项参数"属性，
	 * 如果是，则委托给抽象方法{@link #getNonOptionArgs()}。如果非选项参数集合为空，此方法返回{@code null}。
	 * 如果非空，则返回一个逗号分隔的字符串，包含所有非选项参数。
	 * 否则，将委托并返回抽象方法{@link #getOptionValues(String)}的结果。
	 */
	@Override
	@Nullable
	public final String getProperty(String name) {
		if (this.nonOptionArgsPropertyName.equals(name)) {
			Collection<String> nonOptionArguments = getNonOptionArgs();
			if (nonOptionArguments.isEmpty()) {
				return null;
			}
			else {
				return StringUtils.collectionToCommaDelimitedString(nonOptionArguments);
			}
		}
		Collection<String> optionValues = getOptionValues(name);
		if (optionValues == null) {
			return null;
		}
		else {
			return StringUtils.collectionToCommaDelimitedString(optionValues);
		}
	}


	/**
	 * Return whether the set of option arguments parsed from the command line contains
	 * an option with the given name.
	 * 返回从命令行解析的选项参数集中是否包含具有给定名称的选项。
	 */
	protected abstract boolean containsOption(String name);

	/**
	 * Return the collection of values associated with the command line option having the
	 * given name.
	 * <ul>
	 * <li>if the option is present and has no argument (e.g.: "--foo"), return an empty
	 * collection ({@code []})</li>
	 * <li>if the option is present and has a single value (e.g. "--foo=bar"), return a
	 * collection having one element ({@code ["bar"]})</li>
	 * <li>if the option is present and the underlying command line parsing library
	 * supports multiple arguments (e.g. "--foo=bar --foo=baz"), return a collection
	 * having elements for each value ({@code ["bar", "baz"]})</li>
	 * <li>if the option is not present, return {@code null}</li>
	 * </ul>
	 * 返回命令行中具有给定名称的选项的值集合。
	 * <ul>
	 * <li>如果选项存在且无参数（例如"--foo"），则返回空集合（{@code []}）</li>
	 * <li>如果选项存在且有一个值（例如"--foo=bar"），则返回具有一个元素的集合（{@code ["bar"]}）</li>
	 * <li>如果选项存在且底层命令行解析库支持多个参数（例如"--foo=bar --foo=baz"），
	 * 则返回一个集合，其中包含每个值的元素（{@code ["bar", "baz"]}）</li>
	 * <li>如果选项不存在，则返回{@code null}</li>
	 * </ul>
	 */
	@Nullable
	protected abstract List<String> getOptionValues(String name);

	/**
	 * Return the collection of non-option arguments parsed from the command line.
	 * Never {@code null}.
	 * 返回从命令行解析的非选项参数集合。
	 * 永远不会为{@code null}。
	 */
	protected abstract List<String> getNonOptionArgs();

}
