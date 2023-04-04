package ee.keel.gradle.js.task

import ee.keel.gradle.StreamLogger
import ee.keel.gradle.js.ZMQHandler

import javax.inject.Inject

import org.gradle.api.NonExtensible
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.deployment.internal.Deployment
import org.gradle.deployment.internal.DeploymentHandle
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.deployment.internal.DeploymentRegistry.ChangeBehavior
import org.gradle.process.internal.DefaultExecSpec
import org.gradle.process.internal.ExecAction
import org.gradle.process.internal.ExecException

import ee.keel.gradle.js.JavascriptPlugin
import ee.keel.gradle.js.Utils
import groovy.transform.CompileStatic

@CompileStatic
class ContinuousExecTask extends Exec
{
	private final static Logger logger = Logging.getLogger(ContinuousExecTask)

	@Input
	final Property<Boolean> continuous = project.objects.property(Boolean).convention(false)

	ContinuousExecTask()
	{
		logging.captureStandardOutput(getStdoutLogLevel())
		logging.captureStandardError(getStdErrLogLevel())

		configure {
			outputs.upToDateWhen { !continuous.get() }
		}
	}

	@Internal
	LogLevel getStdoutLogLevel()
	{
		return LogLevel.INFO
	}

	@Internal
	LogLevel getStdErrLogLevel()
	{
		return LogLevel.WARN
	}

	protected boolean continuousRunning()
	{
		return true
	}

	@Override
	protected void exec()
	{
		if(continuous.get())
		{
			logger.debug("Running {} continuously", getPath())

			def deploymentRegistry = services.get(DeploymentRegistry)
			def deploymentHandle = deploymentRegistry.get(getPath(), ExecDeploymentHandle)
			def zmq, l

			if(!deploymentHandle)
			{
				def field = AbstractExecTask.getDeclaredField("execSpec")
				field.setAccessible(true)
				def taskExecSpec = (DefaultExecSpec) field.get(this)

				def action = execActionFactory.newExecAction()
				taskExecSpec.copyTo(action)

				zmq = new ZMQHandler(project)
				zmq.init()

				l = zmq.getStatusSync(getPath())

				action.environment "ZMQ_ADDR", "tcp://127.0.0.1:"+zmq.port
				action.environment "ZMQ_ID", getPath()

				try
				{
					def plugin = (JavascriptPlugin) Utils.getPlugin(project, JavascriptPlugin.PLUGIN_PROPERTY_NAME)

					action.environment "ZMQ_BROADCAST", "tcp://127.0.0.1:"+plugin.getBroadcastPort()
				}
				catch(Exception e)
				{
					logger.debug("Unable to set ZMQ broadcast info", e)
				}

				if(continuousRunning() && !l.isRunning())
				{
					l.running()
				}

				action.setStandardOutput(new StreamLogger(logger, getStdoutLogLevel()))
				action.setErrorOutput(new StreamLogger(logger, getStdErrLogLevel()))

				deploymentRegistry.start(getPath(), ChangeBehavior.BLOCK, ExecDeploymentHandle, action, zmq)
			}
			else
			{
				zmq = deploymentHandle.zmqHandler
				l = zmq.getStatusSync(getPath())
			}

			logger.lifecycle("Awaiting {} to finish", getPath())

			l.await()

			logger.debug("{} finished", getPath())
		}
		else
		{
			super.exec()
		}
	}

	@CompileStatic
	@NonExtensible
	static class ExecDeploymentHandle implements DeploymentHandle
	{
		private final static Logger logger = Logging.getLogger(ExecDeploymentHandle)

		protected final ExecAction execAction
		protected Thread thread
		protected ZMQHandler zmqHandler

		@Inject
		ExecDeploymentHandle(ExecAction execAction, ZMQHandler zmqHandler)
		{
			this.execAction = execAction
			this.zmqHandler = zmqHandler
		}

		ZMQHandler getZmqHandler()
		{
			return zmqHandler
		}

		@Override
		boolean isRunning()
		{
			return thread && thread.isAlive()
		}

		@Override
		void start(Deployment deployment)
		{
			logger.lifecycle("Starting {}", execAction.executable)

			thread = new Thread() {
				void run()
				{
					logger.lifecycle("Executing {} {}", execAction.executable, execAction.args)

					def result = execAction.execute()

					throw new ExecException("Process exited with status "+result.getExitValue())
				}
			}

			thread.start()
		}

		@Override
		void stop()
		{
			logger.lifecycle("Stopping {}", execAction.executable)

			if(thread)
			{
				thread.interrupt()
			}
		}
	}
}
