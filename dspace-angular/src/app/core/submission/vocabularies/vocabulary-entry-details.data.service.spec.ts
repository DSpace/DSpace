/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { testFindAllDataImplementation } from '../../data/base/find-all-data.spec';
import { VocabularyEntryDetailsDataService } from './vocabulary-entry-details.data.service';
import { testSearchDataImplementation } from '../../data/base/search-data.spec';

describe('VocabularyEntryDetailsDataService', () => {
  function initTestService() {
    return new VocabularyEntryDetailsDataService(null, null, null, null);
  }

  describe('composition', () => {
    const initService = () => new VocabularyEntryDetailsDataService(null, null, null, null);
    testFindAllDataImplementation(initService);
    testSearchDataImplementation(initService);
  });
});
