import { distinctUntilChanged, filter, map, switchMap, take } from 'rxjs/operators';
import { combineLatest as observableCombineLatest, Observable } from 'rxjs';
import { hasValue, isEmpty, isNotEmpty } from '../../shared/empty.util';
import { ObjectCacheService } from '../cache/object-cache.service';
import { Community } from '../shared/community.model';
import { HALLink } from '../shared/hal-link.model';
import { PaginatedList } from './paginated-list.model';
import { RemoteData } from './remote-data';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { getFirstCompletedRemoteData } from '../shared/operators';
import { Bitstream } from '../shared/bitstream.model';
import { Collection } from '../shared/collection.model';
import { BitstreamDataService } from './bitstream-data.service';
import { NoContent } from '../shared/NoContent.model';
import { createFailedRemoteDataObject$ } from '../../shared/remote-data.utils';
import { URLCombiner } from '../url-combiner/url-combiner';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { FindListOptions } from './find-list-options.model';
import { IdentifiableDataService } from './base/identifiable-data.service';
import { PatchData, PatchDataImpl } from './base/patch-data';
import { DeleteData, DeleteDataImpl } from './base/delete-data';
import { FindAllData, FindAllDataImpl } from './base/find-all-data';
import { SearchData, SearchDataImpl } from './base/search-data';
import { RestRequestMethod } from './rest-request-method';
import { CreateData, CreateDataImpl } from './base/create-data';
import { RequestParam } from '../cache/models/request-param.model';
import { RequestService } from './request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { DSOChangeAnalyzer } from './dso-change-analyzer.service';
import { Operation } from 'fast-json-patch';

export abstract class ComColDataService<T extends Community | Collection> extends IdentifiableDataService<T> implements CreateData<T>, FindAllData<T>, SearchData<T>, PatchData<T>, DeleteData<T> {
  private createData: CreateData<T>;
  private findAllData: FindAllData<T>;
  private searchData: SearchData<T>;
  private patchData: PatchData<T>;
  private deleteData: DeleteData<T>;

  protected constructor(
    protected linkPath: string,
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected comparator: DSOChangeAnalyzer<T>,
    protected notificationsService: NotificationsService,
    protected bitstreamDataService: BitstreamDataService,
  ) {
    super(linkPath, requestService, rdbService, objectCache, halService);

    this.createData = new CreateDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive);
    this.findAllData = new FindAllDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.searchData = new SearchDataImpl<T>(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.patchData = new PatchDataImpl<T>(this.linkPath, requestService, rdbService, objectCache, halService, comparator, this.responseMsToLive, this.constructIdEndpoint);
    this.deleteData = new DeleteDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive, this.constructIdEndpoint);
  }

  /**
   * Get the scoped endpoint URL by fetching the object with
   * the given scopeID and returning its HAL link with this
   * data-service's linkPath
   *
   * @param {string} scopeID
   *    the id of the scope object
   * @return { Observable<string> }
   *    an Observable<string> containing the scoped URL
   */
  public getBrowseEndpoint(options: FindListOptions = {}, linkPath: string = this.linkPath): Observable<string> {
    if (isEmpty(options.scopeID)) {
      return this.halService.getEndpoint(linkPath);
    } else {
      const scopeCommunityHrefObs = this.getScopeCommunityHref(options);

      this.createAndSendGetRequest(scopeCommunityHrefObs, true);

      return scopeCommunityHrefObs.pipe(
        switchMap((href: string) => this.rdbService.buildSingle<Community>(href)),
        getFirstCompletedRemoteData(),
        map((response: RemoteData<Community>) => {
          if (response.hasFailed) {
            throw new Error(`The Community with scope ${options.scopeID} couldn't be retrieved`);
          } else {
            return response.payload._links[linkPath];
          }
        }),
        filter((halLink: HALLink) => isNotEmpty(halLink)),
        map((halLink: HALLink) => halLink.href),
        distinctUntilChanged()
      );
    }
  }

  protected abstract getScopeCommunityHref(options: FindListOptions): Observable<string>;

  protected abstract getFindByParentHref(parentUUID: string): Observable<string>;

  public findByParent(parentUUID: string, options: FindListOptions = {}, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<PaginatedList<T>>> {
    const href$ = this.getFindByParentHref(parentUUID).pipe(
      map((href: string) => this.buildHrefFromFindOptions(href, options))
    );
    return this.findListByHref(href$, options, true, true, ...linksToFollow);
  }

  /**
   * Get the endpoint for the community or collection's logo
   * @param id  The community or collection's ID
   */
  public getLogoEndpoint(id: string): Observable<string> {
    return this.halService.getEndpoint(this.linkPath).pipe(
      // We can't use HalLinkService to discover the logo link itself, as objects without a logo
      // don't have the link, and this method is also used in the createLogo method.
      map((href: string) => new URLCombiner(href, id, 'logo').toString())
    );
  }

  /**
   * Delete the logo from the community or collection
   * @param dso The object to delete the logo from
   */
  public deleteLogo(dso: T): Observable<RemoteData<NoContent>> {
    const logo$ = dso.logo;
    if (hasValue(logo$)) {
      // We need to fetch the logo before deleting it, because rest doesn't allow us to send a
      // DELETE request to a `/logo` link. So we need to use the bitstream self link.
      return logo$.pipe(
        getFirstCompletedRemoteData(),
        switchMap((logoRd: RemoteData<Bitstream>) => {
          if (logoRd.hasFailed) {
            console.error(`Couldn't retrieve the logo '${dso._links.logo.href}' in order to delete it.`);
            return [logoRd];
          } else {
            return this.bitstreamDataService.deleteByHref(logoRd.payload._links.self.href);
          }
        })
      );
    } else {
      return createFailedRemoteDataObject$(`The given object doesn't have a logo`, 400);
    }
  }

  public refreshCache(dso: T) {
    const parentCommunityUrl = this.parentCommunityUrlLookup(dso as any);
    if (!hasValue(parentCommunityUrl)) {
      return;
    }
    observableCombineLatest([
      this.findByHref(parentCommunityUrl).pipe(
        getFirstCompletedRemoteData(),
      ),
      this.halService.getEndpoint('communities/search/top').pipe(take(1))
    ]).subscribe(([rd, topHref]: [RemoteData<any>, string]) => {
      if (rd.hasSucceeded && isNotEmpty(rd.payload) && isNotEmpty(rd.payload.id)) {
        this.requestService.setStaleByHrefSubstring(rd.payload.id);
      } else {
        this.requestService.setStaleByHrefSubstring(topHref);
      }
    });
  }

  private parentCommunityUrlLookup(dso: Collection | Community) {
    const parentCommunity = dso._links.parentCommunity;
    return isNotEmpty(parentCommunity) ? parentCommunity.href : null;
  }


  /**
   * Create a new object on the server, and store the response in the object cache
   *
   * @param object    The object to create
   * @param params    Array with additional params to combine with query string
   */
  create(object: T, ...params: RequestParam[]): Observable<RemoteData<T>> {
    return this.createData.create(object, ...params);
  }

  /**
   * Returns {@link RemoteData} of all object with a list of {@link FollowLinkConfig}, to indicate which embedded
   * info should be added to the objects
   *
   * @param options                     Find list options object
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<PaginatedList<T>>>}
   *    Return an observable that emits object list
   */
  public findAll(options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<PaginatedList<T>>> {
    return this.findAllData.findAll(options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
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
  public searchBy(searchMethod: string, options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<PaginatedList<T>>> {
    return this.searchData.searchBy(searchMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
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
  public patch(object: T, operations: []): Observable<RemoteData<T>> {
    return this.patchData.patch(object, operations);
  }

  /**
   * Add a new patch to the object cache
   * The patch is derived from the differences between the given object and its version in the object cache
   * @param {DSpaceObject} object The given object
   */
  public update(object: T): Observable<RemoteData<T>> {
    return this.patchData.update(object);
  }

  /**
   * Return a list of operations representing the difference between an object and its latest value in the cache.
   * @param object  the object to resolve to a list of patch operations
   */
  public createPatchFromCache(object: T): Observable<Operation[]> {
    return this.patchData.createPatchFromCache(object);
  }

  /**
   * Delete an existing object on the server
   * @param   objectId The id of the object to be removed
   * @param   copyVirtualMetadata (optional parameter) the identifiers of the relationship types for which the virtual
   *                            metadata should be saved as real metadata
   * @return  A RemoteData observable with an empty payload, but still representing the state of the request: statusCode,
   *          errorMessage, timeCompleted, etc
   */
  public delete(objectId: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
    return this.deleteData.delete(objectId, copyVirtualMetadata);
  }

  /**
   * Delete an existing object on the server
   * @param   href The self link of the object to be removed
   * @param   copyVirtualMetadata (optional parameter) the identifiers of the relationship types for which the virtual
   *                            metadata should be saved as real metadata
   * @return  A RemoteData observable with an empty payload, but still representing the state of the request: statusCode,
   *          errorMessage, timeCompleted, etc
   *          Only emits once all request related to the DSO has been invalidated.
   */
  public deleteByHref(href: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
    return this.deleteData.deleteByHref(href, copyVirtualMetadata);
  }
}
