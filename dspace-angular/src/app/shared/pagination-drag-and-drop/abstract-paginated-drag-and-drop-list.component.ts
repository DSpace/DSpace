import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { RemoteData } from '../../core/data/remote-data';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { PaginationComponentOptions } from '../pagination/pagination-component-options.model';
import { ObjectUpdatesService } from '../../core/data/object-updates/object-updates.service';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { hasValue } from '../empty.util';
import {
  paginatedListToArray,
  getFirstSucceededRemoteData,
  getAllSucceededRemoteData
} from '../../core/shared/operators';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, ElementRef, EventEmitter, OnDestroy, Output, ViewChild } from '@angular/core';
import { PaginationComponent } from '../pagination/pagination.component';
import { ObjectValuesPipe } from '../utils/object-values-pipe';
import { compareArraysUsing } from '../../item-page/simple/item-types/shared/item-relationships-utils';
import { PaginationService } from '../../core/pagination/pagination.service';
import { FieldUpdate } from '../../core/data/object-updates/field-update.model';
import { FieldUpdates } from '../../core/data/object-updates/field-updates.model';

/**
 * Operator used for comparing {@link FieldUpdate}s by their field's UUID
 */
export const compareArraysUsingFieldUuids = () =>
  compareArraysUsing((fieldUpdate: FieldUpdate) => (hasValue(fieldUpdate) && hasValue(fieldUpdate.field)) ? fieldUpdate.field.uuid : undefined);

/**
 * An abstract component containing general methods and logic to be able to drag and drop objects within a paginated
 * list. This implementation supports being able to drag and drop objects between pages.
 * Dragging an object on top of a page number will automatically detect the page it's being dropped on and send a
 * dropObject event to the parent component containing detailed information about the indexes the object was dropped from
 * and to.
 *
 * To extend this component, it is important to make sure to:
 * - Initialize objectsRD$ within the initializeObjectsRD() method
 * - Initialize a unique URL for this component/page within the initializeURL() method
 * - Add (cdkDropListDropped)="drop($event)" to the cdkDropList element in your template
 * - Add (pageChange)="switchPage($event)" to the ds-pagination element in your template
 * - Use the updates$ observable for building your list of cdkDrag elements in your template
 *
 * An example component extending from this abstract component: PaginatedDragAndDropBitstreamListComponent
 */
@Component({
  selector: 'ds-paginated-drag-drop-abstract',
  template: ''
})
export abstract class AbstractPaginatedDragAndDropListComponent<T extends DSpaceObject> implements OnDestroy {
  /**
   * A view on the child pagination component
   */
  @ViewChild(PaginationComponent) paginationComponent: PaginationComponent;

  /**
   * Send an event when the user drops an object on the pagination
   * The event contains details about the index the object came from and is dropped to (across the entirety of the list,
   * not just within a single page)
   */
  @Output() dropObject: EventEmitter<any> = new EventEmitter<any>();

  /**
   * The URL to use for accessing the object updates from this list
   */
  url: string;

  /**
   * The objects to retrieve data for and transform into field updates
   */
  objectsRD$: Observable<RemoteData<PaginatedList<T>>>;

  /**
   * The updates to the current list
   */
  updates$: Observable<FieldUpdates>;

  /**
   * A list of object UUIDs
   * This is the order the objects will be displayed in
   */
  customOrder: string[];

  /**
   * The amount of objects to display per page
   */
  pageSize = 10;

  /**
   * The page options to use for fetching the objects
   * Start at page 1 and always use the set page size
   */
  options = Object.assign(new PaginationComponentOptions(),{
    id: 'dad',
    currentPage: 1,
    pageSize: this.pageSize
  });

  /**
   * The current page being displayed
   */
  currentPage$ = new BehaviorSubject<PaginationComponentOptions>(this.options);

  /**
   * Whether or not we should display a loading animation
   * This is used to display a loading page when the user drops a bitstream onto a new page. The loading animation
   * should stop once the bitstream has moved to the new page and the new page's response has loaded and contains the
   * dropped object on top (see this.stopLoadingWhenFirstIs below)
   */
  loading$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  /**
   * List of subscriptions
   */
  subs: Subscription[] = [];

  protected constructor(protected objectUpdatesService: ObjectUpdatesService,
                        protected elRef: ElementRef,
                        protected objectValuesPipe: ObjectValuesPipe,
                        protected paginationService: PaginationService
                        ) {
  }

  /**
   * Initialize the observables
   */
  ngOnInit() {
    this.initializeObjectsRD();
    this.initializeURL();
    this.initializeUpdates();
    this.initializePagination();
  }

  /**
   * Overwrite this method to define how the list of objects is initialized and updated
   */
  abstract initializeObjectsRD(): void;

  /**
   * Overwrite this method to define how the URL is set
   */
  abstract initializeURL(): void;

  /**
   * Initialize the current pagination retrieval from the paginationService and push to the currentPage$
   */
  initializePagination() {
    this.paginationService.getCurrentPagination(this.options.id, this.options).subscribe((currentPagination) => {
      this.currentPage$.next(currentPagination);
    });
  }

  /**
   * Initialize the field-updates in the store
   */
  initializeUpdates(): void {
    this.objectsRD$.pipe(
      getFirstSucceededRemoteData(),
      paginatedListToArray(),
    ).subscribe((objects: T[]) => {
      this.objectUpdatesService.initialize(this.url, objects, new Date());
    });
    this.updates$ = this.objectsRD$.pipe(
      getAllSucceededRemoteData(),
      paginatedListToArray(),
      switchMap((objects: T[]) => this.objectUpdatesService.getFieldUpdatesExclusive(this.url, objects))
    );
    this.subs.push(
      this.updates$.pipe(
        map((fieldUpdates) => this.objectValuesPipe.transform(fieldUpdates)),
        distinctUntilChanged(compareArraysUsingFieldUuids())
      ).subscribe((updateValues) => {
        this.customOrder = updateValues.map((fieldUpdate) => fieldUpdate.field.uuid);
        // We received new values, stop displaying a loading indicator if it's present
        this.loading$.next(false);
      }),
      // Disable the pagination when objects are loading
      this.loading$.subscribe((loading) => this.options.disabled = loading)
    );
  }

  /**
   * An object was moved, send updates to the dropObject EventEmitter
   * When the object is dropped on a page within the pagination of this component, the object moves to the top of that
   * page and the pagination automatically loads and switches the view to that page (this is done by calling the event's
   * finish() method after sending patch requests to the REST API)
   * @param event
   */
  drop(event: CdkDragDrop<any>) {
    const dragIndex = event.previousIndex;
    let dropIndex = event.currentIndex;
    const dragPage = this.currentPage$.value.currentPage - 1;
    let dropPage = this.currentPage$.value.currentPage - 1;

    // Check if the user is hovering over any of the pagination's pages at the time of dropping the object
    const droppedOnElement = this.elRef.nativeElement.querySelector('.page-item:hover');
    if (hasValue(droppedOnElement) && hasValue(droppedOnElement.textContent)) {
      // The user is hovering over a page, fetch the page's number from the element
      const droppedPage = Number(droppedOnElement.textContent);
      if (hasValue(droppedPage) && !Number.isNaN(droppedPage)) {
        dropPage = droppedPage - 1;
        dropIndex = 0;
      }
    }

    const isNewPage = dragPage !== dropPage;
    // Move the object in the custom order array if the drop happened within the same page
    // This allows us to instantly display a change in the order, instead of waiting for the REST API's response first
    if (!isNewPage && dragIndex !== dropIndex) {
      moveItemInArray(this.customOrder, dragIndex, dropIndex);
    }

    const redirectPage = dropPage + 1;
    const fromIndex = (dragPage * this.pageSize) + dragIndex;
    const toIndex = (dropPage * this.pageSize) + dropIndex;
    // Send out a drop event (and navigate to the new page) when the "from" and "to" indexes are different from each other
    if (fromIndex !== toIndex) {
      if (isNewPage) {
        this.loading$.next(true);
      }
      this.dropObject.emit(Object.assign({
        fromIndex,
        toIndex,
        finish: () => {
          if (isNewPage) {
            this.paginationComponent.doPageChange(redirectPage);
          }
        }
      }));
    }
  }

  /**
   * unsub all subscriptions
   */
  ngOnDestroy(): void {
    this.subs.filter((sub) => hasValue(sub)).forEach((sub) => sub.unsubscribe());
    this.paginationService.clearPagination(this.options.id);
  }
}
