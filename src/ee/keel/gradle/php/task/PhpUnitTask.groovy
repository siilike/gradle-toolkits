package ee.keel.gradle.php.task

import ee.keel.gradle.dsl.WithEnvironmentProperties
import ee.keel.gradle.php.Utils
import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class PhpUnitTask extends PhpTask implements WithEnvironmentProperties
{
	private final static Logger logger = Logging.getLogger(PhpUnitTask)

	PhpUnitTask()
	{
		super()

		configure {
			enabled = !project.gradle.startParameter.isOffline()

			def ext = Utils.getExt(getProject())

			args(ext.toolsDirectory.file("bin/phpunit").get().asFile.absolutePath)
		}
	}
}
