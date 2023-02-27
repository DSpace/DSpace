/* eslint-disable max-classes-per-file */
/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { ResourceType } from '../../shared/resource-type';
import { BaseDataService } from './base-data.service';
import { HALDataService } from './hal-data-service.interface';
import { dataService, getDataServiceFor } from './data-service.decorator';
import { v4 as uuidv4 } from 'uuid';

class TestService extends BaseDataService<any> {
}

class AnotherTestService implements HALDataService<any> {
  public findListByHref(href$, findListOptions, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow): any {
    return undefined;
  }

  public findByHref(href$, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow): any {
    return undefined;
  }
}

let testType;

describe('@dataService/getDataServiceFor', () => {
  beforeEach(() => {
    testType = new ResourceType(`testType-${uuidv4()}`);
  });

  it('should register a resourcetype for a dataservice', () => {
    dataService(testType)(TestService);
    expect(getDataServiceFor(testType)).toBe(TestService);
  });

  describe(`when the resource type isn't specified`, () => {
    it(`should throw an error`, () => {
      expect(() => {
        dataService(undefined)(TestService);
      }).toThrow();
    });
  });

  describe(`when there already is a registered dataservice for a resourcetype`, () => {
    it(`should throw an error`, () => {
      dataService(testType)(TestService);
      expect(() => {
        dataService(testType)(AnotherTestService);
      }).toThrow();
    });
  });
});
