
const TOOLS_DIR = process.env.TOOLS_DIR || 'tools'

const { globals } = require(TOOLS_DIR + '/webpack/base.config.js')

module.exports =
{
	// TODO convert this into a preset instead
	// preset: undefined,

	cacheDirectory: "<rootDir>/build/jest_rs",

	collectCoverage: true,

	collectCoverageFrom:
	[
		"<rootDir>/js/**/*.js",
	],

	coverageDirectory: "<rootDir>/build/coverage",

	coverageProvider: "v8",

	globals: globals,

	moduleFileExtensions:
	[
		"js",
		"jsx",
	],

	extensionsToTreatAsEsm:
	[
		".jsx",
	],

	reporters:
	[
		'<rootDir>/tools/jest/reporter.js',
	],

	resolver: "<rootDir>/tools/jest/resolver.js",

	setupFilesAfterEnv:
	[
		'jest-chain',
		'<rootDir>/tests/setup.js',
	],

	testEnvironment: "jest-environment-jsdom",

	transform:
	{
		"\\.(js|jsx)$": [ 'babel-jest', { configFile: process.env.BABEL_CONFIG } ],
	},

	transformIgnorePatterns: [
		"<rootDir>/build/node_modules/",
		"<rootDir>/tools/node_modules/",
	],

	verbose: false,
}
