
const TOOLS_DIR = process.env.TOOLS_DIR || 'tools'

const { DefaultReporter } = require(TOOLS_DIR + '/node_modules/@jest/reporters/')

class Reporter extends DefaultReporter
{
	constructor()
	{
		super(...arguments)
	}

	printTestFileHeader(_testPath, config, result)
	{
		const console = result.console

		if(result.numFailingTests === 0 && !result.testExecError)
		{
			result.console = null
		}

		super.printTestFileHeader(...arguments)

		result.console = console
	}
}

module.exports = Reporter
