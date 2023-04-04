
const NODE_ENV = process.env.NODE_ENV || 'production'
const TOOLS_DIR = process.env.TOOLS_DIR || './tools'
const BROWSERSLIST = process.env.BROWSERSLIST || '> 0.5%, not IE11'
const REACT_PRAGMA = process.env.REACT_PRAGMA
const JSTK_DEBUG = process.env.JSTK_DEBUG || 'false'
const IS_TEST = process.env.IS_TEST || 'false'
const HMR = process.env.HMR || 'false'

module.exports = function(api)
{
	api.cache(true)

	var config =
	{
		"sourceType": "unambiguous",
		"presets":
		[
			[
				"@babel/preset-env",
				{
					"useBuiltIns": "usage",
					"corejs": 3,
					"targets": BROWSERSLIST,
					"modules": IS_TEST === 'true' ? 'commonjs' : false,
					"shippedProposals": true,
					"bugfixes": true,
				}
			],
			[
				"@babel/preset-react", REACT_PRAGMA ?
				{
					"pragma": REACT_PRAGMA,
				} : {
					"runtime": "automatic",
				}
			],
		],
		"plugins":
		[
			[ "@babel/plugin-proposal-decorators", { "legacy": true } ],
			[ "@loadable/babel-plugin", {} ],

/*
			[
				"@babel/plugin-transform-runtime",
				{
					"corejs": 3,
					"useESModules": true,
				}
			],
*/

			[ TOOLS_DIR + "/babel/plugin-trace.js", {} ]
		]
	}

	if(NODE_ENV === 'development' && HMR === 'true')
	{
		config.plugins.push("react-refresh/babel");
	}

	if(JSTK_DEBUG === 'true')
	{
		console.log('>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BABEL CONFIGURATION >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>')
		console.log(JSON.stringify(config, null, 2))
		console.log('<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< BABEL CONFIGURATION <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<')
	}

	return config
}
