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
import { CreateData, CreateDataImpl } from './create-data';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { getMockRequestService } from '../../../shared/mocks/request.service.mock';
import { HALEndpointServiceStub } from '../../../shared/testing/hal-endpoint-service.stub';
import { getMockRemoteDataBuildService } from '../../../shared/mocks/remote-data-build.service.mock';
import { RemoteData } from '../remote-data';
import { RequestEntryState } from '../request-entry-state.model';
import { createFailedRemoteDataObject, createSuccessfulRemoteDataObject } from '../../../shared/remote-data.utils';
import { RequestParam } from '../../cache/models/request-param.model';
import { RestRequestMethod } from '../rest-request-method';
import { DSpaceObject } from '../../shared/dspace-object.model';

/**
 * Tests whether calls to `CreateData` methods are correctly patched through in a concrete data service that implements it
 */
export function testCreateDataImplementation(serviceFactory: () => CreateData<any>) {
  let service;

  describe('CreateData implementation', () => {
    const OBJ = Object.assign(new DSpaceObject(), {
      uuid: '08eec68f-45e4-47a3-80c5-f0beb5627079',
    });
    const PARAMS = [
      new RequestParam('abc', 123), new RequestParam('def', 456),
    ];

    beforeAll(() => {
      service = serviceFactory();

      (service as any).createData = jasmine.createSpyObj('createData', {
        create: 'TEST create',
      });
    });

    it('should handle calls to create', () => {
      const out: any = service.create(OBJ, ...PARAMS);

      expect((service as any).createData.create).toHaveBeenCalledWith(OBJ, ...PARAMS);
      expect(out).toBe('TEST create');
    });
  });
}

const endpoint = 'https://rest.api/core';

class TestService extends CreateDataImpl<any> {
  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
  ) {
    super('test', requestService, rdbService, objectCache, halService, notificationsService, undefined);
  }

  public getEndpoint(options: FindListOptions = {}, linkPath: string = this.linkPath): Observable<string> {
    return observableOf(endpoint);
  }
}

describe('CreateDataImpl', () => {
  let service: TestService;
  let requestService;
  let halService;
  let rdbService;
  let objectCache;
  let notificationsService;
  let remoteDataMocks;
  let obj;

  let MOCK_SUCCEEDED_RD;
  let MOCK_FAILED_RD;

  let buildFromRequestUUIDSpy: jasmine.Spy;
  let createOnEndpointSpy: jasmine.Spy;

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
    notificationsService = jasmine.createSpyObj('notificationsService', {
      error: undefined,
    });

    obj = {
      uuid: '1698f1d3-be98-4c51-9fd8-6bfedcbd59b7',
    };

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
      notificationsService,
    );
  }

  beforeEach(() => {
    service = initTestService();

    buildFromRequestUUIDSpy = spyOn(rdbService, 'buildFromRequestUUID').and.callThrough();
    createOnEndpointSpy = spyOn(service, 'createOnEndpoint').and.callThrough();

    MOCK_SUCCEEDED_RD = createSuccessfulRemoteDataObject({});
    MOCK_FAILED_RD = createFailedRemoteDataObject('something went wrong');
  });

  describe('create', () => {
    it('should POST the object to the root endpoint with the given parameters and return the remote data', (done) => {
      const params = [
        new RequestParam('abc', 123), new RequestParam('def', 456)
      ];
      buildFromRequestUUIDSpy.and.returnValue(observableOf(remoteDataMocks.Success));

      service.create(obj, ...params).subscribe(out => {
        expect(createOnEndpointSpy).toHaveBeenCalledWith(obj, jasmine.anything());
        expect(requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({
          method: RestRequestMethod.POST,
          uuid: requestService.generateRequestId(),
          href: 'https://rest.api/core?abc=123&def=456',
          body: JSON.stringify(obj),
        }));
        expect(buildFromRequestUUIDSpy).toHaveBeenCalledWith(requestService.generateRequestId());
        expect(out).toEqual(remoteDataMocks.Success);
        done();
      });
    });
  });

  describe('createOnEndpoint', () => {
    beforeEach(() => {
      buildFromRequestUUIDSpy.and.returnValue(observableOf(remoteDataMocks.Success));
    });

    it('should send a POST request with the object as JSON', (done) => {
      service.createOnEndpoint(obj, observableOf('https://rest.api/core/custom?search')).subscribe(out => {
        expect(requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({
          method: RestRequestMethod.POST,
          body: JSON.stringify(obj),
        }));
        done();
      });
    });

    it('should send the POST request to the given endpoint', (done) => {

      service.createOnEndpoint(obj, observableOf('https://rest.api/core/custom?search')).subscribe(out => {
        expect(requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({
          method: RestRequestMethod.POST,
          href: 'https://rest.api/core/custom?search',
        }));
        done();
      });
    });

    it('should return the remote data for the sent request', (done) => {
      service.createOnEndpoint(obj, observableOf('https://rest.api/core/custom?search')).subscribe(out => {
        expect(requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({
          method: RestRequestMethod.POST,
          uuid: requestService.generateRequestId(),
        }));
        expect(buildFromRequestUUIDSpy).toHaveBeenCalledWith(requestService.generateRequestId());
        expect(notificationsService.error).not.toHaveBeenCalled();
        expect(out).toEqual(remoteDataMocks.Success);
        done();
      });
    });

    it('should show an error notification if the request fails', (done) => {
      buildFromRequestUUIDSpy.and.returnValue(observableOf(remoteDataMocks.Error));

      service.createOnEndpoint(obj, observableOf('https://rest.api/core/custom?search')).subscribe(out => {
        expect(requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({
          method: RestRequestMethod.POST,
          uuid: requestService.generateRequestId(),
        }));
        expect(buildFromRequestUUIDSpy).toHaveBeenCalledWith(requestService.generateRequestId());
        expect(notificationsService.error).toHaveBeenCalled();
        expect(out).toEqual(remoteDataMocks.Error);
        done();
      });
    });
  });
});
