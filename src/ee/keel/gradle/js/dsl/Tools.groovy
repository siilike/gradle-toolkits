package ee.keel.gradle.js.dsl

import ee.keel.gradle.dsl.ToolConfig
import ee.keel.gradle.dsl.ToolkitModel
import ee.keel.gradle.js.Utils
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles

import javax.inject.Inject

@CompileStatic
abstract class WebpackConfig extends ToolkitModel
{
	@Input
	final SetProperty<String> inputFields = objectFactory.setProperty(String)

	@Input
	final SetProperty<String> outputFields = objectFactory.setProperty(String)

	@InputFiles
	final ConfigurableFileCollection inputs = objectFactory.fileCollection()

	@Input
	final DirectoryProperty directory = objectFactory.directoryProperty()

	@Input
	final Property<Boolean> alwaysTranspile = project.objects.property(Boolean).convention(false)

	@Input
	final Property<Boolean> useSWC = project.objects.property(Boolean).convention(false)

	@Inject
	WebpackConfig(Project project)
	{
		super(project)

		inputs.from(
			"${project.buildDir}/node_modules/",
			"${project.projectDir}/libs/",
			"${project.projectDir}/underlay/",
		)

		directory.convention(project.layout.projectDirectory.dir("webpack"))
	}

	void inputField(String k)
	{
		inputFields.add(k)
	}

	void outputField(String k)
	{
		outputFields.add(k)
	}
}

@CompileStatic
abstract class BabelConfig extends ToolkitModel
{
	@InputFile
	final RegularFileProperty config = objectFactory.fileProperty()

	@Input
	final MapProperty<String, String> presets = objectFactory.mapProperty(String, String)

	@Input
	final Property<String> reactPragma = objectFactory.property(String)

	@Inject
	BabelConfig(Project project)
	{
		super(project)

		presets.convention(
		[
			"production": "> 0.5%, last 3 versions, Firefox ESR, not dead, not IE 11, Chrome >= 66",
		])
	}

	void preset(String name, String value)
	{
		presets.put(name, value)
	}
}

@CompileStatic
abstract class NodeConfig extends ToolConfig
{
	@Inject
	NodeConfig(Project project)
	{
		super(project, "node")

		path.convention(project.provider {
			if(version.get() == "local")
			{
				if(!Utils.resolvePath("node"))
				{
					throw new IllegalStateException("Could not find node in \$PATH")
				}

				return "node"
			}

			return Utils.getExt(project).toolsDirectory.file("node/bin/node").get().asFile.absolutePath
		})
	}
}

@CompileStatic
abstract class PnpmConfig extends ToolConfig
{
	@Inject
	PnpmConfig(Project project)
	{
		super(project, "pnpm")

		path.convention(project.provider {
			if(version.get() == "local")
			{
				if(!Utils.resolvePath("pnpm"))
				{
					throw new IllegalStateException("Could not find pnpm in \$PATH")
				}

				return "pnpm"
			}

			return Utils.getExt(project).toolsDirectory.file("pnpm/bin/pnpm.js").get().asFile.absolutePath
		})
	}
}
