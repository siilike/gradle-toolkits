package ee.keel.gradle.php.dsl

import ee.keel.gradle.dsl.NamedToolkitModel
import ee.keel.gradle.dsl.ToolConfig
import ee.keel.gradle.dsl.ToolkitModel
import ee.keel.gradle.php.Utils
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

import javax.inject.Inject

@CompileStatic
abstract class PhpConfig extends ToolConfig
{
	@Inject
	PhpConfig(Project project)
	{
		super(project, "php")

		path.convention(project.provider {
			if(version.get() == "local")
			{
				def path = Utils.resolvePath("php")

				if(!path)
				{
					throw new IllegalStateException("Could not find php in \$PATH")
				}

				return path
			}

			return Utils.getExt(project).toolsDirectory.file("php/php").get().asFile.absolutePath
		})
	}
}

@CompileStatic
abstract class ComposerConfig extends ToolConfig
{
	@Inject
	ComposerConfig(Project project)
	{
		super(project, "composer")

		path.convention(project.provider {
			if(version.get() == "local")
			{
				def path = Utils.resolvePath("composer")

				if(!path)
				{
					throw new IllegalStateException("Could not find composer in \$PATH")
				}

				return path
			}

			return Utils.getExt(project).toolsDirectory.file("composer/composer").get().asFile.absolutePath
		})
	}
}

@CompileStatic
abstract class PhpStanConfig extends ToolkitModel
{
	@Input
	final Property<Boolean> enabled = objectFactory.property(Boolean).convention(true)

	@Input
	final Property<Integer> level = objectFactory.property(Integer).convention(5)

	@Inject
	PhpStanConfig(Project project)
	{
		super(project)
	}
}

@CompileStatic
abstract class PatchConfig extends NamedToolkitModel
{
	@Input
	final Property<Boolean> enabled = objectFactory.property(Boolean).convention(true)

	@Input
	final Property<String> path = objectFactory.property(String).convention(Utils.getExt(project).buildSrcDirectory.map { it.asFile.absolutePath })

	@Inject
	PatchConfig(String name, Project project)
	{
		super(name, project)
	}
}
