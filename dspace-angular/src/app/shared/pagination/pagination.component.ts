import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';

import { Observable, of as observableOf, Subscription } from 'rxjs';

import { HostWindowService } from '../host-window.service';
import { HostWindowState } from '../search/host-window.reducer';
import { PaginationComponentOptions } from './pagination-component-options.model';
import { SortDirection, SortOptions } from '../../core/cache/models/sort-options.model';
import { hasValue } from '../empty.util';
import { PageInfo } from '../../core/shared/page-info.model';
import { PaginationService } from '../../core/pagination/pagination.service';
import { map, take } from 'rxjs/operators';
import { RemoteData } from '../../core/data/remote-data';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { ListableObject } from '../object-collection/shared/listable-object.model';
import { ViewMode } from '../../core/shared/view-mode.model';

/**
 * The default pagination controls component.
 */
@Component({
  exportAs: 'paginationComponent',
  selector: 'ds-pagination',
  styleUrls: ['pagination.component.scss'],
  templateUrl: 'pagination.component.html',
  changeDetection: ChangeDetectionStrategy.Default,
  encapsulation: ViewEncapsulation.Emulated
})
export class PaginationComponent implements OnDestroy, OnInit {
  /**
   * ViewMode that should be passed to {@link ListableObjectComponentLoaderComponent}.
   */
  viewMode: ViewMode = ViewMode.ListElement;

  /**
   * Number of items in collection.
   */
  @Input() collectionSize: number;

  /**
   * Page state of a Remote paginated objects.
   */
  @Input() pageInfoState: Observable<PageInfo> = undefined;

  /**
   * Configuration for the NgbPagination component.
   */
  @Input() paginationOptions: PaginationComponentOptions;

  /**
   * Sort configuration for this component.
   */
  @Input() sortOptions: SortOptions;

  /**
   * Whether or not the pagination should be rendered as simple previous and next buttons instead of the normal pagination
   */
  @Input() showPaginator = true;

  /**
   * The current pagination configuration
   */
   @Input() config?: PaginationComponentOptions;

  /**
   * The list of listable objects to render in this component
   */
  @Input() objects: RemoteData<PaginatedList<ListableObject>>;

  /**
   * The current sorting configuration
   */
  @Input() sortConfig: SortOptions;

  /**
   * An event fired when the page is changed.
   * Event's payload equals to the newly selected page.
   */
  @Output() pageChange: EventEmitter<number> = new EventEmitter<number>();

  /**
   * An event fired when the page wsize is changed.
   * Event's payload equals to the newly selected page size.
   */
  @Output() pageSizeChange: EventEmitter<number> = new EventEmitter<number>();

  /**
   * An event fired when the sort direction is changed.
   * Event's payload equals to the newly selected sort direction.
   */
  @Output() sortDirectionChange: EventEmitter<SortDirection> = new EventEmitter<SortDirection>();

  /**
   * An event fired when the sort field is changed.
   * Event's payload equals to the newly selected sort field.
   */
  @Output() sortFieldChange: EventEmitter<string> = new EventEmitter<string>();

  /**
   * An event fired when the pagination is changed.
   * Event's payload equals to the newly selected sort field.
   */
  @Output() paginationChange: EventEmitter<any> = new EventEmitter<any>();

  /**
   * Option for hiding the pagination detail
   */
  @Input() public hidePaginationDetail = false;

  /**
   * Option for hiding the gear
   */
  @Input() public hideGear = false;

  /**
   * Option for hiding the pager when there is less than 2 pages
   */
  @Input() public hidePagerWhenSinglePage = true;

  /**
   * Option for retaining the scroll position upon navigating to an url with updated params.
   * After the page update the page will scroll back to the current pagination component.
   */
  @Input() public retainScrollPosition = false;

  /**
   * Current page.
   */
  public currentPage$;

  /**
   * Current page in the state of a Remote paginated objects.
   */
  public currentPageState: number = undefined;

  /**
   * An observable of HostWindowState type
   */
  public hostWindow: Observable<HostWindowState>;

  /**
   * ID for the pagination instance. This ID is used in the routing to retrieve the pagination options.
   * This ID needs to be unique between different pagination components when more than one will be displayed on the same page.
   */
  public id: string;

  /**
   * A boolean that indicate if is an extra small devices viewport.
   */
  public isXs: boolean;

  /**
   * Number of items per page.
   */
  public pageSize$;

  /**
   * Declare SortDirection enumeration to use it in the template
   */
  public sortDirections = SortDirection;

  /**
   * A number array that represents options for a context pagination limit.
   */
  public pageSizeOptions: number[];

  /**
   * Direction in which to sort: ascending or descending
   */
  public sortDirection$: Observable<SortDirection>;
  public defaultsortDirection: SortDirection = SortDirection.ASC;

  /**
   * Name of the field that's used to sort by
   */
  public sortField$;
  public defaultSortField = 'name';

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  private subs: Subscription[] = [];

  /**
   * If showPaginator is set to true, emit when the previous button is clicked
   */
  @Output() prev = new EventEmitter<boolean>();

  /**
   * If showPaginator is set to true, emit when the next button is clicked
   */
  @Output() next = new EventEmitter<boolean>();
  /**
   * Method provided by Angular. Invoked after the constructor.
   */
  ngOnInit() {
    this.subs.push(this.hostWindowService.isXs()
      .subscribe((status: boolean) => {
        this.isXs = status;
        this.cdRef.markForCheck();
      }));
    this.checkConfig(this.paginationOptions);
    this.initializeConfig();
  }

  /**
   * Method provided by Angular. Invoked when the instance is destroyed.
   */
  ngOnDestroy() {
    this.subs
      .filter((sub) => hasValue(sub))
      .forEach((sub) => sub.unsubscribe());
  }

  /**
   * Initializes all default variables
   */
  private initializeConfig() {
    // Set initial values
    this.id = this.paginationOptions.id || null;
    this.pageSizeOptions = this.paginationOptions.pageSizeOptions;
    this.currentPage$ = this.paginationService.getCurrentPagination(this.id, this.paginationOptions).pipe(
      map((currentPagination) => currentPagination.currentPage)
    );
    this.pageSize$ = this.paginationService.getCurrentPagination(this.id, this.paginationOptions).pipe(
      map((currentPagination) => currentPagination.pageSize)
    );

    let sortOptions;
    if (this.sortOptions) {
      sortOptions = this.sortOptions;
    } else {
      sortOptions = new SortOptions(this.defaultSortField, this.defaultsortDirection);
    }
      this.sortDirection$ = this.paginationService.getCurrentSort(this.id, sortOptions).pipe(
        map((currentSort) => currentSort.direction)
      );
      this.sortField$ = this.paginationService.getCurrentSort(this.id, sortOptions).pipe(
        map((currentSort) => currentSort.field)
      );
  }

  /**
   * @param cdRef
   *    ChangeDetectorRef is a singleton service provided by Angular.
   * @param route
   *    Route is a singleton service provided by Angular.
   * @param router
   *    Router is a singleton service provided by Angular.
   * @param hostWindowService
   *    the HostWindowService singleton.
   */
  constructor(private cdRef: ChangeDetectorRef,
              private paginationService: PaginationService,
              public hostWindowService: HostWindowService) {
  }

  /**
   * Method to change the route to the given page
   *
   * @param page
   *    The page being navigated to.
   */
  public doPageChange(page: number) {
    this.updateParams({page: page.toString()});
    this.emitPaginationChange();
  }

  /**
   * Method to change the route to the given page size
   *
   * @param pageSize
   *    The page size being navigated to.
   */
  public doPageSizeChange(pageSize: number) {
    this.updateParams({ pageId: this.id, page: 1, pageSize: pageSize });
    this.emitPaginationChange();
  }

  /**
   * Method to change the route to the given sort direction
   *
   * @param sortDirection
   *    The sort direction being navigated to.
   */
  public doSortDirectionChange(sortDirection: SortDirection) {
    this.updateParams({ pageId: this.id, page: 1, sortDirection: sortDirection });
    this.emitPaginationChange();
  }

  /**
   * Method to change the route to the given sort field
   *
   * @param sortField
   *    The sort field being navigated to.
   */
  public doSortFieldChange(field: string) {
    this.updateParams({ pageId: this.id, page: 1, sortField: field });
    this.emitPaginationChange();
  }

  /**
   * Method to emit a general pagination change event
   */
  private emitPaginationChange() {
    this.paginationChange.emit();
  }

  /**
   * Update the current query params and optionally update the route
   * @param params
   */
  private updateParams(params: {}) {
    this.paginationService.updateRoute(this.id, params, {}, this.retainScrollPosition);
  }

  /**
   * Method to get pagination details of the current viewed page.
   */
  public getShowingDetails(collectionSize: number): Observable<any> {
    let showingDetails = observableOf({ range: null + ' - ' + null, total: null });
    if (collectionSize) {
      showingDetails = this.paginationService.getCurrentPagination(this.id, this.paginationOptions).pipe(
        map((currentPaginationOptions) => {
          let firstItem;
          let lastItem;
          const pageMax = currentPaginationOptions.pageSize * currentPaginationOptions.currentPage;

          firstItem = currentPaginationOptions.pageSize * (currentPaginationOptions.currentPage - 1) + 1;
          if (collectionSize > pageMax) {
            lastItem = pageMax;
          } else {
            lastItem = collectionSize;
          }
          return {range: firstItem + ' - ' + lastItem, total: collectionSize};
        })
      );
    }
    return showingDetails;
  }

  /**
   * Method to ensure options passed contains the required properties.
   *
   * @param paginateOptions
   *    The paginate options object.
   */
  private checkConfig(paginateOptions: any) {
    const required = ['id', 'currentPage', 'pageSize', 'pageSizeOptions'];
    const missing = required.filter((prop) => {
      return !(prop in paginateOptions);
    });
    if (0 < missing.length) {
      throw new Error('Paginate: Argument is missing the following required properties: ' + missing.join(', '));
    }
  }

  /**
   * Property to check whether the current pagination object has multiple pages
   * @returns true if there are multiple pages, else returns false
   */
  get hasMultiplePages(): Observable<boolean> {
    return this.paginationService.getCurrentPagination(this.id, this.paginationOptions).pipe(
      map((currentPaginationOptions) =>  this.collectionSize > currentPaginationOptions.pageSize)
    );
  }

  /**
   * Property to check whether the current pagination should show a bottom pages
   * @returns true if a bottom pages should be shown, else returns false
   */
  get shouldShowBottomPager(): Observable<boolean> {
    return this.hasMultiplePages.pipe(
      map((hasMultiplePages) => hasMultiplePages || !this.hidePagerWhenSinglePage)
    );
  }

  /**
   * Go to the previous page
   */
   goPrev() {
    this.prev.emit(true);
    this.updatePagination(-1);
  }

  /**
   * Go to the next page
   */
  goNext() {
    this.next.emit(true);
    this.updatePagination(1);
  }

  /**
   * Update page when next or prev button is clicked
   * @param value
   */
  updatePagination(value: number) {
    this.paginationService.getCurrentPagination(this.id, this.paginationOptions).pipe(take(1)).subscribe((currentPaginationOptions) => {
      this.updateParams({page: (currentPaginationOptions.currentPage + value).toString()});
    });
  }

}
