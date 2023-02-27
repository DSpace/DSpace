import { existsSync, writeFileSync } from 'fs';
import { join } from 'path';

import { AppConfig } from '../src/config/app-config.interface';
import { buildAppConfig } from '../src/config/config.server';

/**
 * Script to set baseHref as `ui.nameSpace` for development mode. Adds `baseHref` to angular.json build options.
 * 
 * Usage (see package.json):
 * 
 * yarn base-href
 */

const appConfig: AppConfig = buildAppConfig();

const angularJsonPath = join(process.cwd(), 'angular.json');

if (!existsSync(angularJsonPath)) {
  console.error(`Error:\n${angularJsonPath} does not exist\n`);
  process.exit(1);
}

try {
  const angularJson = require(angularJsonPath);

  const baseHref = `${appConfig.ui.nameSpace}${appConfig.ui.nameSpace.endsWith('/') ? '' : '/'}`;

  console.log(`Setting baseHref to ${baseHref} in angular.json`);

  angularJson.projects['dspace-angular'].architect.build.options.baseHref = baseHref;

  writeFileSync(angularJsonPath, JSON.stringify(angularJson, null, 2) + '\n');
} catch (e) {
  console.error(e);
}
