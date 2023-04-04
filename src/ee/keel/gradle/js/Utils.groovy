package ee.keel.gradle.js


import ee.keel.gradle.js.dsl.JsToolkitExtension
import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class Utils extends ee.keel.gradle.Utils
{
	static ToolsPlugin getToolsPlugin(Project project)
	{
		return (ToolsPlugin) getPlugin(project, ToolsPlugin.PLUGIN_PROPERTY_NAME)
	}

	static JsToolkitExtension getExt(Project project)
	{
		return (JsToolkitExtension) project.extensions.getByName(BasePlugin.PLUGIN_EXTENSION_NAME)
	}
}
