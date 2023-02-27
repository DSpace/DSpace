import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, switchMap, take } from 'rxjs/operators';
import { hasValue } from '../../shared/empty.util';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { Bundle } from '../shared/bundle.model';
import { BUNDLE } from '../shared/bundle.resource-type';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { Item } from '../shared/item.model';
import { PaginatedList } from './paginated-list.model';
import { RemoteData } from './remote-data';
import { GetRequest } from './request.models';
import { RequestService } from './request.service';
import { PaginatedSearchOptions } from '../../shared/search/models/paginated-search-options.model';
import { Bitstream } from '../shared/bitstream.model';
import { RequestEntryState } from './request-entry-state.model';
import { FindListOptions } from './find-list-options.model';
import { IdentifiableDataService } from './base/identifiable-data.service';
import { PatchData, PatchDataImpl } from './base/patch-data';
import { DSOChangeAnalyzer } from './dso-change-analyzer.service';
import { RestRequestMethod } from './rest-request-method';
import { Operation } from 'fast-json-patch';
import { dataService } from './base/data-service.decorator';

/**
 * A service to retrieve {@link Bundle}s from the REST API
 */
@Injectable(
  { providedIn: 'root' },
)
@dataService(BUNDLE)
export class BundleDataService extends IdentifiableDataService<Bundle> implements PatchData<Bundle> {
  private bitstreamsEndpoint = 'bitstreams';

  private patchData: PatchDataImpl<Bundle>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected comparator: DSOChangeAnalyzer<Bundle>,
  ) {
    super('bundles', requestService, rdbService, objectCache, halService);

    this.patchData = new PatchDataImpl<Bundle>(this.linkPath, requestService, rdbService, objectCache, halService, comparator, this.responseMsToLive, this.constructIdEndpoint);
  }

  /**
   * Retrieve all {@link Bundle}s in the given {@link Item}
   *
   * @param item                        the {@link Item} the {@link Bundle}s are a part of
   * @param options                     the {@link FindListOptions} for the request
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  findAllByItem(item: Item, options?: FindListOptions, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Bundle>[]): Observable<RemoteData<PaginatedList<Bundle>>> {
    return this.findListByHref(item._links.bundles.href, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Retrieve a {@link Bundle} in the given {@link Item} by name
   *
   * @param item                        the {@link Item} the {@link Bundle}s are a part of
   * @param bundleName                  the name of the {@link Bundle} to retrieve
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  // TODO should be implemented rest side
  findByItemAndName(item: Item, bundleName: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Bundle>[]): Observable<RemoteData<Bundle>> {
    return this.findAllByItem(item, { elementsPerPage: 9999 }, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow).pipe(
      map((rd: RemoteData<PaginatedList<Bundle>>) => {
        if (hasValue(rd.payload) && hasValue(rd.payload.page)) {
          const matchingBundle = rd.payload.page.find((bundle: Bundle) =>
            bundle.name === bundleName);
          if (hasValue(matchingBundle)) {
            return new RemoteData(
              rd.timeCompleted,
              rd.msToLive,
              rd.lastUpdated,
              RequestEntryState.Success,
              null,
              matchingBundle,
              200
            );
          } else {
            return new RemoteData(
              rd.timeCompleted,
              rd.msToLive,
              rd.lastUpdated,
              RequestEntryState.Error,
              `The bundle with name ${bundleName} was not found.`,
              null,
              404
            );
          }
        } else {
          return rd as any;
        }
      }),
    );
  }

  /**
   * Get the bitstreams endpoint for a bundle
   * @param bundleId
   * @param searchOptions
   */
  getBitstreamsEndpoint(bundleId: string, searchOptions?: PaginatedSearchOptions): Observable<string> {
    return this.getBrowseEndpoint().pipe(
      switchMap((href: string) => this.halService.getEndpoint(this.bitstreamsEndpoint, `${href}/${bundleId}`)),
      map((href) => searchOptions ? searchOptions.toRestUrl(href) : href)
    );
  }

  /**
   * Get a bundle's bitstreams using paginated search options
   * @param bundleId        The bundle's ID
   * @param searchOptions   The search options to use
   * @param linksToFollow   The {@link FollowLinkConfig}s for the request
   */
  getBitstreams(bundleId: string, searchOptions?: PaginatedSearchOptions, ...linksToFollow: FollowLinkConfig<Bitstream>[]): Observable<RemoteData<PaginatedList<Bitstream>>> {
    const hrefObs = this.getBitstreamsEndpoint(bundleId, searchOptions);

    hrefObs.pipe(
      take(1),
    ).subscribe((href) => {
      const request = new GetRequest(this.requestService.generateRequestId(), href);
      this.requestService.send(request, true);
    });

    return this.rdbService.buildList<Bitstream>(hrefObs, ...linksToFollow);
  }

  /**
   * Commit current object changes to the server
   * @param method The RestRequestMethod for which de server sync buffer should be committed
   */
  public commitUpdates(method?: RestRequestMethod): void {
    this.patchData.commitUpdates(method);
  }

  /**
   * Send a patch request for a specified object
   * @param {T} object The object to send a patch request for
   * @param {Operation[]} operations The patch operations to be performed
   */
  public patch(object: Bundle, operations: Operation[]): Observable<RemoteData<Bundle>> {
    return this.patchData.patch(object, operations);
  }

  /**
   * Add a new patch to the object cache
   * The patch is derived from the differences between the given object and its version in the object cache
   * @param {DSpaceObject} object The given object
   */
  public update(object: Bundle): Observable<RemoteData<Bundle>> {
    return this.patchData.update(object);
  }

  /**
   * Return a list of operations representing the difference between an object and its latest value in the cache.
   * @param object  the object to resolve to a list of patch operations
   */
  public createPatchFromCache(object: Bundle): Observable<Operation[]> {
    return this.patchData.createPatchFromCache(object);
  }
}
