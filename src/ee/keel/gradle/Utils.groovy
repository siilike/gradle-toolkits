package ee.keel.gradle

import ee.keel.gradle.dsl.AbstractToolkitExtension
import ee.keel.gradle.dsl.EnvironmentExtension
import ee.keel.gradle.js.BasePlugin
import ee.keel.gradle.js.dsl.JsToolkitExtension
import ee.keel.gradle.php.dsl.PhpToolkitExtension
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

@CompileStatic
@Slf4j("logger")
class Utils
{
	static Plugin getPlugin(Project project, String which)
	{
		def ret = (Plugin) project.extensions.extraProperties.get(which)

		if(!ret)
		{
			throw new IllegalStateException("Plugin ${which.replace('__', '')} not defined!")
		}

		return ret
	}

	static DirectoryProperty getToolsDirectory(Project project)
	{
		JsToolkitExtension js = project.extensions.findByName(BasePlugin.PLUGIN_EXTENSION_NAME) as JsToolkitExtension

		if(js)
		{
			return js.toolsDirectory
		}

		PhpToolkitExtension php = project.extensions.findByName(ee.keel.gradle.php.BasePlugin.PLUGIN_EXTENSION_NAME) as PhpToolkitExtension

		if(php)
		{
			return php.toolsDirectory
		}

		return null
	}

	static EnvironmentExtension getEnvExt(Project project)
	{
		return (EnvironmentExtension) project.extensions.getByName("environments")
	}

	static AbstractToolkitExtension getAnyExt(Project project)
	{
		try
		{
			return ee.keel.gradle.js.Utils.getExt(project)
		}
		catch(Exception e)
		{
			return ee.keel.gradle.php.Utils.getExt(project)
		}
	}

	static String resolvePath(String name)
	{
		return System.getenv("PATH")
				.split(Pattern.quote(File.pathSeparator))
				.collect { Paths.get(it).resolve(name) }
				.find { Files.exists(it) && Files.isExecutable(it) }
	}

	static String buildAndFilterEnv(Map<String, Object> env)
	{
		def s = System.getenv()

		return env.findAll { k, v -> s[k] !== v }.collect { k, v -> k+'="'+String.valueOf(v).replaceAll(/"/, '\"')+'"' }.join("\n")
	}

	static <T extends Task> TaskProvider<T> getOrCreateTaskProvider(Project project, String name, Class<T> task)
	{
		TaskProvider<T> t

		try
		{
			t = project.tasks.named(name, task)
		}
		catch(UnknownTaskException e)
		{
			t = project.tasks.register(name, task)
		}

		return t
	}

	static void debugTaskIO(Project project)
	{
		def width = 80

		project.gradle.taskGraph.afterTask { Task task ->
			println " ${task.name} ".center(width, ">")
			println "name: ${task.name}"
			println "input files:"

			task.inputs.files.each {
				println "\t${it.absolutePath}"
			}

			println "input properties:"

			task.inputs.properties.sort().each { k, v ->
				println "\t${k} = ${v}"
			}

			println "output files:"

			task.outputs.files.each {
				println "\t${it.absolutePath}"
			}

			println " ${task.name} ".center(width, "<")
		}
	}

	@CompileDynamic
	static void silenceFileWatcher()
	{
		def name = "org.gradle.internal.watch.registry.impl.NonHierarchicalFileWatcherUpdater"

		try
		{
			def l = LoggerFactory.getLogger(name)

			if(l instanceof org.gradle.internal.logging.slf4j.OutputEventListenerBackedLogger)
			{
				l.context.addNoOpLogger(name)

				org.gradle.internal.watch.registry.impl.NonHierarchicalFileWatcherUpdater.getDeclaredField("LOGGER").with {
					def m = java.lang.reflect.Field.class.getDeclaredField("modifiers")
					m.setAccessible(true)
					m.setInt(it, getModifiers() & ~java.lang.reflect.Modifier.FINAL)

					setAccessible(true)
					set(null, LoggerFactory.getLogger(name))
				}

				logger.info("Successfully suppressed {} logging", name)
			}
			else
			{
				logger.info("Logger {} is of unknown class {}", name, l.getClass())
			}
		}
		catch(Exception e)
		{
			logger.info("Unable to suppress {} logging", name, e)
		}
	}
}
