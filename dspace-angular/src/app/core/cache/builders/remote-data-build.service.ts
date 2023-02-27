import { Injectable } from '@angular/core';
import {
  AsyncSubject,
  combineLatest as observableCombineLatest,
  Observable,
  of as observableOf,
} from 'rxjs';
import { map, switchMap, filter, distinctUntilKeyChanged, startWith } from 'rxjs/operators';
import { hasValue, isEmpty, isNotEmpty, hasNoValue, isUndefined } from '../../../shared/empty.util';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { FollowLinkConfig, followLink } from '../../../shared/utils/follow-link-config.model';
import { PaginatedList } from '../../data/paginated-list.model';
import { RemoteData } from '../../data/remote-data';
import { RequestService } from '../../data/request.service';
import { ObjectCacheService } from '../object-cache.service';
import { LinkService } from './link.service';
import { HALLink } from '../../shared/hal-link.model';
import { GenericConstructor } from '../../shared/generic-constructor';
import { getClassForType } from './build-decorators';
import { HALResource } from '../../shared/hal-resource.model';
import { PAGINATED_LIST } from '../../data/paginated-list.resource-type';
import { getUrlWithoutEmbedParams } from '../../index/index.selectors';
import { getResourceTypeValueFor } from '../object-cache.reducer';
import { hasSucceeded, isStale, RequestEntryState } from '../../data/request-entry-state.model';
import { getRequestFromRequestHref, getRequestFromRequestUUID } from '../../shared/request.operators';
import { RequestEntry } from '../../data/request-entry.model';
import { ResponseState } from '../../data/response-state.model';
import { getFirstCompletedRemoteData } from '../../shared/operators';

@Injectable()
export class RemoteDataBuildService {
  constructor(protected objectCache: ObjectCacheService,
              protected linkService: LinkService,
              protected requestService: RequestService) {
  }

  /**
   * Creates an Observable<T> with the payload for a RemoteData object
   *
   * @param requestEntry$   The {@link RequestEntry} to create a {@link RemoteData} object from
   * @param href$           The self link of the object to retrieve. If left empty, the root
   *                        payload link from the response will be used. These links will differ in
   *                        case we're retrieving an object that was embedded in the request for
   *                        another
   * @param linksToFollow   List of {@link FollowLinkConfig} that indicate which {@link HALLink}s
   *                        should be automatically resolved
   * @private
   */
  private buildPayload<T>(requestEntry$: Observable<RequestEntry>, href$?: Observable<string>, ...linksToFollow: FollowLinkConfig<any>[]): Observable<T> {
    if (hasNoValue(href$)) {
      href$ = observableOf(undefined);
    }
    return observableCombineLatest([href$, requestEntry$]).pipe(
      switchMap(([href, entry]: [string, RequestEntry]) => {
        const hasExactMatchInObjectCache = this.hasExactMatchInObjectCache(href, entry);
        if (hasValue(entry.response) &&
          (hasExactMatchInObjectCache || this.isCacheablePayload(entry) || this.isUnCacheablePayload(entry))) {
          if (hasExactMatchInObjectCache) {
            return this.objectCache.getObjectByHref(href);
          } else if (this.isCacheablePayload(entry)) {
            return this.objectCache.getObjectByHref(entry.response.payloadLink.href);
          } else {
            return [this.plainObjectToInstance<T>(entry.response.unCacheableObject)];
          }
        } else if (hasSucceeded(entry.state)) {
          return [null];
        } else {
          return [undefined];
        }
      }),
      switchMap((obj: T) => {
        if (hasValue(obj)) {
          if (getResourceTypeValueFor((obj as any).type) === PAGINATED_LIST.value) {
            return this.buildPaginatedList<T>(obj, ...linksToFollow);
          } else if (isNotEmpty(linksToFollow)) {
            return [this.linkService.resolveLinks(obj, ...linksToFollow)];
          }
        }
        return [obj];
      })
    );
  }

  /**
   * When an object is returned from the store, it's possibly a plain javascript object (in case
   * it was first instantiated on the server). This method will turn it in to an instance of the
   * class corresponding with its type property. If it doesn't have one, or we can't find a
   * constructor for that type, it will remain a plain object.
   *
   * @param obj  The object to turn in to a class instance based on its type property
   */
  private plainObjectToInstance<T>(obj: any): T {
    const type: GenericConstructor<T> = getClassForType(obj.type);
    if (typeof type === 'function') {
      return Object.assign(new type(), obj) as T;
    } else {
      return Object.assign({}, obj) as T;
    }
  }

  /**
   * Returns true if there is a match for the given self link and request entry in the object cache,
   * false otherwise. The goal is to find objects that were not the root object of the request, but
   * embedded.
   *
   * @param href the self link to check
   * @param entry the request entry the object has to match
   * @private
   */
  private hasExactMatchInObjectCache(href: string, entry: RequestEntry): boolean {
    return hasValue(entry) && hasValue(entry.request) && isNotEmpty(entry.request.uuid) &&
      hasValue(href) && this.objectCache.hasByHref(href, entry.request.uuid);
  }

  /**
   * Returns true if the given entry has a valid payloadLink, false otherwise
   * @param entry the RequestEntry to check
   * @private
   */
  private isCacheablePayload(entry: RequestEntry): boolean {
    return hasValue(entry.response.payloadLink) && isNotEmpty(entry.response.payloadLink.href);
  }

  /**
   * Returns true if the given entry has an unCacheableObject, false otherwise
   * @param entry the RequestEntry to check
   * @private
   */
  private isUnCacheablePayload(entry: RequestEntry): boolean {
    return hasValue(entry.response.unCacheableObject);
  }

  /**
   * Build a PaginatedList by creating a new PaginatedList instance from the given object, to ensure
   * it has the correct prototype (you can't be sure if it came from the ngrx store), by
   * retrieving the objects in the list and following any links.
   *
   * @param object          A plain object to be turned in to a {@link PaginatedList}
   * @param linksToFollow   List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  private buildPaginatedList<T>(object: any, ...linksToFollow: FollowLinkConfig<any>[]): Observable<T> {
    const pageLink = linksToFollow.find((linkToFollow: FollowLinkConfig<any>) => linkToFollow.name === 'page');
    const otherLinks = linksToFollow.filter((linkToFollow: FollowLinkConfig<any>) => linkToFollow.name !== 'page');

    const paginatedList = Object.assign(new PaginatedList(), object);

    if (hasValue(pageLink)) {
      if (isEmpty(paginatedList.page)) {
        const pageSelfLinks = paginatedList._links.page.map((link: HALLink) => link.href);
        return this.objectCache.getList(pageSelfLinks).pipe(map((page: any[]) => {
          paginatedList.page = page
            .map((obj: any) => this.plainObjectToInstance<T>(obj))
            .map((obj: any) =>
              this.linkService.resolveLinks(obj, ...pageLink.linksToFollow)
            );
          if (isNotEmpty(otherLinks)) {
            return this.linkService.resolveLinks(paginatedList, ...otherLinks);
          }
          return paginatedList;
        }));
      } else {
        // in case the elements of the paginated list were already filled in, because they're UnCacheableObjects
        paginatedList.page = paginatedList.page
          .map((obj: any) => this.plainObjectToInstance<T>(obj))
          .map((obj: any) =>
            this.linkService.resolveLinks(obj, ...pageLink.linksToFollow)
          );
        if (isNotEmpty(otherLinks)) {
          return observableOf(this.linkService.resolveLinks(paginatedList, ...otherLinks));
        }
      }
    }
    return observableOf(paginatedList as any);
  }

  /**
   * Creates a {@link RemoteData} object for a rest request and its response
   *
   * @param requestUUID$      The UUID of the request we want to retrieve
   * @param linksToFollow     List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  buildFromRequestUUID<T>(requestUUID$: string | Observable<string>, ...linksToFollow: FollowLinkConfig<any>[]): Observable<RemoteData<T>> {
    if (typeof requestUUID$ === 'string') {
      requestUUID$ = observableOf(requestUUID$);
    }
    const requestEntry$ = requestUUID$.pipe(getRequestFromRequestUUID(this.requestService));

    const payload$ = this.buildPayload<T>(requestEntry$, undefined, ...linksToFollow);

    return this.toRemoteDataObservable<T>(requestEntry$, payload$);
  }

  /**
   * Creates a {@link RemoteData} object for a rest request and its response
   * and emits it only after the callback function is completed.
   *
   * @param requestUUID$    The UUID of the request we want to retrieve
   * @param callback        A function that returns an Observable. It will only be called once the request has succeeded.
   *                        Then, the response will only be emitted after this callback function has emitted.
   * @param linksToFollow   List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  buildFromRequestUUIDAndAwait<T>(requestUUID$: string | Observable<string>, callback: (rd?: RemoteData<T>) => Observable<unknown>, ...linksToFollow: FollowLinkConfig<any>[]): Observable<RemoteData<T>> {
    const response$ = this.buildFromRequestUUID(requestUUID$, ...linksToFollow);

    const callbackDone$ = new AsyncSubject<boolean>();
    response$.pipe(
      getFirstCompletedRemoteData(),
      switchMap((rd: RemoteData<any>) => {
        if (rd.hasSucceeded) {
          // if the request succeeded, execute the callback
          return callback(rd);
        } else {
          // otherwise, emit right away so the subscription doesn't stick around
          return [true];
        }
      }),
    ).subscribe(() => {
      callbackDone$.next(true);
      callbackDone$.complete();
    });

    return response$.pipe(
      switchMap((rd: RemoteData<any>) => {
        if (rd.hasSucceeded) {
          // if the request succeeded, wait for the callback to finish
          return callbackDone$.pipe(
            map(() => rd),
          );
        } else {
          return [rd];
        }
      })
    );
  }

  /**
   * Creates a {@link RemoteData} object for a rest request and its response
   *
   * @param href$             self link of object we want to retrieve
   * @param linksToFollow     List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  buildFromHref<T>(href$: string | Observable<string>, ...linksToFollow: FollowLinkConfig<any>[]): Observable<RemoteData<T>> {
    if (typeof href$ === 'string') {
      href$ = observableOf(href$);
    }

    href$ = href$.pipe(map((href: string) => getUrlWithoutEmbedParams(href)));

    const requestUUID$ = href$.pipe(
      switchMap((href: string) =>
        this.objectCache.getRequestUUIDBySelfLink(href)),
    );

    const requestEntry$ = observableCombineLatest([
      href$.pipe(getRequestFromRequestHref(this.requestService), startWith(undefined)),
      requestUUID$.pipe(getRequestFromRequestUUID(this.requestService), startWith(undefined)),
    ]).pipe(
      filter(([r1, r2]) => hasValue(r1) || hasValue(r2)),
      map(([r1, r2]) => {
        // If one of the two requests has no value, return the other (both is impossible due to the filter above)
        if (hasNoValue(r2)) {
          return r1;
        } else if (hasNoValue(r1)) {
          return r2;
        }

        if ((isStale(r1.state) && isStale(r2.state)) || (!isStale(r1.state) && !isStale(r2.state))) {
          // Neither or both are stale, pick the most recent request
          return r1.lastUpdated >= r2.lastUpdated ? r1 : r2;
        } else {
          // One of the two is stale, return the not stale request
          return isStale(r2.state) ? r1 : r2;
        }
      }),
      distinctUntilKeyChanged('lastUpdated')
    );

    const payload$ = this.buildPayload<T>(requestEntry$, href$, ...linksToFollow);

    return this.toRemoteDataObservable<T>(requestEntry$, payload$);
  }

  /**
   * Creates a single {@link RemoteData} object based on the response of a request to the REST server, with a list of
   * {@link FollowLinkConfig} that indicate which embedded info should be added to the object
   * @param href$             Observable href of object we want to retrieve
   * @param linksToFollow     List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  buildSingle<T>(href$: string | Observable<string>, ...linksToFollow: FollowLinkConfig<any>[]): Observable<RemoteData<T>> {
    return this.buildFromHref(href$, ...linksToFollow);
  }

  toRemoteDataObservable<T>(requestEntry$: Observable<RequestEntry>, payload$: Observable<T>) {
    return observableCombineLatest([
      requestEntry$,
      payload$
    ]).pipe(
      filter(([entry,payload]: [RequestEntry, T]) =>
        hasValue(entry) &&
        // filter out cases where the state is successful, but the payload isn't yet set
        !(hasSucceeded(entry.state) && isUndefined(payload))
      ),
      map(([entry, payload]: [RequestEntry, T]) => {
        let response = entry.response;
        if (hasNoValue(response)) {
          response = {} as ResponseState;
        }

        return new RemoteData(
          response.timeCompleted,
          entry.request.responseMsToLive,
          entry.lastUpdated,
          entry.state,
          response.errorMessage,
          payload,
          response.statusCode
        );
      })
    );
  }

  /**
   * Creates a list of {@link RemoteData} objects based on the response of a request to the REST server, with a list of
   *
   * Note: T extends HALResource not CacheableObject, because a PaginatedList is a CacheableObject in and of itself
   *
   * {@link FollowLinkConfig} that indicate which embedded info should be added to the objects
   * @param href$             Observable href of objects we want to retrieve
   * @param linksToFollow     List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  buildList<T extends HALResource>(href$: string | Observable<string>, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<PaginatedList<T>>> {
    return this.buildFromHref<PaginatedList<T>>(href$, followLink('page', { shouldEmbed: false }, ...linksToFollow));
  }

  /**
   * Turns an array of RemoteData observables in to an observable RemoteData[]
   *
   * By doing this you lose most of the info about the status of the original
   * RemoteData objects, as you have to squash them down in to one. So use
   * this only if the list you need isn't available on the REST API. If you
   * need to use it, it's likely an indication that a REST endpoint is missing
   *
   * @param input     the array of RemoteData observables to start from
   */
  aggregate<T>(input: Observable<RemoteData<T>>[]): Observable<RemoteData<T[]>> {

    if (isEmpty(input)) {
      return createSuccessfulRemoteDataObject$([], new Date().getTime());
    }

    return observableCombineLatest(input).pipe(
      map((arr) => {
        const timeCompleted = arr
          .map((d: RemoteData<T>) => d.timeCompleted)
          .reduce((max: number, current: number) => current > max ? current : max);

        const msToLive = arr
          .map((d: RemoteData<T>) => d.msToLive)
          .reduce((min: number, current: number) => current < min ? current : min);

        const lastUpdated = arr
          .map((d: RemoteData<T>) => d.lastUpdated)
          .reduce((max: number, current: number) => current > max ? current : max);

        let state: RequestEntryState;
        if (arr.some((d: RemoteData<T>) => d.isRequestPending)) {
          state = RequestEntryState.RequestPending;
        } else if (arr.some((d: RemoteData<T>) => d.isResponsePending)) {
          state = RequestEntryState.ResponsePending;
        } else if (arr.some((d: RemoteData<T>) => d.isErrorStale)) {
          state = RequestEntryState.ErrorStale;
        } else if (arr.some((d: RemoteData<T>) => d.isError)) {
          state = RequestEntryState.Error;
        } else if (arr.some((d: RemoteData<T>) => d.isSuccessStale)) {
          state = RequestEntryState.SuccessStale;
        } else {
          state = RequestEntryState.Success;
        }

        const errorMessage: string = arr
          .map((d: RemoteData<T>) => d.errorMessage)
          .map((e: string, idx: number) => {
            if (hasValue(e)) {
              return `[${idx}]: ${e}`;
            }
          }).filter((e: string) => hasValue(e))
          .join(', ');

        const statusCodes = new Set(arr
          .map((d: RemoteData<T>) => d.statusCode));

        let statusCode: number;

        if (statusCodes.size === 1) {
          statusCode = statusCodes.values().next().value;
        } else if (statusCodes.size > 1) {
          statusCode = 207;
        }

        const payload: T[] = arr.map((d: RemoteData<T>) => d.payload);

        return new RemoteData(
          timeCompleted,
          msToLive,
          lastUpdated,
          state,
          errorMessage,
          payload,
          statusCode
        );
      }));
  }
}
