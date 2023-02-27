import { HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

import { Operation, ReplaceOperation } from 'fast-json-patch';
import { Observable, of as observableOf } from 'rxjs';
import { find, map, mergeMap } from 'rxjs/operators';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { DefaultChangeAnalyzer } from '../data/default-change-analyzer.service';
import { ItemDataService } from '../data/item-data.service';
import { RemoteData } from '../data/remote-data';
import { RequestService } from '../data/request.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { NoContent } from '../shared/NoContent.model';
import { getFirstCompletedRemoteData } from '../shared/operators';
import { ResearcherProfile } from './model/researcher-profile.model';
import { RESEARCHER_PROFILE } from './model/researcher-profile.resource-type';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import { PostRequest } from '../data/request.models';
import { hasValue, isEmpty } from '../../shared/empty.util';
import { followLink, FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { Item } from '../shared/item.model';
import { createFailedRemoteDataObject$ } from '../../shared/remote-data.utils';
import { IdentifiableDataService } from '../data/base/identifiable-data.service';
import { CreateData, CreateDataImpl } from '../data/base/create-data';
import { SearchData, SearchDataImpl } from '../data/base/search-data';
import { PatchData, PatchDataImpl } from '../data/base/patch-data';
import { DeleteData, DeleteDataImpl } from '../data/base/delete-data';
import { RestRequestMethod } from '../data/rest-request-method';
import { RequestParam } from '../cache/models/request-param.model';
import { FindListOptions } from '../data/find-list-options.model';
import { PaginatedList } from '../data/paginated-list.model';
import { dataService } from '../data/base/data-service.decorator';

/**
 * A service that provides methods to make REST requests with researcher profile endpoint.
 */
@Injectable()
@dataService(RESEARCHER_PROFILE)
export class ResearcherProfileDataService extends IdentifiableDataService<ResearcherProfile> implements CreateData<ResearcherProfile>, SearchData<ResearcherProfile>, PatchData<ResearcherProfile>, DeleteData<ResearcherProfile> {
  private createData: CreateDataImpl<ResearcherProfile>;
  private searchData: SearchDataImpl<ResearcherProfile>;
  private patchData: PatchDataImpl<ResearcherProfile>;
  private deleteData: DeleteDataImpl<ResearcherProfile>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
    protected router: Router,
    protected comparator: DefaultChangeAnalyzer<ResearcherProfile>,
    protected itemService: ItemDataService,
  ) {
    super('profiles', requestService, rdbService, objectCache, halService, 10 * 1000);

    this.createData = new CreateDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive);
    this.patchData = new PatchDataImpl<ResearcherProfile>(this.linkPath, requestService, rdbService, objectCache, halService, comparator, this.responseMsToLive, this.constructIdEndpoint);
    this.searchData = new SearchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.deleteData = new DeleteDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive, this.constructIdEndpoint);
  }

  /**
   * Find a researcher profile by its own related item
   *
   * @param item
   */
  public findByRelatedItem(item: Item): Observable<RemoteData<ResearcherProfile>> {
    const profileId = item.firstMetadata('dspace.object.owner')?.authority;
    if (isEmpty(profileId)) {
      return createFailedRemoteDataObject$();
    } else {
      return this.findById(profileId);
    }
  }

  /**
   * Find the item id related to the given researcher profile.
   *
   * @param researcherProfile the profile to find for
   */
  public findRelatedItemId(researcherProfile: ResearcherProfile): Observable<string> {
    const relatedItem$ = researcherProfile.item ? researcherProfile.item : this.itemService.findByHref(researcherProfile._links.item.href, false);
    return relatedItem$.pipe(
      getFirstCompletedRemoteData(),
      map((itemRD: RemoteData<Item>) => (itemRD.hasSucceeded && itemRD.payload) ? itemRD.payload.id : null)
    );
  }

  /**
   * Change the visibility of the given researcher profile setting the given value.
   *
   * @param researcherProfile the profile to update
   * @param visible the visibility value to set
   */
  public setVisibility(researcherProfile: ResearcherProfile, visible: boolean): Observable<RemoteData<ResearcherProfile>> {
    const replaceOperation: ReplaceOperation<boolean> = {
      path: '/visible',
      op: 'replace',
      value: visible
    };

    return this.patch(researcherProfile, [replaceOperation]);
  }

  /**
   * Creates a researcher profile starting from an external source URI
   * @param sourceUri URI of source item of researcher profile.
   */
  public createFromExternalSource(sourceUri: string): Observable<RemoteData<ResearcherProfile>> {
    const options: HttpOptions = Object.create({});
    let headers = new HttpHeaders();
    headers = headers.append('Content-Type', 'text/uri-list');
    options.headers = headers;

    const requestId = this.requestService.generateRequestId();
    const href$ = this.halService.getEndpoint(this.getLinkPath());

    href$.pipe(
      find((href: string) => hasValue(href)),
      map((href: string) => this.buildHrefWithParams(href, [], followLink('item'))),
    ).subscribe((endpoint: string) => {
      const request = new PostRequest(requestId, endpoint, sourceUri, options);
      this.requestService.send(request);
    });

    return this.rdbService.buildFromRequestUUID(requestId, followLink('item'));
  }

  /**
   * Creates a researcher profile starting from an external source URI and returns the related item's ID
   * Emits null if the researcher profile doesn't exist after sending out the request
   * @param sourceUri
   */
  createFromExternalSourceAndReturnRelatedItemId(sourceUri: string): Observable<string> {
    return this.createFromExternalSource(sourceUri).pipe(
      getFirstCompletedRemoteData(),
      mergeMap((rd: RemoteData<ResearcherProfile>) => {
        if (rd.hasSucceeded) {
          return this.findRelatedItemId(rd.payload);
        } else {
          return observableOf(null);
        }
      }),
    );
  }


  /**
   * Create a new object on the server, and store the response in the object cache
   *
   * @param object    The object to create
   * @param params    Array with additional params to combine with query string
   */
  public create(object?: ResearcherProfile, ...params: RequestParam[]): Observable<RemoteData<ResearcherProfile>> {
    if (isEmpty(object)) {
      object = new ResearcherProfile();
    }
    return this.createData.create(object, ...params);
  }

  searchBy(searchMethod: string, options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<ResearcherProfile>[]): Observable<RemoteData<PaginatedList<ResearcherProfile>>> {
    return this.searchData.searchBy(searchMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  getSearchByHref?(searchMethod: string, options?: FindListOptions, ...linksToFollow: FollowLinkConfig<ResearcherProfile>[]): Observable<string> {
    return this.searchData.getSearchByHref(searchMethod, options, ...linksToFollow);
  }

  /**
   * Commit current object changes to the server
   * @param method The RestRequestMethod for which de server sync buffer should be committed
   */
  public commitUpdates(method?: RestRequestMethod): void {
    this.patchData.commitUpdates(method);
  }

  /**
   * Return a list of operations representing the difference between an object and its latest value in the cache.
   * @param object  the object to resolve to a list of patch operations
   */
  public createPatchFromCache(object: ResearcherProfile): Observable<Operation[]> {
    return this.patchData.createPatchFromCache(object);
  }

  /**
   * Send a patch request for a specified object
   * @param {T} object The object to send a patch request for
   * @param {Operation[]} operations The patch operations to be performed
   */
  public patch(object: ResearcherProfile, operations: Operation[]): Observable<RemoteData<ResearcherProfile>> {
    return this.patchData.patch(object, operations);
  }

  /**
   * Add a new patch to the object cache
   * The patch is derived from the differences between the given object and its version in the object cache
   * @param {DSpaceObject} object The given object
   */
  public update(object: ResearcherProfile): Observable<RemoteData<ResearcherProfile>> {
    return this.patchData.update(object);
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
