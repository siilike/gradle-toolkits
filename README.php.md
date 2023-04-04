
# Gradle PHP Toolkit

This is a plugin for PHP development using Gradle as the build tool based on:

  * composer
  * phpunit
  * phpstan
  * etc

It supports continuous building, patching, Sentry, packaging (deb, rpm, zip), and downloading required external software (including composer).

There are three plugins:

  * PhpToolkitBasePlugin -- only the DSL
  * PhpToolkitToolsPlugin -- all tasks for installing build tools and dependencies
  * PhpToolkitPlugin -- everything, creates all relevant tasks

## Usage

Usage in `build.gradle`:

```
plugins {
	id "ee.keel.gradle.php.PhpToolkitPlugin" version "3.0"
}

phpToolkit {
	// see examples/php/build.gradle
}
```

## Dependencies

  * Gradle that is supported by the gradle-ospackage-plugin, tested with Gradle 8
  * `wget` for downloading `composer`
  * the relevant `php` version (not necessarily with all production extensions installed)

## Configuration

`hjson` is used to define the `composer.json` file: the `composer.hjson` file is automatically converted into `build/src/composer.json`.

## Directory structure

  * `src` -- PHP source files
  * `tests` -- tests
  * `patches` -- patches to apply (mostly useful for patching `vendor`)
  * `tools` -- `composer`, `phpunit`, build tools -- should be added to `.gitignore`

# DSL

See [`src/ee/keel/gradle/php/dsl/`](src/ee/keel/gradle/php/dsl/) for all possible options.

## php
```
php {
	path = "/usr/bin/php8.2"
}
```

## composer

```
composer {
	version = "local" // default
	version = "latest-stable" // download latest version
}
```

## Build tools

```
packages {
	version "phpunit", "^10"
}
```

See defaults in [`src/ee/keel/gradle/php/dsl/PhpToolkitExtension.groovy`](src/ee/keel/gradle/php/dsl/PhpToolkitExtension.groovy).

After a successful build you should run the `saveBuildToolsLocks` to copy `tools/composer.lock` as `composer-tools.lock` into the root directory for predictable builds and also add it to version control.

You can use the `tools/venv` to load an environment where `tools/bin` is in `$PATH`, so `php`, `composer`, `phpunit`, and other tools defined in the project are used by default:

```
/gradle-toolkits/examples/php$ tools/venv
/gradle-toolkits/examples/php$ (venv) whereis phpunit
phpunit: /gradle-toolkits/examples/php/tools/bin/phpunit
/gradle-toolkits/examples/php$ (venv) whereis composer
composer: /gradle-toolkits/examples/php/tools/bin/composer
```

## Patches

```
patches {
	"disabled-patch" {
		enabled = false
	}
	"some-patch" {}
}
```

Use `diff -Naur original.php patched.php` to generate the patch and save it as `patches/some-patch.patch`.

## Distribution

```
distribution {
	repo {
		// where the files are located after installing the package
		path = "/data1/apps/${project.name}/v${project.version}/"
	}
}
```

# Tasks and options

See `gradle tasks --all` for all available tasks.

The most important tasks are:

  * `assemble` -- assembles the project, copies `src/*` into `build/src/`, runs `composer install`, applies patches, etc
  * `test` -- runs tests
  * `build` -- `assemble` + `test`
  * `distDeb` -- builds the `deb` package

Available project options:

  * version -- the version of the application to be built
  * dev -- enables development mode

For example:

```
gradle -Pdev build
gradle -Pversion=5.master distDeb
```

## Handling versions

Generally it is useful to use build numbers as versions. For multibranch projects the branch name can also be included.

## Continuous build

The plugin supports continuous building.
