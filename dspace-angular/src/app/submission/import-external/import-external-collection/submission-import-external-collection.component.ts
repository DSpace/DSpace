import { Component, EventEmitter, Output } from '@angular/core';
import { CollectionListEntry } from '../../../shared/collection-dropdown/collection-dropdown.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

/**
 * Wrap component for 'ds-collection-dropdown'.
 */
@Component({
  selector: 'ds-submission-import-external-collection',
  styleUrls: ['./submission-import-external-collection.component.scss'],
  templateUrl: './submission-import-external-collection.component.html'
})
export class SubmissionImportExternalCollectionComponent {
  /**
   * The event passed by 'ds-collection-dropdown'.
   */
  @Output() public selectedEvent = new EventEmitter<CollectionListEntry>();

  /**
   * If present this value is used to filter collection list by entity type
   */
  public entityType: string;

  /**
   * If collection searching is pending or not
   */
  public loading = true;

  /**
   * Initialize the component variables.
   * @param {NgbActiveModal} activeModal
   */
  constructor(
    private activeModal: NgbActiveModal
  ) { }

  /**
   * This method emits the selected Collection from the 'selectedEvent' variable.
   */
  public selectObject(object: CollectionListEntry): void {
    this.selectedEvent.emit(object);
  }

  /**
   * This method closes the modal.
   */
  public closeCollectionModal(): void {
    this.activeModal.dismiss(false);
  }

  /**
   * Propagate the onlySelectable collection
   * @param theOnlySelectable
   */
  public theOnlySelectable(theOnlySelectable: CollectionListEntry) {
    this.selectedEvent.emit(theOnlySelectable);
  }

  /**
   * Set the hasChoice state
   * @param hasChoice
   */
  public searchComplete() {
    this.loading = false;
  }

  /**
   * If the component is in loading state.
   */
  public isLoading(): boolean {
    return !!this.loading;
  }

}
