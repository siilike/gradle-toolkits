package ee.keel.gradle.js.dsl

import ee.keel.gradle.dsl.NamedToolkitModel
import ee.keel.gradle.dsl.WithIncludes
import ee.keel.gradle.dsl.WithOutputs

import javax.inject.Inject

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

import groovy.transform.CompileStatic

@CompileStatic
abstract class LibraryConfig extends NamedToolkitModel implements WithOutputs, WithIncludes
{
	@Input
	final Property<Boolean> enabled = objectFactory.property(Boolean)

	@Input
	final Property<BabelConfig> babel = objectFactory.property(BabelConfig)

	@Inject
	LibraryConfig(String name, Project project)
	{
		super(name, project)

		enabled.convention(true)
		babel.convention(objectFactory.newInstance(BabelConfig, project))
	}
}
