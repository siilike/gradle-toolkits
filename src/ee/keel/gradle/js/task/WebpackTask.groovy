package ee.keel.gradle.js.task


import groovy.transform.CompileStatic

@CompileStatic
class WebpackTask extends AbstractWebpackTask
{
	WebpackTask()
	{
		super()

		configure {
			args jstk.toolsDirectory.get().file("node_modules/webpack/bin/webpack.js").asFile.absolutePath

			if(project.logger.debugEnabled)
			{
				args '--stats-errors', '--stats-error-details', 'true', '--stats-error-stack', '--stats-chunks', '--stats-modules', '--stats-reasons', '--stats-warnings', '--stats-assets'
			}

			environmentFile.fileProvider(this.module.map { new File(project.buildDir, "webpack."+it+".env") })
			environmentProvider "RESOLVE_FILE", this.module.map { new File(project.buildDir, "webpack."+it+".js") }
		}
	}

	@Override
	protected void exec0()
	{
		super.exec0()

		args '--config', config.get().asFile.absolutePath

		if(continuous.get())
		{
			args '--watch'
		}
	}
}
