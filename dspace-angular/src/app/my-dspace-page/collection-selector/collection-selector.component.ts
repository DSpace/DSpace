import { Component } from '@angular/core';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { CollectionListEntry } from '../../shared/collection-dropdown/collection-dropdown.component';

/**
 * This component displays the dialog that shows the list of selectable collections
 * on the MyDSpace page
 */
@Component({
  selector: 'ds-collection-selector',
  templateUrl: './collection-selector.component.html',
  styleUrls: ['./collection-selector.component.scss']
})
export class CollectionSelectorComponent {

  constructor(protected activeModal: NgbActiveModal) {}

  /**
   * Method called when an element has been selected from collection list.
   * Its close the active modal and send selected value to the component container
   *
   * @param event The event object containing a CollectionListEntry
   */
  selectObject(event: CollectionListEntry) {
    this.activeModal.close(event.collection);
  }

  /**
   * Close the modal
   */
  close() {
    this.activeModal.close();
  }
}
