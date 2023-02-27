import { TestBed, waitForAsync } from '@angular/core/testing';

import { cold } from 'jasmine-marbles';
import { Store, StoreModule } from '@ngrx/store';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { Operation } from 'fast-json-patch';
import { empty, of as observableOf } from 'rxjs';
import { first } from 'rxjs/operators';

import { coreReducers} from '../core.reducers';
import { RestRequestMethod } from '../data/rest-request-method';
import { Item } from '../shared/item.model';
import {
  AddDependentsObjectCacheAction,
  RemoveDependentsObjectCacheAction,
  AddPatchObjectCacheAction,
  AddToObjectCacheAction,
  ApplyPatchObjectCacheAction,
  RemoveFromObjectCacheAction,
} from './object-cache.actions';
import { Patch } from './object-cache.reducer';
import { ObjectCacheService } from './object-cache.service';
import { AddToSSBAction } from './server-sync-buffer.actions';
import { RemoveFromIndexBySubstringAction } from '../index/index.actions';
import { HALLink } from '../shared/hal-link.model';
import { storeModuleConfig } from '../../app.reducer';
import { TestColdObservable } from 'jasmine-marbles/src/test-observables';
import { IndexName } from '../index/index-name.model';
import { CoreState } from '../core-state.model';
import { TestScheduler } from 'rxjs/testing';

describe('ObjectCacheService', () => {
  let service: ObjectCacheService;
  let store: Store<CoreState>;
  let mockStore: MockStore<CoreState>;
  let linkServiceStub;
  let initialState: any;

  let selfLink;
  let anotherLink;
  let altLink1;
  let altLink2;
  let requestUUID;
  let requestUUID2;
  let alternativeLink;
  let timestamp;
  let timestamp2;
  let msToLive;
  let msToLive2;
  let objectToCache;
  let cacheEntry;
  let cacheEntry2;
  let invalidCacheEntry;
  let operations;

  function init() {
    selfLink = 'https://rest.api/endpoint/1698f1d3-be98-4c51-9fd8-6bfedcbd59b7';
    anotherLink = 'https://another.link/endpoint/1234';
    altLink1 = 'https://alternative.link/endpoint/1234';
    altLink2 = 'https://alternative.link/endpoint/5678';
    requestUUID = '4d3a4ce8-a375-4b98-859b-39f0a014d736';
    requestUUID2 = 'c0f486c1-c4d3-4a03-b293-ca5b71ff0054';
    alternativeLink = 'https://rest.api/endpoint/5e4f8a5-be98-4c51-9fd8-6bfedcbd59b7/item';
    timestamp = new Date().getTime();
    timestamp2 = new Date().getTime() - 200;
    msToLive = 900000;
    msToLive2 = 120000;
    objectToCache = {
      type: Item.type,
      _links: {
        self: { href: selfLink },
        anotherLink: { href: anotherLink }
      }
    };
    cacheEntry = {
      data: objectToCache,
      timeCompleted: timestamp,
      msToLive: msToLive,
      alternativeLinks: [altLink1, altLink2],
      requestUUIDs: [requestUUID],
      dependentRequestUUIDs: [],
    };
    cacheEntry2 = {
      data: objectToCache,
      timeCompleted: timestamp2,
      msToLive: msToLive2,
      alternativeLinks: [altLink2],
      requestUUIDs: [requestUUID2],
      dependentRequestUUIDs: [],
    };
    invalidCacheEntry = Object.assign({}, cacheEntry, { msToLive: -1 });
    operations = [{ op: 'replace', path: '/name', value: 'random string' } as Operation];
    initialState = {
      core: {
        'cache/object': {},
        'cache/syncbuffer': {},
        'cache/object-updates': {},
        'data/request': {},
        'index': {}
      }
    };
  }

  beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot(coreReducers, storeModuleConfig)
      ],
      providers: [
        provideMockStore({ initialState }),
        { provide: ObjectCacheService, useValue: service }
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    init();
    store = TestBed.inject(Store);
    mockStore = store as MockStore<CoreState>;
    mockStore.setState(initialState);
    linkServiceStub = {
      removeResolvedLinks: (a) => a
    };
    spyOn(linkServiceStub, 'removeResolvedLinks').and.callThrough();
    spyOn(store, 'dispatch');
    service = new ObjectCacheService(store, linkServiceStub);

    spyOn(Date.prototype, 'getTime').and.callFake(() => {
      return timestamp;
    });
  });

  describe('add', () => {
    it('should dispatch an ADD action with the object to add, the time to live, and the current timestamp', () => {
      service.add(objectToCache, msToLive, requestUUID, alternativeLink);
      expect(store.dispatch).toHaveBeenCalledWith(new AddToObjectCacheAction(objectToCache, timestamp, msToLive, requestUUID, alternativeLink));
      expect(linkServiceStub.removeResolvedLinks).toHaveBeenCalledWith(objectToCache);
    });
  });

  describe('remove', () => {
    beforeEach(() => {
      spyOn(service as any, 'getByHref').and.returnValue(observableOf(cacheEntry));
    });

    it('should dispatch a REMOVE action with the self link of the object to remove', () => {
      service.remove(selfLink);
      expect(store.dispatch).toHaveBeenCalledWith(new RemoveFromObjectCacheAction(selfLink));
    });

    it('should dispatch a REMOVE_BY_SUBSTRING action on the index state for each alternativeLink in the object', () => {
      service.remove(selfLink);
      cacheEntry.alternativeLinks.forEach(
        (link: string) => expect(store.dispatch).toHaveBeenCalledWith(new RemoveFromIndexBySubstringAction(IndexName.ALTERNATIVE_OBJECT_LINK, link)));
    });

    it('should dispatch a REMOVE_BY_SUBSTRING action on the index state for each _links in the object, except the self link', () => {
      service.remove(selfLink);
      Object.entries(objectToCache._links).forEach(([key, value]: [string, HALLink]) => {
        if (key !== 'self') {
          expect(store.dispatch).toHaveBeenCalledWith(new RemoveFromIndexBySubstringAction(IndexName.ALTERNATIVE_OBJECT_LINK, value.href));
        }
      });
    });
  });

  describe('getByHref', () => {
    describe('if getBySelfLink emits a valid object and getByAlternativeLink emits undefined', () => {
      beforeEach(() => {
        spyOn(service as any, 'getBySelfLink').and.returnValue(observableOf(cacheEntry));
        spyOn(service as any, 'getByAlternativeLink').and.returnValue(observableOf(undefined));
      });

      it('should return the object emitted by getBySelfLink', () => {
        const result = service.getByHref(selfLink);
        const expected: TestColdObservable = cold('(a|)', { a: cacheEntry });
        expect(result).toBeObservable(expected);
      });
    });

    describe('if getBySelfLink emits undefined and getByAlternativeLink a valid object', () => {
      beforeEach(() => {
        spyOn(service as any, 'getBySelfLink').and.returnValue(observableOf(undefined));
        spyOn(service as any, 'getByAlternativeLink').and.returnValue(observableOf(cacheEntry));
      });

      it('should return the object emitted by getByAlternativeLink', () => {
        const result = service.getByHref(selfLink);
        const expected: TestColdObservable = cold('(a|)', { a: cacheEntry });
        expect(result).toBeObservable(expected);
      });
    });

    describe('if getBySelfLink emits an invalid and getByAlternativeLink a valid object', () => {
      beforeEach(() => {
        spyOn(service as any, 'getBySelfLink').and.returnValue(observableOf(cacheEntry));
        spyOn(service as any, 'getByAlternativeLink').and.returnValue(observableOf(cacheEntry2));
      });

      it('should return the object emitted by getByAlternativeLink', () => {
        const result = service.getByHref(selfLink);
        const expected: TestColdObservable = cold('(a|)', { a: cacheEntry2 });
        expect(result).toBeObservable(expected);
      });
    });
  });

  describe('getList', () => {
    it('should return an observable of the array of cached objects with the specified self link and type', () => {
      const item = Object.assign(new Item(), {
        _links: { self: { href: selfLink } }
      });
      spyOn(service, 'getObjectByHref').and.returnValue(observableOf(item));

      service.getList([selfLink, selfLink]).pipe(first()).subscribe((arr) => {
        expect(arr[0]._links.self.href).toBe(selfLink);
        expect(arr[0] instanceof Item).toBeTruthy();
      });
    });
  });

  describe('hasByHref', () => {
    describe('with requestUUID not specified', () => {
      describe('getByHref emits an object', () => {
        beforeEach(() => {
          spyOn(service, 'getByHref').and.returnValue(observableOf(cacheEntry));
        });

        it('should return true', () => {
          expect(service.hasByHref(selfLink)).toBe(true);
        });
      });

      describe('getByHref emits nothing', () => {
        beforeEach(() => {
          spyOn(service, 'getByHref').and.returnValue(empty());
        });

        it('should return false', () => {
          expect(service.hasByHref(selfLink)).toBe(false);
        });
      });
    });

    describe('with requestUUID specified', () => {
      describe('getByHref emits an object that includes the specified requestUUID', () => {
        beforeEach(() => {
          spyOn(service, 'getByHref').and.returnValue(observableOf(Object.assign(cacheEntry, {
            requestUUIDs: [
              'something',
              'something-else',
              'specific-request',
            ]
          })));
        });

        it('should return true', () => {
          expect(service.hasByHref(selfLink, 'specific-request')).toBe(true);
        });
      });

      describe('getByHref emits an object that doesn\'t include the specified requestUUID', () => {
        beforeEach(() => {
          spyOn(service, 'getByHref').and.returnValue(observableOf(Object.assign(cacheEntry, {
            requestUUIDs: [
              'something',
              'something-else',
            ]
          })));
        });

        it('should return true', () => {
          expect(service.hasByHref(selfLink, 'specific-request')).toBe(false);
        });
      });

      describe('getByHref emits nothing', () => {
        beforeEach(() => {
          spyOn(service, 'getByHref').and.returnValue(empty());
        });

        it('should return false', () => {
          expect(service.hasByHref(selfLink, 'specific-request')).toBe(false);
        });
      });
    });
  });

  describe('getBySelfLink', () => {
    it('should return the entry returned by the select method', () => {
      const state = Object.assign({}, initialState, {
        core: Object.assign({}, initialState.core, {
          'cache/object': {
            [selfLink]: cacheEntry
          }
        })
      });
      mockStore.setState(state);
      const expected: TestColdObservable = cold('a', { a: cacheEntry });
      expect((service as any).getBySelfLink(selfLink)).toBeObservable(expected);
    });
  });

  describe('getByAlternativeLink', () => {
    beforeEach(() => {
      spyOn(service as any, 'getBySelfLink').and.callThrough();
    });
    it('should call getBySelfLink with the value returned by the select method', () => {
      const state = Object.assign({}, initialState, {
        core: Object.assign({}, initialState.core, {
          'cache/object': {
            [selfLink]: cacheEntry
          },
          'index': {
            'object/alt-link-to-self-link': {
              [anotherLink]: selfLink
            }
          }
        })
      });
      mockStore.setState(state);
      (service as any).getByAlternativeLink(anotherLink).subscribe();
      expect((service as any).getBySelfLink).toHaveBeenCalledWith(selfLink);
    });
  });

  describe('patch methods', () => {
    it('should dispatch the correct actions when addPatch is called', () => {
      service.addPatch(selfLink, operations);
      expect(store.dispatch).toHaveBeenCalledWith(new AddPatchObjectCacheAction(selfLink, operations));
      expect(store.dispatch).toHaveBeenCalledWith(new AddToSSBAction(selfLink, RestRequestMethod.PATCH));
    });

    it('isDirty should return true when the patches list in the cache entry is not empty', () => {
      cacheEntry.patches = [
        {
          operations: operations
        } as Patch
      ];
      const result = (service as any).isDirty(cacheEntry);
      expect(result).toBe(true);
    });

    it('isDirty should return false when the patches list in the cache entry is empty', () => {
      cacheEntry.patches = [];
      const result = (service as any).isDirty(cacheEntry);
      expect(result).toBe(false);
    });
    it('should dispatch the correct actions when applyPatchesToCachedObject is called', () => {
      (service as any).applyPatchesToCachedObject(selfLink);
      expect(store.dispatch).toHaveBeenCalledWith(new ApplyPatchObjectCacheAction(selfLink));
    });
  });

  describe('request dependencies', () => {
    beforeEach(() => {
      const state = Object.assign({}, initialState, {
        core: Object.assign({}, initialState.core, {
          'cache/object': {
            ['objectWithoutDependents']: {
              dependentRequestUUIDs: [],
            },
            ['objectWithDependents']: {
              dependentRequestUUIDs: [requestUUID],
            },
            [selfLink]: cacheEntry,
          },
          'index': {
            'object/alt-link-to-self-link': {
              [anotherLink]: selfLink,
              ['objectWithoutDependentsAlt']: 'objectWithoutDependents',
              ['objectWithDependentsAlt']: 'objectWithDependents',
            }
          }
        })
      });
      mockStore.setState(state);
    });

    describe('addDependency', () => {
      it('should dispatch an ADD_DEPENDENTS action', () => {
        service.addDependency(selfLink, 'objectWithoutDependents');
        expect(store.dispatch).toHaveBeenCalledOnceWith(new AddDependentsObjectCacheAction('objectWithoutDependents', [requestUUID]));
      });

      it('should resolve alt links', () => {
        service.addDependency(anotherLink, 'objectWithoutDependentsAlt');
        expect(store.dispatch).toHaveBeenCalledOnceWith(new AddDependentsObjectCacheAction('objectWithoutDependents', [requestUUID]));
      });

      it('should not dispatch if either href cannot be resolved to a cached self link', () => {
        service.addDependency(selfLink, 'unknown');
        service.addDependency('unknown', 'objectWithoutDependents');
        service.addDependency('nothing', 'matches');
        expect(store.dispatch).not.toHaveBeenCalled();
      });

      it('should not dispatch if either href is undefined', () => {
        service.addDependency(selfLink, undefined);
        service.addDependency(undefined, 'objectWithoutDependents');
        service.addDependency(undefined, undefined);
        expect(store.dispatch).not.toHaveBeenCalled();
      });

      it('should not dispatch if the dependency exists already', () => {
        service.addDependency(selfLink, 'objectWithDependents');
        expect(store.dispatch).not.toHaveBeenCalled();
      });

      it('should work with observable hrefs', () => {
        service.addDependency(observableOf(selfLink), observableOf('objectWithoutDependents'));
        expect(store.dispatch).toHaveBeenCalledOnceWith(new AddDependentsObjectCacheAction('objectWithoutDependents', [requestUUID]));
      });

      it('should only dispatch once for the first value of either observable href', () => {
        const testScheduler = new TestScheduler((actual, expected) => {
          expect(actual).toEqual(expected);
        });

        testScheduler.run(({ cold: tsCold, flush }) => {
          const href$ = tsCold('--y-n-n', {
            y: selfLink,
            n: 'NOPE'
          });
          const dependsOnHref$ = tsCold('-y-n-n', {
            y: 'objectWithoutDependents',
            n: 'NOPE'
          });

          service.addDependency(href$, dependsOnHref$);
          flush();

          expect(store.dispatch).toHaveBeenCalledOnceWith(new AddDependentsObjectCacheAction('objectWithoutDependents', [requestUUID]));
        });
      });

      it('should not dispatch if either of the hrefs emits undefined', () => {
        const testScheduler = new TestScheduler((actual, expected) => {
          expect(actual).toEqual(expected);
        });

        testScheduler.run(({ cold: tsCold, flush }) => {
          const undefined$ = tsCold('--u');

          service.addDependency(selfLink, undefined$);
          service.addDependency(undefined$, 'objectWithoutDependents');
          service.addDependency(undefined$, undefined$);
          flush();

          expect(store.dispatch).not.toHaveBeenCalled();
        });
      });
    });

    describe('removeDependents', () => {
      it('should dispatch a REMOVE_DEPENDENTS action', () => {
        service.removeDependents('objectWithDependents');
        expect(store.dispatch).toHaveBeenCalledOnceWith(new RemoveDependentsObjectCacheAction('objectWithDependents'));
      });

      it('should resolve alt links', () => {
        service.removeDependents('objectWithDependentsAlt');
        expect(store.dispatch).toHaveBeenCalledOnceWith(new RemoveDependentsObjectCacheAction('objectWithDependents'));
      });

      it('should not dispatch if the href cannot be resolved to a cached self link', () => {
        service.removeDependents('unknown');
        expect(store.dispatch).not.toHaveBeenCalled();
      });
    });
  });
});
