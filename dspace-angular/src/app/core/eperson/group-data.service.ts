import { HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { createSelector, select, Store } from '@ngrx/store';
import { Observable, zip as observableZip } from 'rxjs';
import { filter, map, take } from 'rxjs/operators';
import {
  GroupRegistryCancelGroupAction,
  GroupRegistryEditGroupAction
} from '../../access-control/group-registry/group-registry.actions';
import { GroupRegistryState } from '../../access-control/group-registry/group-registry.reducers';
import { AppState } from '../../app.reducer';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { RequestParam } from '../cache/models/request-param.model';
import { ObjectCacheService } from '../cache/object-cache.service';
import { DSOChangeAnalyzer } from '../data/dso-change-analyzer.service';
import { PaginatedList } from '../data/paginated-list.model';
import { RemoteData } from '../data/remote-data';
import { CreateRequest, DeleteRequest, PostRequest } from '../data/request.models';

import { RequestService } from '../data/request.service';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { getFirstCompletedRemoteData } from '../shared/operators';
import { EPerson } from './models/eperson.model';
import { Group } from './models/group.model';
import { GROUP } from './models/group.resource-type';
import { DSONameService } from '../breadcrumbs/dso-name.service';
import { Community } from '../shared/community.model';
import { Collection } from '../shared/collection.model';
import { NoContent } from '../shared/NoContent.model';
import { FindListOptions } from '../data/find-list-options.model';
import { CreateData, CreateDataImpl } from '../data/base/create-data';
import { IdentifiableDataService } from '../data/base/identifiable-data.service';
import { SearchData, SearchDataImpl } from '../data/base/search-data';
import { PatchData, PatchDataImpl } from '../data/base/patch-data';
import { DeleteData, DeleteDataImpl } from '../data/base/delete-data';
import { Operation } from 'fast-json-patch';
import { RestRequestMethod } from '../data/rest-request-method';
import { dataService } from '../data/base/data-service.decorator';

const groupRegistryStateSelector = (state: AppState) => state.groupRegistry;
const editGroupSelector = createSelector(groupRegistryStateSelector, (groupRegistryState: GroupRegistryState) => groupRegistryState.editGroup);

/**
 * Provides methods to retrieve eperson group resources from the REST API & Group related CRUD actions.
 */
@Injectable()
@dataService(GROUP)
export class GroupDataService extends IdentifiableDataService<Group> implements CreateData<Group>, SearchData<Group>, PatchData<Group>, DeleteData<Group> {
  protected browseEndpoint = '';
  public ePersonsEndpoint = 'epersons';
  public subgroupsEndpoint = 'subgroups';

  private createData: CreateData<Group>;
  private searchData: SearchData<Group>;
  private patchData: PatchData<Group>;
  private deleteData: DeleteData<Group>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected comparator: DSOChangeAnalyzer<Group>,
    protected notificationsService: NotificationsService,
    protected nameService: DSONameService,
    protected store: Store<any>,
  ) {
    super('groups', requestService, rdbService, objectCache, halService);

    this.createData = new CreateDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive);
    this.searchData = new SearchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.patchData = new PatchDataImpl<Group>(this.linkPath, requestService, rdbService, objectCache, halService, comparator, this.responseMsToLive, this.constructIdEndpoint);
    this.deleteData = new DeleteDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive, this.constructIdEndpoint);
  }

  /**
   * Returns a search result list of groups, with certain query (searches in group name and by exact uuid)
   * Endpoint used: /eperson/groups/search/byMetadata?query=<:name>
   * @param query                       search query param
   * @param options
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  public searchGroups(query: string, options?: FindListOptions, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Group>[]): Observable<RemoteData<PaginatedList<Group>>> {
    const searchParams = [new RequestParam('query', query)];
    let findListOptions = new FindListOptions();
    if (options) {
      findListOptions = Object.assign(new FindListOptions(), options);
    }
    if (findListOptions.searchParams) {
      findListOptions.searchParams = [...findListOptions.searchParams, ...searchParams];
    } else {
      findListOptions.searchParams = searchParams;
    }
    return this.searchBy('byMetadata', findListOptions, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Check if the current user is member of to the indicated group
   *
   * @param groupName
   *    the group name
   * @return boolean
   *    true if user is member of the indicated group, false otherwise
   */
  isMemberOf(groupName: string): Observable<boolean> {
    const searchHref = 'isMemberOf';
    const options = new FindListOptions();
    options.searchParams = [new RequestParam('groupName', groupName)];

    return this.searchBy(searchHref, options).pipe(
      filter((groups: RemoteData<PaginatedList<Group>>) => !groups.isResponsePending),
      take(1),
      map((groups: RemoteData<PaginatedList<Group>>) => groups.payload.totalElements > 0)
    );
  }

  /**
   * Adds given subgroup as a subgroup to the given active group and waits until the {@link activeGroup} and
   * the {@link subgroup} are invalidated.
   * @param activeGroup   Group we want to add subgroup to
   * @param subgroup      Group we want to add as subgroup to activeGroup
   */
  addSubGroupToGroup(activeGroup: Group, subgroup: Group): Observable<RemoteData<Group>> {
    const requestId = this.requestService.generateRequestId();
    const options: HttpOptions = Object.create({});
    let headers = new HttpHeaders();
    headers = headers.append('Content-Type', 'text/uri-list');
    options.headers = headers;
    const postRequest = new PostRequest(requestId, activeGroup.self + '/' + this.subgroupsEndpoint, subgroup.self, options);
    this.requestService.send(postRequest);

    return this.rdbService.buildFromRequestUUIDAndAwait(requestId, () => observableZip(
      this.invalidateByHref(activeGroup._links.self.href),
      this.requestService.setStaleByHrefSubstring(activeGroup._links.subgroups.href).pipe(take(1)),
    ));
  }

  /**
   * Deletes a given subgroup from the subgroups of the given active group and waits until the {@link activeGroup} and
   * the {@link subgroup} are invalidated.
   * are invalidated.
   * @param activeGroup   Group we want to delete subgroup from
   * @param subgroup      Subgroup we want to delete from activeGroup
   */
  deleteSubGroupFromGroup(activeGroup: Group, subgroup: Group): Observable<RemoteData<NoContent>> {
    const requestId = this.requestService.generateRequestId();
    const deleteRequest = new DeleteRequest(requestId, activeGroup.self + '/' + this.subgroupsEndpoint + '/' + subgroup.id);
    this.requestService.send(deleteRequest);

    return this.rdbService.buildFromRequestUUIDAndAwait(requestId, () => observableZip(
      this.invalidateByHref(activeGroup._links.self.href),
      this.requestService.setStaleByHrefSubstring(activeGroup._links.subgroups.href).pipe(take(1)),
    ));
  }

  /**
   * Adds given ePerson as member to a given group and invalidates the ePerson and waits until the {@link ePerson} and
   * the {@link activeGroup} are invalidated.
   * @param activeGroup   Group we want to add member to
   * @param ePerson       EPerson we want to add as member to given activeGroup
   */
  addMemberToGroup(activeGroup: Group, ePerson: EPerson): Observable<RemoteData<Group>> {
    const requestId = this.requestService.generateRequestId();
    const options: HttpOptions = Object.create({});
    let headers = new HttpHeaders();
    headers = headers.append('Content-Type', 'text/uri-list');
    options.headers = headers;
    const postRequest = new PostRequest(requestId, activeGroup.self + '/' + this.ePersonsEndpoint, ePerson.self, options);
    this.requestService.send(postRequest);

    return this.rdbService.buildFromRequestUUIDAndAwait(requestId, () => observableZip(
      this.invalidateByHref(ePerson._links.self.href),
      this.requestService.setStaleByHrefSubstring(ePerson._links.groups.href).pipe(take(1)),
      this.requestService.setStaleByHrefSubstring(activeGroup._links.epersons.href).pipe(take(1)),
    ));
  }

  /**
   * Deletes a given ePerson from the members of the given active group and waits until the {@link ePerson} and the
   * {@link activeGroup} are invalidated.
   * @param activeGroup   Group we want to delete member from
   * @param ePerson       EPerson we want to delete from members of given activeGroup
   */
  deleteMemberFromGroup(activeGroup: Group, ePerson: EPerson): Observable<RemoteData<NoContent>> {
    const requestId = this.requestService.generateRequestId();
    const deleteRequest = new DeleteRequest(requestId, activeGroup.self + '/' + this.ePersonsEndpoint + '/' + ePerson.id);
    this.requestService.send(deleteRequest);

    return this.rdbService.buildFromRequestUUIDAndAwait(requestId, () => observableZip(
      this.invalidateByHref(ePerson._links.self.href),
      this.requestService.setStaleByHrefSubstring(ePerson._links.groups.href).pipe(take(1)),
      this.requestService.setStaleByHrefSubstring(activeGroup._links.epersons.href).pipe(take(1)),
    ));
  }

  /**
   * Method to retrieve the group that is currently being edited
   */
  public getActiveGroup(): Observable<Group> {
    return this.store.pipe(select(editGroupSelector));
  }

  /**
   * Method to cancel editing a group, dispatches a cancel group action
   */
  public cancelEditGroup() {
    this.store.dispatch(new GroupRegistryCancelGroupAction());
  }

  /**
   * Method to set the group being edited, dispatches an edit group action
   * @param group The group to edit
   */
  public editGroup(group: Group) {
    this.store.dispatch(new GroupRegistryEditGroupAction(group));
  }

  /**
   * Method that clears a cached groups request
   */
  public clearGroupsRequests(): void {
    this.getBrowseEndpoint().pipe(take(1)).subscribe((link: string) => {
      this.requestService.removeByHrefSubstring(link);
    });
  }

  /**
   * Method that clears a cached get subgroups of certain group request
   */
  public clearGroupLinkRequests(href: string): void {
    this.requestService.setStaleByHrefSubstring(href);
  }

  public getGroupRegistryRouterLink(): string {
    return '/access-control/groups';
  }

  /**
   * Change which group is being edited and return the link for the edit page of the new group being edited
   * @param newGroup New group to edit
   */
  public startEditingNewGroup(newGroup: Group): string {
    this.getActiveGroup().pipe(take(1)).subscribe((activeGroup: Group) => {
      if (newGroup === activeGroup) {
        this.cancelEditGroup();
      } else {
        this.editGroup(newGroup);
      }
    });
    return this.getGroupEditPageRouterLinkWithID(newGroup.id);
  }

  /**
   * Get Edit page of group
   * @param group Group we want edit page for
   */
  public getGroupEditPageRouterLink(group: Group): string {
    return this.getGroupEditPageRouterLinkWithID(group.id);
  }

  /**
   * Get Edit page of group
   * @param groupID Group ID we want edit page for
   */
  public getGroupEditPageRouterLinkWithID(groupId: string): string {
    return '/access-control/groups/' + groupId;
  }

  /**
   * Extract optional UUID from a string
   * @param stringWithUUID  String with possible UUID
   */
  public getUUIDFromString(stringWithUUID: string): string {
    let foundUUID = '';
    const uuidMatches = stringWithUUID.match(/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}/g);
    if (uuidMatches != null) {
      foundUUID = uuidMatches[0];
    }
    return foundUUID;
  }

  /**
   * Create a group for a given role for a given community or collection.
   *
   * @param dso         The community or collection for which to create a group
   * @param role        The name of the role for which to create a group
   * @param link        The REST endpoint to create the group
   */
  createComcolGroup(dso: Community | Collection, role: string, link: string): Observable<RemoteData<Group>> {

    const requestId = this.requestService.generateRequestId();
    const group = Object.assign(new Group(), {
      metadata: {
        'dc.description': [
          {
            value: `${this.nameService.getName(dso)} ${role} group`,
          }
        ],
      },
    });

    this.requestService.send(
      new CreateRequest(
        requestId,
        link,
        JSON.stringify(group),
      ));

    const responseRD$ = this.rdbService.buildFromRequestUUID<Group>(requestId).pipe(
      getFirstCompletedRemoteData(),
    );

    responseRD$.subscribe((responseRD: RemoteData<Group>) => {
      if (responseRD.hasSucceeded) {
        this.requestService.removeByHrefSubstring(link);
      }
    });

    return responseRD$;
  }

  /**
   * Delete the group for a given role for a given community or collection.
   *
   * @param link        The REST endpoint to delete the group
   */
  deleteComcolGroup(link: string): Observable<RemoteData<NoContent>> {

    const requestId = this.requestService.generateRequestId();

    this.requestService.send(
      new DeleteRequest(
        requestId,
        link,
      ));

    const responseRD$ = this.rdbService.buildFromRequestUUID(requestId).pipe(
      getFirstCompletedRemoteData(),
    );

    responseRD$.subscribe((responseRD: RemoteData<NoContent>) => {
      if (responseRD.hasSucceeded) {
        this.requestService.removeByHrefSubstring(link);
      }
    });

    return responseRD$;
  }


  public create(object: Group, ...params: RequestParam[]): Observable<RemoteData<Group>> {
    return this.createData.create(object, ...params);
  }


  searchBy(searchMethod: string, options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<Group>[]): Observable<RemoteData<PaginatedList<Group>>> {
    return this.searchData.searchBy(searchMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  public createPatchFromCache(object: Group): Observable<Operation[]> {
    return this.patchData.createPatchFromCache(object);
  }

  patch(object: Group, operations: Operation[]): Observable<RemoteData<Group>> {
    return this.patchData.patch(object, operations);
  }

  update(object: Group): Observable<RemoteData<Group>> {
    return this.patchData.update(object);
  }

  commitUpdates(method?: RestRequestMethod): void {
    this.patchData.commitUpdates(method);
  }

  delete(objectId: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
    return this.deleteData.delete(objectId, copyVirtualMetadata);
  }

  public deleteByHref(href: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
    return this.deleteData.deleteByHref(href, copyVirtualMetadata);
  }
}
