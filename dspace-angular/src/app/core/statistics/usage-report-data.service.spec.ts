/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { testSearchDataImplementation } from '../data/base/search-data.spec';
import { UsageReportDataService } from './usage-report-data.service';

describe('UsageReportDataService', () => {
  describe('composition', () => {
    const initService = () => new UsageReportDataService(null, null, null, null);
    testSearchDataImplementation(initService);
  });
});
