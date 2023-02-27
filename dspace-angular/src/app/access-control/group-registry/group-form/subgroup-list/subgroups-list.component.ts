import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable, of as observableOf, Subscription } from 'rxjs';
import { map, mergeMap, switchMap, take } from 'rxjs/operators';
import { PaginatedList } from '../../../../core/data/paginated-list.model';
import { RemoteData } from '../../../../core/data/remote-data';
import { GroupDataService } from '../../../../core/eperson/group-data.service';
import { Group } from '../../../../core/eperson/models/group.model';
import {
  getFirstCompletedRemoteData,
  getFirstSucceededRemoteData,
  getRemoteDataPayload
} from '../../../../core/shared/operators';
import { NotificationsService } from '../../../../shared/notifications/notifications.service';
import { PaginationComponentOptions } from '../../../../shared/pagination/pagination-component-options.model';
import { NoContent } from '../../../../core/shared/NoContent.model';
import { PaginationService } from '../../../../core/pagination/pagination.service';
import { followLink } from '../../../../shared/utils/follow-link-config.model';

/**
 * Keys to keep track of specific subscriptions
 */
enum SubKey {
  Members,
  ActiveGroup,
  SearchResults,
}

@Component({
  selector: 'ds-subgroups-list',
  templateUrl: './subgroups-list.component.html'
})
/**
 * The list of subgroups in the edit group page
 */
export class SubgroupsListComponent implements OnInit, OnDestroy {

  @Input()
  messagePrefix: string;

  /**
   * Result of search groups, initially all groups
   */
  searchResults$: BehaviorSubject<RemoteData<PaginatedList<Group>>> = new BehaviorSubject(undefined);
  /**
   * List of all subgroups of group being edited
   */
  subGroups$: BehaviorSubject<RemoteData<PaginatedList<Group>>> = new BehaviorSubject(undefined);

  /**
   * Map of active subscriptions
   */
  subs: Map<SubKey, Subscription> = new Map();

  /**
   * Pagination config used to display the list of groups that are result of groups search
   */
  configSearch: PaginationComponentOptions = Object.assign(new PaginationComponentOptions(), {
    id: 'ssgl',
    pageSize: 5,
    currentPage: 1
  });
  /**
   * Pagination config used to display the list of subgroups of currently active group being edited
   */
  config: PaginationComponentOptions = Object.assign(new PaginationComponentOptions(), {
    id: 'sgl',
    pageSize: 5,
    currentPage: 1
  });

  // The search form
  searchForm;

  // Current search in edit group - groups search form
  currentSearchQuery: string;

  // Whether or not user has done a Groups search yet
  searchDone: boolean;

  // current active group being edited
  groupBeingEdited: Group;

  constructor(public groupDataService: GroupDataService,
              private translateService: TranslateService,
              private notificationsService: NotificationsService,
              private formBuilder: FormBuilder,
              private paginationService: PaginationService,
              private router: Router) {
    this.currentSearchQuery = '';
  }

  ngOnInit() {
    this.searchForm = this.formBuilder.group(({
      query: '',
    }));
    this.subs.set(SubKey.ActiveGroup, this.groupDataService.getActiveGroup().subscribe((activeGroup: Group) => {
      if (activeGroup != null) {
        this.groupBeingEdited = activeGroup;
        this.retrieveSubGroups();
      }
    }));
  }

  /**
   * Retrieve the Subgroups that are members of the group
   *
   * @param page the number of the page to retrieve
   * @private
   */
  private retrieveSubGroups() {
    this.unsubFrom(SubKey.Members);
    this.subs.set(
      SubKey.Members,
      this.paginationService.getCurrentPagination(this.config.id, this.config).pipe(
        switchMap((config) => this.groupDataService.findListByHref(this.groupBeingEdited._links.subgroups.href, {
            currentPage: config.currentPage,
            elementsPerPage: config.pageSize
          },
          true,
          true,
          followLink('object')
        ))
      ).subscribe((rd: RemoteData<PaginatedList<Group>>) => {
        this.subGroups$.next(rd);
      }));
  }

  /**
   * Whether or not the given group is a subgroup of the group currently being edited
   * @param possibleSubgroup Group that is a possible subgroup (being tested) of the group currently being edited
   */
  isSubgroupOfGroup(possibleSubgroup: Group): Observable<boolean> {
    return this.groupDataService.getActiveGroup().pipe(take(1),
      mergeMap((activeGroup: Group) => {
        if (activeGroup != null) {
          if (activeGroup.uuid === possibleSubgroup.uuid) {
            return observableOf(false);
          } else {
            return this.groupDataService.findListByHref(activeGroup._links.subgroups.href, {
              currentPage: 1,
              elementsPerPage: 9999
            })
              .pipe(
                getFirstSucceededRemoteData(),
                getRemoteDataPayload(),
                map((listTotalGroups: PaginatedList<Group>) => listTotalGroups.page.filter((groupInList: Group) => groupInList.id === possibleSubgroup.id)),
                map((groups: Group[]) => groups.length > 0));
          }
        } else {
          return observableOf(false);
        }
      }));
  }

  /**
   * Whether or not the given group is the current group being edited
   * @param group Group that is possibly the current group being edited
   */
  isActiveGroup(group: Group): Observable<boolean> {
    return this.groupDataService.getActiveGroup().pipe(take(1),
      mergeMap((activeGroup: Group) => {
        if (activeGroup != null && activeGroup.uuid === group.uuid) {
          return observableOf(true);
        }
        return observableOf(false);
      }));
  }

  /**
   * Deletes given subgroup from the group currently being edited
   * @param subgroup  Group we want to delete from the subgroups of the group currently being edited
   */
  deleteSubgroupFromGroup(subgroup: Group) {
    this.groupDataService.getActiveGroup().pipe(take(1)).subscribe((activeGroup: Group) => {
      if (activeGroup != null) {
        const response = this.groupDataService.deleteSubGroupFromGroup(activeGroup, subgroup);
        this.showNotifications('deleteSubgroup', response, subgroup.name, activeGroup);
      } else {
        this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.failure.noActiveGroup'));
      }
    });
  }

  /**
   * Adds given subgroup to the group currently being edited
   * @param subgroup  Subgroup to add to group currently being edited
   */
  addSubgroupToGroup(subgroup: Group) {
    this.groupDataService.getActiveGroup().pipe(take(1)).subscribe((activeGroup: Group) => {
      if (activeGroup != null) {
        if (activeGroup.uuid !== subgroup.uuid) {
          const response = this.groupDataService.addSubGroupToGroup(activeGroup, subgroup);
          this.showNotifications('addSubgroup', response, subgroup.name, activeGroup);
        } else {
          this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.failure.subgroupToAddIsActiveGroup'));
        }
      } else {
        this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.failure.noActiveGroup'));
      }
    });
  }

  /**
   * Search in the groups (searches by group name and by uuid exact match)
   * @param data  Contains query param
   */
  search(data: any) {
    const query: string = data.query;
    if (query != null && this.currentSearchQuery !== query) {
      this.router.navigateByUrl(this.groupDataService.getGroupEditPageRouterLink(this.groupBeingEdited));
      this.currentSearchQuery = query;
      this.configSearch.currentPage = 1;
    }
    this.searchDone = true;

    this.unsubFrom(SubKey.SearchResults);
    this.subs.set(SubKey.SearchResults, this.paginationService.getCurrentPagination(this.configSearch.id, this.configSearch).pipe(
      switchMap((config) => this.groupDataService.searchGroups(this.currentSearchQuery, {
        currentPage: config.currentPage,
        elementsPerPage: config.pageSize
      }, true, true, followLink('object')
      ))
    ).subscribe((rd: RemoteData<PaginatedList<Group>>) => {
      this.searchResults$.next(rd);
    }));
  }

  /**
   * Unsubscribe from a subscription if it's still subscribed, and remove it from the map of
   * active subscriptions
   *
   * @param key The key of the subscription to unsubscribe from
   * @private
   */
  private unsubFrom(key: SubKey) {
    if (this.subs.has(key)) {
      this.subs.get(key).unsubscribe();
      this.subs.delete(key);
    }
  }

  /**
   * unsub all subscriptions
   */
  ngOnDestroy(): void {
    for (const key of this.subs.keys()) {
      this.unsubFrom(key);
    }
    this.paginationService.clearPagination(this.config.id);
    this.paginationService.clearPagination(this.configSearch.id);
  }

  /**
   * Shows a notification based on the success/failure of the request
   * @param messageSuffix   Suffix for message
   * @param response        RestResponse observable containing success/failure request
   * @param nameObject      Object request was about
   * @param activeGroup     Group currently being edited
   */
  showNotifications(messageSuffix: string, response: Observable<RemoteData<Group|NoContent>>, nameObject: string, activeGroup: Group) {
    response.pipe(getFirstCompletedRemoteData()).subscribe((rd: RemoteData<Group>) => {
      if (rd.hasSucceeded) {
        this.notificationsService.success(this.translateService.get(this.messagePrefix + '.notification.success.' + messageSuffix, { name: nameObject }));
        this.groupDataService.clearGroupLinkRequests(activeGroup._links.subgroups.href);
      } else {
        this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.failure.' + messageSuffix, { name: nameObject }));
      }
    });
  }

  /**
   * Reset all input-fields to be empty and search all search
   */
  clearFormAndResetResult() {
    this.searchForm.patchValue({
      query: '',
    });
    this.search({ query: '' });
  }
}
