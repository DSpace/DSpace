import { Component, EventEmitter, Injector, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { RemoteData } from '../../core/data/remote-data';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { PaginationComponentOptions } from '../pagination/pagination-component-options.model';
import { SortDirection, SortOptions } from '../../core/cache/models/sort-options.model';
import { fadeIn, fadeInOut } from '../animations/fade';
import { BehaviorSubject, combineLatest as observableCombineLatest, Observable, Subscription } from 'rxjs';
import { ListableObject } from '../object-collection/shared/listable-object.model';
import { getStartsWithComponent, StartsWithType } from '../starts-with/starts-with-decorator';
import { PaginationService } from '../../core/pagination/pagination.service';
import { ViewMode } from '../../core/shared/view-mode.model';
import { RouteService } from '../../core/services/route.service';
import { map } from 'rxjs/operators';
import { hasValue } from '../empty.util';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'ds-browse-by',
  styleUrls: ['./browse-by.component.scss'],
  templateUrl: './browse-by.component.html',
  animations: [
    fadeIn,
    fadeInOut
  ]
})
/**
 * Component to display a browse-by page for any ListableObject
 */
export class BrowseByComponent implements OnInit, OnDestroy {

  /**
   * ViewMode that should be passed to {@link ListableObjectComponentLoaderComponent}.
   */
  viewMode: ViewMode = ViewMode.ListElement;

  /**
   * The i18n message to display as title
   */
  @Input() title: string;

  /**
   * The parent name
   */
  @Input() parentname: string;
  /**
   * The list of objects to display
   */
  @Input() objects$: Observable<RemoteData<PaginatedList<ListableObject>>>;

  /**
   * The pagination configuration used for the list
   */
  @Input() paginationConfig: PaginationComponentOptions;

  /**
   * The sorting configuration used for the list
   */
  @Input() sortConfig: SortOptions;

  /**
   * The type of StartsWith options used to define what component to render for the options
   * Defaults to text
   */
  @Input() type = StartsWithType.text;

  /**
   * The list of options to render for the StartsWith component
   */
  @Input() startsWithOptions = [];

  /**
   * Whether or not the pagination should be rendered as simple previous and next buttons instead of the normal pagination
   */
  @Input() showPaginator = true;

  /**
   * It is used to hide or show gear
   */
  @Input() hideGear = false;

  /**
   * Emits event when prev button clicked
   */
  @Output() prev = new EventEmitter<boolean>();

  /**
   * Emits event when next button clicked
   */
  @Output() next = new EventEmitter<boolean>();

  /**
   * Emits event when page size is changed
   */
  @Output() pageSizeChange = new EventEmitter<number>();

  /**
   * Emits event when page sort direction is changed
   */
  @Output() sortDirectionChange = new EventEmitter<SortDirection>();

  /**
   * An object injector used to inject the startsWithOptions to the switchable StartsWith component
   */
  objectInjector: Injector;

  /**
   * Declare SortDirection enumeration to use it in the template
   */
  public sortDirections = SortDirection;

  /**
   * Observable that tracks if the back button should be displayed based on the path parameters
   */
  shouldDisplayResetButton$: Observable<boolean>;

  /**
   * Page number of the previous page
   */
  previousPage$ = new BehaviorSubject<string>('1');

  /**
   * Subscription that has to be unsubscribed from on destroy
   */
  sub: Subscription;

  public constructor(private injector: Injector,
                     protected paginationService: PaginationService,
                     protected translateService: TranslateService,
                     private routeService: RouteService,
  ) {

  }

  /**
   * The label used by the back button.
   */
  buttonLabel = this.translateService.get('browse.back.all-results');

  /**
   * The function used for back navigation in metadata browse.
   */
  back = () => {
    const page = +this.previousPage$.value > 1 ? +this.previousPage$.value : 1;
    this.paginationService.updateRoute(this.paginationConfig.id, {page: page}, {[this.paginationConfig.id + '.return']: null, value: null, startsWith: null});
  };

  /**
   * Go to the previous page
   */
  goPrev() {
    this.prev.emit(true);
  }

  /**
   * Go to the next page
   */
  goNext() {
    this.next.emit(true);
  }

  /**
   * Change the page size
   * @param size
   */
  doPageSizeChange(size) {
    this.paginationService.updateRoute(this.paginationConfig.id,{pageSize: size});
  }

  /**
   * Change the sort direction
   * @param direction
   */
  doSortDirectionChange(direction) {
    this.paginationService.updateRoute(this.paginationConfig.id,{sortDirection: direction});
  }

  /**
   * Get the switchable StartsWith component dependant on the type
   */
  getStartsWithComponent() {
    return getStartsWithComponent(this.type);
  }

  ngOnInit(): void {
    this.objectInjector = Injector.create({
      providers: [
        { provide: 'startsWithOptions', useFactory: () => (this.startsWithOptions), deps:[] },
        { provide: 'paginationId', useFactory: () => (this.paginationConfig?.id), deps:[] }
      ],
      parent: this.injector
    });

    const startsWith$ = this.routeService.getQueryParameterValue('startsWith');
    const value$ = this.routeService.getQueryParameterValue('value');

    this.shouldDisplayResetButton$ = observableCombineLatest([startsWith$, value$]).pipe(
      map(([startsWith, value]) => hasValue(startsWith) || hasValue(value))
    );
    this.sub = this.routeService.getQueryParameterValue(this.paginationConfig.id + '.return').subscribe(this.previousPage$);
  }

  ngOnDestroy(): void {
    if (this.sub) {
      this.sub.unsubscribe();
    }
  }
}
