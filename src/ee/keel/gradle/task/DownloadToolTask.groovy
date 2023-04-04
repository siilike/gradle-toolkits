package ee.keel.gradle.task

import ee.keel.gradle.js.Utils
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@CompileStatic
class DownloadToolTask extends DefaultTask
{
	private final static Logger logger = Logging.getLogger(DownloadToolTask)

	@Input
	final Property<String> toolName = project.objects.property(String)

	@Input
	final Property<Object> location = project.objects.property(Object)

	@Input
	final Property<Boolean> stripFirstDirectory = project.objects.property(Boolean).convention(false)

	@Input
	final Property<Boolean> isTar = project.objects.property(Boolean).convention(true)

	@Internal
	final DirectoryProperty toolsDirectory = Utils.getToolsDirectory(project)

	@OutputDirectory
	final DirectoryProperty fullOutputDirectory = project.objects.directoryProperty().fileProvider(project.provider({ new File(toolsDirectory.getAsFile().get(), toolName.get()) }))

	DownloadToolTask()
	{
		logging.captureStandardError(LogLevel.LIFECYCLE)

		configure {
			enabled = !project.gradle.startParameter.isOffline()
		}
	}

	@TaskAction
	void run()
	{
		def l = location.get()

		File outDir = fullOutputDirectory.asFile.get()

		if(outDir.exists())
		{
			outDir.deleteDir()
		}

		outDir.mkdirs()

		File downloaded = null

		try
		{
			if(l instanceof URL)
			{
				URL url = l

				downloaded = toolsDirectory.file(toolName.map { it+".download" }).get().asFile.absoluteFile

				project.exec({ ExecSpec it ->
					it.setExecutable("wget")
					it.setArgs([ l as String, "-O", downloaded.absolutePath ])
				})

				l = downloaded
			}

			if(l instanceof File)
			{
				File file = l
				Path toolPath = outDir.toPath().resolve(toolName.get())

				if(isTar.get())
				{
					project.exec({ ExecSpec it ->
						it.setExecutable("tar")
						it.setArgs([ "xf", file.absolutePath, '--directory', fullOutputDirectory.get().asFile.absolutePath ])

						if(stripFirstDirectory.get())
						{
							it.args "--strip", "1"
						}
					})
				}
				else if(downloaded)
				{
					file.renameTo(toolPath.toFile())
				}
				else
				{
					Files.copy(file.toPath(), toolPath, StandardCopyOption.REPLACE_EXISTING)
				}
			}
			else
			{
				throw new IllegalStateException("Unknown input type ${l}")
			}
		}
		finally
		{
			downloaded?.delete()
		}
	}
}
