import { Store, StoreModule } from '@ngrx/store';
import { cold, getTestScheduler } from 'jasmine-marbles';
import { EMPTY, of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { getMockObjectCacheService } from '../../shared/mocks/object-cache.service.mock';
import { defaultUUID, getMockUUIDService } from '../../shared/mocks/uuid.service.mock';
import { ObjectCacheService } from '../cache/object-cache.service';
import { coreReducers} from '../core.reducers';
import { UUIDService } from '../shared/uuid.service';
import { RequestConfigureAction, RequestExecuteAction, RequestStaleAction } from './request.actions';
import {
  DeleteRequest,
  GetRequest,
  HeadRequest,
  OptionsRequest,
  PatchRequest,
  PostRequest,
  PutRequest
} from './request.models';
import { RequestService } from './request.service';
import { fakeAsync, TestBed, waitForAsync } from '@angular/core/testing';
import { storeModuleConfig } from '../../app.reducer';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { RequestEntryState } from './request-entry-state.model';
import { RestRequest } from './rest-request.model';
import { CoreState } from '../core-state.model';
import { RequestEntry } from './request-entry.model';

describe('RequestService', () => {
  let scheduler: TestScheduler;
  let service: RequestService;
  let serviceAsAny: any;
  let objectCache: ObjectCacheService;
  let uuidService: UUIDService;
  let store: Store<CoreState>;
  let mockStore: MockStore<CoreState>;

  const testUUID = '5f2a0d2a-effa-4d54-bd54-5663b960f9eb';
  const testHref = 'https://rest.api/endpoint/selfLink';
  const testGetRequest = new GetRequest(testUUID, testHref);
  const testPostRequest = new PostRequest(testUUID, testHref);
  const testPutRequest = new PutRequest(testUUID, testHref);
  const testDeleteRequest = new DeleteRequest(testUUID, testHref);
  const testOptionsRequest = new OptionsRequest(testUUID, testHref);
  const testHeadRequest = new HeadRequest(testUUID, testHref);
  const testPatchRequest = new PatchRequest(testUUID, testHref);

  const initialState: any = {
    core: {
      'cache/object': {},
      'cache/syncbuffer': {},
      'cache/object-updates': {},
      'data/request': {},
      'index': {},
    }
  };

  beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot(coreReducers, storeModuleConfig)
      ],
      providers: [
        provideMockStore({ initialState }),
        { provide: RequestService, useValue: service }
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    scheduler = getTestScheduler();

    objectCache = getMockObjectCacheService();
    (objectCache.hasByHref as any).and.returnValue(false);

    uuidService = getMockUUIDService();

    store = TestBed.inject(Store);
    mockStore = store as MockStore<CoreState>;
    mockStore.setState(initialState);
    service = new RequestService(
      objectCache,
      uuidService,
      store,
      undefined
    );
    serviceAsAny = service as any;
  });

  describe('generateRequestId', () => {
    it('should generate a new request ID', () => {
      const result = service.generateRequestId();
      const expected = `client/${defaultUUID}`;

      expect(result).toBe(expected);
    });
  });

  describe('isPending', () => {
    describe('before the request is configured', () => {
      beforeEach(() => {
        spyOn(service, 'getByHref').and.returnValue(observableOf(undefined));
      });

      it('should return false', () => {
        const result = service.isPending(testGetRequest);
        const expected = false;

        expect(result).toBe(expected);
      });
    });

    describe('when the request has been configured but hasn\'t reached the store yet', () => {
      beforeEach(() => {
        spyOn(service, 'getByHref').and.returnValue(observableOf(undefined));
        serviceAsAny.requestsOnTheirWayToTheStore = [testHref];
      });

      it('should return true', () => {
        const result = service.isPending(testGetRequest);
        const expected = true;

        expect(result).toBe(expected);
      });
    });

    describe('when the request has reached the store, before the server responds', () => {
      beforeEach(() => {
        spyOn(service, 'getByHref').and.returnValue(observableOf({
          state: RequestEntryState.ResponsePending
        } as RequestEntry));
      });

      it('should return true', () => {
        const result = service.isPending(testGetRequest);
        const expected = true;

        expect(result).toBe(expected);
      });
    });

    describe('after the server responds', () => {
      beforeEach(() => {
        spyOn(service, 'getByHref').and.returnValues(observableOf({
          state: RequestEntryState.Success
        } as RequestEntry));
      });

      it('should return false', () => {
        const result = service.isPending(testGetRequest);
        const expected = false;

        expect(result).toBe(expected);
      });
    });

  });

  describe('getByUUID', () => {
    describe('if the request with the specified UUID exists in the store', () => {
      let entry;

      beforeEach(() => {
        entry = {
          state: RequestEntryState.Success,
          response: {
            timeCompleted: new Date().getTime()
          },
          request: new GetRequest('request-uuid', 'request-href')
        };

        const state = Object.assign({}, initialState, {
          core: Object.assign({}, initialState.core, {
            'data/request': {
              '5f2a0d2a-effa-4d54-bd54-5663b960f9eb': entry
            },
            'index': {
              'get-request/configured-to-cache-uuid': {
                '5f2a0d2a-effa-4d54-bd54-5663b960f9eb': '5f2a0d2a-effa-4d54-bd54-5663b960f9eb'
              }
            }
          })
        });
        mockStore.setState(state);
      });

      it('should return an Observable of the RequestEntry', () => {
        const result = service.getByUUID(testUUID);
        const expected = cold('b', {
          b: entry
        });

        expect(result).toBeObservable(expected);
      });
    });

    describe(`if the request with the specified UUID doesn't exist in the store `, () => {
      beforeEach(() => {
        // No direct hit in the request cache
        // No hit in the index
        // So no mapped hit in the request cache
        mockStore.setState(initialState);
      });

      it('should return an Observable of undefined', () => {
        const result = service.getByUUID(testUUID);
        const expected = cold('a', { a: undefined });
        expect(result).toBeObservable(expected);
      });
    });

  });

  describe('getByHref', () => {
    describe('when the request with the specified href exists in the store', () => {
      let entry;
      beforeEach(() => {
        entry = {
          state: RequestEntryState.Success,
          response: {
            timeCompleted: new Date().getTime()
          },
          request: new GetRequest('request-uuid', 'request-href')
        };
        const state = Object.assign({}, initialState, {
          core: Object.assign({}, initialState.core, {
            'data/request': {
              '5f2a0d2a-effa-4d54-bd54-5663b960f9eb': entry
            },
            'index': {
              'get-request/configured-to-cache-uuid': {
                '5f2a0d2a-effa-4d54-bd54-5663b960f9eb': '5f2a0d2a-effa-4d54-bd54-5663b960f9eb'
              },
              'get-request/href-to-uuid': {
                'https://rest.api/endpoint/selfLink': '5f2a0d2a-effa-4d54-bd54-5663b960f9eb'
              }
            }
          })
        });
        mockStore.setState(state);
      });

      it('should return an Observable of the RequestEntry', () => {
        const result = service.getByHref(testHref);
        const expected = cold('c', {
          c: entry
        });

        expect(result).toBeObservable(expected);
      });
    });

    describe('when the request with the specified href doesn\'t exist in the store', () => {
      beforeEach(() => {
        // No direct hit in the request cache
        // No hit in the index
        // So no mapped hit in the request cache
        mockStore.setState(initialState);
      });

      it('should return an Observable of undefined', () => {
        const result = service.getByHref(testHref);
        const expected = cold('c', {
          c: undefined
        });

        expect(result).toBeObservable(expected);
      });
    });
  });

  describe('send', () => {
    beforeEach(() => {
      spyOn(serviceAsAny, 'dispatchRequest');
    });

    describe('when the request is a GET request', () => {
      let request: RestRequest;

      beforeEach(() => {
        request = testGetRequest;
      });

      it('should track it on it\'s way to the store', () => {
        spyOn(serviceAsAny, 'trackRequestsOnTheirWayToTheStore');
        spyOn(serviceAsAny, 'shouldDispatchRequest').and.returnValue(true);
        service.send(request);
        expect(serviceAsAny.trackRequestsOnTheirWayToTheStore).toHaveBeenCalledWith(request);
      });
      describe('and it isn\'t cached or pending', () => {
        beforeEach(() => {
          spyOn(serviceAsAny, 'shouldDispatchRequest').and.returnValue(true);
        });

        it('should dispatch the request', () => {
          scheduler.schedule(() => service.send(request, true));
          scheduler.flush();
          expect(serviceAsAny.dispatchRequest).toHaveBeenCalledWith(request);
        });
      });
      describe('and it is already cached or pending', () => {
        beforeEach(() => {
          spyOn(serviceAsAny, 'shouldDispatchRequest').and.returnValue(false);
        });

        it('shouldn\'t dispatch the request', () => {
          service.send(request, true);
          expect(serviceAsAny.dispatchRequest).not.toHaveBeenCalled();
        });
      });
    });

    describe('when the request isn\'t a GET request', () => {
      it('should dispatch the request', () => {
        service.send(testPostRequest);
        expect(serviceAsAny.dispatchRequest).toHaveBeenCalledWith(testPostRequest);

        service.send(testPutRequest);
        expect(serviceAsAny.dispatchRequest).toHaveBeenCalledWith(testPutRequest);

        service.send(testDeleteRequest);
        expect(serviceAsAny.dispatchRequest).toHaveBeenCalledWith(testDeleteRequest);

        service.send(testOptionsRequest);
        expect(serviceAsAny.dispatchRequest).toHaveBeenCalledWith(testOptionsRequest);

        service.send(testHeadRequest);
        expect(serviceAsAny.dispatchRequest).toHaveBeenCalledWith(testHeadRequest);

        service.send(testPatchRequest);
        expect(serviceAsAny.dispatchRequest).toHaveBeenCalledWith(testPatchRequest);
      });
    });

  });

  const expectAllNonGetRequestsToBeTrue = () => {
    expect(serviceAsAny.shouldDispatchRequest(testPostRequest, true)).toBeTrue();
    expect(serviceAsAny.shouldDispatchRequest(testPutRequest, true)).toBeTrue();
    expect(serviceAsAny.shouldDispatchRequest(testDeleteRequest, true)).toBeTrue();
    expect(serviceAsAny.shouldDispatchRequest(testOptionsRequest, true)).toBeTrue();
    expect(serviceAsAny.shouldDispatchRequest(testHeadRequest, true)).toBeTrue();
    expect(serviceAsAny.shouldDispatchRequest(testPatchRequest, true)).toBeTrue();
  };

  describe('shouldDispatchRequest', () => {
    describe(`when it's not a GET request`, () => {
      describe('and it is pending', () => {
        beforeEach(() => {
          spyOn(service, 'isPending').and.returnValue(true);
        });
        it('should return true', expectAllNonGetRequestsToBeTrue);
      });
      describe(`and it isn't pending`, () => {
        beforeEach(() => {
          spyOn(service, 'isPending').and.returnValue(false);
        });

        describe(`and useCachedVersionIfAvailable is false`, () => {
          it('should return true', expectAllNonGetRequestsToBeTrue);
        });

        describe(`and useCachedVersionIfAvailable is true`, () => {
          describe('and it is cached', () => {
            describe('in the ObjectCache', () => {
              beforeEach(() => {
                (objectCache.getByHref as any).and.returnValue(observableOf({ requestUUID: 'some-uuid' }));
                spyOn(serviceAsAny, 'hasByHref').and.returnValue(false);
                spyOn(serviceAsAny, 'hasByUUID').and.returnValue(true);
              });

              it('should return true', expectAllNonGetRequestsToBeTrue);
            });
            describe('in the request cache', () => {
              beforeEach(() => {
                (objectCache.getByHref as any).and.returnValue(observableOf(undefined));
                spyOn(serviceAsAny, 'hasByHref').and.returnValues(true);
                spyOn(serviceAsAny, 'hasByUUID').and.returnValue(false);
              });
              it('should return true', expectAllNonGetRequestsToBeTrue);
            });
          });

          describe(`and it isn't cached`, () => {
            beforeEach(() => {
              (objectCache.getByHref as any).and.returnValue(EMPTY);
              spyOn(serviceAsAny, 'hasByHref').and.returnValues(false);
              spyOn(serviceAsAny, 'hasByUUID').and.returnValue(false);
            });
            it('should return true', expectAllNonGetRequestsToBeTrue);
          });
        });
      });
    });

    describe(`when it is a GET request`, () => {
      describe('and it is pending', () => {
        beforeEach(() => {
          spyOn(service, 'isPending').and.returnValue(true);
        });

        it('should return false', () => {
          const result = serviceAsAny.shouldDispatchRequest(testGetRequest, false);
          const expected = false;

          expect(result).toEqual(expected);
        });
      });
      describe(`and it isn't pending`, () => {
        beforeEach(() => {
          spyOn(service, 'isPending').and.returnValue(false);
        });

        describe(`and useCachedVersionIfAvailable is false`, () => {
          it(`should return true`, () => {
            const result = serviceAsAny.shouldDispatchRequest(testGetRequest, false);
            const expected = true;

            expect(result).toEqual(expected);
          });
        });

        describe(`and useCachedVersionIfAvailable is true`, () => {
          describe('and it is cached', () => {
            describe('in the ObjectCache', () => {
              beforeEach(() => {
                (objectCache.getByHref as any).and.returnValue(observableOf({ requestUUIDs: ['some-uuid'] }));
                spyOn(serviceAsAny, 'hasByHref').and.returnValue(false);
                spyOn(serviceAsAny, 'hasByUUID').and.returnValue(true);
              });

              it('should return false', () => {
                const result = serviceAsAny.shouldDispatchRequest(testGetRequest, true);
                const expected = false;

                expect(result).toEqual(expected);
              });
            });
            describe('in the request cache', () => {
              beforeEach(() => {
                (objectCache.getByHref as any).and.returnValue(observableOf(undefined));
                spyOn(serviceAsAny, 'hasByHref').and.returnValues(true);
                spyOn(serviceAsAny, 'hasByUUID').and.returnValue(false);
              });
              it('should return false', () => {
                const result = serviceAsAny.shouldDispatchRequest(testGetRequest, true);
                const expected = false;

                expect(result).toEqual(expected);
              });
            });
          });
          describe(`and it isn't cached`, () => {
            beforeEach(() => {
              (objectCache.getByHref as any).and.returnValue(EMPTY);
              spyOn(serviceAsAny, 'hasByHref').and.returnValues(false);
              spyOn(serviceAsAny, 'hasByUUID').and.returnValue(false);
            });

            it('should return true', () => {
              const result = serviceAsAny.shouldDispatchRequest(testGetRequest, true);
              const expected = true;

              expect(result).toEqual(expected);
            });
          });
        });
      });
    });
  });

  describe('dispatchRequest', () => {
    let dispatchSpy: jasmine.Spy;
    beforeEach(() => {
      dispatchSpy = spyOn(store, 'dispatch');
    });

    it('should dispatch a RequestConfigureAction', () => {
      const request = testGetRequest;
      serviceAsAny.dispatchRequest(request);
      const firstAction = dispatchSpy.calls.argsFor(0)[0];
      expect(firstAction).toBeInstanceOf(RequestConfigureAction);
      expect(firstAction.payload).toEqual(request);
    });

    it('should dispatch a RequestExecuteAction', () => {
      const request = testGetRequest;
      serviceAsAny.dispatchRequest(request);
      const secondAction = dispatchSpy.calls.argsFor(1)[0];
      expect(secondAction).toBeInstanceOf(RequestExecuteAction);
      expect(secondAction.payload).toEqual(request.uuid);
    });

    describe('when it\'s not a GET request', () => {
      it('shouldn\'t track it', () => {
        spyOn(serviceAsAny, 'trackRequestsOnTheirWayToTheStore');

        serviceAsAny.dispatchRequest(testPostRequest);
        expect(serviceAsAny.trackRequestsOnTheirWayToTheStore).not.toHaveBeenCalled();

        serviceAsAny.dispatchRequest(testPutRequest);
        expect(serviceAsAny.trackRequestsOnTheirWayToTheStore).not.toHaveBeenCalled();

        serviceAsAny.dispatchRequest(testDeleteRequest);
        expect(serviceAsAny.trackRequestsOnTheirWayToTheStore).not.toHaveBeenCalled();

        serviceAsAny.dispatchRequest(testOptionsRequest);
        expect(serviceAsAny.trackRequestsOnTheirWayToTheStore).not.toHaveBeenCalled();

        serviceAsAny.dispatchRequest(testHeadRequest);
        expect(serviceAsAny.trackRequestsOnTheirWayToTheStore).not.toHaveBeenCalled();

        serviceAsAny.dispatchRequest(testPatchRequest);
        expect(serviceAsAny.trackRequestsOnTheirWayToTheStore).not.toHaveBeenCalled();
      });
    });
  });

  describe('trackRequestsOnTheirWayToTheStore', () => {
    let request: GetRequest;
    let entry;

    beforeEach(() => {
      request = testGetRequest;
      entry = {
        state: RequestEntryState.Success,
        response: {
          timeCompleted: new Date().getTime()
        },
        request: request
      };
    });

    describe('when the method is called with a new request', () => {
      it('should start tracking the request', () => {
        expect(serviceAsAny.requestsOnTheirWayToTheStore.includes(request.href)).toBeFalsy();
        serviceAsAny.trackRequestsOnTheirWayToTheStore(request);
        expect(serviceAsAny.requestsOnTheirWayToTheStore.includes(request.href)).toBeTruthy();
      });
    });

    describe('when the request is added to the store', () => {
      it('should stop tracking the request', () => {
        spyOn(serviceAsAny, 'getByHref').and.returnValue(observableOf(entry));
        serviceAsAny.trackRequestsOnTheirWayToTheStore(request);
        expect(serviceAsAny.requestsOnTheirWayToTheStore.includes(request.href)).toBeFalsy();
      });
    });
  });

  describe('hasByHref', () => {
    describe('when nothing is returned by getByHref', () => {
      beforeEach(() => {
        spyOn(service, 'getByHref').and.returnValue(EMPTY);
      });
      it('hasByHref should return false', () => {
        const result = service.hasByHref('');
        expect(result).toBe(false);
      });
    });

    describe('when the RequestEntry is undefined', () => {
      beforeEach(() => {
        spyOn(service, 'getByHref').and.returnValue(observableOf(undefined));
      });
      it('hasByHref should return false', () => {
        const result = service.hasByHref('', false);
        expect(result).toBe(false);
      });
    });

    describe('when the RequestEntry is not undefined', () => {
      beforeEach(() => {
        spyOn(service, 'getByHref').and.returnValue(observableOf({} as any));
      });
      it('hasByHref should return true', () => {
        const result = service.hasByHref('', false);
        expect(result).toBe(true);
      });
    });
  });

  describe('uriEncodeBody', () => {
    it('should properly encode the body', () => {
      const body = {
        'property1': 'multiple\nlines\nto\nsend',
        'property2': 'sp&ci@l characters',
        'sp&ci@l-chars in prop': 'test123',
      };
      const queryParams = service.uriEncodeBody(body);
      expect(queryParams).toEqual(
        'property1=multiple%0Alines%0Ato%0Asend&property2=sp%26ci%40l%20characters&sp%26ci%40l-chars%20in%20prop=test123'
      );
    });

    it('should properly encode the body with an array', () => {
      const body = {
        'property1': 'multiple\nlines\nto\nsend',
        'property2': 'sp&ci@l characters',
        'sp&ci@l-chars in prop': 'test123',
        'arrayParam': ['arrayValue1', 'arrayValue2'],
      };
      const queryParams = service.uriEncodeBody(body);
      expect(queryParams).toEqual(
        'property1=multiple%0Alines%0Ato%0Asend&property2=sp%26ci%40l%20characters&sp%26ci%40l-chars%20in%20prop=test123&arrayParam=arrayValue1&arrayParam=arrayValue2'
      );
    });
  });

  describe('setStaleByUUID', () => {
    let dispatchSpy: jasmine.Spy;
    let getByUUIDSpy: jasmine.Spy;

    beforeEach(() => {
      dispatchSpy = spyOn(store, 'dispatch');
      getByUUIDSpy = spyOn(service, 'getByUUID').and.callThrough();
    });

    it('should dispatch a RequestStaleAction', () => {
      service.setStaleByUUID('something');
      const firstAction = dispatchSpy.calls.argsFor(0)[0];
      expect(firstAction).toBeInstanceOf(RequestStaleAction);
      expect(firstAction.payload).toEqual({ uuid: 'something' });
    });

    it('should return an Observable that emits true as soon as the request is stale', fakeAsync(() => {
      dispatchSpy.and.callFake(() => { /* empty */ });   // don't actually set as stale
      getByUUIDSpy.and.returnValue(cold('a-b--c--d-', {  // but fake the state in the cache
        a: { state: RequestEntryState.ResponsePending },
        b: { state: RequestEntryState.Success },
        c: { state: RequestEntryState.SuccessStale },
        d: { state: RequestEntryState.Error },
      }));

      const done$ = service.setStaleByUUID('something');
      expect(done$).toBeObservable(cold('-----(t|)', { t: true }));
    }));
  });
});
