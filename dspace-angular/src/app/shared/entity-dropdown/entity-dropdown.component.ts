import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { hasValue } from '../empty.util';
import { reduce, startWith, switchMap } from 'rxjs/operators';
import { RemoteData } from '../../core/data/remote-data';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { EntityTypeDataService } from '../../core/data/entity-type-data.service';
import { ItemType } from '../../core/shared/item-relationships/item-type.model';
import { getFirstSucceededRemoteWithNotEmptyData } from '../../core/shared/operators';
import { FindListOptions } from '../../core/data/find-list-options.model';

@Component({
  selector: 'ds-entity-dropdown',
  templateUrl: './entity-dropdown.component.html',
  styleUrls: ['./entity-dropdown.component.scss']
})
export class EntityDropdownComponent implements OnInit, OnDestroy {
  /**
   * The entity list obtained from a search
   * @type {Observable<ItemType[]>}
   */
  public searchListEntity$: Observable<ItemType[]>;

  /**
   * A boolean representing if dropdown list is scrollable to the bottom
   * @type {boolean}
   */
  private scrollableBottom = false;

  /**
   * A boolean representing if dropdown list is scrollable to the top
   * @type {boolean}
   */
  private scrollableTop = false;

  /**
   * The list of entity to render
   */
  public searchListEntity: ItemType[] = [];

  /**
   * TRUE if the parent operation is a 'new submission' operation, FALSE otherwise (eg.: is an 'Import metadata from an external source' operation).
   */
  @Input() isSubmission: boolean;

  /**
   * The entity to output to the parent component
   */
  @Output() selectionChange = new EventEmitter<ItemType>();

  /**
   * A boolean representing if the loader is visible or not
   */
  public isLoadingList: BehaviorSubject<boolean> = new BehaviorSubject(false);

  /**
   * A numeric representig current page
   */
  public currentPage: number;

  /**
   * A boolean representing if exist another page to render
   */
  public hasNextPage: boolean;

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  public subs: Subscription[] = [];

  /**
   * Initialize instance variables
   *
   * @param {ChangeDetectorRef} changeDetectorRef
   * @param {EntityTypeDataService} entityTypeService
   * @param {ElementRef} el
   */
  constructor(
    private changeDetectorRef: ChangeDetectorRef,
    private entityTypeService: EntityTypeDataService,
    private el: ElementRef
  ) { }

  /**
   * Method called on mousewheel event, it prevent the page scroll
   * when arriving at the top/bottom of dropdown menu
   *
   * @param event
   *     mousewheel event
   */
  @HostListener('mousewheel', ['$event']) onMousewheel(event) {
    if (event.wheelDelta > 0 && this.scrollableTop) {
      event.preventDefault();
    }
    if (event.wheelDelta < 0 && this.scrollableBottom) {
      event.preventDefault();
    }
  }

  /**
   * Initialize entity list
   */
  ngOnInit() {
    this.resetPagination();
    this.populateEntityList(this.currentPage);
  }

  /**
   * Check if dropdown scrollbar is at the top or bottom of the dropdown list
   *
   * @param event
   */
  public onScroll(event) {
    this.scrollableBottom = (event.target.scrollTop + event.target.clientHeight === event.target.scrollHeight);
    this.scrollableTop = (event.target.scrollTop === 0);
  }

  /**
   * Method used from infitity scroll for retrive more data on scroll down
   */
  public onScrollDown() {
    if ( this.hasNextPage ) {
      this.populateEntityList(++this.currentPage);
    }
  }

  /**
   * Emit a [selectionChange] event when a new entity is selected from list
   *
   * @param event
   *    the selected [ItemType]
   */
  public onSelect(event: ItemType) {
    this.selectionChange.emit(event);
  }

  /**
   * Method called for populate the entity list
   * @param page page number
   */
  public populateEntityList(page: number) {
    this.isLoadingList.next(true);
    // Set the pagination info
    const findOptions: FindListOptions = {
      elementsPerPage: 10,
      currentPage: page
    };
    let searchListEntity$;
    if (this.isSubmission) {
      searchListEntity$ = this.entityTypeService.getAllAuthorizedRelationshipType(findOptions);
    } else {
      searchListEntity$ = this.entityTypeService.getAllAuthorizedRelationshipTypeImport(findOptions);
    }
    this.searchListEntity$ = searchListEntity$.pipe(
        getFirstSucceededRemoteWithNotEmptyData(),
        switchMap((entityType: RemoteData<PaginatedList<ItemType>>) => {
          if ( (this.searchListEntity.length + findOptions.elementsPerPage) >= entityType.payload.totalElements ) {
            this.hasNextPage = false;
          }
          return entityType.payload.page;
        }),
        reduce((acc: any, value: any) => [...acc, value], []),
        startWith([])
    );
    this.subs.push(
      this.searchListEntity$.subscribe(
        (next) => { this.searchListEntity.push(...next); }, undefined,
        () => { this.hideShowLoader(false); this.changeDetectorRef.detectChanges(); }
      )
    );
  }

  /**
   * Reset pagination values
   */
  public resetPagination() {
    this.currentPage = 1;
    this.hasNextPage = true;
    this.searchListEntity = [];
  }

  /**
   * Hide/Show the entity list loader
   * @param hideShow true for show, false otherwise
   */
  public hideShowLoader(hideShow: boolean) {
    this.isLoadingList.next(hideShow);
  }

  /**
   * Unsubscribe from all subscriptions
   */
  ngOnDestroy(): void {
    this.subs.filter((sub) => hasValue(sub)).forEach((sub) => sub.unsubscribe());
  }
}
