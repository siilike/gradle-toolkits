package ee.keel.gradle.php

import com.netflix.gradle.plugins.packaging.SystemPackagingBasePlugin
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import ee.keel.gradle.dsl.AbstractOutputConfig
import ee.keel.gradle.dsl.EnvironmentsExtension
import ee.keel.gradle.php.dsl.DistributionConfig
import ee.keel.gradle.php.dsl.PatchConfig
import ee.keel.gradle.php.dsl.PhpToolkitExtension
import ee.keel.gradle.php.task.ComposerInstallTask
import ee.keel.gradle.php.task.PatchTask
import ee.keel.gradle.php.task.PhpUnitTask
import ee.keel.gradle.task.HJsonTask
import ee.keel.gradle.task.ManualCopyTask
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskActionListener
import org.gradle.api.file.Directory
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.deprecation.DeprecationLogger
import org.redline_rpm.header.Os

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern

@CompileStatic
abstract class PhpPlugin implements Plugin<Project>
{
	private final static Logger logger = Logging.getLogger(PhpPlugin)

	public static final String PLUGIN_PROPERTY_NAME = "__phpToolkitPlugin"

	public static final Pattern RESERVED_KEYWORDS = ~/^(\..*|runtime)$/

	protected Project project
	protected PhpToolkitExtension ext
	protected EnvironmentsExtension envExt

	PhpPlugin()
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

		project.plugins.apply(ToolsPlugin)
		project.plugins.apply(SystemPackagingBasePlugin)

		project.extensions.extraProperties.set(PLUGIN_PROPERTY_NAME, this)

		createTasks(project)
	}

	protected void createTasks(Project project)
	{
		def buildSrcDir = Utils.getExt(project).buildSrcDirectory.get()

		/******************************* VALIDATION *******************************/

		project.afterEvaluate {
			[
				getExt().patches,
			].each { container ->
				container.names.each { name ->
					if(name =~ RESERVED_KEYWORDS)
					{
						throw new IllegalStateException("\"${name}\" is a reserved keyword")
					}
				}
			}
		}

		/*********************** ARTIFACTS LIST **************************/

		def artifactsFile = project.file("${project.buildDir}/latest-artifacts")

		project.beforeEvaluate {
			artifactsFile.delete()
		}

		project.gradle.addListener(new TaskActionListener()
		{
			@Override
			void beforeActions(Task task)
			{
			}

			@Override
			void afterActions(Task task)
			{
				if(task instanceof AbstractArchiveTask)
				{
					def files = task.outputs.files.asFileTree.files

					logger.debug("Saving files as artifacts: {}", files)

					artifactsFile << files.collect { it }.join("\n") + "\n"
				}
			}
		})

		/*********************** CONTAINERS & CLEANUP **************************/

		def distAll = project.tasks.register("distAll")

		def distDeb = project.tasks.register("distDeb")

		def distRpm = project.tasks.register("distRpm")

		def distTar = project.tasks.register("distTar")

		def distZip = project.tasks.register("distZip")

		def cleanDist = project.tasks.register("cleanDist", { Task it ->
			it.doLast {
				[
					'dist',
					'debian',
					'tmp',
					'latest-artifacts',
				].each {
					project.delete("${project.buildDir}/it")
				}
			}
		})

		def cleanVendor = project.tasks.register("cleanVendor", { Task t ->
			t.doLast {
				project.delete("${project.buildDir}/vendor")
			}
		})

		def cleanComposerJson = project.tasks.register("cleanComposerJson", { Task t ->
			t.doLast {
				project.delete("${buildSrcDir}/composer.json")
				project.delete("${buildSrcDir}/composer.lock")
			}
		})

		def cleanAll = project.tasks.register("cleanPhp", { Task t ->
			t.dependsOn cleanDist, cleanVendor, cleanComposerJson
		})

		/********************************** PACKAGES ************************************/

		def convertComposerJson = project.tasks.register("convertComposerJson", HJsonTask, { HJsonTask t ->
			def input = project.file("composer.hjson")
			t.enabled = input.exists()
			t.input.set(input)
			t.output.set(project.file("${buildSrcDir}/composer.json"))
		})

//		def applySharedVendorPlugin = project.tasks.register("applySharedVendorPlugin", Task, { Task t ->
//			def c = project.file("${buildSrcDir}/composer.json")
//
//			t.enabled = getExt().environment.get() == "development"
//
//			t.inputs.file c
//			t.outputs.file c
//
//			t.doLast {
//				def v = new JsonSlurper().parse(c) as Map<String, Map<String, Object>>
//
//				if(!v.require.containsKey("siilike/shared-vendor"))
//				{
//					v.require.put("siilike/shared-vendor", "*")
//				}
//			}
//		})

		def copyComposerLock = project.tasks.register("copyComposerLock", ManualCopyTask, { ManualCopyTask t ->
			t.dependsOn convertComposerJson

			t.from "${project.projectDir}/composer.lock"
			t.into buildSrcDir

			t.outputs.file "${buildSrcDir}/composer.lock"
		})

		def copySource = project.tasks.register("copySource", /*Sync*/ Copy, { Copy t ->
			t.from getExt().srcDirectory
			t.into buildSrcDir
//			t.preserve {
//				it.include 'vendor/**'
//				it.include 'composer.lock'
//				it.include 'composer.json'
//			}
		})

		def copyTests = project.tasks.register("copyTests", /*Sync*/ Copy, { Copy t ->
			t.from getExt().testsDirectory
			t.into getExt().buildTestsDirectory
		})

		def copyPhpUnit = project.tasks.register("copyPhpUnit", ManualCopyTask,  { ManualCopyTask t ->
			t.from "${project.projectDir}/phpunit.xml"
			t.into project.buildDir

			t.outputs.file "${project.buildDir}/phpunit.xml"
		})

		def installDependencies = project.tasks.register("installDependencies", ComposerInstallTask, { ComposerInstallTask t ->
			t.dependsOn ToolsPlugin.ENSURE_BUILD_TOOLS_TASK
			t.dependsOn copyComposerLock
			t.dependsOn copySource
			t.dependsOn copyPhpUnit
			t.dependsOn copyTests

			t.setWorkingDir(buildSrcDir)
		})

		def saveLocks = project.tasks.register("saveLocks", Task, { Task t ->
			t.dependsOn installDependencies

			t.inputs.files "${buildSrcDir}/composer.lock"
			t.outputs.files "${project.projectDir}/composer.lock"

			t.doLast {
				Files.copy(project.file("${buildSrcDir}/composer.lock").toPath(), project.file("${project.projectDir}/composer.lock").toPath(), StandardCopyOption.REPLACE_EXISTING)
			}
		})

		def assembleSource = project.tasks.register("assembleSource", Task, { Task t ->
			t.dependsOn installDependencies
			t.dependsOn copySource
		})

		def patchSource = project.tasks.register("patchSource", Task, { Task t ->
			t.dependsOn assembleSource
		})

		getExt().patches.configureEach { PatchConfig patch ->
			def name = patch.name.replaceAll(/[\-\/]+/, '_')

			def patchTask = project.tasks.register("patchSource-"+name, PatchTask, { PatchTask t ->
				t.dependsOn assembleSource
				t.enabled = patch.enabled.get()

				t.patch.set(project.file("${project.projectDir}/patches/${patch.name}.patch"))
				t.directory.set(project.file(patch.path))
			})

			patchSource.configure {
				it.dependsOn patchTask
			}
		}

		def dist = project.tasks.register("dist", /*Sync*/ Copy, { Copy t ->
			t.dependsOn patchSource
			t.enabled = getExt().buildSrcDirectory.get().asFile.absolutePath != getExt().buildDistDirectory.get().asFile.absolutePath

			t.from buildSrcDir
			t.into getExt().buildDistDirectory
		})

		distAll.configure {
			it.dependsOn dist
		}

		def phpstan = project.tasks.register("phpstan", Exec, { Exec t ->
			t.dependsOn dist

			def input = "${project.projectDir}/phpstan.neon"
			def out = project.layout.buildDirectory.file("phpstan.xml")

			t.enabled = getExt().phpstan.get().enabled.get() && project.file(input).exists()

			t.inputs.dir buildSrcDir
			t.inputs.file input
			t.outputs.file out

			t.workingDir project.buildDir

			t.ignoreExitValue = true

			t.executable getExt().toolsDirectory.file("bin/php").get().asFile.absolutePath

			t.args [
				getExt().toolsDirectory.file("bin/phpstan").get().asFile.absolutePath,
				"analyze",
				"--memory-limit=2G",
				"--no-interaction",
//				"--no-progress",
				"-l", getExt().phpstan.get().level.get(),
				"--error-format=checkstyle",
				buildSrcDir,
			]

			t.doFirst {
				t.standardOutput = new FileOutputStream(out.get().asFile)

				project.copy {
					from project.projectDir
					into project.buildDir
					include "phpstan.neon"
				}
			}
		})

		def test = project.tasks.register("test", PhpUnitTask, { PhpUnitTask t ->
			t.dependsOn dist
			t.dependsOn copyTests
			t.dependsOn copyPhpUnit

			t.enabled = getExt().buildTestsDirectory.asFile.get().exists()

			t.workingDir project.buildDir
		})

		project.tasks.named("check").configure {
			it.dependsOn dist
			it.dependsOn test
		}

		def analyze = project.tasks.register("analyze", Task, {
			it.dependsOn dist
			it.dependsOn phpstan
		})

		def createArchiveTasks = { DistributionConfig conf, Map<Class, TaskProvider> archiveTasks, Provider<Directory> dst, Provider<String> version, String prefix, TaskProvider<? extends Task> distTask, boolean versionDependentPackageName ->
			def dcap = conf.name.capitalize()
			def pcap = prefix.capitalize()
			Collection<TaskProvider> ret = []

			archiveTasks.keySet().each { taskClass ->
				def name = taskClass.simpleName

				TaskProvider<? extends AbstractArchiveTask> task = project.tasks.register("dist${dcap}${pcap}${name}", taskClass, { AbstractArchiveTask t ->
					t.destinationDirectory.set(dst)
					t.archiveBaseName.set(conf.archive.map { it.name.get() })
					t.archiveVersion.set(version)
					t.archiveFileName.set(conf.archive.map { it.name.get() + '.' + t.archiveExtension.get() })

					if(t instanceof Tar)
					{
						t.compression = Compression.GZIP
						t.archiveExtension.set("tar.gz")
					}

					t.dependsOn dist

					t.from getExt().buildDistDirectory

					AbstractOutputConfig c = null

					if(t instanceof SystemPackagingTask)
					{
						c = conf.repo.get()

						// via delegate to SystemPackagingExtension
						t.packageName = versionDependentPackageName ? "${conf.repo.get().name.get()}-${version.get()}".toLowerCase() : conf.repo.get().name.get()

						DeprecationLogger.whileDisabled {
							t.version = versionDependentPackageName ? '1' : version.get()
						}

						t.os = Os.LINUX
						t.summary = conf.repo.get().name.get()

						t.into conf.repo.get().path

						def p = conf.repo.get().path.get().substring(1)

						if(p[p.length()-1] == "/")
						{
							p = p.substring(0, p.length()-1)
						}

						t.inputs.property "packageName", ""
						t.inputs.property "exten.packageName", ""
						t.inputs.properties.sort().each { k, v ->
							if(k.startsWith("rootSpec\$") && k.endsWith(".destPath") && v.toString().startsWith(p))
							{
								t.inputs.property k, ""
							}
						}

						t.outputs.upToDateWhen { true }
					}
					else
					{
						c = conf.archive.get()
					}

					if(c)
					{
						t.enabled &= c.enabled.get()

						Closure a = c.packageConfigurators.get().get(name.toLowerCase())

						if(a)
						{
							a.setDelegate(t)
							a()
						}
					}

					t
				})

				distTask?.configure {
					it.dependsOn task
				}

				archiveTasks[taskClass].configure {
					((Task) it).dependsOn task
				}

				ret.add(task)
			}

			return ret
		}

		Map<Class, TaskProvider> archiveTasks = [:].with {
			put(Tar, distTar)
			put(Zip, distZip)
			put(ee.keel.gradle.task.Deb, distDeb)
			put(ee.keel.gradle.task.Rpm, distRpm)
			it
		}

		def conf = getExt().distribution.get()

		def dst = project.layout.buildDirectory.map { it.dir(conf.name) }

		createArchiveTasks(conf, archiveTasks, dst, getExt().version, "", distAll, true)

		project.tasks.named("assemble").configure {
			it.dependsOn dist
		}

		project.tasks.named("build").configure {
			it.dependsOn "assemble"
			it.dependsOn test
		}
	}
}
