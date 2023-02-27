import { createFailedRemoteDataObject, createPendingRemoteDataObject, createSuccessfulRemoteDataObject } from '../../../shared/remote-data.utils';
import { buildPaginatedList, PaginatedList } from '../../data/paginated-list.model';
import { Item } from '../../shared/item.model';
import { PageInfo } from '../../shared/page-info.model';
import { RemoteDataBuildService } from './remote-data-build.service';
import { ObjectCacheService } from '../object-cache.service';
import { ITEM } from '../../shared/item.resource-type';
import { getMockLinkService } from '../../../shared/mocks/link-service.mock';
import { getMockRequestService } from '../../../shared/mocks/request.service.mock';
import { getMockObjectCacheService } from '../../../shared/mocks/object-cache.service.mock';
import { LinkService } from './link.service';
import { RequestService } from '../../data/request.service';
import { UnCacheableObject } from '../../shared/uncacheable-object.model';
import { RemoteData } from '../../data/remote-data';
import { Observable, of as observableOf } from 'rxjs';
import { followLink, FollowLinkConfig } from '../../../shared/utils/follow-link-config.model';
import { take } from 'rxjs/operators';
import { HALLink } from '../../shared/hal-link.model';
import { RequestEntryState } from '../../data/request-entry-state.model';
import { RequestEntry } from '../../data/request-entry.model';
import { cold } from 'jasmine-marbles';
import { TestScheduler } from 'rxjs/testing';
import { fakeAsync, tick } from '@angular/core/testing';

describe('RemoteDataBuildService', () => {
  let service: RemoteDataBuildService;
  let objectCache: ObjectCacheService;
  let linkService: LinkService;
  let requestService: RequestService;
  let unCacheableObject: UnCacheableObject;
  let pageInfo: PageInfo;
  let paginatedList: PaginatedList<any>;
  let normalizedPaginatedList: PaginatedList<any>;
  let selfLink1: string;
  let selfLink2: string;
  let pageLinks: HALLink[];
  let array: Item[];
  let arrayRD: RemoteData<any>;
  let paginatedListRD: RemoteData<PaginatedList<any>>;
  let requestEntry$: Observable<RequestEntry>;
  let entrySuccessCacheable: RequestEntry;
  let entrySuccessUnCacheable: RequestEntry;
  let entrySuccessNoContent: RequestEntry;
  let entryError: RequestEntry;
  let linksToFollow: FollowLinkConfig<any>[];

  beforeEach(() => {
    objectCache = getMockObjectCacheService();
    linkService = getMockLinkService();
    requestService = getMockRequestService();
    unCacheableObject = {
      foo: 'bar'
    };
    pageInfo = new PageInfo();
    selfLink1 = 'https://rest.api/some/object';
    selfLink2 = 'https://rest.api/another/object';
    pageLinks = [
      { href: selfLink1 },
      { href: selfLink2 },
    ];
    array = [
      Object.assign(new Item(), {
        metadata: {
          'dc.title': [
            {
              language: 'en_US',
              value: 'Item nr 1'
            }
          ]
        },
        _links: {
          self: {
            href: selfLink1
          }
        }
      }),
      Object.assign(new Item(), {
        metadata: {
          'dc.title': [
            {
              language: 'en_US',
              value: 'Item nr 2'
            }
          ]
        },
        _links: {
          self: {
            href: selfLink2
          }
        }
      })
    ];
    paginatedList = buildPaginatedList(pageInfo, array);
    normalizedPaginatedList = buildPaginatedList(pageInfo, array, true);
    arrayRD = createSuccessfulRemoteDataObject(array);
    paginatedListRD = createSuccessfulRemoteDataObject(paginatedList);
    entrySuccessCacheable = {
      request: {
        uuid: '17820127-0ee5-4ed4-b6da-e654bdff8487'
      },
      state: RequestEntryState.Success,
      response: {
        statusCode: 200,
        payloadLink: {
          href: selfLink1
        }
      }
    } as RequestEntry;
    entrySuccessUnCacheable = {
      request: {
        uuid: '0aa5ec06-d6a7-4e73-952e-1e0462bd1501'
      },
      state: RequestEntryState.Success,
      response: {
        statusCode: 200,
        unCacheableObject,
      }
    } as RequestEntry;
    entrySuccessNoContent = {
      request: {
        uuid: '780a7295-6102-4a43-9775-80f2a4ff673c'
      },
      state: RequestEntryState.Success,
      response: {
        statusCode: 204
      },
    } as RequestEntry;
    entryError = {
      request: {
        uuid: '1609dcbc-8442-4877-966e-864f151cc40c'
      },
      state: RequestEntryState.Error,
      response: {
        statusCode: 500,
      }
    } as RequestEntry;
    requestEntry$ = observableOf(entrySuccessCacheable);
    linksToFollow = [
      followLink('a'),
      followLink('b'),
    ];

    service = new RemoteDataBuildService(objectCache, linkService, requestService);
  });

  describe(`buildPayload`, () => {
    beforeEach(() => {
      spyOn(service as any, 'plainObjectToInstance').and.returnValue(unCacheableObject);
      spyOn(service as any, 'buildPaginatedList').and.returnValue(observableOf(paginatedList));
      (objectCache.getObjectByHref as jasmine.Spy).and.returnValue(observableOf(array[0]));
      (linkService.resolveLinks as jasmine.Spy).and.returnValue(array[1]);
    });

    describe(`when no self link for the object to retrieve is provided`, () => {
      beforeEach(() => {
        spyOn(service as any, 'hasExactMatchInObjectCache').and.returnValue(false);
        spyOn(service as any, 'isCacheablePayload').and.returnValue(true);
        spyOn(service as any, 'isUnCacheablePayload').and.returnValue(false);
      });

      it(`should call hasExactMatchInObjectCache with undefined and the requestEntry`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined, ...linksToFollow)
          .pipe(take(1))
          .subscribe(() => {
            expect((service as any).hasExactMatchInObjectCache).toHaveBeenCalledWith(undefined, entrySuccessCacheable);
            done();
          });
      });

      it(`should call isCacheablePayload with the requestEntry`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined, ...linksToFollow)
          .pipe(take(1))
          .subscribe(() => {
            expect((service as any).isCacheablePayload).toHaveBeenCalledWith(entrySuccessCacheable);
            done();
          });
      });

      it(`should not call isUnCacheablePayload`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined, ...linksToFollow)
          .pipe(take(1))
          .subscribe(() => {
            expect((service as any).isUnCacheablePayload).not.toHaveBeenCalled();
            done();
          });
      });

      it(`should call objectCache.getObjectByHref() with the payloadLink from the response`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined, ...linksToFollow)
          .pipe(take(1))
          .subscribe(() => {
            expect(objectCache.getObjectByHref).toHaveBeenCalledWith(entrySuccessCacheable.response.payloadLink.href);
            done();
          });
      });

      it(`should call linkService.resolveLinks with the object from the cache and the linksToFollow`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined, ...linksToFollow)
          .pipe(take(1))
          .subscribe(() => {
            expect(linkService.resolveLinks).toHaveBeenCalledWith(array[0], ...linksToFollow);
            done();
          });
      });

      it(`should return the object returned from linkService.resolveLinks`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined, ...linksToFollow)
          .pipe(take(1))
          .subscribe((response) => {
            expect(response).toEqual(array[1]);
            done();
          });
      });

    });

    describe(`when a self link for the object to retrieve is provided`, () => {
      beforeEach(() => {
        spyOn(service as any, 'hasExactMatchInObjectCache').and.returnValue(true);
        spyOn(service as any, 'isCacheablePayload').and.returnValue(false);
        spyOn(service as any, 'isUnCacheablePayload').and.returnValue(false);
      });

      it(`should call hasExactMatchInObjectCache with that self link and the requestEntry`, (done) => {
        (service as any).buildPayload(requestEntry$, observableOf(selfLink2), ...linksToFollow)
          .pipe(take(1))
          .subscribe(() => {
            expect((service as any).hasExactMatchInObjectCache).toHaveBeenCalledWith(selfLink2, entrySuccessCacheable);
            done();
          });
      });

      it(`should call objectCache.getObjectByHref() with that self link`, (done) => {
        (service as any).buildPayload(requestEntry$, observableOf(selfLink2), ...linksToFollow)
          .pipe(take(1))
          .subscribe(() => {
            expect(objectCache.getObjectByHref).toHaveBeenCalledWith(selfLink2);
            done();
          });
      });

      it(`should call linkService.resolveLinks with the object from the cache and the linksToFollow`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined, ...linksToFollow)
          .pipe(take(1))
          .subscribe(() => {
            expect(linkService.resolveLinks).toHaveBeenCalledWith(array[0], ...linksToFollow);
            done();
          });
      });

      it(`should return the object returned from linkService.resolveLinks`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined, ...linksToFollow)
          .pipe(take(1))
          .subscribe((response) => {
            expect(response).toEqual(array[1]);
            done();
          });
      });
    });

    describe(`when the entry contains an uncachable payload`, () => {
      beforeEach(() => {
        requestEntry$ = observableOf(entrySuccessUnCacheable);
        spyOn(service as any, 'hasExactMatchInObjectCache').and.returnValue(false);
        spyOn(service as any, 'isCacheablePayload').and.returnValue(false);
        spyOn(service as any, 'isUnCacheablePayload').and.returnValue(true);
      });

      it(`should call hasExactMatchInObjectCache with undefined and the requestEntry`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined)
          .pipe(take(1))
          .subscribe(() => {
            expect((service as any).hasExactMatchInObjectCache).toHaveBeenCalledWith(undefined, entrySuccessUnCacheable);
            done();
          });
      });

      it(`should call isCacheablePayload with the requestEntry`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined)
          .pipe(take(1))
          .subscribe(() => {
            expect((service as any).isCacheablePayload).toHaveBeenCalledWith(entrySuccessUnCacheable);
            done();
          });
      });

      it(`should call isUnCacheablePayload with the requestEntry`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined)
          .pipe(take(1))
          .subscribe(() => {
            expect((service as any).isUnCacheablePayload).toHaveBeenCalledWith(entrySuccessUnCacheable);
            done();
          });
      });

      it(`should call plainObjectToInstance with the unCacheableObject from the response`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined)
          .pipe(take(1))
          .subscribe(() => {
            expect((service as any).plainObjectToInstance).toHaveBeenCalledWith(unCacheableObject);
            done();
          });
      });

      it(`should return the uncacheable object from the response`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined)
          .pipe(take(1))
          .subscribe((response) => {
            expect(response).toBe(unCacheableObject);
            done();
          });
      });
    });

    describe(`when the entry contains a 204 response`, () => {
      beforeEach(() => {
        requestEntry$ = observableOf(entrySuccessNoContent);
        spyOn(service as any, 'hasExactMatchInObjectCache').and.returnValue(false);
        spyOn(service as any, 'isCacheablePayload').and.returnValue(false);
        spyOn(service as any, 'isUnCacheablePayload').and.returnValue(false);
      });

      it(`should return null`, (done) => {
        (service as any).buildPayload(requestEntry$, observableOf(selfLink2), ...linksToFollow)
          .pipe(take(1))
          .subscribe((response) => {
            expect(response).toBeNull();
            done();
          });
      });
    });

    describe(`when the entry contains an error`, () => {
      beforeEach(() => {
        requestEntry$ = observableOf(entryError);
        spyOn(service as any, 'hasExactMatchInObjectCache').and.returnValue(false);
        spyOn(service as any, 'isCacheablePayload').and.returnValue(false);
        spyOn(service as any, 'isUnCacheablePayload').and.returnValue(false);
      });

      it(`should return undefined`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined)
          .pipe(take(1))
          .subscribe((response) => {
            expect(response).toBeUndefined();
            done();
          });
      });
    });

    describe(`when the entry contains a link to a paginated list`, () => {
      beforeEach(() => {
        requestEntry$ = observableOf(entrySuccessCacheable);
        (objectCache.getObjectByHref as jasmine.Spy).and.returnValue(observableOf(paginatedList));
        spyOn(service as any, 'hasExactMatchInObjectCache').and.returnValue(false);
        spyOn(service as any, 'isCacheablePayload').and.returnValue(true);
        spyOn(service as any, 'isUnCacheablePayload').and.returnValue(false);
      });

      it(`should call buildPaginatedList with the object from the cache and the linksToFollow`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined, ...linksToFollow)
          .pipe(take(1))
          .subscribe(() => {
            expect((service as any).buildPaginatedList).toHaveBeenCalledWith(paginatedList, ...linksToFollow);
            done();
          });
      });

      it(`should return the paginated list`, (done) => {
        (service as any).buildPayload(requestEntry$, undefined, ...linksToFollow)
          .pipe(take(1))
          .subscribe((response) => {
            expect(response).toEqual(paginatedList);
            done();
          });
      });
    });

  });

  describe('hasExactMatchInObjectCache', () => {
    describe('when the object-cache contains the href, and it has a reference to the request id', () => {
      beforeEach(() => {
        (objectCache.hasByHref as jasmine.Spy).and.returnValue(true);
      });

      it('should return true if the href has a value, and the entry has a request with a valid id', () => {
        expect((service as any).hasExactMatchInObjectCache('href', entrySuccessCacheable)).toEqual(true);
      });

      it('should return false if the href is undefined', () => {
        expect((service as any).hasExactMatchInObjectCache(undefined, entrySuccessCacheable)).toEqual(false);
      });

      it('should return false if the requestEntry is undefined', () => {
        expect((service as any).hasExactMatchInObjectCache('href', undefined)).toEqual(false);
      });

      it('should return false if the requestEntry has no request', () => {
        expect((service as any).hasExactMatchInObjectCache('href', {})).toEqual(false);
      });

      it(`should return false if the requestEntry's request has no id`, () => {
        expect((service as any).hasExactMatchInObjectCache('href', { request: {} })).toEqual(false);
      });
    });

    describe('when the object-cache doesn\'t contain the href', () => {
      beforeEach(() => {
        (objectCache.hasByHref as jasmine.Spy).and.returnValue(false);
      });

      it('should return false if the href has a value', () => {
        expect((service as any).hasExactMatchInObjectCache('href')).toEqual(false);
      });

      it('should return false if the href is undefined', () => {
        expect((service as any).hasExactMatchInObjectCache(undefined)).toEqual(false);
      });
    });
  });

  describe('isCacheablePayload', () => {
    let entry;

    describe('when the entry\'s response contains a cacheable payload', () => {
      beforeEach(() => {
        entry = {
          response: {
            payloadLink: { href: 'payload-link' }
          }
        };
      });

      it('should return true', () => {
        expect((service as any).isCacheablePayload(entry)).toEqual(true);
      });
    });

    describe('when the entry\'s response doesn\'t contain a cacheable payload', () => {
      beforeEach(() => {
        entry = {
          response: {
            payloadLink: undefined
          }
        };
      });

      it('should return false', () => {
        expect((service as any).isCacheablePayload(entry)).toEqual(false);
      });
    });
  });

  describe('isUnCacheablePayload', () => {
    let entry;

    describe('when the entry\'s response contains an uncacheable object', () => {
      beforeEach(() => {
        entry = {
          response: {
            unCacheableObject: Object.assign({})
          }
        };
      });

      it('should return true', () => {
        expect((service as any).isUnCacheablePayload(entry)).toEqual(true);
      });
    });

    describe('when the entry\'s response doesn\'t contain an uncacheable object', () => {
      beforeEach(() => {
        entry = {
          response: {}
        };
      });

      it('should return false', () => {
        expect((service as any).isUnCacheablePayload(entry)).toEqual(false);
      });
    });
  });

  describe(`plainObjectToInstance`, () => {
    describe(`when the object has a recognized type property`, () => {
      it(`should return a new instance of that type`, () => {
        const source: any = {
          type: ITEM,
          uuid: 'some-uuid'
        };

        const result = (service as any).plainObjectToInstance(source);
        result.foo = 'bar';

        expect(result).toEqual(jasmine.any(Item));
        expect(result.uuid).toEqual('some-uuid');
        expect(result.foo).toEqual('bar');
        expect(source.foo).toBeUndefined();
      });
    });
    describe(`when the object doesn't have a recognized type property`, () => {
      it(`should return a new plain JS object`, () => {
        const source: any = {
          type: 'foobar',
          uuid: 'some-uuid'
        };

        const result = (service as any).plainObjectToInstance(source);
        result.foo = 'bar';

        expect(result).toEqual(jasmine.any(Object));
        expect(result.uuid).toEqual('some-uuid');
        expect(result.foo).toEqual('bar');
        expect(source.foo).toBeUndefined();
      });
    });
  });

  describe(`buildPaginatedList`, () => {
    beforeEach(() => {
      (objectCache.getList as jasmine.Spy).and.returnValue(observableOf(array));
      (linkService.resolveLinks as jasmine.Spy).and.callFake((obj) => obj);
      spyOn(service as any, 'plainObjectToInstance').and.callFake((obj) => obj);
    });
    describe(`when linksToFollow contains a 'page' link`, () => {
      let paginatedLinksToFollow;
      beforeEach(() => {
        paginatedLinksToFollow = [
          followLink('page', {}, ...linksToFollow),
          ...linksToFollow
        ];
      });
      describe(`and the given list doesn't have a page property already`, () => {
        it(`should call objectCache.getList with the links in _links.page of the given object`, (done) => {
          (service as any).buildPaginatedList(normalizedPaginatedList, ...paginatedLinksToFollow)
            .pipe(take(1))
            .subscribe(() => {
              expect(objectCache.getList).toHaveBeenCalledWith(pageLinks.map((link: HALLink) => link.href));
              done();
            });
        });

        it(`should call plainObjectToInstance for each of the page objects`, (done) => {
          (service as any).buildPaginatedList(normalizedPaginatedList, ...paginatedLinksToFollow)
            .pipe(take(1))
            .subscribe(() => {
              array.forEach((element) => {
                expect((service as any).plainObjectToInstance).toHaveBeenCalledWith(element);
              });
              done();
            });
        });

        it(`should call linkService.resolveLinks for each of the page objects`, (done) => {
          (service as any).buildPaginatedList(normalizedPaginatedList, ...paginatedLinksToFollow)
            .pipe(take(1))
            .subscribe(() => {
              array.forEach((element) => {
                expect(linkService.resolveLinks).toHaveBeenCalledWith(element, ...linksToFollow);
              });
              done();
            });
        });

        it(`should return a new PaginatedList instance based on the given object`, (done) => {
          const listAsPlainJSObj = Object.assign({}, normalizedPaginatedList);
          (service as any).buildPaginatedList(listAsPlainJSObj, ...paginatedLinksToFollow)
            .pipe(take(1))
            .subscribe((result) => {
              expect(listAsPlainJSObj).toEqual(jasmine.any(Object));
              expect(result).toEqual(jasmine.any(PaginatedList));
              expect(result).toEqual(paginatedList);
              done();
            });
        });

        describe(`when there are other links as well`, () => {
          it(`should call linkservice.resolveLinks for those other links`, (done) => {
            (service as any).buildPaginatedList(normalizedPaginatedList, ...paginatedLinksToFollow)
              .pipe(take(1))
              .subscribe(() => {
                expect(linkService.resolveLinks).toHaveBeenCalledWith(paginatedList, ...linksToFollow);
                done();
              });
          });
        });
      });

      describe(`and the given list already has a page property`, () => {
        it(`should call plainObjectToInstance for each of the page objects`, (done) => {
          (service as any).buildPaginatedList(paginatedList, ...paginatedLinksToFollow)
            .pipe(take(1))
            .subscribe(() => {
              array.forEach((element) => {
                expect((service as any).plainObjectToInstance).toHaveBeenCalledWith(element);
              });
              done();
            });
        });

        it(`should call linkService.resolveLinks for each of the page objects`, (done) => {
          (service as any).buildPaginatedList(paginatedList, ...paginatedLinksToFollow)
            .pipe(take(1))
            .subscribe(() => {
              array.forEach((element) => {
                expect(linkService.resolveLinks).toHaveBeenCalledWith(element, ...linksToFollow);
              });
              done();
            });
        });

        it(`should return a new PaginatedList instance based on the given object`, (done) => {
          const listAsPlainJSObj = Object.assign({}, paginatedList);
          (service as any).buildPaginatedList(listAsPlainJSObj, ...paginatedLinksToFollow)
            .pipe(take(1))
            .subscribe((result) => {
              expect(listAsPlainJSObj).toEqual(jasmine.any(Object));
              expect(result).toEqual(jasmine.any(PaginatedList));
              expect(result).toEqual(paginatedList);
              done();
            });
        });

        describe(`when there are other links as well`, () => {
          it(`should call linkservice.resolveLinks for those other links`, (done) => {
            (service as any).buildPaginatedList(paginatedList, ...paginatedLinksToFollow)
              .pipe(take(1))
              .subscribe(() => {
                expect(linkService.resolveLinks).toHaveBeenCalledWith(paginatedList, ...linksToFollow);
                done();
              });
          });
        });
      });
    });

    describe(`when linksToFollow doesn't contain a 'page' link`, () => {
      it(`should return a new PaginatedList instance based on the given object`, (done) => {
        const listAsPlainJSObj = Object.assign({}, paginatedList);
        (service as any).buildPaginatedList(listAsPlainJSObj, ...linksToFollow)
          .pipe(take(1))
          .subscribe((result) => {
            expect(listAsPlainJSObj).toEqual(jasmine.any(Object));
            expect(result).toEqual(jasmine.any(PaginatedList));
            expect(result).toEqual(paginatedList);
            done();
          });
      });
    });
  });

  describe('buildFromHref', () => {
    beforeEach(() => {
      (objectCache.getRequestUUIDBySelfLink as jasmine.Spy).and.returnValue(cold('a', { a: 'request/uuid' }));
    });

    describe('when both getRequestFromRequestHref and getRequestFromRequestUUID emit nothing', () => {
      beforeEach(() => {
        (requestService.getByHref as jasmine.Spy).and.returnValue(cold('a', { a: undefined }));
        (requestService.getByUUID as jasmine.Spy).and.returnValue(cold('a', { a: undefined }));
      });

      it('should not emit anything', () => {
        expect(service.buildFromHref(cold('a', { a: 'rest/api/endpoint' }))).toBeObservable(cold(''));
      });
    });

    describe('when one of getRequestFromRequestHref or getRequestFromRequestUUID emits nothing', () => {
      let requestEntry: RequestEntry;

      beforeEach(() => {
        requestEntry = Object.assign(new RequestEntry(), {
          state: RequestEntryState.Success,
          request: {},
        });
        (requestService.getByHref as jasmine.Spy).and.returnValue(cold('a', { a: undefined }));
        (requestService.getByUUID as jasmine.Spy).and.returnValue(cold('a', { a: requestEntry }));
        spyOn((service as any), 'buildPayload').and.returnValue(cold('a', { a: {} }));
      });

      it('should create remote-data with the existing request-entry', () => {
        expect(service.buildFromHref(cold('a', { a: 'rest/api/endpoint' }))).toBeObservable(cold('a', {
          a: new RemoteData(undefined, undefined, undefined, RequestEntryState.Success, undefined, {}, undefined),
        }));
      });
    });

    describe('when one of getRequestFromRequestHref or getRequestFromRequestUUID is stale', () => {
      let requestEntry1: RequestEntry;
      let requestEntry2: RequestEntry;

      beforeEach(() => {
        requestEntry1 = Object.assign(new RequestEntry(), {
          state: RequestEntryState.Success,
          request: {},
        });
        requestEntry2 = Object.assign(new RequestEntry(), {
          state: RequestEntryState.SuccessStale,
          request: {},
        });
        (requestService.getByHref as jasmine.Spy).and.returnValue(cold('a', { a: requestEntry1 }));
        (requestService.getByUUID as jasmine.Spy).and.returnValue(cold('a', { a: requestEntry2 }));
        spyOn((service as any), 'buildPayload').and.returnValue(cold('a', { a: {} }));
      });

      it('should create remote-data with the non-stale request-entry', () => {
        expect(service.buildFromHref(cold('a', { a: 'rest/api/endpoint' }))).toBeObservable(cold('a', {
          a: new RemoteData(undefined, undefined, undefined, RequestEntryState.Success, undefined, {}, undefined),
        }));
      });
    });

    describe('when both getRequestFromRequestHref and getRequestFromRequestUUID are stale', () => {
      let requestEntry1: RequestEntry;
      let requestEntry2: RequestEntry;

      beforeEach(() => {
        requestEntry1 = Object.assign(new RequestEntry(), {
          state: RequestEntryState.SuccessStale,
          request: {},
          lastUpdated: 20,
        });
        requestEntry2 = Object.assign(new RequestEntry(), {
          state: RequestEntryState.SuccessStale,
          request: {},
          lastUpdated: 10,
        });
        (requestService.getByHref as jasmine.Spy).and.returnValue(cold('a', { a: requestEntry1 }));
        (requestService.getByUUID as jasmine.Spy).and.returnValue(cold('a', { a: requestEntry2 }));
        spyOn((service as any), 'buildPayload').and.returnValue(cold('a', { a: {} }));
      });

      it('should create remote-data with the most up-to-date request-entry', () => {
        expect(service.buildFromHref(cold('a', { a: 'rest/api/endpoint' }))).toBeObservable(cold('a', {
          a: new RemoteData(undefined, undefined, 20, RequestEntryState.SuccessStale, undefined, {}, undefined),
        }));
      });
    });

    describe('when both getRequestFromRequestHref and getRequestFromRequestUUID are not stale', () => {
      let requestEntry1: RequestEntry;
      let requestEntry2: RequestEntry;

      beforeEach(() => {
        requestEntry1 = Object.assign(new RequestEntry(), {
          state: RequestEntryState.Success,
          request: {},
          lastUpdated: 25,
        });
        requestEntry2 = Object.assign(new RequestEntry(), {
          state: RequestEntryState.Success,
          request: {},
          lastUpdated: 5,
        });
        (requestService.getByHref as jasmine.Spy).and.returnValue(cold('a', { a: requestEntry1 }));
        (requestService.getByUUID as jasmine.Spy).and.returnValue(cold('a', { a: requestEntry2 }));
        spyOn((service as any), 'buildPayload').and.returnValue(cold('a', { a: {} }));
      });

      it('should create remote-data with the most up-to-date request-entry', () => {
        expect(service.buildFromHref(cold('a', { a: 'rest/api/endpoint' }))).toBeObservable(cold('a', {
          a: new RemoteData(undefined, undefined, 25, RequestEntryState.Success, undefined, {}, undefined),
        }));
      });
    });
  });

  describe('buildFromRequestUUIDAndAwait', () => {
    let testScheduler;

    let callback: jasmine.Spy;
    let buildFromRequestUUIDSpy;

    const BOOLEAN = { t: true, f: false };

    const MOCK_PENDING_RD = createPendingRemoteDataObject();
    const MOCK_SUCCEEDED_RD = createSuccessfulRemoteDataObject({});
    const MOCK_FAILED_RD = createFailedRemoteDataObject('failed');

    const RDs = {
      p: MOCK_PENDING_RD,
      s: MOCK_SUCCEEDED_RD,
      f: MOCK_FAILED_RD,
    };


    beforeEach(() => {
      testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });

      callback = jasmine.createSpy('callback');
      callback.and.returnValue(observableOf(undefined));
      buildFromRequestUUIDSpy = spyOn(service, 'buildFromRequestUUID').and.callThrough();
    });

    it('should patch through href & followLinks to buildFromRequestUUID', () => {
      buildFromRequestUUIDSpy.and.returnValue(observableOf(MOCK_SUCCEEDED_RD));
      service.buildFromRequestUUIDAndAwait('some-href', callback, ...linksToFollow);
      expect(buildFromRequestUUIDSpy).toHaveBeenCalledWith('some-href', ...linksToFollow);
    });

    it('should trigger the callback on successful RD', (done) => {
      buildFromRequestUUIDSpy.and.returnValue(observableOf(MOCK_SUCCEEDED_RD));

      service.buildFromRequestUUIDAndAwait('some-href', callback).subscribe(rd => {
        expect(rd).toBe(MOCK_SUCCEEDED_RD);
        expect(callback).toHaveBeenCalled();
        done();
      });
    });

    it('should trigger the callback on successful RD even if nothing subscribes to the returned Observable', fakeAsync(() => {
      buildFromRequestUUIDSpy.and.returnValue(observableOf(MOCK_SUCCEEDED_RD));

      service.buildFromRequestUUIDAndAwait('some-href', callback);
      tick();

      expect(callback).toHaveBeenCalled();
    }));

    it('should not trigger the callback on pending RD', (done) => {
      buildFromRequestUUIDSpy.and.returnValue(observableOf(MOCK_PENDING_RD));

      service.buildFromRequestUUIDAndAwait('some-href', callback).subscribe(rd => {
        expect(rd).toBe(MOCK_PENDING_RD);
        expect(callback).not.toHaveBeenCalled();
        done();
      });
    });

    it('should not trigger the callback on failed RD', (done) => {
      buildFromRequestUUIDSpy.and.returnValue(observableOf(MOCK_FAILED_RD));

      service.buildFromRequestUUIDAndAwait('some-href', callback).subscribe(rd => {
        expect(rd).toBe(MOCK_FAILED_RD);
        expect(callback).not.toHaveBeenCalled();
        done();
      });
    });

    it('should only emit after the callback is done', () => {
      testScheduler.run(({ cold: tsCold, expectObservable }) => {
        buildFromRequestUUIDSpy.and.returnValue(
          tsCold('-p----s', RDs)
        );
        callback.and.returnValue(
          tsCold('      --t', BOOLEAN)
        );

        const done$ = service.buildFromRequestUUIDAndAwait('some-href', callback);
        expectObservable(done$).toBe(
          '       -p------s', RDs       // resulting duration between pending & successful includes the callback
        );
      });
    });
  });
});
