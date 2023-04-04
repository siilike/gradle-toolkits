package ee.keel.gradle.dsl

import javax.inject.Inject

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory

import groovy.transform.CompileStatic

@CompileStatic
interface IToolkitModel
{
	ObjectFactory getObjectFactory()
	ProviderFactory getProviderFactory()
	Project getProject()
}

@CompileStatic
abstract class ToolkitModel implements IToolkitModel
{
	protected final Project project

	ToolkitModel(Project project)
	{
		this.project = project
	}

	ToolkitModel()
	{
		this(null)
	}

	@Inject
	abstract ObjectFactory getObjectFactory()

	@Inject
	abstract ProviderFactory getProviderFactory()

	Project getProject()
	{
		return project
	}
}

@CompileStatic
abstract class NamedToolkitModel extends ToolkitModel
{
	final String name

	NamedToolkitModel(String name, Project project)
	{
		super(project)

		this.name = name
	}
}
