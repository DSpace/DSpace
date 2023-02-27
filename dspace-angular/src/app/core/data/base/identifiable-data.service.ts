/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { CacheableObject } from '../../cache/cacheable-object.model';
import { FollowLinkConfig } from '../../../shared/utils/follow-link-config.model';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { RemoteData } from '../remote-data';
import { BaseDataService } from './base-data.service';
import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';

/**
 * Shorthand type for the method to construct an ID endpoint.
 */
export type ConstructIdEndpoint = (endpoint: string, resourceID: string) => string;

/**
 * The default method to construct an ID endpoint
 */
export const constructIdEndpointDefault = (endpoint, resourceID) => `${endpoint}/${resourceID}`;

/**
 * A type of data service that deals with objects that have an ID.
 *
 * The effective endpoint to use for the ID can be adjusted by providing a different {@link ConstructIdEndpoint} method.
 * This method is passed as an argument so that it can be set on data service features without having to override them.
 */
export class IdentifiableDataService<T extends CacheableObject> extends BaseDataService<T> {
  constructor(
    protected linkPath: string,
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected responseMsToLive?: number,
    protected constructIdEndpoint: ConstructIdEndpoint = constructIdEndpointDefault,
  ) {
    super(linkPath, requestService, rdbService, objectCache, halService, responseMsToLive);
  }

  /**
   * Returns an observable of {@link RemoteData} of an object, based on its ID, with a list of
   * {@link FollowLinkConfig}, to automatically resolve {@link HALLink}s of the object
   * @param id                          ID of object we want to retrieve
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  findById(id: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<T>> {
    const href$ = this.getIDHrefObs(encodeURIComponent(id), ...linksToFollow);
    return this.findByHref(href$, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Create the HREF for a specific object based on its identifier; with possible embed query params based on linksToFollow
   * @param endpoint The base endpoint for the type of object
   * @param resourceID The identifier for the object
   * @param linksToFollow   List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  getIDHref(endpoint, resourceID, ...linksToFollow: FollowLinkConfig<T>[]): string {
    return this.buildHrefFromFindOptions(this.constructIdEndpoint(endpoint, resourceID), {}, [], ...linksToFollow);
  }

  /**
   * Create an observable for the HREF of a specific object based on its identifier
   * @param resourceID The identifier for the object
   * @param linksToFollow   List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  getIDHrefObs(resourceID: string, ...linksToFollow: FollowLinkConfig<T>[]): Observable<string> {
    return this.getEndpoint().pipe(
      map((endpoint: string) => this.getIDHref(endpoint, resourceID, ...linksToFollow)));
  }
}
