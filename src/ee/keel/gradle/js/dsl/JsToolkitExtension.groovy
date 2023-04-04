package ee.keel.gradle.js.dsl

import ee.keel.gradle.dsl.AbstractToolkitExtension
import ee.keel.gradle.js.BasePlugin
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input

@CompileStatic
abstract class JsToolkitExtension extends AbstractToolkitExtension
{
	private final static Logger logger = Logging.getLogger(JsToolkitExtension)

	@Input
	final Property<String> librariesVersion

	@Input
	final Property<NodeConfig> node

	@Input
	final Property<PnpmConfig> pnpm

	@Input
	final Property<WebpackConfig> webpack

	@Input
	final Property<BabelConfig> babel

	@Input
	final Property<SentryConfig> sentry

	@Input
	final NamedDomainObjectContainer<OutputConfig> outputs

	@Input
	final NamedDomainObjectContainer<LibraryConfig> libraries

	@Input
	final NamedDomainObjectContainer<ModuleConfig> modules

	@Input
	final NamedDomainObjectContainer<DistributionConfig> distributions

	@Input
	final NamedDomainObjectContainer<RuntimeConfig> runtimes

	JsToolkitExtension(Project project, BasePlugin plugin)
	{
		super(project)

		ObjectFactory of = getObjectFactory()
		ProviderFactory pf = getProviderFactory()

		librariesVersion = of.property(String)

		node = of.property(NodeConfig).convention(of.newInstance(NodeConfig, project))
		pnpm = of.property(PnpmConfig).convention(of.newInstance(PnpmConfig, project))
		webpack = of.property(WebpackConfig).convention(of.newInstance(WebpackConfig, project))
		babel = of.property(BabelConfig).convention(of.newInstance(BabelConfig, project))
		sentry = of.property(SentryConfig).convention(of.newInstance(SentryConfig, project))

		outputs = objectFactory.domainObjectContainer(OutputConfig, { name -> of.newInstance(OutputConfig, name, project) })
		libraries = of.domainObjectContainer(LibraryConfig, { name -> of.newInstance(LibraryConfig, name, project) })
		modules = of.domainObjectContainer(ModuleConfig, { name -> of.newInstance(ModuleConfig, name, project) })
//		patches = of.domainObjectContainer(PatchConfig, { name -> of.newInstance(PatchConfig, name, project) })
		runtimes = of.domainObjectContainer(RuntimeConfig, { name -> of.newInstance(RuntimeConfig, name, project) })
		distributions = of.domainObjectContainer(DistributionConfig, { name -> of.newInstance(DistributionConfig, name, project) })

		toolsLockfile.convention(project.layout.projectDirectory.file("pnpm-lock-tools.yaml"))

		if(project.hasProperty("libsVersion"))
		{
			librariesVersion.convention(project.properties.libsVersion as String)
		}
		else
		{
			librariesVersion.convention(version)
		}

		applyDefaults()
	}

	@CompileDynamic
	protected void applyDefaults()
	{
		logger.debug("Applying default tool versions")

		packages {
			version "@babel/core", "latest"
			version "@babel/plugin-proposal-class-properties", "latest"
			version "@babel/plugin-proposal-decorators", "latest"
			version "@babel/plugin-proposal-object-rest-spread", "latest"
			version "@babel/plugin-transform-runtime", "latest"
			version "@babel/plugin-syntax-dynamic-import", "latest"
			version "@babel/plugin-transform-regenerator", "latest"
			version "@babel/preset-env", "latest"
			version "@babel/preset-react", "latest"
			version "babel-loader", "latest"
			version "core-js", "latest"
			version "browserslist-useragent-regexp", "latest"
//			version "@babel/runtime-corejs3", "latest"
			version "@babel/runtime", "latest"
			version "@loadable/babel-plugin", "latest"

			version "@swc/core", "latest"
			version "swc-loader", "latest"
			version "@swc/helpers", "latest"
			version "@swc/plugin-loadable-components", "latest"
			version "style-loader", "latest"
			version "css-loader", "latest"

			version "postcss", "latest"
			version "postcss-cli", "siilike/postcss-cli"
			version "postcss-advanced-variables", "latest"
			version "postcss-css-variables", "siilike/postcss-css-variables"
			version "postcss-atroot", "latest"
			version "postcss-extend-rule", "latest"
			version "postcss-import", "latest"
			version "postcss-nested", "5.0.1"
			version "postcss-nested-props", "latest"
			version "postcss-preset-env", "latest"
			version "postcss-property-lookup", "latest"
			version "postcss-scss", "latest"
			version "postcss-use", "latest"
			version "postcss-url", "latest"
			version "postcss-push", "latest"
			version "modify-selectors", "latest"
			version "autoprefixer", "latest"
			version "cssnano", "latest"
			version "cssnano-preset-advanced", "latest"

			version "webpack", "^5"
			version "webpack-cli", "latest"
			version "terser", "latest"
			version "terser-webpack-plugin", "latest"
			version "webpack-babel-env-deps", "latest"
			version "enhanced-resolve", "latest"
			version "html-webpack-plugin", "latest"

			version "webpack-visualizer-plugin", "siilike/webpack-visualizer"
			version "madge", "latest"

			version "react-refresh", "latest"
//			version "@pmmmwh/react-refresh-webpack-plugin", "0.5.0-beta.0"
			version "@pmmmwh/react-refresh-webpack-plugin", "latest"
 			version "webpack-plugin-serve", "siilike/webpack-plugin-serve"
			version "webpack-notifier", "latest"

			version "hjson", "latest"

			version "@sentry/cli", "latest"

			version "zeromq", "6.0.0-beta.6"

			version "jest", "latest"
			version "@jest/reporters", "latest"
			version "babel-jest", "latest"
			version "jest-chain", "siilike/jest-chain"
		}
	}

	def node(Closure c)
	{
		c.delegate = node.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}

	def pnpm(Closure c)
	{
		c.delegate = pnpm.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}

	def webpack(Closure c)
	{
		c.delegate = webpack.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}

	def babel(Closure c)
	{
		c.delegate = babel.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}

	def sentry(Closure c)
	{
		c.delegate = sentry.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}
}
