/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { FindListOptions } from '../find-list-options.model';
import { getMockRequestService } from '../../../shared/mocks/request.service.mock';
import { HALEndpointServiceStub } from '../../../shared/testing/hal-endpoint-service.stub';
import { getMockRemoteDataBuildService } from '../../../shared/mocks/remote-data-build.service.mock';
import { followLink } from '../../../shared/utils/follow-link-config.model';
import { TestScheduler } from 'rxjs/testing';
import { RemoteData } from '../remote-data';
import { RequestEntryState } from '../request-entry-state.model';
import { Observable, of as observableOf } from 'rxjs';
import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { IdentifiableDataService } from './identifiable-data.service';
import { EMBED_SEPARATOR } from './base-data.service';

const endpoint = 'https://rest.api/core';

class TestService extends IdentifiableDataService<any> {
  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
  ) {
    super(undefined, requestService, rdbService, objectCache, halService);
  }

  public getBrowseEndpoint(options: FindListOptions = {}, linkPath: string = this.linkPath): Observable<string> {
    return observableOf(endpoint);
  }
}

describe('IdentifiableDataService', () => {
  let service: TestService;
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
      }
    } as any;
    selfLink = 'https://rest.api/endpoint/1698f1d3-be98-4c51-9fd8-6bfedcbd59b7';
    linksToFollow = [
      followLink('a'),
      followLink('b')
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

  describe('getIDHref', () => {
    const endpointMock = 'https://dspace7-internal.atmire.com/server/api/core/items';
    const resourceIdMock = '003c99b4-d4fe-44b0-a945-e12182a7ca89';

    it('should return endpoint', () => {
      const result = (service as any).getIDHref(endpointMock, resourceIdMock);
      expect(result).toEqual(endpointMock + '/' + resourceIdMock);
    });

    it('should include single linksToFollow as embed', () => {
      const expected = `${endpointMock}/${resourceIdMock}?embed=bundles`;
      const result = (service as any).getIDHref(endpointMock, resourceIdMock, followLink('bundles'));
      expect(result).toEqual(expected);
    });

    it('should include multiple linksToFollow as embed', () => {
      const expected = `${endpointMock}/${resourceIdMock}?embed=bundles&embed=owningCollection&embed=templateItemOf`;
      const result = (service as any).getIDHref(endpointMock, resourceIdMock, followLink('bundles'), followLink('owningCollection'), followLink('templateItemOf'));
      expect(result).toEqual(expected);
    });

    it('should not include linksToFollow with shouldEmbed = false', () => {
      const expected = `${endpointMock}/${resourceIdMock}?embed=templateItemOf`;
      const result = (service as any).getIDHref(
        endpointMock,
        resourceIdMock,
        followLink('bundles', { shouldEmbed: false }),
        followLink('owningCollection', { shouldEmbed: false }),
        followLink('templateItemOf')
      );
      expect(result).toEqual(expected);
    });

    it('should include nested linksToFollow 3lvl', () => {
      const expected = `${endpointMock}/${resourceIdMock}?embed=owningCollection${EMBED_SEPARATOR}itemtemplate${EMBED_SEPARATOR}relationships`;
      const result = (service as any).getIDHref(endpointMock, resourceIdMock, followLink('owningCollection', {}, followLink('itemtemplate', {}, followLink('relationships'))));
      expect(result).toEqual(expected);
    });
  });
});
