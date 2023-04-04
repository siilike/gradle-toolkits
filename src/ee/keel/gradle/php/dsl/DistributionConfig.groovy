package ee.keel.gradle.php.dsl

import ee.keel.gradle.dsl.ArchiveConfig
import ee.keel.gradle.dsl.RepoConfig
import ee.keel.gradle.dsl.ToolkitModel
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

import javax.inject.Inject

@CompileStatic
abstract class DistributionConfig extends ToolkitModel
{
	@Input
	final String name

	@Input
	final Property<RepoConfig> repo = objectFactory.property(RepoConfig)

	@Input
	final Property<ArchiveConfig> archive = objectFactory.property(ArchiveConfig)

	@Inject
	DistributionConfig(String name, Project project)
	{
		super(project)

		this.name = name

		repo.convention(objectFactory.newInstance(RepoConfig, project))
		archive.convention(objectFactory.newInstance(ArchiveConfig, project))
	}

	void repo(Closure c)
	{
		c.delegate = repo.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}

	void archive(Closure c)
	{
		c.delegate = archive.get()
		c.setResolveStrategy(Closure.DELEGATE_FIRST)
		c()
	}
}
