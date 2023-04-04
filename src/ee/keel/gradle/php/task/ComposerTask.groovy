package ee.keel.gradle.php.task

import ee.keel.gradle.dsl.WithEnvironmentProperties
import ee.keel.gradle.php.Utils
import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

@CompileStatic
class ComposerTask extends PhpTask implements WithEnvironmentProperties
{
	private final static Logger logger = Logging.getLogger(ComposerTask)

	@Input
	final Property<String> env = project.objects.property(String).convention(Utils.getExt(project).environment)

	@Input
	final Property<String> vendorDirectoryName = project.objects.property(String).convention("vendor")

	@Input
	final Property<String> composerJsonName = project.objects.property(String).convention("composer.json")

	@Input
	final Property<String> composerLockName = project.objects.property(String).convention("composer.lock")

	ComposerTask()
	{
		super()

		configure {
			enabled = !project.gradle.startParameter.isOffline()

			def ext = Utils.getExt(getProject())

			args(ext.composer.get().path.get())
		}

		environmentProperty "COMPOSER", composerJsonName
		environmentProperty "COMPOSER_VENDOR_DIR", vendorDirectoryName
	}
}
