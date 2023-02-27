/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { Observable } from 'rxjs';
import { FollowLinkConfig } from '../../../shared/utils/follow-link-config.model';
import { RemoteData } from '../remote-data';
import { FindListOptions } from '../find-list-options.model';
import { PaginatedList } from '../paginated-list.model';
import { HALResource } from '../../shared/hal-resource.model';

/**
 * An interface defining the minimum functionality needed for a data service to resolve HAL resources.
 */
export interface HALDataService<T extends HALResource> {
  /**
   * Returns an Observable of {@link RemoteData} of an object, based on an href,
   * with a list of {@link FollowLinkConfig}, to automatically resolve {@link HALLink}s of the object
   *
   * @param href$                       The url of object we want to retrieve. Can be a string or an Observable<string>
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's no valid cached version.
   * @param reRequestOnStale            Whether or not the request should automatically be re-requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  findByHref(href$: string | Observable<string>, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<T>>;

  /**
   * Returns an Observable of a {@link RemoteData} of a {@link PaginatedList} of objects, based on an href,
   * with a list of {@link FollowLinkConfig}, to automatically resolve {@link HALLink}s of the object
   *
   * @param href$                       The url of list we want to retrieve. Can be a string or an Observable<string>
   * @param findListOptions             The options for to use for this find list request.
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's no valid cached version.
   * @param reRequestOnStale            Whether or not the request should automatically be re-requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  findListByHref(href$: string | Observable<string>, findListOptions?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<PaginatedList<T>>>;
}
