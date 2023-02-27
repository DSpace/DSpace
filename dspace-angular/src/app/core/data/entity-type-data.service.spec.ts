/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { testFindAllDataImplementation } from './base/find-all-data.spec';
import { testSearchDataImplementation } from './base/search-data.spec';
import { EntityTypeDataService } from './entity-type-data.service';

describe('EntityTypeDataService', () => {
  describe('composition', () => {
    const initService = () => new EntityTypeDataService(null, null, null, null, null);
    testFindAllDataImplementation(initService);
    testSearchDataImplementation(initService);
  });
});
