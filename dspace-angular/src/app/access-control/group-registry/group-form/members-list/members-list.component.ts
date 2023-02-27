import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
  Observable,
  of as observableOf,
  Subscription,
  BehaviorSubject,
  combineLatest as observableCombineLatest,
  ObservedValueOf,
} from 'rxjs';
import { defaultIfEmpty, map, mergeMap, switchMap, take } from 'rxjs/operators';
import { buildPaginatedList, PaginatedList } from '../../../../core/data/paginated-list.model';
import { RemoteData } from '../../../../core/data/remote-data';
import { EPersonDataService } from '../../../../core/eperson/eperson-data.service';
import { GroupDataService } from '../../../../core/eperson/group-data.service';
import { EPerson } from '../../../../core/eperson/models/eperson.model';
import { Group } from '../../../../core/eperson/models/group.model';
import {
  getFirstSucceededRemoteData,
  getFirstCompletedRemoteData,
  getAllCompletedRemoteData,
  getRemoteDataPayload
} from '../../../../core/shared/operators';
import { NotificationsService } from '../../../../shared/notifications/notifications.service';
import { PaginationComponentOptions } from '../../../../shared/pagination/pagination-component-options.model';
import { EpersonDtoModel } from '../../../../core/eperson/models/eperson-dto.model';
import { PaginationService } from '../../../../core/pagination/pagination.service';

/**
 * Keys to keep track of specific subscriptions
 */
enum SubKey {
  ActiveGroup,
  MembersDTO,
  SearchResultsDTO,
}

/**
 * The layout config of the buttons in the last column
 */
export interface EPersonActionConfig {
  /**
   * The css classes that should be added to the button
   */
  css?: string;
  /**
   * Whether the button should be disabled
   */
  disabled: boolean;
  /**
   * The Font Awesome icon that should be used
   */
  icon: string;
}

/**
 * The {@link EPersonActionConfig} that should be used to display the button. The remove config will be used when the
 * {@link EPerson} is already a member of the {@link Group} and the remove config will be used otherwise.
 *
 * *See {@link actionConfig} for an example*
 */
export interface EPersonListActionConfig {
  add: EPersonActionConfig;
  remove: EPersonActionConfig;
}

@Component({
  selector: 'ds-members-list',
  templateUrl: './members-list.component.html'
})
/**
 * The list of members in the edit group page
 */
export class MembersListComponent implements OnInit, OnDestroy {

  @Input()
  messagePrefix: string;

  @Input()
  actionConfig: EPersonListActionConfig = {
    add: {
      css: 'btn-outline-primary',
      disabled: false,
      icon: 'fas fa-plus fa-fw',
    },
    remove: {
      css: 'btn-outline-danger',
      disabled: false,
      icon: 'fas fa-trash-alt fa-fw'
    },
  };

  /**
   * EPeople being displayed in search result, initially all members, after search result of search
   */
  ePeopleSearchDtos: BehaviorSubject<PaginatedList<EpersonDtoModel>> = new BehaviorSubject<PaginatedList<EpersonDtoModel>>(undefined);
  /**
   * List of EPeople members of currently active group being edited
   */
  ePeopleMembersOfGroupDtos: BehaviorSubject<PaginatedList<EpersonDtoModel>> = new BehaviorSubject<PaginatedList<EpersonDtoModel>>(undefined);

  /**
   * Pagination config used to display the list of EPeople that are result of EPeople search
   */
  configSearch: PaginationComponentOptions = Object.assign(new PaginationComponentOptions(), {
    id: 'sml',
    pageSize: 5,
    currentPage: 1
  });
  /**
   * Pagination config used to display the list of EPerson Membes of active group being edited
   */
  config: PaginationComponentOptions = Object.assign(new PaginationComponentOptions(), {
    id: 'ml',
    pageSize: 5,
    currentPage: 1
  });

  /**
   * Map of active subscriptions
   */
  subs: Map<SubKey, Subscription> = new Map();

  // The search form
  searchForm;

  // Current search in edit group - epeople search form
  currentSearchQuery: string;
  currentSearchScope: string;

  // Whether or not user has done a EPeople search yet
  searchDone: boolean;

  // current active group being edited
  groupBeingEdited: Group;

  constructor(
    protected groupDataService: GroupDataService,
    public ePersonDataService: EPersonDataService,
    protected translateService: TranslateService,
    protected notificationsService: NotificationsService,
    protected formBuilder: FormBuilder,
    protected paginationService: PaginationService,
    private router: Router
  ) {
    this.currentSearchQuery = '';
    this.currentSearchScope = 'metadata';
  }

  ngOnInit(): void {
    this.searchForm = this.formBuilder.group(({
      scope: 'metadata',
      query: '',
    }));
    this.subs.set(SubKey.ActiveGroup, this.groupDataService.getActiveGroup().subscribe((activeGroup: Group) => {
      if (activeGroup != null) {
        this.groupBeingEdited = activeGroup;
        this.retrieveMembers(this.config.currentPage);
      }
    }));
  }

  /**
   * Retrieve the EPersons that are members of the group
   *
   * @param page the number of the page to retrieve
   * @private
   */
  retrieveMembers(page: number): void {
    this.unsubFrom(SubKey.MembersDTO);
    this.subs.set(SubKey.MembersDTO,
      this.paginationService.getCurrentPagination(this.config.id, this.config).pipe(
        switchMap((currentPagination) => {
          return this.ePersonDataService.findListByHref(this.groupBeingEdited._links.epersons.href, {
              currentPage: currentPagination.currentPage,
              elementsPerPage: currentPagination.pageSize
            }
          );
        }),
        getAllCompletedRemoteData(),
        map((rd: RemoteData<any>) => {
          if (rd.hasFailed) {
            this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.failure', { cause: rd.errorMessage }));
          } else {
            return rd;
          }
        }),
        switchMap((epersonListRD: RemoteData<PaginatedList<EPerson>>) => {
          const dtos$ = observableCombineLatest([...epersonListRD.payload.page.map((member: EPerson) => {
            const dto$: Observable<EpersonDtoModel> = observableCombineLatest(
              this.isMemberOfGroup(member), (isMember: ObservedValueOf<Observable<boolean>>) => {
                const epersonDtoModel: EpersonDtoModel = new EpersonDtoModel();
                epersonDtoModel.eperson = member;
                epersonDtoModel.memberOfGroup = isMember;
                return epersonDtoModel;
              });
            return dto$;
          })]);
          return dtos$.pipe(defaultIfEmpty([]), map((dtos: EpersonDtoModel[]) => {
            return buildPaginatedList(epersonListRD.payload.pageInfo, dtos);
          }));
        }))
        .subscribe((paginatedListOfDTOs: PaginatedList<EpersonDtoModel>) => {
          this.ePeopleMembersOfGroupDtos.next(paginatedListOfDTOs);
        }));
  }

  /**
   * Whether the given ePerson is a member of the group currently being edited
   * @param possibleMember  EPerson that is a possible member (being tested) of the group currently being edited
   */
  isMemberOfGroup(possibleMember: EPerson): Observable<boolean> {
    return this.groupDataService.getActiveGroup().pipe(take(1),
      mergeMap((group: Group) => {
        if (group != null) {
          return this.ePersonDataService.findListByHref(group._links.epersons.href, {
            currentPage: 1,
            elementsPerPage: 9999
          })
            .pipe(
              getFirstSucceededRemoteData(),
              getRemoteDataPayload(),
              map((listEPeopleInGroup: PaginatedList<EPerson>) => listEPeopleInGroup.page.filter((ePersonInList: EPerson) => ePersonInList.id === possibleMember.id)),
              map((epeople: EPerson[]) => epeople.length > 0));
        } else {
          return observableOf(false);
        }
      }));
  }

  /**
   * Unsubscribe from a subscription if it's still subscribed, and remove it from the map of
   * active subscriptions
   *
   * @param key The key of the subscription to unsubscribe from
   * @private
   */
  protected unsubFrom(key: SubKey) {
    if (this.subs.has(key)) {
      this.subs.get(key).unsubscribe();
      this.subs.delete(key);
    }
  }

  /**
   * Deletes a given EPerson from the members list of the group currently being edited
   * @param ePerson   EPerson we want to delete as member from group that is currently being edited
   */
  deleteMemberFromGroup(ePerson: EpersonDtoModel) {
    this.groupDataService.getActiveGroup().pipe(take(1)).subscribe((activeGroup: Group) => {
      if (activeGroup != null) {
        const response = this.groupDataService.deleteMemberFromGroup(activeGroup, ePerson.eperson);
        this.showNotifications('deleteMember', response, ePerson.eperson.name, activeGroup);
      } else {
        this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.failure.noActiveGroup'));
      }
    });
  }

  /**
   * Adds a given EPerson to the members list of the group currently being edited
   * @param ePerson   EPerson we want to add as member to group that is currently being edited
   */
  addMemberToGroup(ePerson: EpersonDtoModel) {
    ePerson.memberOfGroup = true;
    this.groupDataService.getActiveGroup().pipe(take(1)).subscribe((activeGroup: Group) => {
      if (activeGroup != null) {
        const response = this.groupDataService.addMemberToGroup(activeGroup, ePerson.eperson);
        this.showNotifications('addMember', response, ePerson.eperson.name, activeGroup);
      } else {
        this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.failure.noActiveGroup'));
      }
    });
  }

  /**
   * Search in the EPeople by name, email or metadata
   * @param data  Contains scope and query param
   */
  search(data: any) {
    this.unsubFrom(SubKey.SearchResultsDTO);
    this.subs.set(SubKey.SearchResultsDTO,
      this.paginationService.getCurrentPagination(this.configSearch.id, this.configSearch).pipe(
        switchMap((paginationOptions) => {

          const query: string = data.query;
          const scope: string = data.scope;
          if (query != null && this.currentSearchQuery !== query && this.groupBeingEdited) {
            this.router.navigate([], {
              queryParamsHandling: 'merge'
            });
            this.currentSearchQuery = query;
            this.paginationService.resetPage(this.configSearch.id);
          }
          if (scope != null && this.currentSearchScope !== scope && this.groupBeingEdited) {
            this.router.navigate([], {
              queryParamsHandling: 'merge'
            });
            this.currentSearchScope = scope;
            this.paginationService.resetPage(this.configSearch.id);
          }
          this.searchDone = true;

          return this.ePersonDataService.searchByScope(this.currentSearchScope, this.currentSearchQuery, {
            currentPage: paginationOptions.currentPage,
            elementsPerPage: paginationOptions.pageSize
          });
        }),
        getAllCompletedRemoteData(),
        map((rd: RemoteData<any>) => {
          if (rd.hasFailed) {
            this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.failure', { cause: rd.errorMessage }));
          } else {
            return rd;
          }
        }),
        switchMap((epersonListRD: RemoteData<PaginatedList<EPerson>>) => {
          const dtos$ = observableCombineLatest([...epersonListRD.payload.page.map((member: EPerson) => {
            const dto$: Observable<EpersonDtoModel> = observableCombineLatest(
              this.isMemberOfGroup(member), (isMember: ObservedValueOf<Observable<boolean>>) => {
                const epersonDtoModel: EpersonDtoModel = new EpersonDtoModel();
                epersonDtoModel.eperson = member;
                epersonDtoModel.memberOfGroup = isMember;
                return epersonDtoModel;
              });
            return dto$;
          })]);
          return dtos$.pipe(defaultIfEmpty([]), map((dtos: EpersonDtoModel[]) => {
            return buildPaginatedList(epersonListRD.payload.pageInfo, dtos);
          }));
        }))
        .subscribe((paginatedListOfDTOs: PaginatedList<EpersonDtoModel>) => {
          this.ePeopleSearchDtos.next(paginatedListOfDTOs);
        }));
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
  showNotifications(messageSuffix: string, response: Observable<RemoteData<any>>, nameObject: string, activeGroup: Group) {
    response.pipe(getFirstCompletedRemoteData()).subscribe((rd: RemoteData<any>) => {
      if (rd.hasSucceeded) {
        this.notificationsService.success(this.translateService.get(this.messagePrefix + '.notification.success.' + messageSuffix, { name: nameObject }));
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
