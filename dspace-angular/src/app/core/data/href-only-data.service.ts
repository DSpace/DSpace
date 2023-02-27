import { RequestService } from './request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { Injectable } from '@angular/core';
import { VOCABULARY_ENTRY } from '../submission/vocabularies/models/vocabularies.resource-type';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { RemoteData } from './remote-data';
import { Observable } from 'rxjs';
import { PaginatedList } from './paginated-list.model';
import { ITEM_TYPE } from '../shared/item-relationships/item-type.resource-type';
import { LICENSE } from '../shared/license.resource-type';
import { CacheableObject } from '../cache/cacheable-object.model';
import { FindListOptions } from './find-list-options.model';
import { BaseDataService } from './base/base-data.service';
import { HALDataService } from './base/hal-data-service.interface';
import { dataService } from './base/data-service.decorator';

/**
 * A DataService with only findByHref methods. Its purpose is to be used for resources that don't
 * need to be retrieved by ID, or have any way to update them, but require a DataService in order
 * for their links to be resolved by the LinkService.
 *
 * an @dataService annotation can be added for any number of these resource types
 *
 *
 * Additionally, this service may be used to retrieve objects by `href` regardless of their type
 * For example
 * ```
 * const items$: Observable<RemoteData<PaginatedList<Item>>> = hrefOnlyDataService.findListByHref<Item>(href);
 * const sites$: Observable<RemoteData<PaginatedList<Site>>> = hrefOnlyDataService.findListByHref<Site>(href);
 * ```
 * This means we cannot extend from {@link BaseDataService} directly because the method signatures would not match.
 */
@Injectable({
  providedIn: 'root',
})
@dataService(VOCABULARY_ENTRY)
@dataService(ITEM_TYPE)
@dataService(LICENSE)
export class HrefOnlyDataService implements HALDataService<any> {
  /**
   * Works with a {@link BaseDataService} internally, but only exposes two of its methods
   * with altered signatures to (optionally) constrain the arbitrary return type.
   * @private
   */
  private dataService: BaseDataService<any>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
  ) {
    this.dataService = new BaseDataService(undefined, requestService, rdbService, objectCache, halService);
  }

  /**
   * Returns an observable of {@link RemoteData} of an object, based on an href, with a list of {@link FollowLinkConfig},
   * to automatically resolve {@link HALLink}s of the object
   * @param href                        The url of object we want to retrieve
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  findByHref<T extends CacheableObject>(href: string | Observable<string>, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<T>> {
    return this.dataService.findByHref(href, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Returns a list of observables of {@link RemoteData} of objects, based on an href, with a list of {@link FollowLinkConfig},
   * to automatically resolve {@link HALLink}s of the object
   * @param href                        The url of object we want to retrieve
   * @param findListOptions             Find list options object
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  findListByHref<T extends CacheableObject>(href: string | Observable<string>, findListOptions: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<PaginatedList<T>>> {
    return this.dataService.findListByHref(href, findListOptions, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }
}
