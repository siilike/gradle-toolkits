package ee.keel.gradle.php

import ee.keel.gradle.dsl.ToolConfig
import ee.keel.gradle.php.dsl.PhpToolkitExtension
import ee.keel.gradle.php.task.ComposerInstallTask
import ee.keel.gradle.task.DownloadToolTask
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Delete

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions

@CompileStatic
abstract class ToolsPlugin implements Plugin<Project>
{
	private final static Logger logger = Logging.getLogger(ToolsPlugin)

	public static final String PLUGIN_PROPERTY_NAME = "__phpToolkitToolsPlugin"

	public static final String CLEAN_TOOLS_TASK = "cleanTools"
	public static final String INSTALL_BUILD_TOOLS_TASK = "installBuildTools"
	public static final String ENSURE_BUILD_TOOLS_TASK = "ensureBuildTools"

	protected Project project

	ToolsPlugin()
	{
	}

	PhpToolkitExtension getExt()
	{
		return Utils.getExt(project)
	}

	@Override
	void apply(Project project)
	{
		this.project = project

		project.plugins.apply(BasePlugin)

		project.extensions.extraProperties.set(PLUGIN_PROPERTY_NAME, this)

		project.afterEvaluate {
			createTasks(project)
		}
	}

	Project getParentConfigProject()
	{
		def proj = project.parent

		while(proj)
		{
			def plugins = (Collection<Plugin>) proj.plugins // CompileStatic fail

			// plugins.findPlugin does not work
			if(plugins.find { it instanceof ToolsPlugin })
			{
				return proj
			}

			proj = proj.parent
		}

		return null
	}

	protected void createTasks(Project project)
	{
		if(getExt().preferParentTools.get())
		{
			getExt().preferParentTools.finalizeValue()

			def parent = getParentConfigProject()

			if(parent)
			{
				logger.warn("{} uses tools configuration from parent project {}", project.name, parent.name)

				def parentConfig = Utils.getExt(parent)

				getExt().toolsDirectory.set(parentConfig.toolsDirectory)
				getExt().php.get().path.set(parentConfig.php.get().path)
				getExt().composer.get().path.set(parentConfig.composer.get().path)

				def ensureBuildTools = project.tasks.register(ENSURE_BUILD_TOOLS_TASK, { Task it ->
					it.dependsOn parent.tasks.named(INSTALL_BUILD_TOOLS_TASK)
				})

				return
			}
		}

		def cleanTools = project.tasks.register(CLEAN_TOOLS_TASK, Delete, { Delete it ->
			it.followSymlinks = false
			it.delete getExt().toolsDirectory
		})

		def downloadComposer = project.tasks.register("downloadComposer", DownloadToolTask, { DownloadToolTask it ->
			def version = getExt().composer.map { it.version.get() }

			it.enabled = !project.gradle.startParameter.isOffline() && version.get() != "local"

			it.isTar.set(false)
			it.toolName.set("composer")
			it.location.set(version.map { v ->
				if(v =~ /^[0-9]+\.[0-9]+\.[0-9]+$/ || v =~ /^latest-/)
				{
					return new URL("https://getcomposer.org/download/${v}/composer.phar")
				}

				return v
			})
		})

		def copyPhpDeps = project.tasks.register("copyPhpDependencies", { Task it ->
			[
				"php/composer.json",
			].each { f ->
				def outFile = new File(getExt().toolsDirectory.asFile.get(), f)

				it.outputs.file outFile

				it.doLast {
					Files.copy(ToolsPlugin.getResourceAsStream("/${f}"), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
				}
			}
		})

		def setupTools = project.tasks.register("setupTools", { Task it ->
			it.dependsOn downloadComposer
			it.dependsOn copyPhpDeps
		})

		def generateBuildToolsComposerJson = project.tasks.register("generateBuildToolsComposerJson", { Task it ->
			def configFile = getExt().packages.get().configFile.present ? getExt().packages.get().configFile : getExt().toolsDirectory.file("php/composer.json")

			it.dependsOn setupTools

			it.inputs.property "versions", getExt().packages.get().versions
			it.inputs.files getExt().toolsLockfile

			if(configFile.present)
			{
				it.inputs.files configFile
			}

			it.outputs.files getExt().toolsDirectory.file("composer.json")
			it.outputs.files getExt().toolsDirectory.file("composer.lock")

			it.doFirst {
				Map<String, ?> input = (configFile.present ? new JsonSlurper().parse(configFile.get().asFile) : [:]) as Map<String, ?>

				if(!input.containsKey("require"))
				{
					input.put("require", [:])
				}

				def require = input.require as Map<String, String>

				getExt().packages.get().packageVersions.each {
					if(!require.containsKey(it.key))
					{
						require[it.key] = it.value
					}
				}

				getExt().toolsDirectory.file("composer.json").get().asFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(input))
			}

			it.doFirst {
				def lf = getExt().toolsLockfile.asFile.get()

				if(lf.exists())
				{
					Files.copy(lf.toPath(), getExt().toolsDirectory.file("composer.lock").get().asFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
				}
			}
		})

		def installBuildToolsComposer = project.tasks.register("installBuildToolsComposer", ComposerInstallTask, { ComposerInstallTask it ->
			it.dependsOn setupTools
			it.dependsOn generateBuildToolsComposerJson

			it.inputs.files generateBuildToolsComposerJson.get().outputs
			it.outputs.dir getExt().toolsDirectory.dir("bin")

			it.setWorkingDir(getExt().toolsDirectory)
		})

		def writeToolsSymlinks = project.tasks.register("writeToolsSymlinks", { Task t ->
			t.dependsOn setupTools

			[
				getExt().php,
				getExt().composer,
			].each {
				def tool = (ToolConfig) it.get()
				def bin = getExt().toolsDirectory.file("bin/"+tool.name).get().asFile

				t.inputs.files tool.path
				t.outputs.files bin

				t.doLast {
					if(bin.exists())
					{
						bin.delete()
					}

					if(tool.name == 'php')
					{
						Files.createSymbolicLink(bin.toPath(), Path.of(tool.path.get()))
					}
					else
					{
						def phpPath = getExt().php.get().path.get()
						def toolPath = tool.path.get()
						def toolsDir = getExt().toolsDirectory.get().asFile.absolutePath

						if(toolPath.indexOf(toolsDir) == 0)
						{
							toolPath = '`dirname $0`/../' + toolPath.substring(toolsDir.length())
						}

						bin.text = """#!/bin/sh
exec "${phpPath}" "${toolPath}" "\$@"
"""

						Files.setPosixFilePermissions(bin.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"))
					}
				}
			}
		})

		def rewriteComposerBinPhpPath = project.tasks.register("rewriteComposerBinPhpPath", Task, { Task it ->
			it.dependsOn installBuildToolsComposer
			it.dependsOn writeToolsSymlinks

			def binDir = getExt().toolsDirectory.dir("bin")

			it.inputs.dir binDir
			it.outputs.dir binDir

			it.doLast {
				binDir.get().asFile.listFiles().each {
					def file = it.text

					if(file =~ /^#!\/usr\/bin\/(env php|php)\n/)
					{
//						it.text = file.replaceFirst(/^#!\/usr\/bin\/(env php|php)/, "#!"+binDir.get().file("php").asFile.absolutePath)
//						it.text = file.replaceFirst(/^#!\/usr\/bin\/(env php|php)/, '#!/bin/env -S /bin/sh -c \'`dirname \\\$0`/php \\\$0\'')
						it.text = file.replaceFirst(/^#!\/usr\/bin\/(env php|php)/, "#!"+getExt().php.get().path.get())
					}
				}
			}
		})

		def saveBuildToolsLocks = project.tasks.register("saveBuildToolsLocks", { Task it ->
			it.dependsOn installBuildToolsComposer

			it.inputs.files getExt().toolsDirectory.file("composer.lock")

			it.outputs.files getExt().toolsLockfile

			it.doLast {
				Files.copy(getExt().toolsDirectory.file("composer.lock").get().asFile.toPath(), getExt().toolsLockfile.asFile.get().toPath(), StandardCopyOption.REPLACE_EXISTING)
			}
		})

		def generateVEnv = project.tasks.register("generateVEnv", { Task it ->
			def f = getExt().toolsDirectory.get().file("venv").asFile

			it.inputs.property 'directory', getExt().toolsDirectory.get().asFile.path
			it.outputs.file f

			it.doLast {
				f.text = '''#!/bin/bash
export PATH="$(realpath `dirname $0`)/bin:$PATH"
exec bash --rcfile <(echo 'PS1="$PS1(venv) "') -i
'''

				Files.setPosixFilePermissions(f.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"))
			}
		})

		def installBuildTools = project.tasks.register(INSTALL_BUILD_TOOLS_TASK, { Task it ->
			it.dependsOn installBuildToolsComposer
			it.dependsOn rewriteComposerBinPhpPath
			it.dependsOn writeToolsSymlinks
			it.dependsOn generateVEnv
		})

		def ensureBuildTools = project.tasks.register(ENSURE_BUILD_TOOLS_TASK, { Task it ->
			it.dependsOn INSTALL_BUILD_TOOLS_TASK
		})
	}
}
