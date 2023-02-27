import { Component, EventEmitter, Output } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'ds-item-versions-delete-modal',
  templateUrl: './item-versions-delete-modal.component.html',
  styleUrls: ['./item-versions-delete-modal.component.scss']
})
export class ItemVersionsDeleteModalComponent {
  /**
   * An event fired when the cancel or confirm button is clicked, with respectively false or true
   */
  @Output()
  response = new EventEmitter<boolean>();

  versionNumber: number;

  constructor(
    protected activeModal: NgbActiveModal,) {
  }

  onModalClose() {
    this.response.emit(false);
    this.activeModal.dismiss();
  }

  onModalSubmit() {
    this.response.emit(true);
    this.activeModal.close();
  }

}
