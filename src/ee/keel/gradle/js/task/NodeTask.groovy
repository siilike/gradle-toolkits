package ee.keel.gradle.js.task

import ee.keel.gradle.dsl.WithEnvironmentProperties
import ee.keel.gradle.js.Utils
import groovy.transform.CompileStatic
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

@CompileStatic
class NodeTask extends ContinuousExecTask implements WithEnvironmentProperties
{
	private final static Logger logger = Logging.getLogger(NodeTask)

	@OutputFile @Optional
	final RegularFileProperty environmentFile = project.objects.fileProperty()

	NodeTask()
	{
		super()

		configure {
			def jstk = Utils.getExt(getProject())

			def n = jstk.node.get()

			setExecutable(n.path.get())

			if(project.hasProperty("debugNode")) {
				args "--inspect"
			}
			else if(project.hasProperty("debugNodeBrk")) {
				args "--inspect-brk"
			}

			args((List<String>) n.args.get())

			environmentProperty "NODE_ENV", jstk.environment

			environment 'BUILD_DIR', project.buildDir.absolutePath
			environment 'PROJECT_DIR', project.projectDir.absolutePath

			if(project.logger.debugEnabled)
			{
				environment "JSTK_DEBUG", "true"
			}
		}
	}

	@Override
	protected void exec()
	{
		applyEnvironmentProperties()

		if(environmentFile.getOrNull() != null)
		{
			environmentFile.get().asFile.text = ee.keel.gradle.Utils.buildAndFilterEnv(getEnvironment())
		}

		super.exec()
	}
}
