/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { FindListOptions } from '../find-list-options.model';
import { Observable, of as observableOf, combineLatest as observableCombineLatest } from 'rxjs';
import { getMockRequestService } from '../../../shared/mocks/request.service.mock';
import { HALEndpointServiceStub } from '../../../shared/testing/hal-endpoint-service.stub';
import { getMockRemoteDataBuildService } from '../../../shared/mocks/remote-data-build.service.mock';
import { followLink } from '../../../shared/utils/follow-link-config.model';
import { TestScheduler } from 'rxjs/testing';
import { RemoteData } from '../remote-data';
import { RequestEntryState } from '../request-entry-state.model';
import { fakeAsync, tick } from '@angular/core/testing';
import { BaseDataService } from './base-data.service';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';

const endpoint = 'https://rest.api/core';

const BOOLEAN = { f: false, t: true };

class TestService extends BaseDataService<any> {
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

describe('BaseDataService', () => {
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
      },
      addDependency: () => {
        /* empty */
      },
      removeDependents: () => {
        /* empty */
      },
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

  describe(`reRequestStaleRemoteData`, () => {
    let callback: jasmine.Spy<jasmine.Func>;

    beforeEach(() => {
      callback = jasmine.createSpy();
    });


    describe(`when shouldReRequest is false`, () => {
      it(`shouldn't do anything`, () => {
        testScheduler.run(({ cold, expectObservable, flush }) => {
          const expected = 'a-b-c-d-e-f';
          const values = {
            a: remoteDataMocks.RequestPending,
            b: remoteDataMocks.ResponsePending,
            c: remoteDataMocks.Success,
            d: remoteDataMocks.SuccessStale,
            e: remoteDataMocks.Error,
            f: remoteDataMocks.ErrorStale,
          };

          expectObservable((service as any).reRequestStaleRemoteData(false, callback)(cold(expected, values))).toBe(expected, values);
          // since the callback happens in a tap(), flush to ensure it has been executed
          flush();
          expect(callback).not.toHaveBeenCalled();
        });
      });
    });

    describe(`when shouldReRequest is true`, () => {
      it(`should call the callback for stale RemoteData objects, but still pass the source observable unmodified`, () => {
        testScheduler.run(({ cold, expectObservable, flush }) => {
          const expected = 'a-b';
          const values = {
            a: remoteDataMocks.SuccessStale,
            b: remoteDataMocks.ErrorStale,
          };

          expectObservable((service as any).reRequestStaleRemoteData(true, callback)(cold(expected, values))).toBe(expected, values);
          // since the callback happens in a tap(), flush to ensure it has been executed
          flush();
          expect(callback).toHaveBeenCalledTimes(2);
        });
      });

      it(`should only call the callback for stale RemoteData objects if something is subscribed to it`, (done) => {
        testScheduler.run(({ cold, expectObservable }) => {
          const expected = 'a';
          const values = {
            a: remoteDataMocks.SuccessStale,
          };

          const result$ = (service as any).reRequestStaleRemoteData(true, callback)(cold(expected, values));
          expectObservable(result$).toBe(expected, values);
          expect(callback).not.toHaveBeenCalled();
          result$.subscribe(() => {
            expect(callback).toHaveBeenCalled();
            done();
          });
        });
      });

      it(`shouldn't do anything for RemoteData objects that aren't stale`, () => {
        testScheduler.run(({ cold, expectObservable, flush }) => {
          const expected = 'a-b-c-d';
          const values = {
            a: remoteDataMocks.RequestPending,
            b: remoteDataMocks.ResponsePending,
            c: remoteDataMocks.Success,
            d: remoteDataMocks.Error,
          };

          expectObservable((service as any).reRequestStaleRemoteData(true, callback)(cold(expected, values))).toBe(expected, values);
          // since the callback happens in a tap(), flush to ensure it has been executed
          flush();
          expect(callback).not.toHaveBeenCalled();
        });
      });
    });

  });

  describe(`findByHref`, () => {
    beforeEach(() => {
      spyOn(service as any, 'createAndSendGetRequest').and.callFake((href$) => { href$.subscribe().unsubscribe(); });
    });

    it(`should call buildHrefFromFindOptions with href and linksToFollow`, () => {
      testScheduler.run(({ cold }) => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue(selfLink);
        spyOn(rdbService, 'buildSingle').and.returnValue(cold('a', { a: remoteDataMocks.Success }));
        spyOn(service as any, 'reRequestStaleRemoteData').and.returnValue(() => cold('a', { a: remoteDataMocks.Success }));

        service.findByHref(selfLink, true, true, ...linksToFollow);
        expect(service.buildHrefFromFindOptions).toHaveBeenCalledWith(selfLink, {}, [], ...linksToFollow);
      });
    });

    it(`should call createAndSendGetRequest with the result from buildHrefFromFindOptions and useCachedVersionIfAvailable`, () => {
      testScheduler.run(({ cold, expectObservable }) => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue('bingo!');
        spyOn(rdbService, 'buildSingle').and.returnValue(cold('a', { a: remoteDataMocks.Success }));
        spyOn(service as any, 'reRequestStaleRemoteData').and.returnValue(() => cold('a', { a: remoteDataMocks.Success }));

        service.findByHref(selfLink, true, true, ...linksToFollow);
        expect((service as any).createAndSendGetRequest).toHaveBeenCalledWith(jasmine.anything(), true);
        expectObservable(rdbService.buildSingle.calls.argsFor(0)[0]).toBe('(a|)', { a: 'bingo!' });

        service.findByHref(selfLink, false, true, ...linksToFollow);
        expect((service as any).createAndSendGetRequest).toHaveBeenCalledWith(jasmine.anything(), false);
        expectObservable(rdbService.buildSingle.calls.argsFor(1)[0]).toBe('(a|)', { a: 'bingo!' });
      });
    });

    it(`should call rdbService.buildSingle with the result from buildHrefFromFindOptions and linksToFollow`, () => {
      testScheduler.run(({ cold, expectObservable }) => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue('bingo!');
        spyOn(rdbService, 'buildSingle').and.returnValue(cold('a', { a: remoteDataMocks.Success }));
        spyOn(service as any, 'reRequestStaleRemoteData').and.returnValue(() => cold('a', { a: remoteDataMocks.Success }));

        service.findByHref(selfLink, true, true, ...linksToFollow);
        expect(rdbService.buildSingle).toHaveBeenCalledWith(jasmine.anything() as any, ...linksToFollow);
        expectObservable(rdbService.buildSingle.calls.argsFor(0)[0]).toBe('(a|)', { a: 'bingo!' });
      });
    });

    it(`should return a the output from reRequestStaleRemoteData`, () => {
      testScheduler.run(({ cold, expectObservable }) => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue(selfLink);
        spyOn(rdbService, 'buildSingle').and.returnValue(cold('a', { a: remoteDataMocks.Success }));
        spyOn(service as any, 'reRequestStaleRemoteData').and.returnValue(() => cold('a', { a: 'bingo!' }));
        const expected = 'a';
        const values = {
          a: 'bingo!',
        };

        expectObservable(service.findByHref(selfLink, true, true, ...linksToFollow)).toBe(expected, values);
      });
    });

    it(`should call reRequestStaleRemoteData with reRequestOnStale and the exact same findByHref call as a callback`, () => {
      testScheduler.run(({ cold, expectObservable }) => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue(selfLink);
        spyOn(rdbService, 'buildSingle').and.returnValue(cold('a', { a: remoteDataMocks.SuccessStale }));
        spyOn(service as any, 'reRequestStaleRemoteData').and.returnValue(() => cold('a', { a: remoteDataMocks.SuccessStale }));

        service.findByHref(selfLink, true, true, ...linksToFollow);
        expect((service as any).reRequestStaleRemoteData.calls.argsFor(0)[0]).toBeTrue();
        spyOn(service, 'findByHref').and.returnValue(cold('a', { a: remoteDataMocks.SuccessStale }));
        // prove that the spy we just added hasn't been called yet
        expect(service.findByHref).not.toHaveBeenCalled();
        // call the callback passed to reRequestStaleRemoteData
        (service as any).reRequestStaleRemoteData.calls.argsFor(0)[1]();
        // verify that findByHref _has_ been called now, with the same params as the original call
        expect(service.findByHref).toHaveBeenCalledWith(jasmine.anything(), true, true, ...linksToFollow);
        // ... except for selflink, which will have been turned in to an observable.
        expectObservable((service.findByHref as jasmine.Spy).calls.argsFor(0)[0]).toBe('(a|)', { a: selfLink });
      });
    });

    describe(`when useCachedVersionIfAvailable is true`, () => {
      beforeEach(() => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue(selfLink);
        spyOn(service as any, 'reRequestStaleRemoteData').and.callFake(() => (source) => source);
      });

      it(`should emit a cached completed RemoteData immediately, and keep emitting if it gets rerequested`, () => {
        testScheduler.run(({ cold, expectObservable }) => {
          spyOn(rdbService, 'buildSingle').and.returnValue(cold('a-b-c-d-e', {
            a: remoteDataMocks.Success,
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          }));
          const expected = 'a-b-c-d-e';
          const values = {
            a: remoteDataMocks.Success,
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          };

          expectObservable(service.findByHref(selfLink, true, true, ...linksToFollow)).toBe(expected, values);
        });
      });

      it(`should not emit a cached stale RemoteData, but only start emitting after the state first changes to RequestPending`, () => {
        testScheduler.run(({ cold, expectObservable }) => {
          spyOn(rdbService, 'buildSingle').and.returnValue(cold('a-b-c-d-e', {
            a: remoteDataMocks.SuccessStale,
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          }));
          const expected = '--b-c-d-e';
          const values = {
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          };

          expectObservable(service.findByHref(selfLink, true, true, ...linksToFollow)).toBe(expected, values);
        });
      });

    });

    describe(`when useCachedVersionIfAvailable is false`, () => {
      beforeEach(() => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue(selfLink);
        spyOn(service as any, 'reRequestStaleRemoteData').and.callFake(() => (source) => source);
      });


      it(`should not emit a cached completed RemoteData, but only start emitting after the state first changes to RequestPending`, () => {
        testScheduler.run(({ cold, expectObservable }) => {
          spyOn(rdbService, 'buildSingle').and.returnValue(cold('a-b-c-d-e', {
            a: remoteDataMocks.Success,
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          }));
          const expected = '--b-c-d-e';
          const values = {
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          };

          expectObservable(service.findByHref(selfLink, false, true, ...linksToFollow)).toBe(expected, values);
        });
      });

      it(`should not emit a cached stale RemoteData, but only start emitting after the state first changes to RequestPending`, () => {
        testScheduler.run(({ cold, expectObservable }) => {
          spyOn(rdbService, 'buildSingle').and.returnValue(cold('a-b-c-d-e', {
            a: remoteDataMocks.SuccessStale,
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          }));
          const expected = '--b-c-d-e';
          const values = {
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          };

          expectObservable(service.findByHref(selfLink, false, true, ...linksToFollow)).toBe(expected, values);
        });
      });

    });

  });

  describe(`findListByHref`, () => {
    let findListOptions;
    beforeEach(() => {
      findListOptions = { currentPage: 5 };
      spyOn(service as any, 'createAndSendGetRequest').and.callFake((href$) => { href$.subscribe().unsubscribe(); });
    });

    it(`should call buildHrefFromFindOptions with href and linksToFollow`, () => {
      testScheduler.run(({ cold }) => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue(selfLink);
        spyOn(rdbService, 'buildList').and.returnValue(cold('a', { a: remoteDataMocks.Success }));
        spyOn(service as any, 'reRequestStaleRemoteData').and.returnValue(() => cold('a', { a: remoteDataMocks.Success }));

        service.findListByHref(selfLink, findListOptions, true, true, ...linksToFollow);
        expect(service.buildHrefFromFindOptions).toHaveBeenCalledWith(selfLink, findListOptions, [], ...linksToFollow);
      });
    });

    it(`should call createAndSendGetRequest with the result from buildHrefFromFindOptions and useCachedVersionIfAvailable`, () => {
      testScheduler.run(({ cold, expectObservable }) => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue('bingo!');
        spyOn(rdbService, 'buildList').and.returnValue(cold('a', { a: remoteDataMocks.Success }));
        spyOn(service as any, 'reRequestStaleRemoteData').and.returnValue(() => cold('a', { a: remoteDataMocks.Success }));

        service.findListByHref(selfLink, findListOptions, true, true, ...linksToFollow);
        expect((service as any).createAndSendGetRequest).toHaveBeenCalledWith(jasmine.anything(), true);
        expectObservable(rdbService.buildList.calls.argsFor(0)[0]).toBe('(a|)', { a: 'bingo!' });

        service.findListByHref(selfLink, findListOptions, false, true, ...linksToFollow);
        expect((service as any).createAndSendGetRequest).toHaveBeenCalledWith(jasmine.anything(), false);
        expectObservable(rdbService.buildList.calls.argsFor(1)[0]).toBe('(a|)', { a: 'bingo!' });
      });
    });

    it(`should call rdbService.buildList with the result from buildHrefFromFindOptions and linksToFollow`, () => {
      testScheduler.run(({ cold, expectObservable }) => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue('bingo!');
        spyOn(rdbService, 'buildList').and.returnValue(cold('a', { a: remoteDataMocks.Success }));
        spyOn(service as any, 'reRequestStaleRemoteData').and.returnValue(() => cold('a', { a: remoteDataMocks.Success }));

        service.findListByHref(selfLink, findListOptions, true, true, ...linksToFollow);
        expect(rdbService.buildList).toHaveBeenCalledWith(jasmine.anything() as any, ...linksToFollow);
        expectObservable(rdbService.buildList.calls.argsFor(0)[0]).toBe('(a|)', { a: 'bingo!' });
      });
    });

    it(`should call reRequestStaleRemoteData with reRequestOnStale and the exact same findListByHref call as a callback`, () => {
      testScheduler.run(({ cold, expectObservable }) => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue('bingo!');
        spyOn(rdbService, 'buildList').and.returnValue(cold('a', { a: remoteDataMocks.SuccessStale }));
        spyOn(service as any, 'reRequestStaleRemoteData').and.returnValue(() => cold('a', { a: remoteDataMocks.SuccessStale }));

        service.findListByHref(selfLink, findListOptions, true, true, ...linksToFollow);
        expect((service as any).reRequestStaleRemoteData.calls.argsFor(0)[0]).toBeTrue();
        spyOn(service, 'findListByHref').and.returnValue(cold('a', { a: remoteDataMocks.SuccessStale }));
        // prove that the spy we just added hasn't been called yet
        expect(service.findListByHref).not.toHaveBeenCalled();
        // call the callback passed to reRequestStaleRemoteData
        (service as any).reRequestStaleRemoteData.calls.argsFor(0)[1]();
        // verify that findListByHref _has_ been called now, with the same params as the original call
        expect(service.findListByHref).toHaveBeenCalledWith(jasmine.anything(), findListOptions, true, true, ...linksToFollow);
        // ... except for selflink, which will have been turned in to an observable.
        expectObservable((service.findListByHref as jasmine.Spy).calls.argsFor(0)[0]).toBe('(a|)', { a: selfLink });
      });
    });

    it(`should return a the output from reRequestStaleRemoteData`, () => {
      testScheduler.run(({ cold, expectObservable }) => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue(selfLink);
        spyOn(rdbService, 'buildList').and.returnValue(cold('a', { a: remoteDataMocks.Success }));
        spyOn(service as any, 'reRequestStaleRemoteData').and.returnValue(() => cold('a', { a: 'bingo!' }));
        const expected = 'a';
        const values = {
          a: 'bingo!',
        };

        expectObservable(service.findListByHref(selfLink, findListOptions, true, true, ...linksToFollow)).toBe(expected, values);
      });
    });

    describe(`when useCachedVersionIfAvailable is true`, () => {
      beforeEach(() => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue(selfLink);
        spyOn(service as any, 'reRequestStaleRemoteData').and.callFake(() => (source) => source);
      });

      it(`should emit a cached completed RemoteData immediately, and keep emitting if it gets rerequested`, () => {
        testScheduler.run(({ cold, expectObservable }) => {
          spyOn(rdbService, 'buildList').and.returnValue(cold('a-b-c-d-e', {
            a: remoteDataMocks.Success,
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          }));
          const expected = 'a-b-c-d-e';
          const values = {
            a: remoteDataMocks.Success,
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          };

          expectObservable(service.findListByHref(selfLink, findListOptions, true, true, ...linksToFollow)).toBe(expected, values);
        });
      });

      it(`should not emit a cached stale RemoteData, but only start emitting after the state first changes to RequestPending`, () => {
        testScheduler.run(({ cold, expectObservable }) => {
          spyOn(rdbService, 'buildList').and.returnValue(cold('a-b-c-d-e', {
            a: remoteDataMocks.SuccessStale,
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          }));
          const expected = '--b-c-d-e';
          const values = {
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          };

          expectObservable(service.findListByHref(selfLink, findListOptions, true, true, ...linksToFollow)).toBe(expected, values);
        });
      });

    });

    describe(`when useCachedVersionIfAvailable is false`, () => {
      beforeEach(() => {
        spyOn(service, 'buildHrefFromFindOptions').and.returnValue(selfLink);
        spyOn(service as any, 'reRequestStaleRemoteData').and.callFake(() => (source) => source);
      });


      it(`should not emit a cached completed RemoteData, but only start emitting after the state first changes to RequestPending`, () => {
        testScheduler.run(({ cold, expectObservable }) => {
          spyOn(rdbService, 'buildList').and.returnValue(cold('a-b-c-d-e', {
            a: remoteDataMocks.Success,
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          }));
          const expected = '--b-c-d-e';
          const values = {
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          };

          expectObservable(service.findListByHref(selfLink, findListOptions, false, true, ...linksToFollow)).toBe(expected, values);
        });
      });

      it(`should not emit a cached stale RemoteData, but only start emitting after the state first changes to RequestPending`, () => {
        testScheduler.run(({ cold, expectObservable }) => {
          spyOn(rdbService, 'buildList').and.returnValue(cold('a-b-c-d-e', {
            a: remoteDataMocks.SuccessStale,
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          }));
          const expected = '--b-c-d-e';
          const values = {
            b: remoteDataMocks.RequestPending,
            c: remoteDataMocks.ResponsePending,
            d: remoteDataMocks.Success,
            e: remoteDataMocks.SuccessStale,
          };

          expectObservable(service.findListByHref(selfLink, findListOptions, false, true, ...linksToFollow)).toBe(expected, values);
        });
      });

    });
  });

  describe('invalidateByHref', () => {
    let getByHrefSpy: jasmine.Spy;

    beforeEach(() => {
      getByHrefSpy = spyOn(objectCache, 'getByHref').and.returnValue(observableOf({
        requestUUIDs: ['request1', 'request2', 'request3'],
        dependentRequestUUIDs: ['request4', 'request5']
      }));

    });

    it('should call setStaleByUUID for every request associated with this DSO', (done) => {
      service.invalidateByHref('some-href').subscribe((ok) => {
        expect(ok).toBeTrue();
        expect(getByHrefSpy).toHaveBeenCalledWith('some-href');
        expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request1');
        expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request2');
        expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request3');
        expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request4');
        expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request5');
        done();
      });
    });

    it('should call setStaleByUUID even if not subscribing to returned Observable', fakeAsync(() => {
      service.invalidateByHref('some-href');
      tick();

      expect(getByHrefSpy).toHaveBeenCalledWith('some-href');
      expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request1');
      expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request2');
      expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request3');
      expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request4');
      expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request5');
    }));

    it('should return an Observable that only emits true once all requests are stale', () => {
      testScheduler.run(({ cold, expectObservable }) => {
        requestService.setStaleByUUID.and.callFake((uuid) => {
          switch (uuid) {   // fake requests becoming stale at different times
            case 'request1':
              return cold('--(t|)', BOOLEAN);
            case 'request2':
              return cold('------(t|)', BOOLEAN);
            case 'request3':
              return cold('---(t|)', BOOLEAN);
            case 'request4':
              return cold('-(t|)', BOOLEAN);
            case 'request5':
              return cold('----(t|)', BOOLEAN);
          }
        });

        const done$ = service.invalidateByHref('some-href');

        // emit true as soon as the final request is stale
        expectObservable(done$).toBe('------(t|)', BOOLEAN);
      });
    });

    it('should only fire for the current state of the object (instead of tracking it)', () => {
      testScheduler.run(({ cold, flush }) => {
        getByHrefSpy.and.returnValue(cold('a---b---c---', {
          a: { requestUUIDs: ['request1'], dependentRequestUUIDs: [] },  // this is the state at the moment we're invalidating the cache
          b: { requestUUIDs: ['request2'], dependentRequestUUIDs: [] },  // we shouldn't keep tracking the state
          c: { requestUUIDs: ['request3'], dependentRequestUUIDs: [] },  // because we may invalidate when we shouldn't
        }));

        service.invalidateByHref('some-href');
        flush();

        // requests from the first state are marked as stale
        expect(requestService.setStaleByUUID).toHaveBeenCalledWith('request1');

        // request from subsequent states are ignored
        expect(requestService.setStaleByUUID).not.toHaveBeenCalledWith('request2');
        expect(requestService.setStaleByUUID).not.toHaveBeenCalledWith('request3');
      });
    });
  });

  describe('addDependency', () => {
    let addDependencySpy;

    beforeEach(() => {
      addDependencySpy = spyOn(objectCache, 'addDependency');
    });

    it('should call objectCache.addDependency with the object\'s self link', () => {
      addDependencySpy.and.callFake((href$: Observable<string>, dependsOn$: Observable<string>) => {
        observableCombineLatest([href$, dependsOn$]).subscribe(([href, dependsOn]) => {
          expect(href).toBe('object-href');
          expect(dependsOn).toBe('dependsOnHref');
        });
      });

      (service as any).addDependency(
        createSuccessfulRemoteDataObject$({ _links: { self: { href: 'object-href' } } }),
        observableOf('dependsOnHref')
      );
      expect(addDependencySpy).toHaveBeenCalled();
    });

    it('should call objectCache.addDependency without an href if request failed', () => {
      addDependencySpy.and.callFake((href$: Observable<string>, dependsOn$: Observable<string>) => {
        observableCombineLatest([href$, dependsOn$]).subscribe(([href, dependsOn]) => {
          expect(href).toBe(undefined);
          expect(dependsOn).toBe('dependsOnHref');
        });
      });

      (service as any).addDependency(
        createFailedRemoteDataObject$('something went wrong'),
        observableOf('dependsOnHref')
      );
      expect(addDependencySpy).toHaveBeenCalled();
    });
  });
});
