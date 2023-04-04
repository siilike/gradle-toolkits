package ee.keel.gradle.php.task

import ee.keel.gradle.dsl.WithEnvironmentProperties
import ee.keel.gradle.php.Utils
import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input

@CompileStatic
class PhpTask extends Exec implements WithEnvironmentProperties
{
	private final static Logger logger = Logging.getLogger(PhpTask)

	@Input
	final ListProperty<String> command = project.objects.listProperty(String)

	PhpTask()
	{
		super()

		configure {
			def ext = Utils.getExt(getProject())

			setExecutable(ext.php.get().path.get())
		}
	}

	@Override
	protected void exec()
	{
		applyEnvironmentProperties()

		args(command.get())

		super.exec()
	}
}
