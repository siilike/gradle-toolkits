
const fs = require("fs")
const { CachedInputFileSystem, ResolverFactory } = require("enhanced-resolve")

const webpackConfig = require(process.env.WEBPACK_CONFIG)
const JSTK_DEBUG = process.env.JSTK_DEBUG || 'false'

const nodeContext =
{
	environments: ["node+es3+es5+process+native"]
}

const resolverConfig =
{
	useSyncFileSystemCalls: true,
	fileSystem: new CachedInputFileSystem(fs, 4000),
	...webpackConfig.resolve,
	// conditionNames: [ 'node', 'import', 'require', 'default' ],
	conditionNames: [ 'node', 'require', 'default' ],
	mainFields: [ 'main' ],
}

if(JSTK_DEBUG === 'true')
{
	console.log('>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> JEST RESOLVER CONFIGURATION >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>')
	console.log(JSON.stringify(resolverConfig, null, 2))
	console.log('<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< JEST RESOLVER CONFIGURATION <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<')
}

const resolver = ResolverFactory.createResolver(resolverConfig)

module.exports = (request, options) =>
{
	return resolver.resolveSync(nodeContext, options.basedir, request)
}
