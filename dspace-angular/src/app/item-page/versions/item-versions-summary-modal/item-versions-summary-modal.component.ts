import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { BehaviorSubject } from 'rxjs';
import { ModalBeforeDismiss } from '../../../shared/interfaces/modal-before-dismiss.interface';

@Component({
  selector: 'ds-item-versions-summary-modal',
  templateUrl: './item-versions-summary-modal.component.html',
  styleUrls: ['./item-versions-summary-modal.component.scss']
})
export class ItemVersionsSummaryModalComponent implements OnInit, ModalBeforeDismiss {

  versionNumber: number;
  newVersionSummary: string;
  firstVersion = true;
  submitted$: BehaviorSubject<boolean>;

  @Output() createVersionEvent: EventEmitter<string> = new EventEmitter<string>();

  constructor(
    protected activeModal: NgbActiveModal,
  ) {
  }

  ngOnInit() {
    this.submitted$ = new BehaviorSubject<boolean>(false);
  }

  onModalClose() {
    this.activeModal.dismiss();
  }

  beforeDismiss(): boolean | Promise<boolean> {
    // prevent the modal from being dismissed after version creation is initiated
    return !this.submitted$.getValue();
  }

  onModalSubmit() {
    this.createVersionEvent.emit(this.newVersionSummary);
    this.submitted$.next(true);
    // NOTE: the caller of this modal is responsible for closing it,
    //       e.g. after the version creation POST request completed.
  }

}
