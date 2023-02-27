/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { testFindAllDataImplementation } from '../base/find-all-data.spec';
import { ProcessDataService } from './process-data.service';
import { testDeleteDataImplementation } from '../base/delete-data.spec';

describe('ProcessDataService', () => {
  describe('composition', () => {
    const initService = () => new ProcessDataService(null, null, null, null, null, null);
    testFindAllDataImplementation(initService);
    testDeleteDataImplementation(initService);
  });
});
