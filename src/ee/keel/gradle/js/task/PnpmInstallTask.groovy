package ee.keel.gradle.js.task

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

import groovy.transform.CompileStatic

@CompileStatic
class PnpmInstallTask extends PnpmTask
{
	private final static Logger logger = Logging.getLogger(PnpmInstallTask)

	PnpmInstallTask()
	{
		super()

		configure {
			command.add("install")
			command.add("--no-strict-peer-dependencies")
			command.add("--virtual-store-dir=.pnpm")
			command.add("--config.resolution-mode=highest")
		}

		logging.captureStandardOutput(LogLevel.INFO)
		logging.captureStandardError(LogLevel.WARN)
	}

	@OutputDirectory
	File getOutputDirectory()
	{
		return new File(getWorkingDir(), "node_modules")
	}

	@InputFile
	File getPackagesJson()
	{
		return new File(getWorkingDir(), "package.json")
	}

	@Optional
	@InputFile
	File getPackageLock()
	{
		def ret = new File(getWorkingDir(), "pnpm-lock.yaml")

		if(!ret.exists())
		{
			return null
		}

		return ret
	}
}
