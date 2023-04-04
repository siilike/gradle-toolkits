package ee.keel.gradle.task

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.hjson.JsonValue

@CompileStatic
class HJsonTask extends DefaultTask
{
	private final static Logger logger = Logging.getLogger(HJsonTask)

	@InputFile
	final RegularFileProperty input = project.objects.fileProperty()

	@OutputFile
	final RegularFileProperty output = project.objects.fileProperty()

	HJsonTask()
	{
		super()
	}

	@TaskAction
	void run()
	{
		output.asFile.get().text = JsonValue.readHjson(input.asFile.get().text).toString()
	}
}
