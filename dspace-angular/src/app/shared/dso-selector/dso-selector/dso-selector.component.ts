import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  QueryList,
  ViewChildren
} from '@angular/core';
import { FormControl } from '@angular/forms';

import {
  BehaviorSubject,
  combineLatest as observableCombineLatest,
  Observable,
  of as observableOf,
  Subscription
} from 'rxjs';
import { debounceTime, map, startWith, switchMap, tap } from 'rxjs/operators';

import { SearchService } from '../../../core/shared/search/search.service';
import { CollectionElementLinkType } from '../../object-collection/collection-element-link.type';
import { PaginatedSearchOptions } from '../../search/models/paginated-search-options.model';
import { DSpaceObjectType } from '../../../core/shared/dspace-object-type.model';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { ViewMode } from '../../../core/shared/view-mode.model';
import { Context } from '../../../core/shared/context.model';
import { getFirstCompletedRemoteData, getFirstSucceededRemoteDataPayload } from '../../../core/shared/operators';
import { hasNoValue, hasValue, isEmpty, isNotEmpty } from '../../empty.util';
import { buildPaginatedList, PaginatedList } from '../../../core/data/paginated-list.model';
import { SearchResult } from '../../search/models/search-result.model';
import { RemoteData } from '../../../core/data/remote-data';
import { NotificationsService } from '../../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { DSONameService } from '../../../core/breadcrumbs/dso-name.service';
import {
  ListableNotificationObject
} from '../../object-list/listable-notification-object/listable-notification-object.model';
import { ListableObject } from '../../object-collection/shared/listable-object.model';
import { NotificationType } from '../../notifications/models/notification-type';
import {
  LISTABLE_NOTIFICATION_OBJECT
} from '../../object-list/listable-notification-object/listable-notification-object.resource-type';

@Component({
  selector: 'ds-dso-selector',
  styleUrls: ['./dso-selector.component.scss'],
  templateUrl: './dso-selector.component.html'
})

/**
 * Component to render a list of DSO's of which one can be selected
 * The user can search the list by using the input field
 */
export class DSOSelectorComponent implements OnInit, OnDestroy {
  /**
   * The view mode of the listed objects
   */
  viewMode = ViewMode.ListElement;
  /**
   * The initially selected DSO's uuid
   */
  @Input() currentDSOId: string;

  /**
   * The types of DSpace objects this components shows a list of
   */
  @Input() types: DSpaceObjectType[];

  // list of allowed selectable dsoTypes
  typesString: string;

  /**
   * Emits the selected Object when a user selects it in the list
   */
  @Output() onSelect: EventEmitter<DSpaceObject> = new EventEmitter();

  /**
   * Input form control to query the list
   */
  public input: FormControl = new FormControl();

  /**
   * Default pagination for this feature
   */
  defaultPagination = { id: 'dso-selector', currentPage: 1, pageSize: 10 } as any;

  /**
   * List with search results of DSpace objects for the current query
   */
  listEntries$: BehaviorSubject<ListableObject[]> = new BehaviorSubject(null);

  /**
   * The current page to load
   * Dynamically goes up as the user scrolls down until it reaches the last page possible
   */
  currentPage$ = new BehaviorSubject(1);

  /**
   * Whether or not the list contains a next page to load
   * This allows us to avoid next pages from trying to load when there are none
   */
  hasNextPage = false;

  /**
   * Whether or not new results are currently loading
   */
  loading = false;

  /**
   * List of element references to all elements
   */
  @ViewChildren('listEntryElement') listElements: QueryList<ElementRef>;

  /**
   * Time to wait before sending a search request to the server when a user types something
   */
  debounceTime = 500;

  /**
   * The available link types
   */
  linkTypes = CollectionElementLinkType;

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  public subs: Subscription[] = [];

  constructor(
    protected searchService: SearchService,
    protected notifcationsService: NotificationsService,
    protected translate: TranslateService,
    protected dsoNameService: DSONameService,
  ) {
  }

  /**
   * Fills the listEntries variable with search results based on the input field's current value and the current page
   * The search will always start with the initial currentDSOId value
   */
  ngOnInit(): void {
    this.typesString = this.types.map((type: string) => type.toString().toLowerCase()).join(', ');

    // Create an observable searching for the current DSO (return empty list if there's no current DSO)
    let currentDSOResult$: Observable<PaginatedList<SearchResult<DSpaceObject>>>;
    if (isNotEmpty(this.currentDSOId)) {
      currentDSOResult$ = this.search(this.getCurrentDSOQuery(), 1).pipe(getFirstSucceededRemoteDataPayload());
    } else {
      currentDSOResult$ = observableOf(buildPaginatedList(undefined, []));
    }

    // Combine current DSO, query and page
    this.subs.push(observableCombineLatest(
      currentDSOResult$,
      this.input.valueChanges.pipe(
        debounceTime(this.debounceTime),
        startWith(''),
        tap(() => this.currentPage$.next(1))
      ),
      this.currentPage$
    ).pipe(
      switchMap(([currentDSOResult, query, page]: [PaginatedList<SearchResult<DSpaceObject>>, string, number]) => {
        this.loading = true;
        if (page === 1) {
          // The first page is loading, this means we should reset the list instead of adding to it
          this.listEntries$.next(null);
        }
        return this.search(query, page).pipe(
          map((rd) => {
            if (rd.hasSucceeded) {
              // If it's the first page and no query is entered, add the current DSO to the start of the list
              // If no query is entered, filter out the current DSO from the results, as it'll be displayed at the start of the list already
              rd.payload.page = [
                ...((isEmpty(query) && page === 1) ? currentDSOResult.page : []),
                ...rd.payload.page.filter((result) => isNotEmpty(query) || result.indexableObject.id !== this.currentDSOId)
              ];
            } else if (rd.hasFailed) {
              this.notifcationsService.error(this.translate.instant('dso-selector.error.title', { type: this.typesString }), rd.errorMessage);
            }
            return rd;
          })
        );
      })
    ).subscribe((rd: RemoteData<PaginatedList<SearchResult<DSpaceObject>>>) => {
      this.updateList(rd);
    }));
  }

  updateList(rd: RemoteData<PaginatedList<SearchResult<DSpaceObject>>>) {
    this.loading = false;
    const currentEntries = this.listEntries$.getValue();
    if (rd.hasSucceeded) {
      if (hasNoValue(currentEntries)) {
        this.listEntries$.next(rd.payload.page);
      } else {
        this.listEntries$.next([...currentEntries, ...rd.payload.page]);
      }
      // Check if there are more pages available after the current one
      this.hasNextPage = rd.payload.totalElements > this.listEntries$.getValue().length;
    } else {
      this.listEntries$.next([...(hasNoValue(currentEntries) ? [] : this.listEntries$.getValue()), new ListableNotificationObject(NotificationType.Error, 'dso-selector.results-could-not-be-retrieved', LISTABLE_NOTIFICATION_OBJECT.value)]);
      this.hasNextPage = false;
    }
  }

  /**
   * Get a query to send for retrieving the current DSO
   */
  getCurrentDSOQuery(): string {
    return `search.resourceid:${this.currentDSOId}`;
  }

  /**
   * Perform a search for the current query and page
   * @param query Query to search objects for
   * @param page  Page to retrieve
   * @param useCache Whether or not to use the cache
   */
  search(query: string, page: number, useCache: boolean = true): Observable<RemoteData<PaginatedList<SearchResult<DSpaceObject>>>> {
    return this.searchService.search(
      new PaginatedSearchOptions({
        query: query,
        dsoTypes: this.types,
        pagination: Object.assign({}, this.defaultPagination, {
          currentPage: page
        })
      }),
      null,
      useCache,
    ).pipe(
      getFirstCompletedRemoteData()
    );
  }

  /**
   * When the user reaches the bottom of the page (or almost) and there's a next page available, increase the current page
   */
  onScrollDown() {
    if (this.hasNextPage && !this.loading) {
      this.currentPage$.next(this.currentPage$.value + 1);
    }
  }

  /**
   * Set focus on the first list element when there is only one result
   */
  selectSingleResult(): void {
    if (this.listElements.length > 0) {
      this.listElements.first.nativeElement.click();
    }
  }

  /**
   * Get the context for element with the given id
   */
  getContext(id: string) {
    if (id === this.currentDSOId) {
      return Context.SideBarSearchModalCurrent;
    } else {
      return Context.SideBarSearchModal;
    }
  }

  /**
   * Unsubscribe from all subscriptions
   */
  ngOnDestroy(): void {
    this.subs.filter((sub) => hasValue(sub)).forEach((sub) => sub.unsubscribe());
  }

  /**
   * Handles the user clicks on the {@link ListableObject}s. When the {@link listableObject} is a
   * {@link ListableObject} it will retry the error when the user clicks it. Otherwise it will emit the {@link onSelect}.
   *
   * @param listableObject The {@link ListableObject} to evaluate
   */
  onClick(listableObject: ListableObject): void {
    if (hasValue((listableObject as SearchResult<DSpaceObject>).indexableObject)) {
      this.onSelect.emit((listableObject as SearchResult<DSpaceObject>).indexableObject);
    } else {
      this.listEntries$.value.pop();
      this.hasNextPage = true;
      this.search(this.input.value ? this.input.value : '', this.currentPage$.value, false).pipe(
        getFirstCompletedRemoteData(),
      ).subscribe((rd: RemoteData<PaginatedList<SearchResult<DSpaceObject>>>) => {
        this.updateList(rd);
      });
    }
  }

  getName(listableObject: ListableObject): string {
    return hasValue((listableObject as SearchResult<DSpaceObject>).indexableObject) ?
      this.dsoNameService.getName((listableObject as SearchResult<DSpaceObject>).indexableObject) : null;
  }
}
