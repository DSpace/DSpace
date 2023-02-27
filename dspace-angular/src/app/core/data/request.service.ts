import { Injectable } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';

import { createSelector, MemoizedSelector, select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { filter, map, take, tap } from 'rxjs/operators';
import cloneDeep from 'lodash/cloneDeep';
import { hasValue, isEmpty, isNotEmpty, hasNoValue } from '../../shared/empty.util';
import { ObjectCacheEntry } from '../cache/object-cache.reducer';
import { ObjectCacheService } from '../cache/object-cache.service';
import { IndexState, MetaIndexState } from '../index/index.reducer';
import { requestIndexSelector, getUrlWithoutEmbedParams } from '../index/index.selectors';
import { UUIDService } from '../shared/uuid.service';
import {
  RequestConfigureAction,
  RequestExecuteAction,
  RequestStaleAction
} from './request.actions';
import { GetRequest} from './request.models';
import { CommitSSBAction } from '../cache/server-sync-buffer.actions';
import { RestRequestMethod } from './rest-request-method';
import { coreSelector } from '../core.selectors';
import { isLoading, isStale } from './request-entry-state.model';
import { RestRequest } from './rest-request.model';
import { CoreState } from '../core-state.model';
import { RequestState } from './request-state.model';
import { RequestEntry } from './request-entry.model';

/**
 * The base selector function to select the request state in the store
 */
const requestCacheSelector = createSelector(
  coreSelector,
  (state: CoreState) => state['data/request']
);

/**
 * Selector function to select a request entry by uuid from the store
 * @param uuid The uuid of the request
 */
const entryFromUUIDSelector = (uuid: string): MemoizedSelector<CoreState, RequestEntry> => createSelector(
  requestCacheSelector,
  (state: RequestState) => {
    return hasValue(state) ? state[uuid] : undefined;
  }
);

/**
 * Selector function to select a request entry by href from the store
 * @param href The href of the request
 */
const entryFromHrefSelector = (href: string): MemoizedSelector<CoreState, RequestEntry> => createSelector(
  requestIndexSelector,
  requestCacheSelector,
  (indexState: IndexState, requestState: RequestState) => {
    let uuid: any;
    if (hasValue(indexState)) {
      uuid = indexState[getUrlWithoutEmbedParams(href)];
    } else {
      return undefined;
    }

    if (hasValue(requestState)) {
      return requestState[uuid];
    } else {
      return undefined;
    }
  }
);

/**
 * Create a selector that fetches a list of request UUIDs from a given index substate of which the request href
 * contains a given substring
 * @param selector    MemoizedSelector to start from
 * @param href        Substring that the request's href should contain
 */
const uuidsFromHrefSubstringSelector =
  (selector: MemoizedSelector<CoreState, IndexState>, href: string): MemoizedSelector<CoreState, string[]> => createSelector(
    selector,
    (state: IndexState) => getUuidsFromHrefSubstring(state, href)
  );

/**
 * Fetch a list of request UUIDs from a given index substate of which the request href contains a given substring
 * @param state   The IndexState
 * @param href    Substring that the request's href should contain
 */
const getUuidsFromHrefSubstring = (state: IndexState, href: string): string[] => {
  let result = [];
  if (isNotEmpty(state)) {
    result = Object.keys(state).filter((key) => key.includes(href)).map((key) => state[key]);
  }
  return result;
};

/**
 * Check whether a cached entry exists and isn't stale
 *
 * @param entry
 *    the entry to check
 * @return boolean
 *    false if the entry has no value, or its time to live has exceeded,
 *    true otherwise
 */
const isValid = (entry: RequestEntry): boolean => {
  if (hasNoValue(entry)) {
    // undefined entries are invalid
    return false;
  } else {
    if (isLoading(entry.state)) {
      // entries that are still loading are always valid
      return true;
    } else {
      if (isStale(entry.state)) {
        // entries that are stale are always invalid
        return false;
      } else {
        // check whether it should be stale
        const timeOutdated = entry.response.timeCompleted + entry.request.responseMsToLive;
        const now = new Date().getTime();
        const isOutDated = now > timeOutdated;
        return !isOutDated;
      }
    }
  }
};

/**
 * A service to interact with the request state in the store
 */
@Injectable({
  providedIn: 'root'
})
export class RequestService {
  private requestsOnTheirWayToTheStore: string[] = [];

  constructor(private objectCache: ObjectCacheService,
              private uuidService: UUIDService,
              private store: Store<CoreState>,
              private indexStore: Store<MetaIndexState>) {
  }

  generateRequestId(): string {
    return `client/${this.uuidService.generate()}`;
  }

  /**
   * Check if a GET request is currently pending
   */
  isPending(request: RestRequest): boolean {
    // If the request is not a GET request, it will never be considered pending, because you may
    // want to execute the exact same e.g. POST multiple times
    if (request.method !== RestRequestMethod.GET) {
      return false;
    }

    // check requests that haven't made it to the store yet
    if (this.requestsOnTheirWayToTheStore.includes(request.href)) {
      return true;
    }

    // then check the store
    let isPending = false;
    this.getByHref(request.href).pipe(
      take(1))
      .subscribe((re: RequestEntry) => {
        isPending = (hasValue(re) && isLoading(re.state));
      });
    return isPending;
  }

  /**
   * Retrieve a RequestEntry based on their uuid
   */
  getByUUID(uuid: string): Observable<RequestEntry> {
    return this.store.pipe(
      select(entryFromUUIDSelector(uuid)),
      this.fixRequestHeaders(),
      this.checkStale()
    );
  }

  /**
   * Operator that turns the request headers back in to an HttpHeaders instance after an entry has
   * been retrieved from the ngrx store
   * @private
   */
  private fixRequestHeaders() {
    return (source: Observable<RequestEntry>): Observable<RequestEntry> => {
      return source.pipe(map((entry: RequestEntry) => {
          // Headers break after being retrieved from the store (because of lazy initialization)
          // Combining them with a new object fixes this issue
          if (hasValue(entry) && hasValue(entry.request) && hasValue(entry.request.options) && hasValue(entry.request.options.headers)) {
            entry = cloneDeep(entry);
            entry.request.options.headers = Object.assign(new HttpHeaders(), entry.request.options.headers);
          }
          return entry;
        })
      );
    };
  }

  /**
   * Operator that will check if an entry should be stale, and will dispatch an action to set it to
   * stale if it should
   * @private
   */
  private checkStale() {
    return (source: Observable<RequestEntry>): Observable<RequestEntry> => {
      return source.pipe(
        tap((entry: RequestEntry) => {
          if (hasValue(entry) && hasValue(entry.request) && !isStale(entry.state) && !isValid(entry)) {
            this.store.dispatch(new RequestStaleAction(entry.request.uuid));
          }
        })
      );
    };
  }

  /**
   * Retrieve a RequestEntry based on its href
   */
  getByHref(href: string): Observable<RequestEntry> {
    return this.store.pipe(
      select(entryFromHrefSelector(href)),
      this.fixRequestHeaders(),
      this.checkStale()
    );
  }

  /**
   * Add the given request to the ngrx store, and send it to the rest api
   *
   * @param request                       The request to send out
   * @param useCachedVersionIfAvailable   If this is true, the request will only be sent if there's
   *                                      no valid cached version. Defaults to false
   * @returns true if the request was sent, false otherwise
   */
  send(request: RestRequest, useCachedVersionIfAvailable = false): boolean {
    if (useCachedVersionIfAvailable && request.method !== RestRequestMethod.GET) {
      console.warn(`${JSON.stringify(request, null, 2)} is not a GET request. In general only GET requests should reuse cached data.`);
    }

    if (this.shouldDispatchRequest(request, useCachedVersionIfAvailable)) {
      this.dispatchRequest(request);
      if (request.method === RestRequestMethod.GET) {
        this.trackRequestsOnTheirWayToTheStore(request as GetRequest);
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Convert request Payload to a URL-encoded string
   *
   * e.g.  uriEncodeBody({param: value, param1: value1, param2: [value3, value4]})
   * returns: param=value&param1=value1&param2=value3&param2=value4
   *
   * @param body
   *    The request Payload to convert
   * @return string
   *    URL-encoded string
   */
  public uriEncodeBody(body: any) {
    let queryParams = '';
    if (isNotEmpty(body) && typeof body === 'object') {
      Object.keys(body)
        .forEach((param: string) => {
          const encodedParam = encodeURIComponent(param);
          if (Array.isArray(body[param])) {
            for (const element of body[param]) {
              const encodedBody = encodeURIComponent(element);
              const paramValue = `${encodedParam}=${encodedBody}`;
              queryParams = isEmpty(queryParams) ? queryParams.concat(paramValue) : queryParams.concat('&', paramValue);
            }
          } else {
            const encodedBody = encodeURIComponent(body[param]);
            const paramValue = `${encodedParam}=${encodedBody}`;
            queryParams = isEmpty(queryParams) ? queryParams.concat(paramValue) : queryParams.concat('&', paramValue);
          }
        });
    }
    return queryParams;
  }

  /**
   * Set all requests that match (part of) the href to stale
   *
   * @param href    A substring of the request(s) href
   * @return        Returns an observable emitting whether or not the cache is removed
   * @deprecated    use setStaleByHrefSubstring instead
   */
  removeByHrefSubstring(href: string): Observable<boolean> {
    return this.setStaleByHrefSubstring(href);
  }

  /**
   * Set all requests that match (part of) the href to stale
   *
   * @param href    A substring of the request(s) href
   * @return        Returns an observable emitting whether or not the cache is removed
   */
  setStaleByHrefSubstring(href: string): Observable<boolean> {
    this.store.pipe(
      select(uuidsFromHrefSubstringSelector(requestIndexSelector, href)),
      take(1)
    ).subscribe((uuids: string[]) => {
      for (const uuid of uuids) {
        this.store.dispatch(new RequestStaleAction(uuid));
      }
    });
    this.requestsOnTheirWayToTheStore = this.requestsOnTheirWayToTheStore.filter((reqHref: string) => reqHref.indexOf(href) < 0);

    return this.store.pipe(
      select(uuidsFromHrefSubstringSelector(requestIndexSelector, href)),
      map((uuids) => isEmpty(uuids))
    );
  }

  /**
   * Mark a request as stale
   * @param uuid  the UUID of the request
   * @return      an Observable that will emit true once the Request becomes stale
   */
  setStaleByUUID(uuid: string): Observable<boolean> {
    this.store.dispatch(new RequestStaleAction(uuid));

    return this.getByUUID(uuid).pipe(
      map((request: RequestEntry) => isStale(request.state)),
      filter((stale: boolean) => stale),
      take(1),
    );
  }

  /**
   * Check if a GET request is in the cache or if it's still pending
   * @param {GetRequest} request The request to check
   * @param {boolean} useCachedVersionIfAvailable Whether or not to allow the use of a cached version
   * @returns {boolean} True if the request is cached or still pending
   */
  public shouldDispatchRequest(request: RestRequest, useCachedVersionIfAvailable: boolean): boolean {
    // if it's not a GET request
    if (request.method !== RestRequestMethod.GET) {
      return true;
    // if it is a GET request, check it isn't pending
    } else if (this.isPending(request)) {
      return false;
    // if it is pending, check if we're allowed to use a cached version
    } else if (!useCachedVersionIfAvailable) {
      return true;
    } else {
      // if we are, check the request cache
      const urlWithoutEmbedParams = getUrlWithoutEmbedParams(request.href);
      if (this.hasByHref(urlWithoutEmbedParams) === true) {
        return false;
      } else {
        // if it isn't in the request cache, check the object cache
        let inObjCache = false;
        this.objectCache.getByHref(urlWithoutEmbedParams)
          .subscribe((entry: ObjectCacheEntry) => {
            // if the object cache has a match, check if the request that the object came with is
            // still valid
            inObjCache = this.hasByUUID(entry.requestUUIDs[0]);
          }).unsubscribe();

        // we should send the request if it isn't cached
        return !inObjCache;
      }
    }
  }

  /**
   * Configure and execute the request
   * @param {RestRequest} request to dispatch
   */
  private dispatchRequest(request: RestRequest) {
    this.store.dispatch(new RequestConfigureAction(request));
    this.store.dispatch(new RequestExecuteAction(request.uuid));
  }

  /**
   * ngrx action dispatches are asynchronous. But this.isPending needs to return true as soon as the
   * send method for a GET request has been executed, otherwise certain requests will happen multiple times.
   *
   * This method will store the href of every GET request that gets configured in a local variable, and
   * remove it as soon as it can be found in the store.
   */
  private trackRequestsOnTheirWayToTheStore(request: GetRequest) {
    this.requestsOnTheirWayToTheStore = [...this.requestsOnTheirWayToTheStore, request.href];
    this.getByHref(request.href).pipe(
      filter((re: RequestEntry) => hasValue(re) && hasValue(re.request) && re.request.uuid === request.uuid),
      take(1)
    ).subscribe((re: RequestEntry) => {
      this.requestsOnTheirWayToTheStore = this.requestsOnTheirWayToTheStore.filter((pendingHref: string) => pendingHref !== request.href);
    });
  }

  /**
   * Dispatch commit action to send all changes (for a certain method) to the server (buffer)
   * @param {RestRequestMethod} method RestRequestMethod for which the changes should be committed
   */
  commit(method?: RestRequestMethod) {
    this.store.dispatch(new CommitSSBAction(method));
  }

  /**
   * Check whether the request with the specified href is cached
   *
   * @param href
   *    The link of the request to check
   * @param checkValidity
   *    Whether or not to check the validity of an entry if one is found
   * @return boolean
   *    true if a request with the specified href is cached and is valid (if checkValidity is true)
   *    false otherwise
   */
  hasByHref(href: string, checkValidity = true): boolean {
    let result = false;
    /* NB: that this is only a solution because the select method is synchronous, see: https://github.com/ngrx/store/issues/296#issuecomment-269032571*/
    this.hasByHref$(href, checkValidity)
      .subscribe((hasByHref: boolean) => result = hasByHref)
      .unsubscribe();
    return result;
  }

  /**
   * Check whether the request with the specified href is cached
   *
   * @param href
   *    The href of the request to check
   * @param checkValidity
   *    Whether or not to check the validity of an entry if one is found
   * @return Observable<boolean>
   *    true if a request with the specified href is cached and is valid (if checkValidity is true)
   *    false otherwise
   */
  hasByHref$(href: string, checkValidity = true): Observable<boolean> {
    return this.getByHref(href).pipe(
      map((requestEntry: RequestEntry) => checkValidity ? isValid(requestEntry) : hasValue(requestEntry))
    );
  }

  /**
   * Check whether the request with the specified uuid is cached
   *
   * @param uuid
   *    The link of the request to check
   * @param checkValidity
   *    Whether or not to check the validity of an entry if one is found
   * @return boolean
   *    true if a request with the specified uuid is cached and is valid (if checkValidity is true)
   *    false otherwise
   */
  hasByUUID(uuid: string, checkValidity = true): boolean {
    let result = false;
    /* NB: that this is only a solution because the select method is synchronous, see: https://github.com/ngrx/store/issues/296#issuecomment-269032571*/
    this.hasByUUID$(uuid, checkValidity)
      .subscribe((hasByUUID: boolean) => result = hasByUUID)
      .unsubscribe();
    return result;
  }

  /**
   * Check whether the request with the specified uuid is cached
   *
   * @param uuid
   *    The uuid of the request to check
   * @param checkValidity
   *    Whether or not to check the validity of an entry if one is found
   * @return Observable<boolean>
   *    true if a request with the specified uuid is cached and is valid (if checkValidity is true)
   *    false otherwise
   */
  hasByUUID$(uuid: string, checkValidity = true): Observable<boolean> {
    return this.getByUUID(uuid).pipe(
      map((requestEntry: RequestEntry) => checkValidity ? isValid(requestEntry) : hasValue(requestEntry))
    );
  }

}
