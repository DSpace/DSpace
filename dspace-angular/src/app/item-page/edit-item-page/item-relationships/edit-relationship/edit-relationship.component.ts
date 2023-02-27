import { Component, Input, OnChanges } from '@angular/core';
import { combineLatest as observableCombineLatest, Observable, of } from 'rxjs';
import { filter, map, switchMap, take } from 'rxjs/operators';
import {
  DeleteRelationship,
  RelationshipIdentifiable
} from '../../../../core/data/object-updates/object-updates.reducer';
import { ObjectUpdatesService } from '../../../../core/data/object-updates/object-updates.service';
import { Item } from '../../../../core/shared/item.model';
import { getFirstSucceededRemoteData, getRemoteDataPayload } from '../../../../core/shared/operators';
import { ViewMode } from '../../../../core/shared/view-mode.model';
import { hasValue, isNotEmpty } from '../../../../shared/empty.util';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { FieldUpdate } from '../../../../core/data/object-updates/field-update.model';
import { FieldChangeType } from '../../../../core/data/object-updates/field-change-type.model';

@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'ds-edit-relationship',
  styleUrls: ['./edit-relationship.component.scss'],
  templateUrl: './edit-relationship.component.html',
})
export class EditRelationshipComponent implements OnChanges {
  /**
   * The current field, value and state of the relationship
   */
  @Input() fieldUpdate: FieldUpdate;

  /**
   * The current url of this page
   */
  @Input() url: string;

  /**
   * The item being edited
   */
  @Input() editItem: Item;

  /**
   * The relationship being edited
   */
  get relationship() {
    return this.update.relationship;
  }

  get update() {
    return this.fieldUpdate.field as RelationshipIdentifiable;
  }

  get nameVariant() {
    return this.update.nameVariant;
  }

  public leftItem$: Observable<Item>;
  public rightItem$: Observable<Item>;

  /**
   * The related item of this relationship
   */
  relatedItem$: Observable<Item>;

  /**
   * The view-mode we're currently on
   */
  viewMode = ViewMode.ListElement;

  /**
   * Reference to NgbModal
   */
  public modalRef: NgbModalRef;

  constructor(
    private objectUpdatesService: ObjectUpdatesService,
    private modalService: NgbModal,
  ) {
  }

  /**
   * Sets the current relationship based on the fieldUpdate input field
   */
  ngOnChanges(): void {
    if (this.relationship && (!!this.relationship.leftItem || !!this.relationship.rightItem)) {
      this.leftItem$ = this.relationship.leftItem.pipe(
        getFirstSucceededRemoteData(),
        getRemoteDataPayload(),
        filter((item: Item) => hasValue(item) && isNotEmpty(item.uuid))
      );
      this.rightItem$ = this.relationship.rightItem.pipe(
        getFirstSucceededRemoteData(),
        getRemoteDataPayload(),
        filter((item: Item) => hasValue(item) && isNotEmpty(item.uuid))
      );
      this.relatedItem$ = observableCombineLatest(
        this.leftItem$,
        this.rightItem$,
      ).pipe(
        map((items: Item[]) =>
          items.find((item) => item.uuid !== this.editItem.uuid)
        )
      );
    } else {
      this.relatedItem$ = of(this.update.relatedItem);
    }
  }

  /**
   * Sends a new remove update for this field to the object updates service
   */
  remove(): void {
    this.closeVirtualMetadataModal();
    observableCombineLatest(
      this.leftItem$,
      this.rightItem$,
    ).pipe(
      map((items: Item[]) =>
        items.map((item) => this.objectUpdatesService
          .isSelectedVirtualMetadata(this.url, this.relationship.id, item.uuid))
      ),
      switchMap((selection$) => observableCombineLatest(selection$)),
      map((selection: boolean[]) => {
        return Object.assign({},
          this.fieldUpdate.field,
          {
            keepLeftVirtualMetadata: selection[0] === true,
            keepRightVirtualMetadata: selection[1] === true,
          }
        ) as DeleteRelationship;
      }),
      take(1),
    ).subscribe((deleteRelationship: DeleteRelationship) =>
      this.objectUpdatesService.saveRemoveFieldUpdate(this.url, deleteRelationship)
    );
  }

  openVirtualMetadataModal(content: any) {
    this.modalRef = this.modalService.open(content);
  }

  closeVirtualMetadataModal() {
    this.modalRef.close();
  }

  /**
   * Cancels the current update for this field in the object updates service
   */
  undo(): void {
    this.objectUpdatesService.removeSingleFieldUpdate(this.url, this.fieldUpdate.field.uuid);
  }

  /**
   * Check if a user should be allowed to remove this field
   */
  canRemove(): boolean {
    return this.fieldUpdate.changeType !== FieldChangeType.REMOVE
      && this.fieldUpdate.changeType !== FieldChangeType.ADD;
  }

  /**
   * Check if a user should be allowed to cancel the update to this field
   */
  canUndo(): boolean {
    return this.fieldUpdate.changeType >= 0;
  }
}
