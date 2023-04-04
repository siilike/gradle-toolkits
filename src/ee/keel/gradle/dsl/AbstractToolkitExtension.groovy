package ee.keel.gradle.dsl

import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input

@Slf4j("logger")
abstract class AbstractToolkitExtension extends ToolkitModel
{
	@Input
	final Property<String> environment

	@Input
	final Property<EnvironmentExtension> currentEnvironment

	@Input
	final Property<String> name

	@Input
	final Property<String> version

	@Input
	final Property<PackagesConfig> packages

	@Input
	final Property<Boolean> preferParentTools

	@Input
	final DirectoryProperty toolsDirectory

	@Input
	final RegularFileProperty toolsLockfile

	AbstractToolkitExtension(Project project)
	{
		ObjectFactory of = getObjectFactory()
		ProviderFactory pf = getProviderFactory()

		name = of.property(String).convention(project.name)
		packages = of.property(PackagesConfig).convention(of.newInstance(PackagesConfig, project))
		preferParentTools = of.property(Boolean).convention(true)
		toolsDirectory = of.directoryProperty().convention(project.layout.projectDirectory.dir("tools"))
		toolsLockfile = of.fileProperty().convention(project.layout.projectDirectory.file("pnpm-lock-tools.yaml"))

		environment = of.property(String).convention(project.provider {
			def proj = project

			while(proj)
			{
				if(proj.hasProperty("dev"))
				{
					return "development"
				}

				proj = proj.parent
			}

			return "production"
		})

		version = of.property(String).convention(project.provider {
			def proj = project

			while(proj)
			{
				def versionProperty = "${project.name}Version"

				if(proj.hasProperty(versionProperty))
				{
					logger.debug("Using version form property {}", versionProperty)

					return proj.getProperties().get(versionProperty) as String
				}

				if(proj.version && proj.version != "unspecified")
				{
					logger.debug("Using project {} version", project.name)

					return proj.version as String
				}

				proj = proj.parent
			}

			return "snapshot"
		})

		currentEnvironment = of.property(EnvironmentExtension)
		currentEnvironment.set(pf.provider({ plugin.envExt.getByName(environment.get()) }))
		currentEnvironment.disallowChanges()

		project.extensions.extraProperties.set("env", new EnvironmentHelper(currentEnvironment))
	}

	def packages(Closure c)
	{
		c.delegate = packages.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}
}
