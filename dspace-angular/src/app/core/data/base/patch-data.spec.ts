/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/* eslint-disable max-classes-per-file */
import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { FindListOptions } from '../find-list-options.model';
import { Observable, of as observableOf } from 'rxjs';
import { getMockRequestService } from '../../../shared/mocks/request.service.mock';
import { HALEndpointServiceStub } from '../../../shared/testing/hal-endpoint-service.stub';
import { getMockRemoteDataBuildService } from '../../../shared/mocks/remote-data-build.service.mock';
import { followLink } from '../../../shared/utils/follow-link-config.model';
import { TestScheduler } from 'rxjs/testing';
import { RemoteData } from '../remote-data';
import { RequestEntryState } from '../request-entry-state.model';
import { PatchData, PatchDataImpl } from './patch-data';
import { ChangeAnalyzer } from '../change-analyzer';
import { Item } from '../../shared/item.model';
import { compare, Operation } from 'fast-json-patch';
import { PatchRequest } from '../request.models';
import { DSpaceObject } from '../../shared/dspace-object.model';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { constructIdEndpointDefault } from './identifiable-data.service';
import { RestRequestMethod } from '../rest-request-method';

/**
 * Tests whether calls to `PatchData` methods are correctly patched through in a concrete data service that implements it
 */
export function testPatchDataImplementation(serviceFactory: () => PatchData<any>) {
  let service;

  describe('PatchData implementation', () => {
    const OBJ = Object.assign(new DSpaceObject(), {
      uuid: '08eec68f-45e4-47a3-80c5-f0beb5627079',
    });
    const OPERATIONS = [
      { op: 'replace', path: '/0/value', value: 'test' },
      { op: 'add', path: '/2/value', value: 'test2' },
    ] as Operation[];
    const METHOD = RestRequestMethod.POST;

    beforeAll(() => {
      service = serviceFactory();
      (service as any).patchData = jasmine.createSpyObj('patchData', {
        patch: 'TEST patch',
        update: 'TEST update',
        commitUpdates: undefined,
        createPatchFromCache: 'TEST createPatchFromCache',
      });
    });

    it('should handle calls to patch', () => {
      const out: any = service.patch(OBJ, OPERATIONS);

      expect((service as any).patchData.patch).toHaveBeenCalledWith(OBJ, OPERATIONS);
      expect(out).toBe('TEST patch');
    });

    it('should handle calls to update', () => {
      const out: any = service.update(OBJ);

      expect((service as any).patchData.update).toHaveBeenCalledWith(OBJ);
      expect(out).toBe('TEST update');
    });

    it('should handle calls to commitUpdates', () => {
      service.commitUpdates(METHOD);
      expect((service as any).patchData.commitUpdates).toHaveBeenCalledWith(METHOD);
    });

    it('should handle calls to createPatchFromCache', () => {
      const out: any = service.createPatchFromCache(OBJ);

      expect((service as any).patchData.createPatchFromCache).toHaveBeenCalledWith(OBJ);
      expect(out).toBe('TEST createPatchFromCache');
    });
  });
}

const endpoint = 'https://rest.api/core';

class TestService extends PatchDataImpl<any> {
  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected comparator: ChangeAnalyzer<Item>,
  ) {
    super(undefined, requestService, rdbService, objectCache, halService, comparator, undefined, constructIdEndpointDefault);
  }

  public getBrowseEndpoint(options: FindListOptions = {}, linkPath: string = this.linkPath): Observable<string> {
    return observableOf(endpoint);
  }
}

class DummyChangeAnalyzer implements ChangeAnalyzer<Item> {
  diff(object1: Item, object2: Item): Operation[] {
    return compare((object1 as any).metadata, (object2 as any).metadata);
  }
}

describe('PatchDataImpl', () => {
  let service: TestService;
  let requestService;
  let halService;
  let rdbService;
  let comparator;
  let objectCache;
  let selfLink;
  let linksToFollow;
  let testScheduler;
  let remoteDataMocks;

  function initTestService(): TestService {
    requestService = getMockRequestService();
    halService = new HALEndpointServiceStub('url') as any;
    rdbService = getMockRemoteDataBuildService();
    comparator = new DummyChangeAnalyzer() as any;
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
      comparator,
    );
  }

  beforeEach(() => {
    service = initTestService();
  });

  describe('patch', () => {
    const dso = {
      uuid: 'dso-uuid',
      _links: {
        self: {
          href: 'dso-href',
        }
      }
    };
    const operations = [
      Object.assign({
        op: 'move',
        from: '/1',
        path: '/5'
      }) as Operation
    ];

    it('should send a PatchRequest', () => {
      service.patch(dso, operations);
      expect(requestService.send).toHaveBeenCalledWith(jasmine.any(PatchRequest));
    });

    it('should invalidate the cached object if successfully patched', () => {
      spyOn(rdbService, 'buildFromRequestUUIDAndAwait');
      spyOn(service, 'invalidateByHref');

      service.patch(dso, operations);

      expect(rdbService.buildFromRequestUUIDAndAwait).toHaveBeenCalled();
      expect((rdbService.buildFromRequestUUIDAndAwait as jasmine.Spy).calls.argsFor(0)[0]).toBe(requestService.generateRequestId());
      const callback = (rdbService.buildFromRequestUUIDAndAwait as jasmine.Spy).calls.argsFor(0)[1];
      callback();

      expect(service.invalidateByHref).toHaveBeenCalledWith('dso-href');
    });
  });

  describe('update', () => {
    let operations;
    let dso;
    let dso2;
    const name1 = 'random string';
    const name2 = 'another random string';
    beforeEach(() => {
      operations = [{ op: 'replace', path: '/0/value', value: name2 } as Operation];

      dso = Object.assign(new DSpaceObject(), {
        _links: { self: { href: selfLink } },
        metadata: [{ key: 'dc.title', value: name1 }]
      });

      dso2 = Object.assign(new DSpaceObject(), {
        _links: { self: { href: selfLink } },
        metadata: [{ key: 'dc.title', value: name2 }]
      });

      spyOn(service, 'findByHref').and.returnValue(createSuccessfulRemoteDataObject$(dso));
      spyOn(objectCache, 'addPatch');
    });

    it('should call addPatch on the object cache with the right parameters when there are differences', () => {
      service.update(dso2).subscribe();
      expect(objectCache.addPatch).toHaveBeenCalledWith(selfLink, operations);
    });

    it('should not call addPatch on the object cache with the right parameters when there are no differences', () => {
      service.update(dso).subscribe();
      expect(objectCache.addPatch).not.toHaveBeenCalled();
    });
  });
});
