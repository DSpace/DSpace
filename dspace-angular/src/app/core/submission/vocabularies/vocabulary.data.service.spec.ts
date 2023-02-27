/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { VocabularyDataService } from './vocabulary.data.service';
import { testFindAllDataImplementation } from '../../data/base/find-all-data.spec';

describe('VocabularyDataService', () => {
  function initTestService() {
    return new VocabularyDataService(null, null, null, null);
  }

  describe('composition', () => {
    const initService = () => new VocabularyDataService(null, null, null, null);
    testFindAllDataImplementation(initService);
  });
});
