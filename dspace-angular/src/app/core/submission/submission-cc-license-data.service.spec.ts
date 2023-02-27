/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { SubmissionCcLicenseDataService } from './submission-cc-license-data.service';
import { testFindAllDataImplementation } from '../data/base/find-all-data.spec';

describe('SubmissionCcLicenseDataService', () => {

  describe('composition', () => {
    const initService = () => new SubmissionCcLicenseDataService(null, null, null, null);
    testFindAllDataImplementation(initService);
  });
});
