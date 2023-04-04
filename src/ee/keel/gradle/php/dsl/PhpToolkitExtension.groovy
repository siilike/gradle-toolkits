package ee.keel.gradle.php.dsl

import ee.keel.gradle.dsl.AbstractToolkitExtension
import ee.keel.gradle.php.BasePlugin
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input

@CompileStatic
abstract class PhpToolkitExtension extends AbstractToolkitExtension
{
	private final static Logger logger = Logging.getLogger(PhpToolkitExtension)

	@Input
	final Property<PhpConfig> php

	@Input
	final Property<ComposerConfig> composer

	@Input
	final Property<PhpStanConfig> phpstan

	@Input
	final NamedDomainObjectContainer<PatchConfig> patches

	@Input
	final Property<DistributionConfig> distribution

	@Input
	final DirectoryProperty srcDirectory

	@Input
	final DirectoryProperty testsDirectory

	@Input
	final DirectoryProperty buildTestsDirectory

	@Input
	final DirectoryProperty buildSrcDirectory

	@Input
	final DirectoryProperty buildDistDirectory

	PhpToolkitExtension(Project project, BasePlugin plugin)
	{
		super(project)

		ObjectFactory of = getObjectFactory()
		ProviderFactory pf = getProviderFactory()

		php = of.property(PhpConfig).convention(of.newInstance(PhpConfig, project))
		composer = of.property(ComposerConfig).convention(of.newInstance(ComposerConfig, project))
		phpstan = of.property(PhpStanConfig).convention(of.newInstance(PhpStanConfig, project))

		distribution = of.property(DistributionConfig).convention(of.newInstance(DistributionConfig, "main", project))
		patches = of.domainObjectContainer(PatchConfig, { name -> of.newInstance(PatchConfig, name, project) })

		srcDirectory = of.directoryProperty().convention(project.layout.projectDirectory.dir("src"))
		testsDirectory = of.directoryProperty().convention(project.layout.projectDirectory.dir("tests"))
		buildTestsDirectory = of.directoryProperty().convention(project.layout.buildDirectory.dir("tests"))
		buildSrcDirectory = of.directoryProperty().convention(project.layout.buildDirectory.dir("src"))
//		buildDistDirectory = of.directoryProperty().convention(project.layout.buildDirectory.dir("dist"))
		buildDistDirectory = of.directoryProperty().convention(buildSrcDirectory)

		toolsLockfile.convention(project.layout.projectDirectory.file("composer-tools.lock"))

		applyDefaults()
	}

	@CompileDynamic
	protected void applyDefaults()
	{
		logger.debug("Applying default tool versions")

		packages {
			version "siilike/translator-weblate", "dev-master"
			version "vimeo/psalm", "*"
			version "phan/phan", "*"
			version "phpstan/phpstan", "*"
			version "phpunit/phpunit", "*"
			version "opis/json-schema", "*"
		}
	}

	def php(Closure c)
	{
		c.delegate = php.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}

	def composer(Closure c)
	{
		c.delegate = composer.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}

	def distribution(Closure c)
	{
		c.delegate = distribution.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}
}
