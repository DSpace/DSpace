/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { FindAllData, FindAllDataImpl } from './find-all-data';
import { FindListOptions } from '../find-list-options.model';
import { followLink } from '../../../shared/utils/follow-link-config.model';
import { getMockRequestService } from '../../../shared/mocks/request.service.mock';
import { HALEndpointServiceStub } from '../../../shared/testing/hal-endpoint-service.stub';
import { getMockRemoteDataBuildService } from '../../../shared/mocks/remote-data-build.service.mock';
import { TestScheduler } from 'rxjs/testing';
import { RemoteData } from '../remote-data';
import { RequestEntryState } from '../request-entry-state.model';
import { SortDirection, SortOptions } from '../../cache/models/sort-options.model';
import { RequestParam } from '../../cache/models/request-param.model';

import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { Observable, of as observableOf } from 'rxjs';
import { EMBED_SEPARATOR } from './base-data.service';

/**
 * Tests whether calls to `FindAllData` methods are correctly patched through in a concrete data service that implements it
 */
export function testFindAllDataImplementation(serviceFactory: () => FindAllData<any>) {
  let service;

  describe('FindAllData implementation', () => {
    const OPTIONS = Object.assign(new FindListOptions(), { elementsPerPage: 10, currentPage: 3 });
    const FOLLOWLINKS = [
      followLink('test'),
      followLink('something'),
    ];

    beforeAll(() => {
      service = serviceFactory();
      (service as any).findAllData =  jasmine.createSpyObj('findAllData', {
        findAll: 'TEST findAll',
      });
    });

    it('should handle calls to findAll', () => {
      const out: any = service.findAll(OPTIONS, false, true, ...FOLLOWLINKS);

      expect((service as any).findAllData.findAll).toHaveBeenCalledWith(OPTIONS, false, true, ...FOLLOWLINKS);
      expect(out).toBe('TEST findAll');
    });
  });
}

const endpoint = 'https://rest.api/core';

class TestService extends FindAllDataImpl<any> {
  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
  ) {
    super(undefined, requestService, rdbService, objectCache, halService, undefined);
  }

  public getBrowseEndpoint(options: FindListOptions = {}, linkPath: string = this.linkPath): Observable<string> {
    return observableOf(endpoint);
  }
}

describe('FindAllDataImpl', () => {
  let service: TestService;
  let options: FindListOptions;
  let requestService;
  let halService;
  let rdbService;
  let objectCache;
  let selfLink;
  let linksToFollow;
  let testScheduler;
  let remoteDataMocks;

  function initTestService(): TestService {
    requestService = getMockRequestService();
    halService = new HALEndpointServiceStub('url') as any;
    rdbService = getMockRemoteDataBuildService();
    objectCache = {

      addPatch: () => {
        /* empty */
      },
      getObjectBySelfLink: () => {
        /* empty */
      },
      getByHref: () => {
        /* empty */
      },
    } as any;
    selfLink = 'https://rest.api/endpoint/1698f1d3-be98-4c51-9fd8-6bfedcbd59b7';
    linksToFollow = [
      followLink('a'),
      followLink('b'),
    ];

    testScheduler = new TestScheduler((actual, expected) => {
      // asserting the two objects are equal
      // e.g. using chai.
      expect(actual).toEqual(expected);
    });

    const timeStamp = new Date().getTime();
    const msToLive = 15 * 60 * 1000;
    const payload = { foo: 'bar' };
    const statusCodeSuccess = 200;
    const statusCodeError = 404;
    const errorMessage = 'not found';
    remoteDataMocks = {
      RequestPending: new RemoteData(undefined, msToLive, timeStamp, RequestEntryState.RequestPending, undefined, undefined, undefined),
      ResponsePending: new RemoteData(undefined, msToLive, timeStamp, RequestEntryState.ResponsePending, undefined, undefined, undefined),
      Success: new RemoteData(timeStamp, msToLive, timeStamp, RequestEntryState.Success, undefined, payload, statusCodeSuccess),
      SuccessStale: new RemoteData(timeStamp, msToLive, timeStamp, RequestEntryState.SuccessStale, undefined, payload, statusCodeSuccess),
      Error: new RemoteData(timeStamp, msToLive, timeStamp, RequestEntryState.Error, errorMessage, undefined, statusCodeError),
      ErrorStale: new RemoteData(timeStamp, msToLive, timeStamp, RequestEntryState.ErrorStale, errorMessage, undefined, statusCodeError),
    };

    return new TestService(
      requestService,
      rdbService,
      objectCache,
      halService,
    );
  }

  beforeEach(() => {
    service = initTestService();
  });

  describe('getFindAllHref', () => {

    it('should return an observable with the endpoint', () => {
      options = {};

      (service as any).getFindAllHref(options).subscribe((value) => {
          expect(value).toBe(endpoint);
        },
      );
    });

    it('should include page in href if currentPage provided in options', () => {
      options = { currentPage: 2 };
      const expected = `${endpoint}?page=${options.currentPage - 1}`;

      (service as any).getFindAllHref(options).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include size in href if elementsPerPage provided in options', () => {
      options = { elementsPerPage: 5 };
      const expected = `${endpoint}?size=${options.elementsPerPage}`;

      (service as any).getFindAllHref(options).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include sort href if SortOptions provided in options', () => {
      const sortOptions = new SortOptions('field1', SortDirection.ASC);
      options = { sort: sortOptions };
      const expected = `${endpoint}?sort=${sortOptions.field},${sortOptions.direction}`;

      (service as any).getFindAllHref(options).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include startsWith in href if startsWith provided in options', () => {
      options = { startsWith: 'ab' };
      const expected = `${endpoint}?startsWith=${options.startsWith}`;

      (service as any).getFindAllHref(options).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include all provided options in href', () => {
      const sortOptions = new SortOptions('field1', SortDirection.DESC);
      options = {
        currentPage: 6,
        elementsPerPage: 10,
        sort: sortOptions,
        startsWith: 'ab',

      };
      const expected = `${endpoint}?page=${options.currentPage - 1}&size=${options.elementsPerPage}` +
        `&sort=${sortOptions.field},${sortOptions.direction}&startsWith=${options.startsWith}`;

      (service as any).getFindAllHref(options).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include all searchParams in href if any provided in options', () => {
      options = {
        searchParams: [
          new RequestParam('param1', 'test'),
          new RequestParam('param2', 'test2'),
        ],
      };
      const expected = `${endpoint}?param1=test&param2=test2`;

      (service as any).getFindAllHref(options).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include linkPath in href if any provided', () => {
      const expected = `${endpoint}/test/entries`;

      (service as any).getFindAllHref({}, 'test/entries').subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include single linksToFollow as embed', () => {
      const expected = `${endpoint}?embed=bundles`;

      (service as any).getFindAllHref({}, null, followLink('bundles')).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include single linksToFollow as embed and its size', () => {
      const expected = `${endpoint}?embed.size=bundles=5&embed=bundles`;
      const config: FindListOptions = Object.assign(new FindListOptions(), {
        elementsPerPage: 5,
      });
      (service as any).getFindAllHref({}, null, followLink('bundles', { findListOptions: config })).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include multiple linksToFollow as embed', () => {
      const expected = `${endpoint}?embed=bundles&embed=owningCollection&embed=templateItemOf`;

      (service as any).getFindAllHref({}, null, followLink('bundles'), followLink('owningCollection'), followLink('templateItemOf')).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include multiple linksToFollow as embed and its sizes if given', () => {
      const expected = `${endpoint}?embed=bundles&embed.size=owningCollection=2&embed=owningCollection&embed=templateItemOf`;

      const config: FindListOptions = Object.assign(new FindListOptions(), {
        elementsPerPage: 2,
      });

      (service as any).getFindAllHref({}, null, followLink('bundles'), followLink('owningCollection', { findListOptions: config }), followLink('templateItemOf')).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should not include linksToFollow with shouldEmbed = false', () => {
      const expected = `${endpoint}?embed=templateItemOf`;

      (service as any).getFindAllHref(
        {},
        null,
        followLink('bundles', { shouldEmbed: false }),
        followLink('owningCollection', { shouldEmbed: false }),
        followLink('templateItemOf'),
      ).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include nested linksToFollow 3lvl', () => {
      const expected = `${endpoint}?embed=owningCollection${EMBED_SEPARATOR}itemtemplate${EMBED_SEPARATOR}relationships`;

      (service as any).getFindAllHref({}, null, followLink('owningCollection', {}, followLink('itemtemplate', {}, followLink('relationships')))).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });

    it('should include nested linksToFollow 2lvl and nested embed\'s size', () => {
      const expected = `${endpoint}?embed.size=owningCollection${EMBED_SEPARATOR}itemtemplate=4&embed=owningCollection${EMBED_SEPARATOR}itemtemplate`;
      const config: FindListOptions = Object.assign(new FindListOptions(), {
        elementsPerPage: 4,
      });
      (service as any).getFindAllHref({}, null, followLink('owningCollection', {}, followLink('itemtemplate', { findListOptions: config }))).subscribe((value) => {
        expect(value).toBe(expected);
      });
    });
  });
});
