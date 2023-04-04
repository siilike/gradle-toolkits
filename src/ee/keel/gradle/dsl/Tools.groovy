package ee.keel.gradle.dsl

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

import javax.inject.Inject

@CompileStatic
abstract class ToolConfig extends ToolkitModel
{
	final String name

	@Input
	final Property<Object> version = objectFactory.property(Object).convention("local")

	@Input
	final Property<String> path = objectFactory.property(String)

	@Input
	final ListProperty<String> args = objectFactory.listProperty(String)

	@Inject
	ToolConfig(Project project, String name)
	{
		super(project)

		this.name = name
	}

	void arg(Object... k)
	{
		args.addAll(k.collect { String.valueOf(it) })
	}
}

@CompileStatic
abstract class PackagesConfig extends ToolkitModel
{
	@Input
	final MapProperty<String, String> versions = objectFactory.mapProperty(String, String)

	@Optional
	@InputFile
	final RegularFileProperty configFile = objectFactory.fileProperty()

	@Inject
	PackagesConfig(Project project)
	{
		super(project)
	}

	void version(String k, Object v)
	{
		versions.put(k, String.valueOf(v))
	}

	Map<String, String> getPackageVersions()
	{
		return versions.get()
	}

	void clearVersions()
	{
		versions.empty()
	}
}
