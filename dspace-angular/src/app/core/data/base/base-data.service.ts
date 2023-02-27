/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { AsyncSubject, from as observableFrom, Observable, of as observableOf } from 'rxjs';
import { map, mergeMap, skipWhile, switchMap, take, tap, toArray } from 'rxjs/operators';
import { hasValue, isNotEmpty, isNotEmptyOperator } from '../../../shared/empty.util';
import { FollowLinkConfig } from '../../../shared/utils/follow-link-config.model';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { RequestParam } from '../../cache/models/request-param.model';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { URLCombiner } from '../../url-combiner/url-combiner';
import { RemoteData } from '../remote-data';
import { GetRequest } from '../request.models';
import { RequestService } from '../request.service';
import { CacheableObject } from '../../cache/cacheable-object.model';
import { FindListOptions } from '../find-list-options.model';
import { PaginatedList } from '../paginated-list.model';
import { ObjectCacheEntry } from '../../cache/object-cache.reducer';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { HALDataService } from './hal-data-service.interface';
import { getFirstCompletedRemoteData } from '../../shared/operators';

export const EMBED_SEPARATOR = '%2F';
/**
 * Common functionality for data services.
 * Specific functionality that not all services would need
 * is implemented in "DataService feature" classes (e.g. {@link CreateData}
 *
 * All DataService (or DataService feature) classes must
 *   - extend this class (or {@link IdentifiableDataService})
 *   - implement any DataService features it requires in order to forward calls to it
 *
 * ```
 * export class SomeDataService extends BaseDataService<Something> implements CreateData<Something>, SearchData<Something> {
 *   private createData: CreateData<Something>;
 *   private searchData: SearchDataData<Something>;
 *
 *   create(...) {
 *     return this.createData.create(...);
 *   }
 *
 *   searchBy(...) {
 *     return this.searchData.searchBy(...);
 *   }
 * }
 * ```
 */
export class BaseDataService<T extends CacheableObject> implements HALDataService<T> {
  constructor(
    protected linkPath: string,
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected responseMsToLive?: number,
  ) {
  }

  /**
   * Allows subclasses to reset the response cache time.
   */

  /**
   * Get the endpoint for browsing
   * @param options The [[FindListOptions]] object
   * @param linkPath The link path for the object
   * @returns {Observable<string>}
   */
  getBrowseEndpoint(options: FindListOptions = {}, linkPath?: string): Observable<string> {
    return this.getEndpoint();
  }

  /**
   * Get the base endpoint for all requests
   */
  protected getEndpoint(): Observable<string> {
    return this.halService.getEndpoint(this.linkPath);
  }

  /**
   * Turn an options object into a query string and combine it with the given HREF
   *
   * @param href The HREF to which the query string should be appended
   * @param options The [[FindListOptions]] object
   * @param extraArgs Array with additional params to combine with query string
   * @return {Observable<string>}
   *    Return an observable that emits created HREF
   * @param linksToFollow   List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  public buildHrefFromFindOptions(href: string, options: FindListOptions, extraArgs: string[] = [], ...linksToFollow: FollowLinkConfig<T>[]): string {
    let args = [...extraArgs];

    if (hasValue(options.currentPage) && typeof options.currentPage === 'number') {
      /* TODO: this is a temporary fix for the pagination start index (0 or 1) discrepancy between the rest and the frontend respectively */
      args = this.addHrefArg(href, args, `page=${options.currentPage - 1}`);
    }
    if (hasValue(options.elementsPerPage)) {
      args = this.addHrefArg(href, args, `size=${options.elementsPerPage}`);
    }
    if (hasValue(options.sort)) {
      args = this.addHrefArg(href, args, `sort=${options.sort.field},${options.sort.direction}`);
    }
    if (hasValue(options.startsWith)) {
      args = this.addHrefArg(href, args, `startsWith=${options.startsWith}`);
    }
    if (hasValue(options.searchParams)) {
      options.searchParams.forEach((param: RequestParam) => {
        args = this.addHrefArg(href, args, `${param.fieldName}=${param.fieldValue}`);
      });
    }
    args = this.addEmbedParams(href, args, ...linksToFollow);
    if (isNotEmpty(args)) {
      return new URLCombiner(href, `?${args.join('&')}`).toString();
    } else {
      return href;
    }
  }

  /**
   * Turn an array of RequestParam into a query string and combine it with the given HREF
   *
   * @param href The HREF to which the query string should be appended
   * @param params Array with additional params to combine with query string
   * @param linksToFollow   List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   *
   * @return {Observable<string>}
   * Return an observable that emits created HREF
   */
  buildHrefWithParams(href: string, params: RequestParam[], ...linksToFollow: FollowLinkConfig<T>[]): string {

    let  args = [];
    if (hasValue(params)) {
      params.forEach((param: RequestParam) => {
        args = this.addHrefArg(href, args, `${param.fieldName}=${param.fieldValue}`);
      });
    }

    args = this.addEmbedParams(href, args, ...linksToFollow);

    if (isNotEmpty(args)) {
      return new URLCombiner(href, `?${args.join('&')}`).toString();
    } else {
      return href;
    }
  }
  /**
   * Adds the embed options to the link for the request
   * @param href            The href the params are to be added to
   * @param args            params for the query string
   * @param linksToFollow   links we want to embed in query string if shouldEmbed is true
   */
  protected addEmbedParams(href: string, args: string[], ...linksToFollow: FollowLinkConfig<T>[]) {
    linksToFollow.forEach((linkToFollow: FollowLinkConfig<T>) => {
      if (hasValue(linkToFollow) && linkToFollow.shouldEmbed) {
        const embedString = 'embed=' + String(linkToFollow.name);
        // Add the embeds size if given in the FollowLinkConfig.FindListOptions
        if (hasValue(linkToFollow.findListOptions) && hasValue(linkToFollow.findListOptions.elementsPerPage)) {
          args = this.addHrefArg(href, args,
            'embed.size=' + String(linkToFollow.name) + '=' + linkToFollow.findListOptions.elementsPerPage);
        }
        // Adds the nested embeds and their size if given
        if (isNotEmpty(linkToFollow.linksToFollow)) {
          args = this.addNestedEmbeds(embedString, href, args, ...linkToFollow.linksToFollow);
        } else {
          args = this.addHrefArg(href, args, embedString);
        }
      }
    });
    return args;
  }

  /**
   * Add a new argument to the list of arguments, only if it doesn't already exist in the given href,
   * or the current list of arguments
   *
   * @param href        The href the arguments are to be added to
   * @param currentArgs The current list of arguments
   * @param newArg      The new argument to add
   * @return            The next list of arguments, with newArg included if it wasn't already.
   *                    Note this function will not modify any of the input params.
   */
  protected addHrefArg(href: string, currentArgs: string[], newArg: string): string[] {
    if (href.includes(newArg) || currentArgs.includes(newArg)) {
      return [...currentArgs];
    } else {
      return [...currentArgs, newArg];
    }
  }

  /**
   * Add the nested followLinks to the embed param, separated by a /, and their sizes, recursively
   * @param embedString     embedString so far (recursive)
   * @param href            The href the params are to be added to
   * @param args            params for the query string
   * @param linksToFollow   links we want to embed in query string if shouldEmbed is true
   */
  protected addNestedEmbeds(embedString: string, href: string, args: string[], ...linksToFollow: FollowLinkConfig<T>[]): string[] {
    let nestEmbed = embedString;
    linksToFollow.forEach((linkToFollow: FollowLinkConfig<T>) => {
      if (hasValue(linkToFollow) && linkToFollow.shouldEmbed) {
        nestEmbed = nestEmbed + EMBED_SEPARATOR + String(linkToFollow.name);
        // Add the nested embeds size if given in the FollowLinkConfig.FindListOptions
        if (hasValue(linkToFollow.findListOptions) && hasValue(linkToFollow.findListOptions.elementsPerPage)) {
          const nestedEmbedSize = 'embed.size=' + nestEmbed.split('=')[1] + '=' + linkToFollow.findListOptions.elementsPerPage;
          args = this.addHrefArg(href, args, nestedEmbedSize);
        }
        if (hasValue(linkToFollow.linksToFollow) && isNotEmpty(linkToFollow.linksToFollow)) {
          args = this.addNestedEmbeds(nestEmbed, href, args, ...linkToFollow.linksToFollow);
        } else {
          args = this.addHrefArg(href, args, nestEmbed);
        }
      }
    });
    return args;
  }

  /**
   * An operator that will call the given function if the incoming RemoteData is stale and
   * shouldReRequest is true
   *
   * @param shouldReRequest  Whether or not to call the re-request function if the RemoteData is stale
   * @param requestFn        The function to call if the RemoteData is stale and shouldReRequest is
   *                         true
   */
  protected reRequestStaleRemoteData<O>(shouldReRequest: boolean, requestFn: () => Observable<RemoteData<O>>) {
    return (source: Observable<RemoteData<O>>): Observable<RemoteData<O>> => {
      if (shouldReRequest === true) {
        return source.pipe(
          tap((remoteData: RemoteData<O>) => {
            if (hasValue(remoteData) && remoteData.isStale) {
              requestFn();
            }
          })
        );
      } else {
        return source;
      }
    };
  }

  /**
   * Returns an observable of {@link RemoteData} of an object, based on an href, with a list of
   * {@link FollowLinkConfig}, to automatically resolve {@link HALLink}s of the object
   * @param href$                       The url of object we want to retrieve. Can be a string or
   *                                    an Observable<string>
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  findByHref(href$: string | Observable<string>, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<T>> {
    if (typeof href$ === 'string') {
      href$ = observableOf(href$);
    }

    const requestHref$ = href$.pipe(
      isNotEmptyOperator(),
      take(1),
      map((href: string) => this.buildHrefFromFindOptions(href, {}, [], ...linksToFollow)),
    );

    this.createAndSendGetRequest(requestHref$, useCachedVersionIfAvailable);

    return this.rdbService.buildSingle<T>(requestHref$, ...linksToFollow).pipe(
      // This skip ensures that if a stale object is present in the cache when you do a
      // call it isn't immediately returned, but we wait until the remote data for the new request
      // is created. If useCachedVersionIfAvailable is false it also ensures you don't get a
      // cached completed object
      skipWhile((rd: RemoteData<T>) => useCachedVersionIfAvailable ? rd.isStale : rd.hasCompleted),
      this.reRequestStaleRemoteData(reRequestOnStale, () =>
        this.findByHref(href$, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow)),
    );
  }

  /**
   * Returns an Observable of a {@link RemoteData} of a {@link PaginatedList} of objects, based on an href,
   * with a list of {@link FollowLinkConfig}, to automatically resolve {@link HALLink}s of the object
   *
   * @param href$                       The url of list we want to retrieve. Can be a string or an Observable<string>
   * @param options
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's no valid cached version.
   * @param reRequestOnStale            Whether or not the request should automatically be re-requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  findListByHref(href$: string | Observable<string>, options: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<PaginatedList<T>>> {
    if (typeof href$ === 'string') {
      href$ = observableOf(href$);
    }

    const requestHref$ = href$.pipe(
      isNotEmptyOperator(),
      take(1),
      map((href: string) => this.buildHrefFromFindOptions(href, options, [], ...linksToFollow)),
    );

    this.createAndSendGetRequest(requestHref$, useCachedVersionIfAvailable);

    return this.rdbService.buildList<T>(requestHref$, ...linksToFollow).pipe(
      // This skip ensures that if a stale object is present in the cache when you do a
      // call it isn't immediately returned, but we wait until the remote data for the new request
      // is created. If useCachedVersionIfAvailable is false it also ensures you don't get a
      // cached completed object
      skipWhile((rd: RemoteData<PaginatedList<T>>) => useCachedVersionIfAvailable ? rd.isStale : rd.hasCompleted),
      this.reRequestStaleRemoteData(reRequestOnStale, () =>
        this.findListByHref(href$, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow)),
    );
  }

  /**
   * Create a GET request for the given href, and send it.
   *
   * @param href$                       The url of object we want to retrieve. Can be a string or
   *                                    an Observable<string>
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   */
  protected createAndSendGetRequest(href$: string | Observable<string>, useCachedVersionIfAvailable = true): void {
    if (isNotEmpty(href$)) {
      if (typeof href$ === 'string') {
        href$ = observableOf(href$);
      }

      href$.pipe(
        isNotEmptyOperator(),
        take(1)
      ).subscribe((href: string) => {
        const requestId = this.requestService.generateRequestId();
        const request = new GetRequest(requestId, href);
        if (hasValue(this.responseMsToLive)) {
          request.responseMsToLive = this.responseMsToLive;
        }
        this.requestService.send(request, useCachedVersionIfAvailable);
      });
    }
  }

  /**
   * Return the links to traverse from the root of the api to the
   * endpoint this DataService represents
   *
   * e.g. if the api root links to 'foo', and the endpoint at 'foo'
   * links to 'bar' the linkPath for the BarDataService would be
   * 'foo/bar'
   */
  getLinkPath(): string {
    return this.linkPath;
  }

  /**
   *  Shorthand method to add a dependency to a cached object
   *  ```
   *  const out$ = this.findByHref(...); // or another method that sends a request
   *  this.addDependency(out$, dependsOnHref);
   *  ```
   *  When {@link dependsOnHref$} is invalidated, {@link object$} will be invalidated as well.
   *
   *
   * @param object$         the cached object
   * @param dependsOnHref$  the href of the object it should depend on
   */
  protected addDependency(object$: Observable<RemoteData<T | PaginatedList<T>>>, dependsOnHref$: string | Observable<string>) {
    this.objectCache.addDependency(
      object$.pipe(
        getFirstCompletedRemoteData(),
        switchMap((rd: RemoteData<T>) => {
          if (rd.hasSucceeded) {
            return [rd.payload._links.self.href];
          } else {
            // undefined href will be skipped in objectCache.addDependency
            return [undefined];
          }
        }),
      ),
      dependsOnHref$
    );
  }

  /**
   * Invalidate an existing DSpaceObject by marking all requests it is included in as stale
   * @param   href The self link of the object to be invalidated
   * @return  An Observable that will emit `true` once all requests are stale
   */
  invalidateByHref(href: string): Observable<boolean> {
    const done$ = new AsyncSubject<boolean>();

    this.objectCache.getByHref(href).pipe(
      take(1),
      switchMap((oce: ObjectCacheEntry) => {
        return observableFrom([
          ...oce.requestUUIDs,
          ...oce.dependentRequestUUIDs
        ]).pipe(
          mergeMap((requestUUID: string) => this.requestService.setStaleByUUID(requestUUID)),
          toArray(),
        );
      }),
    ).subscribe(() => {
      this.objectCache.removeDependents(href);
      done$.next(true);
      done$.complete();
    });

    return done$;
  }
}
