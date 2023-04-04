package ee.keel.gradle.js

import ee.keel.gradle.dsl.EnvironmentsExtension
import ee.keel.gradle.js.dsl.JsToolkitExtension
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
abstract class BasePlugin implements Plugin<Project>
{
	private final static Logger logger = Logging.getLogger(BasePlugin)

	public static final String BASE_PLUGIN_NAME = "JsToolkitBasePlugin"
	public static final String TOOLS_PLUGIN_NAME = "JsToolkitToolsPlugin"
	public static final String JAVASCRIPT_PLUGIN_NAME = "JsToolkitPlugin"

	public static final String ENV_EXTENSION_NAME = "environments"
	public static final String PLUGIN_EXTENSION_NAME = "jsToolkit"

	public static final String PLUGIN_PROPERTY_NAME = "__jsToolkitBasePlugin"

	protected Project project
	protected JsToolkitExtension ext
	protected EnvironmentsExtension envExt

	BasePlugin()
	{
		Utils.silenceFileWatcher()
	}

	EnvironmentsExtension getEnvExt()
	{
		return envExt
	}

	JsToolkitExtension getExt()
	{
		return ext
	}

	@Override
	void apply(Project project)
	{
		this.project = project

		project.extensions.extraProperties.set(PLUGIN_PROPERTY_NAME, this)

//		envExt = project.extensions.create(ENV_EXTENSION_NAME, EnvironmentsExtension, project)
		ext = project.extensions.create(PLUGIN_EXTENSION_NAME, JsToolkitExtension, project, this)

		if(project.hasProperty("debugTaskIO"))
		{
			Utils.debugTaskIO(project)
		}

		project.afterEvaluate {
			logger.info("Building {} version {}", project.name, project.version)
		}
	}
}
