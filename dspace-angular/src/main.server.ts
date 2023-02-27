import 'core-js/es/reflect';
import 'zone.js';
import 'reflect-metadata';

import { enableProdMode } from '@angular/core';
/******************************************************************
 * Load `$localize` - not used for i18n in this project, we use ngx-translate.
 * It's used for localization of dates, numbers, currencies, etc.
 */
import '@angular/localize/init';

import { environment } from './environments/environment';

if (environment.production) {
  enableProdMode();
}

export { ServerAppModule } from './modules/app/server-app.module';
export { ngExpressEngine } from '@nguniversal/express-engine';
export { renderModule, renderModuleFactory } from '@angular/platform-server';
