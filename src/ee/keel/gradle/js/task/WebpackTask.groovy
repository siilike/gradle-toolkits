package ee.keel.gradle.js.task

import groovy.transform.CompileStatic

@CompileStatic
class WebpackTask extends AbstractWebpackTask
{
	WebpackTask()
	{
		super()

		doLast { t ->
			new File(project.buildDir, "webpack."+this.module.get()+".env").text = getEnvironment().collect { k, v -> k+'="'+String.valueOf(v).replaceAll(/"/, '\"')+'"' }.join("\n")
		}

		configure {
			if(project.hasProperty('inspect')) {
				args '--inspect-brk'
			}

// 			args '--inspect'

			args jstk.toolsDirectory.get().file("node_modules/webpack/bin/webpack.js").asFile.absolutePath

			if(project.logger.debugEnabled)
			{
				args '--stats-errors', '--stats-error-details', 'true', '--stats-error-stack', '--stats-chunks', '--stats-modules', '--stats-reasons', '--stats-warnings', '--stats-assets'
			}
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

		environment "RESOLVE_FILE", new File(project.buildDir, "webpack."+module.get()+".js")
	}
}
