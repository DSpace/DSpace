[![Build Status](https://github.com/DSpace/dspace-angular/workflows/Build/badge.svg?branch=main)](https://github.com/DSpace/dspace-angular/actions?query=workflow%3ABuild) [![Coverage Status](https://codecov.io/gh/DSpace/dspace-angular/branch/main/graph/badge.svg)](https://codecov.io/gh/DSpace/dspace-angular) [![Universal Angular](https://img.shields.io/badge/universal-angular2-brightgreen.svg?style=flat)](https://github.com/angular/universal)

dspace-angular
==============

> The DSpace User Interface built on [Angular](https://angular.io/), written in [TypeScript](https://www.typescriptlang.org/) and using [Angular Universal](https://angular.io/guide/universal).

Overview
--------

DSpace open source software is a turnkey repository application used by more than
2,000 organizations and institutions worldwide to provide durable access to digital resources.
For more information, visit http://www.dspace.org/

DSpace consists of both a Java-based backend and an Angular-based frontend.

* Backend (https://github.com/DSpace/DSpace/) provides a REST API, along with other machine-based interfaces (e.g. OAI-PMH, SWORD, etc)
    * The REST Contract is at https://github.com/DSpace/RestContract
* Frontend (this codebase) is the User Interface built on the REST API

Downloads
---------

* Backend (REST API): https://github.com/DSpace/DSpace/releases
* Frontend (User Interface): https://github.com/DSpace/dspace-angular/releases


## Documentation / Installation

Documentation for each release may be viewed online or downloaded via our [Documentation Wiki](https://wiki.lyrasis.org/display/DSDOC/).

The latest DSpace Installation instructions are available at:
https://wiki.lyrasis.org/display/DSDOC7x/Installing+DSpace

Quick start
-----------

**Ensure you're running [Node](https://nodejs.org) `v16.x` or `v18.x`, [npm](https://www.npmjs.com/) >= `v5.x` and [yarn](https://yarnpkg.com) == `v1.x`**

```bash
# clone the repo
git clone https://github.com/DSpace/dspace-angular.git

# change directory to our repo
cd dspace-angular

# install the local dependencies
yarn install

# start the server
yarn start
```

Then go to [http://localhost:4000](http://localhost:4000) in your browser

Not sure where to start? watch the training videos linked in the [Introduction to the technology](#introduction-to-the-technology) section below.

Table of Contents
-----------------

-	[Introduction to the technology](#introduction-to-the-technology)
-	[Requirements](#requirements)
-	[Installing](#installing)
    - [Configuring](#configuring)
-	[Running the app](#running-the-app)
    - [Running in production mode](#running-in-production-mode)
    - [Deploy](#deploy)
    - [Running the application with Docker](#running-the-application-with-docker)
-	[Cleaning](#cleaning)
-	[Testing](#testing)
    - [Test a Pull Request](#test-a-pull-request)
	- [Unit Tests](#unit-tests)
	- [E2E Tests](#e2e-tests)
		- [Writing E2E Tests](#writing-e2e-tests)
-	[Documentation](#documentation)
-	[Other commands](#other-commands)
-	[Recommended Editors/IDEs](#recommended-editorsides)
-	[Collaborating](#collaborating)
-	[File Structure](#file-structure)
-	[Managing Dependencies (via yarn)](#managing-dependencies-via-yarn)
-	[Frequently asked questions](#frequently-asked-questions)
-	[License](#license)

Introduction to the technology
------------------------------

You can find more information on the technologies used in this project (Angular.io, Angular CLI, Typescript, Angular Universal, RxJS, etc) on the [LYRASIS wiki](https://wiki.lyrasis.org/display/DSPACE/DSpace+7+UI+Technology+Stack)

Requirements
------------

-	[Node.js](https://nodejs.org) and [yarn](https://yarnpkg.com)
-	Ensure you're running node `v16.x` or `v18.x` and yarn == `v1.x`

If you have [`nvm`](https://github.com/creationix/nvm#install-script) or [`nvm-windows`](https://github.com/coreybutler/nvm-windows) installed, which is highly recommended, you can run `nvm install --lts && nvm use` to install and start using the latest Node LTS.

Installing
----------

-	`yarn install` to install the local dependencies

### Configuring

Default runtime configuration file is located in `config/` folder. These configurations can be changed without rebuilding the distribution.

To override the default configuration values, create local files that override the parameters you need to change. You can use `config.example.yml` as a starting point.

-	Create a new `config.(dev or development).yml` file in `config/` for a `development` environment;
-	Create a new `config.(prod or production).yml` file in `config/` for a `production` environment;

The settings can also be overwritten using an environment file or environment variables.

This file should be called `.env` and be placed in the project root.

The following non-convention settings:

```bash
DSPACE_HOST # The host name of the angular application
DSPACE_PORT # The port number of the angular application
DSPACE_NAMESPACE # The namespace of the angular application
DSPACE_SSL # Whether the angular application uses SSL [true/false]
```

All other settings can be set using the following convention for naming the environment variables:

1. replace all `.` with `_`
2. convert all characters to upper case
3. prefix with `DSPACE_`

e.g.

```bash
# The host name of the REST application
rest.host => DSPACE_REST_HOST

# The port number of the REST application
rest.port => DSPACE_REST_PORT

# The namespace of the REST application
rest.nameSpace => DSPACE_REST_NAMESPACE

# Whether the angular REST uses SSL [true/false]
rest.ssl => DSPACE_REST_SSL

cache.msToLive.default => DSPACE_CACHE_MSTOLIVE_DEFAULT
auth.ui.timeUntilIdle => DSPACE_AUTH_UI_TIMEUNTILIDLE
```

The equavelant to the non-conventional legacy settings:

```bash
DSPACE_UI_HOST => DSPACE_HOST
DSPACE_UI_PORT => DSPACE_PORT
DSPACE_UI_NAMESPACE => DSPACE_NAMESPACE
DSPACE_UI_SSL => DSPACE_SSL
```

The same settings can also be overwritten by setting system environment variables instead, E.g.:
```bash
export DSPACE_HOST=api7.dspace.org
export DSPACE_UI_PORT=4200
```

The priority works as follows: **environment variable** overrides **variable in `.env` file** overrides external config set by `DSPACE_APP_CONFIG_PATH` overrides **`config.(prod or dev).yml`**

These configuration sources are collected **at run time**, and written to `dist/browser/assets/config.json` for production and `src/app/assets/config.json` for development.

The configuration file can be externalized by using environment variable `DSPACE_APP_CONFIG_PATH`.

#### Buildtime Configuring

Buildtime configuration must defined before build in order to include in transpiled JavaScript. This is primarily for the server. These settings can be found under `src/environment/` folder.

To override the default configuration values for development, create local file that override the build time parameters you need to change.

-	Create a new `environment.(dev or development).ts` file in `src/environment/` for a `development` environment;

If needing to update default configurations values for production, update local file that override the build time parameters you need to change.

-	Update `environment.production.ts` file in `src/environment/` for a `production` environment;

The environment object is provided for use as import in code and is extended with the runtime configuration on bootstrap of the application.

> Take caution moving runtime configs into the buildtime configuration. They will be overwritten by what is defined in the runtime config on bootstrap.

#### Using environment variables in code
To use environment variables in a UI component, use:

```typescript
import { AppConfig, APP_CONFIG } from 'src/config/app-config.interface';
...
constructor(@Inject(APP_CONFIG) private appConfig: AppConfig) {}
...
```

or

```typescript
import { environment } from '../environment.ts';
```

Running the app
---------------

After you have installed all dependencies you can now run the app. Run `yarn run start:dev` to start a local server which will watch for changes, rebuild the code, and reload the server for you. You can visit it at `http://localhost:4000`.

### Running in production mode

When building for production we're using Ahead of Time (AoT) compilation. With AoT, the browser downloads a pre-compiled version of the application, so it can render the application immediately, without waiting to compile the app first. The compiler is roughly half the size of Angular itself, so omitting it dramatically reduces the application payload.

To build the app for production and start the server (in one command) run:

```bash
yarn start
```
This will run the application in an instance of the Express server, which is included.

If you only want to build for production, without starting, run:

```bash
yarn run build:prod
```
This will build the application and put the result in the `dist` folder.  You can copy this folder to wherever you need it for your application server.  If you will be using the built-in Express server, you'll also need a copy of the `node_modules` folder tucked inside your copy of `dist`.

After building the app for production, it can be started by running:
```bash
yarn run serve:ssr
```

### Running the application with Docker
NOTE: At this time, we do not have production-ready Docker images for DSpace.
That said, we do have quick-start Docker Compose scripts for development or testing purposes.

See [Docker Runtime Options](docker/README.md)


Cleaning
--------

```bash
# clean everything, including node_modules. You'll need to run yarn install again afterwards.
yarn run clean

# clean files generated by the production build (.ngfactory files, css files, etc)
yarn run clean:prod

# cleans the distribution directory
yarn run clean:dist
```


Testing
-------

### Test a Pull Request

If you would like to contribute by testing a Pull Request (PR), here's how to do so. Keep in mind, you **do not need to have a DSpace backend / REST API installed locally to test a PR**. By default, the dspace-angular project points at our demo REST API

1. Pull down the branch that the Pull Request was built from.  Easy instructions for doing so can be found on the Pull Request itself.
	* Next to the "Merge" button, you'll see a link that says "command line instructions".
	* Click it, and follow "Step 1" of those instructions to checkout the pull down the PR branch.
2. `yarn run clean`  (This resets your local dependencies to ensure you are up-to-date with this PR)
3. `yarn install` (Updates your local dependencies to those in the PR)
4. `yarn start` (Rebuilds the project, and deploys to localhost:4000, by default)
5. At this point, the code from the PR will be deployed to http://localhost:4000.  Test it out, and ensure that it does what is described in the PR (or fixes the bug described in the ticket linked to the PR).

Once you have tested the Pull Request, please add a comment and/or approval to the PR to let us know whether you found it to be successful (or not). Thanks!


### Unit Tests

Unit tests use the [Jasmine test framework](https://jasmine.github.io/), and are run via [Karma](https://karma-runner.github.io/).

You can find the Karma configuration file at the same level of this README file:`./karma.conf.js` If you are going to use a remote test environment you need to edit the `./karma.conf.js`. Follow the instructions you will find inside it. To executing tests whenever any file changes you can modify the 'autoWatch' option to 'true' and 'singleRun' option to 'false'. A coverage report is also available at: http://localhost:9876/ after you run: `yarn run coverage`.

The default browser is Google Chrome.

Place your tests in the same location of the application source code files that they test, e.g. ending with `*.component.spec.ts`

and run: `yarn test`

If you run into odd test errors, see the Angular guide to debugging tests: https://angular.io/guide/test-debugging

### E2E Tests

E2E tests (aka integration tests) use [Cypress.io](https://www.cypress.io/). Configuration for cypress can be found in the `cypress.json` file in the root directory.

The test files can be found in the `./cypress/integration/` folder.

Before you can run e2e tests, two things are REQUIRED:
1. You MUST be running the DSpace backend (i.e. REST API) locally. The e2e tests will *NOT* succeed if run against our demo REST API (https://api7.dspace.org/server/), as that server is uncontrolled and may have content added/removed at any time.
    * After starting up your backend on localhost, make sure either your `config.prod.yml` or `config.dev.yml` has its `rest` settings defined to use that localhost backend.
	* If you'd prefer, you may instead use environment variables as described at [Configuring](#configuring). For example:
       ```
       DSPACE_REST_SSL = false
       DSPACE_REST_HOST = localhost
       DSPACE_REST_PORT = 8080
       ```
2. Your backend MUST include our [Entities Test Data set](https://github.com/DSpace-Labs/AIP-Files/releases/tag/demo-entities-data). Some tests run against a specific Community/Collection/Item UUID. These UUIDs are all valid for our Entities Test Data set.
	 * (Recommended) The Entities Test Data set may be installed easily via Docker, see https://github.com/DSpace/DSpace/tree/main/dspace/src/main/docker-compose#ingest-option-2-ingest-entities-test-data
	 * Alternatively, the Entities Test Data set may be installed via a simple SQL import (e. g. `psql -U dspace < dspace7-entities-data.sql`). See instructions in link above.

After performing the above setup, you can run the e2e tests using
```
ng e2e
````
NOTE: By default these tests will run against the REST API backend configured via environment variables or in `config.prod.yml`. If you'd rather it use `config.dev.yml`, just set the NODE_ENV environment variable like this:
```
NODE_ENV=development ng e2e
```

The `ng e2e` command will start Cypress and allow you to select the browser you wish to use, as well as whether you wish to run all tests or an individual test file.  Once you click run on test(s), this opens the [Cypress Test Runner](https://docs.cypress.io/guides/core-concepts/test-runner) to run your test(s) and show you the results.

#### Writing E2E Tests

All E2E tests must be created under the `./cypress/integration/` folder, and must end in `.spec.ts`. Subfolders are allowed.

* The easiest way to start creating new tests is by running `ng e2e`. This builds the app and brings up Cypress.
* From here, if you are editing an existing test file, you can either open it in your IDE or run it first to see what it already does.
* To create a new test file, click `+ New Spec File`.  Choose a meaningful name ending in `spec.ts` (Please make sure it ends in `.ts` so that it's a Typescript file, and not plain Javascript)
* Start small. Add a basic `describe` and `it` which just [cy.visit](https://docs.cypress.io/api/commands/visit) the page you want to test. For example:
   ```
   describe('Community/Collection Browse Page', () => {
    it('should exist as a page', () => {
        cy.visit('/community-list');
    });
   });
   ```
* Run your test file from the Cypress window. This starts the [Cypress Test Runner](https://docs.cypress.io/guides/core-concepts/test-runner) in a new browser window.
* In the [Cypress Test Runner](https://docs.cypress.io/guides/core-concepts/test-runner), you'll Cypress automatically visit the page.  This first test will succeed, as all you are doing is making sure the _page exists_.
* From here, you can use the [Selector Playground](https://docs.cypress.io/guides/core-concepts/test-runner#Selector-Playground) in the Cypress Test Runner window to determine how to tell Cypress to interact with a specific HTML element on that page.
    * Most commands start by telling Cypress to [get()](https://docs.cypress.io/api/commands/get) a specific element, using a CSS or jQuery style selector
      * It's generally best not to rely on attributes like `class` and `id` in tests, as those are likely to change later on. Instead, you can add a `data-test` attribute to makes it clear that it's required for a test. 
    * Cypress can then do actions like [click()](https://docs.cypress.io/api/commands/click) an element, or [type()](https://docs.cypress.io/api/commands/type) text in an input field, etc.
      * When running with server-side rendering enabled, the client first receives HTML without the JS; only once the page is rendered client-side do some elements (e.g. a button that toggles a Bootstrap dropdown) become fully interactive. This can trip up Cypress in some cases as it may try to `click` or `type` in an element that's not fully loaded yet, causing tests to fail. 
      * To work around this issue, define the attributes you use for Cypress selectors as `[attr.data-test]="'button' | ngBrowserOnly"`. This will only show the attribute in CSR HTML, forcing Cypress to wait until CSR is complete before interacting with the element.
    * Cypress can also validate that something occurs, using [should()](https://docs.cypress.io/api/commands/should) assertions.
* Any time you save your test file, the Cypress Test Runner will reload & rerun it. This allows you can see your results quickly as you write the tests & correct any broken tests rapidly.
* Cypress also has a great guide on [writing your first test](https://on.cypress.io/writing-first-test) with much more info. Keep in mind, while the examples in the Cypress docs often involve Javascript files (.js), the same examples will work in our Typescript (.ts) e2e tests.

_Hint: Creating e2e tests is easiest in an IDE (like Visual Studio), as it can help prompt/autocomplete your Cypress commands._

More Information: [docs.cypress.io](https://docs.cypress.io/) has great guides & documentation helping you learn more about writing/debugging e2e tests in Cypress.

### Learning how to build tests

See our [DSpace Code Testing Guide](https://wiki.lyrasis.org/display/DSPACE/Code+Testing+Guide) for more hints/tips.

Documentation
--------------

Official DSpace documentation is available in the DSpace wiki at https://wiki.lyrasis.org/display/DSDOC7x/

Some UI specific configuration documentation is also found in the [`./docs`](docs) folder of this codebase.

### Building code documentation

To build the code documentation we use [TYPEDOC](http://typedoc.org). TYPEDOC is a documentation generator for TypeScript projects. It extracts information from properly formatted comments that can be written within the code files. Follow the instructions [here](http://typedoc.org/guides/doccomments/) to know how to make those comments.

Run:`yarn run docs` to produce the documentation that will be available in the 'doc' folder.

Other commands
--------------

There are many more commands in the `scripts` section of `package.json`. Most of these are executed by one of the commands mentioned above.

A command with a name that starts with `pre` or `post` will be executed automatically before or after the script with the matching name. e.g. if you type `yarn run start` the `prestart` script will run first, then the `start` script will trigger.

Recommended Editors/IDEs
------------------------

To get the most out of TypeScript, you'll need a TypeScript-aware editor. We've had good experiences using these editors:

-	Free
	-	[Visual Studio Code](https://code.visualstudio.com/)
		-	[Debugger for Chrome](https://marketplace.visualstudio.com/items?itemName=msjsdiag.debugger-for-chrome)
-	Paid
	-	[Webstorm](https://www.jetbrains.com/webstorm/download/) or [IntelliJ IDEA Ultimate](https://www.jetbrains.com/idea/)
	-	[Sublime Text](http://www.sublimetext.com/3)
		-	[Typescript-Sublime-Plugin](https://github.com/Microsoft/Typescript-Sublime-plugin#installation)

Contributing
-------------

See [Contributing documentation](CONTRIBUTING.md)

File Structure
--------------

```
dspace-angular
├── config                                              *
│   └── config.yml                                      * Default app config
├── cypress                                             * Folder for Cypress (https://cypress.io/) / e2e tests
│   ├── downloads                                       *
│   ├── fixtures                                        * Folder for e2e/integration test files
│   ├── integration                                     * Folder for any fixtures needed by e2e tests
│   ├── plugins                                         * Folder for Cypress plugins (if any)
│   ├── support                                         * Folder for global e2e test actions/commands (run for all tests)
│   └── tsconfig.json                                   * TypeScript configuration file for e2e tests
├── docker                                              * See docker/README.md for details
│   ├── cli.assetstore.yml                              *
│   ├── cli.ingest.yml                                  *
│   ├── cli.yml                                         *
│   ├── db.entities.yml                                 *
│   ├── docker-compose-ci.yml                           *
│   ├── docker-compose-rest.yml                         *
│   ├── docker-compose.yml                              *
│   └── README.md                                       *
├── docs                                                * Folder for documentation
│   └── Configuration.md                                * Configuration documentation
├── scripts                                             *
│   ├── merge-i18n-files.ts                             *
│   ├── serve.ts                                        *
│   ├── sync-i18n-files.ts                              *
│   ├── test-rest.ts                                    *
│   └── webpack.js                                      *
├── src                                                 * The source of the application
│   ├── app                                             * The source code of the application, subdivided by module/page.
│   ├── assets                                          * Folder for static resources
│   │   ├── fonts                                       * Folder for fonts
│   │   ├── i18n                                        * Folder for i18n translations
│   │   └── images                                      * Folder for images
│   ├── backend                                         * Folder containing a mock of the REST API, hosted by the express server
│   ├── config                                          *
│   ├── environments                                    *
│   │   ├── environment.production.ts                   * Production configuration files
│   │   ├── environment.test.ts                         * Test configuration files
│   │   └── environment.ts                              * Default (development) configuration files
│   ├── mirador-viewer                                  *
│   ├── modules                                         *
│   ├── ngx-translate-loaders                           *
│   ├── styles                                          * Folder containing global styles
│   ├── themes                                          * Folder containing available themes
│   │   ├── custom                                      * Template folder for creating a custom theme
│   │   └── dspace                                      * Default 'dspace' theme
│   ├── index.csr.html                                  * The index file for client side rendering fallback
│   ├── index.html                                      * The index file
│   ├── main.browser.ts                                 * The bootstrap file for the client
│   ├── main.server.ts                                  * The express (http://expressjs.com/) config and bootstrap file for the server
│   ├── polyfills.ts                                    *
│   ├── robots.txt                                      * The robots.txt file
│   ├── test.ts                                         *
│   └── typings.d.ts                                    *
├── webpack                                             *
│   ├── helpers.ts                                      * Webpack helpers
│   ├── webpack.browser.ts                              * Webpack (https://webpack.github.io/) config for browser build
│   ├── webpack.common.ts                               * Webpack (https://webpack.github.io/) common build config
│   ├── webpack.mirador.config.ts                       * Webpack (https://webpack.github.io/) config for mirador config build
│   ├── webpack.prod.ts                                 * Webpack (https://webpack.github.io/) config for prod build
│   └── webpack.test.ts                                 * Webpack (https://webpack.github.io/) config for test build
├── angular.json                                        * Angular CLI (https://angular.io/cli) configuration
├── cypress.json                                        * Cypress Test (https://www.cypress.io/) configuration
├── Dockerfile                                          *
├── karma.conf.js                                       * Karma configuration file for Unit Test
├── LICENSE                                             *
├── LICENSES_THIRD_PARTY                                *
├── nodemon.json                                        * Nodemon (https://nodemon.io/) configuration
├── package.json                                        * This file describes the npm package for this project, its dependencies, scripts, etc.
├── postcss.config.js                                   * PostCSS (http://postcss.org/) configuration
├── README.md                                           * This document
├── SECURITY.md                                         *
├── server.ts                                           * Angular Universal Node.js Express server
├── tsconfig.app.json                                   * TypeScript config for browser (app)
├── tsconfig.json                                       * TypeScript common config
├── tsconfig.server.json                                * TypeScript config for server
├── tsconfig.spec.json                                  * TypeScript config for tests
├── tsconfig.ts-node.json                               * TypeScript config for using ts-node directly
├── tslint.json                                         * TSLint (https://palantir.github.io/tslint/) configuration
├── typedoc.json                                        * TYPEDOC configuration
└── yarn.lock                                           * Yarn lockfile (https://yarnpkg.com/en/docs/yarn-lock)
```

Managing Dependencies (via yarn)
-------------

This project makes use of [`yarn`](https://yarnpkg.com/en/) to ensure that the exact same dependency versions are used every time you install it.

* `yarn` creates a [`yarn.lock`](https://yarnpkg.com/en/docs/yarn-lock) to track those versions. That file is updated automatically by whenever dependencies are added/updated/removed via yarn.
* **Adding new dependencies**: To install/add a new dependency (third party library), use [`yarn add`](https://yarnpkg.com/en/docs/cli/add). For example: `yarn add some-lib`.
    * If you are adding a new build tool dependency (to `devDependencies`), use `yarn add some-lib --dev`
* **Upgrading existing dependencies**: To upgrade existing dependencies, you can use [`yarn upgrade`](https://yarnpkg.com/en/docs/cli/upgrade).  For example: `yarn upgrade some-lib` or `yarn upgrade some-lib@version`
* **Removing dependencies**: If a dependency is no longer needed, or replaced, use [`yarn remove`](https://yarnpkg.com/en/docs/cli/remove) to remove it.

As you can see above, using `yarn` commandline tools means that you should never need to modify the `package.json` manually. *We recommend always using `yarn` to keep dependencies updated / in sync.*

### Adding Typings for libraries

If the library does not include typings, you can install them using yarn:

```bash
yarn add d3
yarn add @types/d3 --dev
```

If the library doesn't have typings available at `@types/`, you can still use it by manually adding typings for it:

1.	In `src/typings.d.ts`, add the following code:

	```typescript
	  declare module 'typeless-package';
	```

2.	Then, in the component or file that uses the library, add the following code:

	```typescript
	  import * as typelessPackage from 'typeless-package';
	  typelessPackage.method();
	```

Done. Note: you might need or find useful to define more typings for the library that you're trying to use.

If you're importing a module that uses CommonJS you need to import as

```typescript
import * as _ from 'lodash';
```

Frequently asked questions
--------------------------

-	Why is my service, aka provider, is not injecting a parameter correctly?
	-	Please use `@Injectable()` for your service for typescript to correctly attach the metadata
-	Where do I write my tests?
	-	You can write your tests next to your component files. e.g. for `src/app/home/home.component.ts` call it `src/app/home/home.component.spec.ts`
-	How do I start the app when I get `EACCES` and `EADDRINUSE` errors?
	-	The `EADDRINUSE` error means the port `4000` is currently being used and `EACCES` is lack of permission to build files to `./dist/`
-	What are the naming conventions for Angular?
	-	See [the official angular style guide](https://angular.io/styleguide)
-	Why is the size of my app larger in development?
	-	The production build uses a whole host of techniques (ahead-of-time compilation, rollup to remove unreachable code, minification, etc.) to reduce the size, that aren't used during development in the intrest of build speed.
-	node-pre-gyp ERR in yarn install (Windows)
	-	install Python x86 version between 2.5 and 3.0 on windows. See [this issue](https://github.com/AngularClass/angular2-webpack-starter/issues/626)
-	How do I handle merge conflicts in yarn.lock?
	-	first check out the yarn.lock file from the branch you're merging in to yours: e.g. `git checkout --theirs yarn.lock`
	-	now run `yarn install` again. Yarn will create a new lockfile that contains both sets of changes.
	-	then run `git add yarn.lock` to stage the lockfile for commit
	-	and `git commit` to conclude the merge

Getting Help
------------

DSpace provides public mailing lists where you can post questions or raise topics for discussion.
We welcome everyone to participate in these lists:

* [dspace-community@googlegroups.com](https://groups.google.com/d/forum/dspace-community) : General discussion about DSpace platform, announcements, sharing of best practices
* [dspace-tech@googlegroups.com](https://groups.google.com/d/forum/dspace-tech) : Technical support mailing list. See also our guide for [How to troubleshoot an error](https://wiki.lyrasis.org/display/DSPACE/Troubleshoot+an+error).
* [dspace-devel@googlegroups.com](https://groups.google.com/d/forum/dspace-devel) : Developers / Development mailing list

Great Q&A is also available under the [DSpace tag on Stackoverflow](http://stackoverflow.com/questions/tagged/dspace)

Additional support options are at https://wiki.lyrasis.org/display/DSPACE/Support

DSpace also has an active service provider network. If you'd rather hire a service provider to
install, upgrade, customize or host DSpace, then we recommend getting in touch with one of our
[Registered Service Providers](http://www.dspace.org/service-providers).


Issue Tracker
-------------

DSpace uses GitHub to track issues:
* Backend (REST API) issues: https://github.com/DSpace/DSpace/issues
* Frontend (User Interface) issues: https://github.com/DSpace/dspace-angular/issues

License
-------
DSpace source code is freely available under a standard [BSD 3-Clause license](https://opensource.org/licenses/BSD-3-Clause).
The full license is available in the [LICENSE](LICENSE) file or online at http://www.dspace.org/license/

DSpace uses third-party libraries which may be distributed under different licenses. Those licenses are listed
in the [LICENSES_THIRD_PARTY](LICENSES_THIRD_PARTY) file.
