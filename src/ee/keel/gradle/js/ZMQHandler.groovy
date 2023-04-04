package ee.keel.gradle.js

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.AbstractQueuedSynchronizer

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
class ZMQHandler
{
	private final static Logger logger = Logging.getLogger(ZMQHandler)

	protected Project project
	protected ZContext context
	protected ZMQ.Socket socket
	protected int port
	protected Thread receiver
	protected Map<String, String> status = new HashMap<>()
	protected Map<String, Sync> statusSync = new ConcurrentHashMap<>()

	ZMQHandler(Project project)
	{
		this.project = project
	}

	void init()
	{
		if(context)
		{
			logger.debug("Already initialized")
			return
		}

		JavascriptPlugin plugin = null

		try
		{
			plugin = (JavascriptPlugin) Utils.getPlugin(project, JavascriptPlugin.PLUGIN_PROPERTY_NAME)
		}
		catch(Exception e)
		{
			logger.debug("Unable to get JavascriptPlugin", e)
		}

		context = new ZContext()
		socket = context.createSocket(SocketType.PULL)
		port = socket.bindToRandomPort("tcp://127.0.0.1", 20000, 30000)

		logger.debug("Bound handler to port {}", port)

		receiver = new Thread("ZMQHandler:"+port) {
			void run()
			{
				def json = new JsonSlurper()

				while(!isInterrupted())
				{
					Map v = (Map) json.parse(socket.recv(0))
					def k = v.id as String

					logger.debug("Got input from ZMQ {}", v)

					status.put(k, v.event as String)

					def l = getStatusSync(k)

					if(v.event.equals("done"))
					{
						logger.debug("Marking {} as done", k)

						plugin?.broadcast("done", [ "id": k ])

						l.done()
					}
					else if(v.event.equals("watchRun"))
					{
						logger.debug("Marking {} as running", k)

						plugin?.broadcast("build", [ "id": k ])

						l.running()
					}
					else if(v.event.equals("error"))
					{
						logger.error("An error ocurred when running {}", k)

						plugin?.broadcast("error", [ "id": k ])
					}
					else if(v.event.equals("init"))
					{
						logger.debug("{} initialized", k)

						plugin?.broadcast("init", [ "id": k ])
					}
					else
					{
						logger.warn("Unknown event {}", v.event)
					}
				}
			}
		}

		receiver.start()
	}

	int getPort()
	{
		return port
	}

	String getStatus(String k)
	{
		return status[k]
	}

	Sync getStatusSync(String k)
	{
		def ret = statusSync[k]

		if(!ret)
		{
			ret = new Sync()
			statusSync[k] = ret
		}

		return ret
	}

	class Sync extends AbstractQueuedSynchronizer
	{
		protected static final int DONE = 0
		protected static final int RUNNING = 1

		Sync()
		{
			setState(DONE)
		}

		void running()
		{
			setState(RUNNING)
		}

		void done()
		{
			releaseShared(DONE)
		}

		boolean isRunning()
		{
			return getState() == RUNNING
		}

		boolean isPendingState(int state)
		{
			return state == RUNNING
		}

		boolean inPendingState()
		{
			return isPendingState(getState())
		}

		void awaitUninterruptibly()
		{
			if(inPendingState())
			{
				acquireShared(-1)
			}
		}

		void await() throws InterruptedException
		{
			if(inPendingState())
			{
				acquireSharedInterruptibly(-1)
			}
		}

		void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
		{
			if(inPendingState())
			{
				if(!tryAcquireSharedNanos(-1, unit.toNanos(timeout)))
				{
					throw new TimeoutException()
				}
			}
		}

		@Override
		protected int tryAcquireShared(int state)
		{
			if(inPendingState())
			{
				return -1
			}

			if(state != -1)
			{
				setState(state)
			}

			return 1
		}

		@Override
		protected boolean tryReleaseShared(int state)
		{
			if(isPendingState(state))
			{
				return false
			}

			if(state != -1)
			{
				setState(state)
			}

			return true
		}
	}
}
