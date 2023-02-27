import { Component, EventEmitter, Injector, Input, OnDestroy, OnInit, Output } from '@angular/core';

import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import uniqueId from 'lodash/uniqueId';

import { RemoteData } from '../../core/data/remote-data';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { PaginationComponentOptions } from '../pagination/pagination-component-options.model';
import { hasValue, isNotEmpty } from '../empty.util';
import { DSONameService } from '../../core/breadcrumbs/dso-name.service';
import { EPERSON } from '../../core/eperson/models/eperson.resource-type';
import { GROUP } from '../../core/eperson/models/group.resource-type';
import { ResourceType } from '../../core/shared/resource-type';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import { GroupDataService } from '../../core/eperson/group-data.service';
import { fadeInOut } from '../animations/fade';
import { getFirstCompletedRemoteData } from '../../core/shared/operators';
import { PaginationService } from '../../core/pagination/pagination.service';
import { FindListOptions } from '../../core/data/find-list-options.model';
import { getDataServiceFor } from '../../core/data/base/data-service.decorator';

export interface SearchEvent {
  scope: string;
  query: string;
}

@Component({
  selector: 'ds-eperson-group-list',
  styleUrls: ['./eperson-group-list.component.scss'],
  templateUrl: './eperson-group-list.component.html',
  animations: [
    fadeInOut
  ]
})
/**
 * Component that shows a list of eperson or group
 */
export class EpersonGroupListComponent implements OnInit, OnDestroy {

  /**
   * A boolean representing id component should list eperson or group
   */
  @Input() isListOfEPerson = true;

  /**
   * The uuid of eperson or group initially selected
   */
  @Input() initSelected: string;

  /**
   * An event fired when a eperson or group is selected.
   * Event's payload equals to DSpaceObject.
   */
  @Output() select: EventEmitter<DSpaceObject> = new EventEmitter<DSpaceObject>();

  /**
   * Current search query
   */
  public currentSearchQuery = '';

  /**
   * Current search scope
   */
  public currentSearchScope = 'metadata';

  /**
   * Pagination config used to display the list
   */
  public paginationOptions: PaginationComponentOptions = new PaginationComponentOptions();

  /**
   * The data service used to make request.
   * It could be EPersonDataService or GroupDataService
   */
  private dataService: EPersonDataService | GroupDataService;

  /**
   * A list of eperson or group
   */
  private list$: BehaviorSubject<RemoteData<PaginatedList<DSpaceObject>>> = new BehaviorSubject<RemoteData<PaginatedList<DSpaceObject>>>({} as any);

  /**
   * The eperson or group's id selected
   * @type {string}
   */
  private entrySelectedId: BehaviorSubject<string> = new BehaviorSubject<string>('');

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  private subs: Subscription[] = [];

  private pageConfigSub: Subscription;

  /**
   * Initialize instance variables and inject the properly DataService
   *
   * @param {DSONameService} dsoNameService
   * @param {Injector} parentInjector
   */
  constructor(public dsoNameService: DSONameService, private parentInjector: Injector,
              private paginationService: PaginationService) {
  }

  /**
   * Initialize the component
   */
  ngOnInit(): void {
    const resourceType: ResourceType = (this.isListOfEPerson) ? EPERSON : GROUP;
    const provider = getDataServiceFor(resourceType);
    this.dataService = Injector.create({
      providers: [],
      parent: this.parentInjector
    }).get(provider);
    this.paginationOptions.id = uniqueId('egl');
    this.paginationOptions.pageSize = 5;

    if (this.initSelected) {
      this.entrySelectedId.next(this.initSelected);
    }

    this.updateList(this.currentSearchScope, this.currentSearchQuery);
  }

  /**
   * Method called when an entry is selected.
   * Emit a new select Event
   *
   * @param entry The eperson or group selected
   */
  emitSelect(entry: DSpaceObject): void {
    this.select.emit(entry);
    this.entrySelectedId.next(entry.id);
  }

  /**
   * Return the list of eperson or group
   */
  getList(): Observable<RemoteData<PaginatedList<DSpaceObject>>> {
    return this.list$.asObservable();
  }

  /**
   * Return a boolean representing if a table row is selected
   *
   * @return {boolean}
   */
  isSelected(entry: DSpaceObject): Observable<boolean> {
    return this.entrySelectedId.asObservable().pipe(
      map((selectedId) => isNotEmpty(selectedId) && selectedId === entry.id)
    );
  }


  /**
   * Method called on search
   */
  onSearch(searchEvent: SearchEvent) {
    this.currentSearchQuery = searchEvent.query;
    this.currentSearchScope = searchEvent.scope;
    this.paginationService.resetPage(this.paginationOptions.id);
    this.updateList(this.currentSearchScope, this.currentSearchQuery);
  }

  /**
   * Retrieve a paginate list of eperson or group
   */
  updateList(scope: string, query: string): void {
    if (hasValue(this.pageConfigSub)) {
      this.pageConfigSub.unsubscribe();
    }
    this.pageConfigSub = this.paginationService.getCurrentPagination(this.paginationOptions.id, this.paginationOptions)
      .subscribe((paginationOptions) => {
    const options: FindListOptions = Object.assign({}, new FindListOptions(), {
          elementsPerPage: paginationOptions.pageSize,
          currentPage: paginationOptions.currentPage
    });

    const search$: Observable<RemoteData<PaginatedList<DSpaceObject>>> = this.isListOfEPerson ?
      (this.dataService as EPersonDataService).searchByScope(scope, query, options) :
      (this.dataService as GroupDataService).searchGroups(query, options);

    this.subs.push(search$.pipe(getFirstCompletedRemoteData())
      .subscribe((list: RemoteData<PaginatedList<DSpaceObject>>) => {
        if (hasValue(this.list$)) {
          this.list$.next(list);
        }
      })
    );
      });
  }

  /**
   * Unsubscribe from all subscriptions
   */
  ngOnDestroy(): void {
    this.list$ = null;
    this.subs
      .filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
    this.paginationService.clearPagination(this.paginationOptions.id);
  }


}
