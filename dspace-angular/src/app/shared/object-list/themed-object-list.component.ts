import {Component, EventEmitter, Input, Output} from '@angular/core';
import { ObjectListComponent } from './object-list.component';
import { ThemedComponent } from '../theme-support/themed.component';
import {ViewMode} from '../../core/shared/view-mode.model';
import {PaginationComponentOptions} from '../pagination/pagination-component-options.model';
import {SortDirection, SortOptions} from '../../core/cache/models/sort-options.model';
import {CollectionElementLinkType} from '../object-collection/collection-element-link.type';
import {Context} from '../../core/shared/context.model';
import {RemoteData} from '../../core/data/remote-data';
import {PaginatedList} from '../../core/data/paginated-list.model';
import {ListableObject} from '../object-collection/shared/listable-object.model';

/**
 * Themed wrapper for ObjectListComponent
 */
@Component({
  selector: 'ds-themed-object-list',
  styleUrls: [],
  templateUrl: '../theme-support/themed.component.html',
})
export class ThemedObjectListComponent extends ThemedComponent<ObjectListComponent> {
  /**
   * The view mode of the this component
   */
  viewMode = ViewMode.ListElement;

  /**
   * The current pagination configuration
   */
  @Input() config: PaginationComponentOptions;

  /**
   * The current sort configuration
   */
  @Input() sortConfig: SortOptions;

  /**
   * Whether or not the list elements have a border
   */
  @Input() hasBorder = false;

  /**
   * The whether or not the gear is hidden
   */
  @Input() hideGear = false;

  /**
   * Whether or not the pager is visible when there is only a single page of results
   */
  @Input() hidePagerWhenSinglePage = true;
  @Input() selectable = false;
  @Input() selectionConfig: { repeatable: boolean, listId: string };

  /**
   * The link type of the listable elements
   */
  @Input() linkType: CollectionElementLinkType;

  /**
   * The context of the listable elements
   */
  @Input() context: Context;

  /**
   * Option for hiding the pagination detail
   */
  @Input() hidePaginationDetail = false;

  /**
   * Whether or not to add an import button to the object
   */
  @Input() importable = false;

  /**
   * Config used for the import button
   */
  @Input() importConfig: { importLabel: string };

  /**
   * Whether or not the pagination should be rendered as simple previous and next buttons instead of the normal pagination
   */
  @Input() showPaginator = true;

  /**
   * Emit when one of the listed object has changed.
   */
  @Output() contentChange = new EventEmitter<any>();

  /**
   * If showPaginator is set to true, emit when the previous button is clicked
   */
  @Output() prev = new EventEmitter<boolean>();

  /**
   * If showPaginator is set to true, emit when the next button is clicked
   */
  @Output() next = new EventEmitter<boolean>();

  /**
   * The current listable objects
   */
  private _objects: RemoteData<PaginatedList<ListableObject>>;

  /**
   * Setter for the objects
   * @param objects The new objects
   */
  @Input() set objects(objects: RemoteData<PaginatedList<ListableObject>>) {
    this._objects = objects;
  }

  /**
   * Getter to return the current objects
   */
  get objects() {
    return this._objects;
  }

  /**
   * An event fired when the page is changed.
   * Event's payload equals to the newly selected page.
   */
  @Output() change: EventEmitter<{
    pagination: PaginationComponentOptions,
    sort: SortOptions
  }> = new EventEmitter<{
    pagination: PaginationComponentOptions,
    sort: SortOptions
  }>();

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
   * An event fired when on of the pagination parameters changes
   */
  @Output() paginationChange: EventEmitter<any> = new EventEmitter<any>();

  @Output() deselectObject: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();

  @Output() selectObject: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();

  /**
   * Send an import event to the parent component
   */
  @Output() importObject: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();

  /**
   * An event fired when the sort field is changed.
   * Event's payload equals to the newly selected sort field.
   */
  @Output() sortFieldChange: EventEmitter<string> = new EventEmitter<string>();

  inAndOutputNames: (keyof ObjectListComponent & keyof this)[] = [
    'config',
    'sortConfig',
    'hasBorder',
    'hideGear',
    'hidePagerWhenSinglePage',
    'selectable',
    'selectionConfig',
    'linkType',
    'context',
    'hidePaginationDetail',
    'importable',
    'importConfig',
    'showPaginator',
    'contentChange',
    'prev',
    'next',
    'objects',
    'change',
    'pageChange',
    'pageSizeChange',
    'sortDirectionChange',
    'paginationChange',
    'deselectObject',
    'selectObject',
    'importObject',
    'sortFieldChange',
  ];

  protected getComponentName(): string {
    return 'ObjectListComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/shared/object-list/object-list.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import('./object-list.component');
  }
}
