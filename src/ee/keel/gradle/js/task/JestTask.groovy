package ee.keel.gradle.js.task

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
			args jstk.toolsDirectory.get().file("node_modules/jest/bin/jest.js").asFile.absolutePath

			environment "IS_TEST", "true"

			environmentProperty "JEST_CONFIG", jestConfig
		}
	}

	@Override
	protected void exec0()
	{
		super.exec0()

		environment "WEBPACK_CONFIG", config.asFile.get().absolutePath

		args '--config', jestConfig.get().asFile.absolutePath
// 		args '--verbose'

		if(continuous.get())
		{
			args '--watchAll'
		}
	}
}
