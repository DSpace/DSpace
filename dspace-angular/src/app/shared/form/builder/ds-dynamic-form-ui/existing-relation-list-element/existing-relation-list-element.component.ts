/* eslint-disable max-classes-per-file */
import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output } from '@angular/core';
import { Store } from '@ngrx/store';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AppState } from '../../../../../app.reducer';
import { Item } from '../../../../../core/shared/item.model';
import { getAllSucceededRemoteData, getRemoteDataPayload } from '../../../../../core/shared/operators';
import { hasValue, isNotEmpty } from '../../../../empty.util';
import { ItemSearchResult } from '../../../../object-collection/shared/item-search-result.model';
import { SelectableListService } from '../../../../object-list/selectable-list/selectable-list.service';
import { RelationshipOptions } from '../../models/relationship-options.model';
import { RemoveRelationshipAction } from '../relation-lookup-modal/relationship.actions';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { ReorderableRelationship } from '../existing-metadata-list-element/existing-metadata-list-element.component';
import { SubmissionService } from '../../../../../submission/submission.service';

/**
 * Abstract class that defines objects that can be reordered
 */
export abstract class Reorderable {

  constructor(public oldIndex?: number, public newIndex?: number) {
  }

  /**
   * Return the id for this Reorderable
   */
  abstract getId(): string;

  /**
   * Return the place metadata for this Reorderable
   */
  abstract getPlace(): number;

  /**
   * Update the Reorderable
   */
  abstract update(): Observable<any>;

  /**
   * Returns true if the oldIndex of this Reorderable
   * differs from the newIndex
   */
  get hasMoved(): boolean {
    return this.oldIndex !== this.newIndex;
  }
}

/**
 * Represents a single existing relationship value as metadata in submission
 */
@Component({
  selector: 'ds-existing-relation-list-element',
  templateUrl: './existing-relation-list-element.component.html',
  styleUrls: ['./existing-relation-list-element.component.scss']
})
export class ExistingRelationListElementComponent implements OnInit, OnChanges, OnDestroy {
  @Input() listId: string;
  @Input() submissionItem: Item;
  @Input() reoRel: ReorderableRelationship;
  @Input() metadataFields: string[];
  @Input() relationshipOptions: RelationshipOptions;
  @Input() submissionId: string;
  relatedItem$: BehaviorSubject<Item> = new BehaviorSubject<Item>(undefined);
  viewType = ViewMode.ListElement;
  @Output() remove: EventEmitter<any> = new EventEmitter();

  /**
   * List of subscriptions to unsubscribe from
   */
  private subs: Subscription[] = [];

  constructor(
    private selectableListService: SelectableListService,
    private submissionService: SubmissionService,
    private store: Store<AppState>
  ) {
  }

  ngOnInit(): void {
    this.ngOnChanges();
  }

  /**
   * Change callback for the component
   */
  ngOnChanges() {
    if (hasValue(this.reoRel)) {
      const item$ = this.reoRel.useLeftItem ?
        this.reoRel.relationship.leftItem : this.reoRel.relationship.rightItem;
      this.subs.push(item$.pipe(
        getAllSucceededRemoteData(),
        getRemoteDataPayload(),
        filter((item: Item) => hasValue(item) && isNotEmpty(item.uuid))
      ).subscribe((item: Item) => {
        this.relatedItem$.next(item);
      }));
    }

  }

  /**
   * Removes the selected relationship from the list
   */
  removeSelection() {
    this.submissionService.dispatchSave(this.submissionId);
    this.selectableListService.deselectSingle(this.listId, Object.assign(new ItemSearchResult(), { indexableObject: this.relatedItem$.getValue() }));
    this.store.dispatch(new RemoveRelationshipAction(this.submissionItem, this.relatedItem$.getValue(), this.relationshipOptions.relationshipType, this.submissionId));
  }

  /**
   * Unsubscribe from all subscriptions
   */
  ngOnDestroy(): void {
    this.subs
      .filter((sub) => hasValue(sub))
      .forEach((sub) => sub.unsubscribe());
  }

}

