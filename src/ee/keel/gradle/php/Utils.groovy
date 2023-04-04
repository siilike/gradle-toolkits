package ee.keel.gradle.php


import ee.keel.gradle.php.dsl.PhpToolkitExtension
import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class Utils extends ee.keel.gradle.Utils
{
	static PhpToolkitExtension getExt(Project project)
	{
		return (PhpToolkitExtension) project.extensions.getByName(BasePlugin.PLUGIN_EXTENSION_NAME)
	}
}
