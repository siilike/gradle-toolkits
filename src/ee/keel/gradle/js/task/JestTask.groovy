package ee.keel.gradle.js.task

import ee.keel.gradle.Utils
import groovy.transform.CompileStatic
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile

@CompileStatic
class JestTask extends AbstractWebpackTask
{
	@InputFile
	final RegularFileProperty jestConfig = project.objects.fileProperty()

	JestTask()
	{
		super()

		configure {
//			args '--experimental-vm-modules'

			args jstk.toolsDirectory.get().file("node_modules/jest/bin/jest.js").asFile.absolutePath

			environmentProperty "JEST_CONFIG", jestConfig

			environmentFile.fileProvider(jestConfig.map { new File(project.buildDir, it.asFile.name+".env") })
		}
	}

	@Override
	protected void exec0()
	{
		super.exec0()

		environment "IS_TEST", "true"
		environment "WEBPACK_CONFIG", config.asFile.get().absolutePath

		args '--config', jestConfig.get().asFile.absolutePath
// 		args '--verbose'

		if(project.hasProperty('jestInBand'))
		{
			args '--runInBand'
		}

		if(project.hasProperty('jestNoCache'))
		{
			args '--no-cache'
		}

		if(continuous.get())
		{
			args '--watchAll'
		}
	}
}
