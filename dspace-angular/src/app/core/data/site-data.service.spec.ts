import { cold, getTestScheduler } from 'jasmine-marbles';
import { SiteDataService } from './site-data.service';
import { RequestService } from './request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { Site } from '../shared/site.model';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { TestScheduler } from 'rxjs/testing';
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { FindListOptions } from './find-list-options.model';
import { testFindAllDataImplementation } from './base/find-all-data.spec';

describe('SiteDataService', () => {
  let scheduler: TestScheduler;
  let service: SiteDataService;
  let halService: HALEndpointService;
  let requestService: RequestService;
  let rdbService: RemoteDataBuildService;
  let objectCache: ObjectCacheService;

  const testObject = Object.assign(new Site(), {
    uuid: '9b4f22f4-164a-49db-8817-3316b6ee5746',
  });

  const requestUUID = '34cfed7c-f597-49ef-9cbe-ea351f0023c2';
  const options = Object.assign(new FindListOptions(), {});

  const siteLink = 'https://rest.api/rest/api/config/sites';

  beforeEach(() => {
    scheduler = getTestScheduler();

    halService = jasmine.createSpyObj('halService', {
      getEndpoint: cold('a', { a: siteLink })
    });
    requestService = jasmine.createSpyObj('requestService', {
      generateRequestId: requestUUID,
      send: true,
    });
    rdbService = jasmine.createSpyObj('rdbService', {
      buildList: cold('a', {
        a: createSuccessfulRemoteDataObject(createPaginatedList([testObject]))
      })
    });

    objectCache = {} as ObjectCacheService;

    service = new SiteDataService(
      requestService,
      rdbService,
      objectCache,
      halService,
    );
  });

  describe('composition', () => {
    const initService = () => new SiteDataService(null, null, null, null);

    testFindAllDataImplementation(initService);
  });

  describe('getBrowseEndpoint', () => {
    it('should return the Static Page endpoint', () => {

      const result = service.getBrowseEndpoint(options);
      const expected = cold('b', { b: siteLink });

      expect(result).toBeObservable(expected);
    });
  });

  describe('find', () => {
    it('should return the Site object', () => {

      spyOn(service, 'findAll').and.returnValue(cold('a', {
        a: createSuccessfulRemoteDataObject(createPaginatedList([testObject]))
      }));

      const expected = cold('(b|)', { b: testObject });
      const result = service.find();

      expect(result).toBeObservable(expected);
    });
  });
});
