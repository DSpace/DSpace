/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { FindListOptions } from '../find-list-options.model';
import { Observable, of as observableOf } from 'rxjs';
import { getMockRequestService } from '../../../shared/mocks/request.service.mock';
import { HALEndpointServiceStub } from '../../../shared/testing/hal-endpoint-service.stub';
import { getMockRemoteDataBuildService } from '../../../shared/mocks/remote-data-build.service.mock';
import { RemoteData } from '../remote-data';
import { RequestEntryState } from '../request-entry-state.model';
import { PutData, PutDataImpl } from './put-data';
import { RestRequestMethod } from '../rest-request-method';
import { DSpaceObject } from '../../shared/dspace-object.model';

/**
 * Tests whether calls to `PutData` methods are correctly patched through in a concrete data service that implements it
 */
export function testPutDataImplementation(serviceFactory: () => PutData<any>) {
  let service;

  describe('PutData implementation', () => {
    const OBJ = Object.assign(new DSpaceObject(), {
      uuid: '08eec68f-45e4-47a3-80c5-f0beb5627079',
    });

    beforeAll(() => {
      service = serviceFactory();
      (service as any).putData = jasmine.createSpyObj('putData', {
        put: 'TEST put',
      });
    });

    it('should handle calls to put', () => {
      const out: any = service.put(OBJ);

      expect((service as any).putData.put).toHaveBeenCalledWith(OBJ);
      expect(out).toBe('TEST put');
    });
  });
}


const endpoint = 'https://rest.api/core';

class TestService extends PutDataImpl<any> {
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

describe('PutDataImpl', () => {
  let service: TestService;
  let requestService;
  let halService;
  let rdbService;
  let objectCache;
  let selfLink;
  let remoteDataMocks;

  let obj;
  let buildFromRequestUUIDSpy: jasmine.Spy;

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

    obj = Object.assign(new DSpaceObject(), {
      uuid: '1698f1d3-be98-4c51-9fd8-6bfedcbd59b7',
      metadata: {           // recognized properties will be serialized
        ['dc.title']: [
          { language: 'en', value: 'some object' },
        ]
      },
      data: [ 1, 2, 3, 4 ], // unrecognized properties won't be serialized
      _links: { self: { href: selfLink } },
    });


    buildFromRequestUUIDSpy = spyOn(rdbService, 'buildFromRequestUUID').and.returnValue(observableOf(remoteDataMocks.Success));
  });

  describe('put', () => {
    it('should send a PUT request with the serialized object', (done) => {
      service.put(obj).subscribe(() => {
        expect(requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({
          method: RestRequestMethod.PUT,
          body: {  // _links are not serialized
            uuid: obj.uuid,
            metadata: obj.metadata
          },
        }));
        done();
      });
    });

    it('should send the PUT request to the object\'s self link', (done) => {
      service.put(obj).subscribe(() => {
        expect(requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({
          method: RestRequestMethod.PUT,
          href: selfLink,
        }));
        done();
      });
    });

    it('should return the remote data for the sent request', (done) => {
      service.put(obj).subscribe(out => {
        expect(requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({
          method: RestRequestMethod.PUT,
          uuid: requestService.generateRequestId(),
        }));
        expect(buildFromRequestUUIDSpy).toHaveBeenCalledWith(requestService.generateRequestId());
        expect(out).toEqual(remoteDataMocks.Success);
        done();
      });
    });
  });
});
