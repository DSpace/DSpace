/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { testFindAllDataImplementation } from '../base/find-all-data.spec';
import { ScriptDataService } from './script-data.service';

describe('ScriptDataService', () => {
  describe('composition', () => {
    const initService = () => new ScriptDataService(null, null, null, null);
    testFindAllDataImplementation(initService);
  });
});
