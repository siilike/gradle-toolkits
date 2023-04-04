package ee.keel.gradle.php.task

import ee.keel.gradle.php.Utils
import io.reflectoring.diffparser.api.UnifiedDiffParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

class PatchTask extends DefaultTask
{
	private final static Logger logger = Logging.getLogger(PatchTask)

	@InputFile
	final RegularFileProperty patch = project.objects.fileProperty()

	@Internal
	final DirectoryProperty directory = project.objects.directoryProperty().convention(Utils.getExt(project).buildSrcDirectory)

	PatchTask()
	{
		super()
	}

	@OutputFiles
	List<File> getOutputFiles()
	{
		return new UnifiedDiffParser().parse(patch.get().asFile.bytes).collect { diff ->
			directory.file(diff.fromFileName).get().asFile
		}
	}

	@TaskAction
	void run()
	{
		def r = project.exec {
			it.ignoreExitValue true

			it.workingDir directory

			it.commandLine "patch"

			it.args = [
				'-r', '-',
				'-p0',
				'-R',
				'--dry-run',
				'--input', patch.get().asFile.absolutePath,
			]
		}

		if(r.getExitValue() != 0)
		{
			project.exec {
				it.workingDir directory

				it.commandLine "patch"

				it.args = [
					'-p0',
					'--input', patch.get().asFile.absolutePath,
				]
			}
		}
	}
}
