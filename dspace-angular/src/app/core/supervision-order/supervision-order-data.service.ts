import { Injectable } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { RequestService } from '../data/request.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { SupervisionOrder } from './models/supervision-order.model';
import { RemoteData } from '../data/remote-data';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { SUPERVISION_ORDER } from './models/supervision-order.resource-type';
import { DefaultChangeAnalyzer } from '../data/default-change-analyzer.service';
import { PaginatedList } from '../data/paginated-list.model';
import { RequestParam } from '../cache/models/request-param.model';
import { isNotEmpty } from '../../shared/empty.util';
import { first, map } from 'rxjs/operators';
import { NoContent } from '../shared/NoContent.model';
import { getFirstCompletedRemoteData } from '../shared/operators';
import { FindListOptions } from '../data/find-list-options.model';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import { PutRequest } from '../data/request.models';
import { GenericConstructor } from '../shared/generic-constructor';
import { ResponseParsingService } from '../data/parsing.service';
import { StatusCodeOnlyResponseParsingService } from '../data/status-code-only-response-parsing.service';
import { GroupDataService } from '../eperson/group-data.service';
import { IdentifiableDataService } from '../data/base/identifiable-data.service';
import { CreateDataImpl } from '../data/base/create-data';
import { SearchDataImpl } from '../data/base/search-data';
import { PatchDataImpl } from '../data/base/patch-data';
import { DeleteDataImpl } from '../data/base/delete-data';
import { dataService } from '../data/base/data-service.decorator';

/**
 * A service responsible for fetching/sending data from/to the REST API on the supervisionorders endpoint
 */
@Injectable()
@dataService(SUPERVISION_ORDER)
export class SupervisionOrderDataService extends IdentifiableDataService<SupervisionOrder> {
  protected searchByGroupMethod = 'group';
  protected searchByItemMethod = 'byItem';

  private createData: CreateDataImpl<SupervisionOrder>;
  private searchData: SearchDataImpl<SupervisionOrder>;
  private patchData: PatchDataImpl<SupervisionOrder>;
  private deleteData: DeleteDataImpl<SupervisionOrder>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
    protected comparator: DefaultChangeAnalyzer<SupervisionOrder>,
    protected groupService: GroupDataService,
  ) {
    super('supervisionorders', requestService, rdbService, objectCache, halService);

    this.createData = new CreateDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive);
    this.searchData = new SearchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.patchData = new PatchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, comparator, this.responseMsToLive, this.constructIdEndpoint);
    this.deleteData = new DeleteDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive, this.constructIdEndpoint);
  }

  /**
   * Create a new SupervisionOrder on the server, and store the response
   * in the object cache
   *
   * @param {SupervisionOrder} supervisionOrder
   *    The supervision order to create
   * @param {string} itemUUID
   *    The uuid of the item that will be grant of the permission.
   * @param {string} groupUUID
   *    The uuid of the group that will be grant of the permission.
   * @param {string} type
   *    The type of the supervision order that will be grant of the permission.
   */
  create(supervisionOrder: SupervisionOrder, itemUUID: string, groupUUID: string, type: string): Observable<RemoteData<SupervisionOrder>> {
    const params = [];
    params.push(new RequestParam('uuid', itemUUID));
    params.push(new RequestParam('group', groupUUID));
    params.push(new RequestParam('type', type));
    return this.createData.create(supervisionOrder, ...params);
  }

  /**
   * Delete an existing SupervisionOrder on the server
   *
   * @param supervisionOrderID The supervision order's id to be removed
   * @return an observable that emits true when the deletion was successful, false when it failed
   */
  delete(supervisionOrderID: string): Observable<boolean> {
    return this.deleteData.delete(supervisionOrderID).pipe(
      getFirstCompletedRemoteData(),
      map((response: RemoteData<NoContent>) => response.hasSucceeded),
    );
  }

  /**
   * Add a new patch to the object cache
   * The patch is derived from the differences between the given object and its version in the object cache
   * @param {SupervisionOrder} object The given object
   */
  update(object: SupervisionOrder): Observable<RemoteData<SupervisionOrder>> {
    return this.patchData.update(object);
  }

  /**
   * Return the {@link SupervisionOrder} list for a {@link Group}
   *
   * @param UUID                        UUID of a given {@link Group}
   * @param itemUUID                    Limit the returned policies to the specified DSO
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  searchByGroup(UUID: string, itemUUID?: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<SupervisionOrder>[]): Observable<RemoteData<PaginatedList<SupervisionOrder>>> {
    const options = new FindListOptions();
    options.searchParams = [new RequestParam('uuid', UUID)];
    if (isNotEmpty(itemUUID)) {
      options.searchParams.push(new RequestParam('item', itemUUID));
    }
    return this.searchData.searchBy(this.searchByGroupMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Return the {@link SupervisionOrder} list for a given DSO
   *
   * @param UUID              UUID of a given DSO
   * @param action            Limit the returned policies to the specified {@link ActionType}
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  searchByItem(UUID: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<SupervisionOrder>[]): Observable<RemoteData<PaginatedList<SupervisionOrder>>> {
    const options = new FindListOptions();
    options.searchParams = [new RequestParam('uuid', UUID)];
    return this.searchData.searchBy(this.searchByItemMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Update the target of the supervision order
   * @param supervisionOrderId the ID of the supervision order
   * @param supervisionOrderHref the link to the supervision order
   * @param targetUUID the UUID of the target to which the permission is being granted
   * @param targetType the type of the target (eperson or group) to which the permission is being granted
   */
  updateTarget(supervisionOrderId: string, supervisionOrderHref: string, targetUUID: string, targetType: string): Observable<RemoteData<any>> {
    const targetService = this.groupService;
    const targetEndpoint$ = targetService.getIDHrefObs(targetUUID);

    const options: HttpOptions = Object.create({});
    let headers = new HttpHeaders();
    headers = headers.append('Content-Type', 'text/uri-list');
    options.headers = headers;

    const requestId = this.requestService.generateRequestId();

    targetEndpoint$.pipe(
      first(),
    ).subscribe((targetEndpoint) => {
      const resourceEndpoint = supervisionOrderHref + '/' + targetType;
      const request = new PutRequest(requestId, resourceEndpoint, targetEndpoint, options);
      Object.assign(request, {
        getResponseParser(): GenericConstructor<ResponseParsingService> {
          return StatusCodeOnlyResponseParsingService;
        }
      });
      this.requestService.send(request);
    });

    return this.rdbService.buildFromRequestUUIDAndAwait(requestId, () => this.invalidateByHref(supervisionOrderHref));
  }

}
