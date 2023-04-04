package ee.keel.gradle.php.task

import groovy.transform.CompileStatic
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

@CompileStatic
class ComposerInstallTask extends ComposerTask
{
	private final static Logger logger = Logging.getLogger(ComposerInstallTask)

	ComposerInstallTask()
	{
		super()

		configure {
			command.add("install")

			if(env.get() == "production")
			{
				command.add("--optimize-autoloader")
			}

			if(System.getenv("CI"))
			{
				command.add "--ignore-platform-reqs"
			}
		}

		logging.captureStandardOutput(LogLevel.INFO)
		logging.captureStandardError(LogLevel.WARN)
	}

	@OutputDirectory
	File getOutputDirectory()
	{
		return new File(getWorkingDir(), vendorDirectoryName.get())
	}

	@InputFile
	File getComposerJson()
	{
		return new File(getWorkingDir(), composerJsonName.get())
	}

	@Optional
	@InputFile
	File getComposerLock()
	{
		def ret = new File(getWorkingDir(), composerLockName.get())

		if(!ret.exists())
		{
			return null
		}

		return ret
	}
}
