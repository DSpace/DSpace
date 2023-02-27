import { Injectable } from '@angular/core';
import { RequestService } from './request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { Observable } from 'rxjs';
import { PaginatedList } from './paginated-list.model';
import { RemoteData } from './remote-data';
import { IdentifiableDataService } from './base/identifiable-data.service';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { FindAllData, FindAllDataImpl } from './base/find-all-data';
import { FindListOptions } from './find-list-options.model';
import { dataService } from './base/data-service.decorator';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { CreateData, CreateDataImpl } from './base/create-data';
import { SYSTEMWIDEALERT } from '../../system-wide-alert/system-wide-alert.resource-type';
import { SystemWideAlert } from '../../system-wide-alert/system-wide-alert.model';
import { PutData, PutDataImpl } from './base/put-data';
import { RequestParam } from '../cache/models/request-param.model';
import { SearchData, SearchDataImpl } from './base/search-data';

/**
 * Dataservice representing a system-wide alert
 */
@Injectable()
@dataService(SYSTEMWIDEALERT)
export class SystemWideAlertDataService extends IdentifiableDataService<SystemWideAlert> implements FindAllData<SystemWideAlert>, CreateData<SystemWideAlert>, PutData<SystemWideAlert>, SearchData<SystemWideAlert> {
  private findAllData: FindAllDataImpl<SystemWideAlert>;
  private createData: CreateDataImpl<SystemWideAlert>;
  private putData: PutDataImpl<SystemWideAlert>;
  private searchData: SearchData<SystemWideAlert>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
  ) {
    super('systemwidealerts', requestService, rdbService, objectCache, halService);

    this.findAllData = new FindAllDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.createData = new CreateDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive);
    this.putData = new PutDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.searchData = new SearchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
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
  findAll(options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<SystemWideAlert>[]): Observable<RemoteData<PaginatedList<SystemWideAlert>>> {
    return this.findAllData.findAll(options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Create a new object on the server, and store the response in the object cache
   *
   * @param object    The object to create
   * @param params    Array with additional params to combine with query string
   */
  create(object: SystemWideAlert, ...params: RequestParam[]): Observable<RemoteData<SystemWideAlert>> {
    return this.createData.create(object, ...params);
  }

  /**
   * Send a PUT request for the specified object
   *
   * @param object The object to send a put request for.
   */
  put(object: SystemWideAlert): Observable<RemoteData<SystemWideAlert>> {
    return this.putData.put(object);
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
  searchBy(searchMethod: string, options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<SystemWideAlert>[]): Observable<RemoteData<PaginatedList<SystemWideAlert>>> {
    return this.searchData.searchBy(searchMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }


}
