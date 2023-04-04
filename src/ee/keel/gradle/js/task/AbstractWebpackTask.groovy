package ee.keel.gradle.js.task

import javax.inject.Inject

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory

import ee.keel.gradle.js.Utils
import ee.keel.gradle.js.dsl.JsToolkitExtension
import groovy.transform.CompileStatic

@CompileStatic
abstract class AbstractWebpackTask extends NodeTask
{
	private final static Logger logger = Logging.getLogger(AbstractWebpackTask)

	protected final JsToolkitExtension jstk = Utils.getExt(project)

	@Input
	final Property<String> module = project.objects.property(String)

	@Input
	final Property<String> env = project.objects.property(String)

	@Input
	final Property<String> browsersListEnv = project.objects.property(String).convention("production")

	@InputFile
	final RegularFileProperty config = project.objects.fileProperty()

	@InputFile
	final RegularFileProperty babelConfig = project.objects.fileProperty()

	@Input
	final Property<String> reactPragma = project.objects.property(String).convention(jstk.babel.map { it.reactPragma.getOrElse("") })

	@Input
	final Property<Boolean> alwaysTranspile = project.objects.property(Boolean).convention(jstk.webpack.map { it.alwaysTranspile.get() })

	@Input
	final Property<Boolean> useSWC = project.objects.property(Boolean).convention(jstk.webpack.map { it.useSWC.get() })

	@Input
	final ListProperty<String> libraries = project.objects.listProperty(String).convention([])

	@Input
	final ListProperty<String> entries = project.objects.listProperty(String).convention([])

	@Input
	final Property<Boolean> preferModules = project.objects.property(Boolean).convention(true)

	@Input
	final Property<Boolean> minify = project.objects.property(Boolean).convention(true)

	@OutputDirectory
	final DirectoryProperty outputDirectory = project.objects.directoryProperty().convention(project.layout.buildDirectory)

	@OutputDirectory
	final DirectoryProperty recordsDirectory = project.objects.directoryProperty().convention(project.layout.buildDirectory.dir("records"))

	// output for library, input for module
	@Internal
	final DirectoryProperty manifestDirectory = project.objects.directoryProperty().convention(project.layout.buildDirectory.dir("manifest"))

	@Input
	final Property<Boolean> hmr = project.objects.property(Boolean).convention(project.hasProperty("hmr"))

	@Input
	final Property<String> sourceMap = project.objects.property(String).convention(jstk.environment.map { env ->
		if(env == 'debug') {
			return 'eval-source-map'
		}

		return 'source-map'
	})

	@Inject
	AbstractWebpackTask()
	{
		super()

		configure {
			setWorkingDir(project.buildDir)

			if(project.hasProperty('sourceMap')) {
				sourceMap.set(String.valueOf(project.properties.sourceMap))
			}

			environmentDirProvider "TOOLS_DIR", jstk.toolsDirectory

			environmentProperty "ENV", env
			environmentProperty "MODULE", module
			environmentProperty "BROWSERSLIST_ENV", browsersListEnv
			environmentProperty "MINIFY", minify
			environmentProperty "PREFER_MODULES", preferModules
			environmentProperty "ALWAYS_TRANSPILE", alwaysTranspile
			environmentProperty "USE_SWC", useSWC
			environmentProperty "REACT_PRAGMA", reactPragma
			environmentProperty "HMR", hmr
			environmentProperty "SOURCE_MAP", sourceMap
			environmentProperty "CONTINUOUS", continuous
		}
	}

	@Override
	protected void exec()
	{
//		applyEnvironmentProperties()

		environment "NODE_PATH", jstk.toolsDirectory.dir("node_modules").get().asFile.absolutePath

		environment 'BABEL_CACHE_DIR', new File(project.buildDir, ".babel").absolutePath
		environment 'OUTPUT_DIR', outputDirectory.asFile.get().absolutePath
		environment 'MANIFEST_DIR', manifestDirectory.asFile.get().absolutePath
		environment 'RECORDS_DIR', recordsDirectory.asFile.get().absolutePath

		environment "BABEL_CONFIG", babelConfig.asFile.get().absolutePath
		environment "LIBRARIES", libraries.get().join(",")
		environment "ENTRIES", entries.get().join(",")

		Map<String, String> presets = jstk.babel.get().presets.get()
		def preset = browsersListEnv.get()

		if(!presets.containsKey(preset))
		{
			throw new IllegalStateException("Preset ${preset} not found!")
		}

		environment "BROWSERSLIST", presets.get(preset)

		exec0()

		super.exec()
	}

	protected void exec0()
	{
		//
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
}
