package ee.keel.gradle.dsl

import ee.keel.gradle.Utils

import javax.inject.Inject

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

import groovy.transform.CompileStatic

@CompileStatic
class CopyFromNamed
{
	String name
	Closure configure
}

@CompileStatic
abstract class Includeable extends ToolkitModel
{
	@Input
	final Property<Boolean> enabled = objectFactory.property(Boolean)

	@Input
	final Property<Boolean> excludeMaps = objectFactory.property(Boolean)

	@Input
	final Collection<CopyFromNamed> copyFrom = []

	@Input
	final Collection<CopyFromNamed> copyFromModule = []

	@Input
	final Collection<CopyFromNamed> copyFromLibrary = []

	@Inject
	Includeable(Project project)
	{
		super(project)

		enabled.convention(true)
		excludeMaps.convention(true)
	}

	void copy(Closure c)
	{
		copyFrom.add(new CopyFromNamed(name: (String) null, configure: c))
	}

	void copyModule(String name, Closure c)
	{
		copyFromModule.add(new CopyFromNamed(name: name, configure: c))
	}

	void copyLibrary(String name, Closure c)
	{
		copyFromLibrary.add(new CopyFromNamed(name: name, configure: c))
	}

	void copyModule(String name)
	{
		copyFromModule.add(new CopyFromNamed(name: name, configure: { Directory a, CopySpec s ->
			s.from {
				project.fileTree(a) { ConfigurableFileTree ft ->
					ft.include "client/**"
					ft.include "css/**"
					ft.include ".version"
					ft.include ".version-*"
				}.files
			}
		}))
	}

	void copyLibrary(String name)
	{
		copyFromLibrary.add(new CopyFromNamed(name: name, configure: { Directory a, CopySpec s ->
			s.from {
				project.fileTree(a) { ConfigurableFileTree ft ->
					ft.include "client/**"
					ft.include ".version"
				}.files
			}
		}))
	}
}


@CompileStatic
abstract class AbstractOutputConfig extends Includeable
{
	@Input
	final Property<String> name = objectFactory.property(String)

	@Input
	final MapProperty<String, Closure> packageConfigurators = objectFactory.mapProperty(String, Closure)

	@Inject
	AbstractOutputConfig(Project project)
	{
		super(project)

		name.convention(project.name)
	}
}

@CompileStatic
abstract class RepoConfig extends AbstractOutputConfig
{
	@Input
	final Property<String> path = objectFactory.property(String)

	@Input
	final Property<String> user = objectFactory.property(String)

	@Input
	final Property<String> group = objectFactory.property(String)

	@Inject
	RepoConfig(Project project)
	{
		super(project)

		path.convention(project.provider { "/data1/apps/"+name.get()+"/v"+ Utils.getAnyExt(project).version.get()+"/" })
		user.convention("root")
		group.convention("root")
	}

	void deb(Closure c)
	{
		packageConfigurators.put("deb", c)
	}

	void rpm(Closure c)
	{
		packageConfigurators.put("rpm", c)
	}
}

@CompileStatic
abstract class ArchiveConfig extends AbstractOutputConfig
{
	@Inject
	ArchiveConfig(Project project)
	{
		super(project)

//		enabled.convention(false)
	}

	void zip(Closure c)
	{
		packageConfigurators.put("zip", c)
	}

	void tar(Closure c)
	{
		packageConfigurators.put("tar", c)
	}
}
