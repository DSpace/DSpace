/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { CacheableObject } from '../../cache/cacheable-object.model';
import { BaseDataService } from './base-data.service';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { hasNoValue, isNotEmpty } from '../../../shared/empty.util';
import { FindListOptions } from '../find-list-options.model';
import { FollowLinkConfig } from '../../../shared/utils/follow-link-config.model';
import { RemoteData } from '../remote-data';
import { PaginatedList } from '../paginated-list.model';
import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';

/**
 * Shorthand type for method to construct a search endpoint
 */
export type ConstructSearchEndpoint = (href: string, searchMethod: string) => string;

/**
 * Default method to construct a search endpoint
 */
export const constructSearchEndpointDefault = (href: string, searchMethod: string): string => `${href}/search/${searchMethod}`;

/**
 * Interface for a data service that can search for objects.
 */
export interface SearchData<T extends CacheableObject> {
  /**
   * Make a new FindListRequest with given search method
   *
   * @param searchMethod                The search method for the object
   * @param options                     The [[FindListOptions]] object
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<PaginatedList<T>>}
   *    Return an observable that emits response from the server
   */
  searchBy(searchMethod: string, options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<PaginatedList<T>>>;
}

/**
 * A DataService feature to search for objects.
 *
 * Concrete data services can use this feature by implementing {@link SearchData}
 * and delegating its method to an inner instance of this class.
 */
export class SearchDataImpl<T extends CacheableObject> extends BaseDataService<T> implements SearchData<T> {
  /**
   * @param linkPath
   * @param requestService
   * @param rdbService
   * @param objectCache
   * @param halService
   * @param responseMsToLive
   * @param constructSearchEndpoint      an optional method to construct the search endpoint, passed as an argument so it can be
   *                            modified without extending this class. Defaults to `${href}/search/${searchMethod}`
   */
  constructor(
    protected linkPath: string,
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected responseMsToLive: number,
    private constructSearchEndpoint: ConstructSearchEndpoint = constructSearchEndpointDefault,
  ) {
    super(linkPath, requestService, rdbService, objectCache, halService, responseMsToLive);
    if (hasNoValue(constructSearchEndpoint)) {
      throw new Error(`SearchDataImpl initialized without a constructSearchEndpoint method (linkPath: ${linkPath})`);
    }
  }

  /**
   * Make a new FindListRequest with given search method
   *
   * @param searchMethod                The search method for the object
   * @param options                     The [[FindListOptions]] object
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<PaginatedList<T>>}
   *    Return an observable that emits response from the server
   */
  searchBy(searchMethod: string, options: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<PaginatedList<T>>> {
    const hrefObs = this.getSearchByHref(searchMethod, options, ...linksToFollow);

    return this.findListByHref(hrefObs, undefined, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Create the HREF for a specific object's search method with given options object
   *
   * @param searchMethod The search method for the object
   * @param options The [[FindListOptions]] object
   * @return {Observable<string>}
   *    Return an observable that emits created HREF
   * @param linksToFollow   List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  getSearchByHref(searchMethod: string, options: FindListOptions = {}, ...linksToFollow: FollowLinkConfig<T>[]): Observable<string> {
    let result$: Observable<string>;
    const args = [];

    result$ = this.getSearchEndpoint(searchMethod);

    return result$.pipe(map((result: string) => this.buildHrefFromFindOptions(result, options, args, ...linksToFollow)));
  }

  /**
   * Return object search endpoint by given search method
   *
   * @param searchMethod The search method for the object
   */
  private getSearchEndpoint(searchMethod: string): Observable<string> {
    return this.halService.getEndpoint(this.linkPath).pipe(
      filter((href: string) => isNotEmpty(href)),
      map(href => this.constructSearchEndpoint(href, searchMethod)),
    );
  }
}
