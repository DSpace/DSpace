import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { startWith, take } from 'rxjs/operators';
import { of, Observable } from 'rxjs';
import { RemoteData } from '../../../core/data/remote-data';
import { PaginatedList } from '../../../core/data/paginated-list.model';
import { PaginationComponentOptions } from '../../pagination/pagination-component-options.model';
import { ObjectSelectService } from '../object-select.service';
import { SortOptions } from '../../../core/cache/models/sort-options.model';
import { FeatureID } from '../../../core/data/feature-authorization/feature-id';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';

/**
 * An abstract component used to select DSpaceObjects from a specific list and returning the UUIDs of the selected DSpaceObjects
 */
@Component({
  selector: 'ds-object-select-abstract',
  template: ''
})
export abstract class ObjectSelectComponent<TDomain> implements OnInit, OnDestroy {

  /**
   * A unique key used for the object select service
   */
  @Input()
  key: string;

  /**
   * The list of DSpaceObjects to display
   */
  @Input()
  dsoRD$: Observable<RemoteData<PaginatedList<TDomain>>>;

  /**
   * The pagination options used to display the DSpaceObjects
   */
  @Input()
  paginationOptions: PaginationComponentOptions;

  /**
   * The sorting options used to display the DSpaceObjects
   */
  @Input()
  sortOptions: SortOptions;

  /**
   * The message key used for the confirm button
   * @type {string}
   */
  @Input()
  confirmButton: string;

  /**
   * Authorize check to enable the selection when present.
   */
  @Input()
  featureId: FeatureID;

  /**
   * The message key used for the cancel button
   * @type {string}
   */
  @Input()
  cancelButton: string;

  /**
   * An event fired when the cancel button is clicked
   */
  @Output()
  cancel = new EventEmitter<any>();

  /**
   * EventEmitter to return the selected UUIDs when the confirm button is pressed
   * @type {EventEmitter<string[]>}
   */
  @Output()
  confirm: EventEmitter<string[]> = new EventEmitter<string[]>();

  /**
   * Whether or not to render the confirm button as danger (for example if confirm deletes objects)
   * Defaults to false
   */
  @Input()
  dangerConfirm = false;

  /**
   * The list of selected UUIDs
   */
  selectedIds$: Observable<string[]>;

  constructor(protected objectSelectService: ObjectSelectService,
              protected authorizationService: AuthorizationDataService) {
  }

  ngOnInit(): void {
    this.selectedIds$ = this.objectSelectService.getAllSelected(this.key);
  }

  ngOnDestroy(): void {
    this.objectSelectService.reset(this.key);
  }

  /**
   * Switch the state of a checkbox
   * @param {string} id
   */
  switch(id: string) {
    this.objectSelectService.switch(this.key, id);
  }

  /**
   * Get the current state of a checkbox
   * @param {string} id   The dso's UUID
   * @returns {Observable<boolean>}
   */
  getSelected(id: string): Observable<boolean> {
    return this.objectSelectService.getSelected(this.key, id);
  }

  /**
   * Return if the item can be selected or not due to authorization check.
   */
  canSelect(item: DSpaceObject): Observable<boolean> {
    if (!this.featureId) {
      return of(true);
    }
    return this.authorizationService.isAuthorized(this.featureId, item.self).pipe(startWith(false));
  }

  /**
   * Called when the confirm button is pressed
   * Sends the selected UUIDs to the parent component
   */
  confirmSelected() {
    this.selectedIds$.pipe(
      take(1)
    ).subscribe((ids: string[]) => {
      this.confirm.emit(ids);
      this.objectSelectService.reset(this.key);
    });
  }

  /**
   * Fire a cancel event
   */
  onCancel() {
    this.cancel.emit();
  }

}
