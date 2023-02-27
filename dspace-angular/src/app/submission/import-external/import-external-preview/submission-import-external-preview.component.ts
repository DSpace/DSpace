import { Component, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { ExternalSourceEntry } from '../../../core/shared/external-source-entry.model';
import { MetadataValue } from '../../../core/shared/metadata.models';
import { Metadata } from '../../../core/shared/metadata.utils';
import { CollectionListEntry } from '../../../shared/collection-dropdown/collection-dropdown.component';
import { mergeMap } from 'rxjs/operators';
import { SubmissionService } from '../../submission.service';
import { SubmissionObject } from '../../../core/submission/models/submission-object.model';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { SubmissionImportExternalCollectionComponent } from '../import-external-collection/submission-import-external-collection.component';

/**
 * This component display a preview of an external source item.
 */
@Component({
  selector: 'ds-submission-import-external-preview',
  styleUrls: ['./submission-import-external-preview.component.scss'],
  templateUrl: './submission-import-external-preview.component.html'
})
export class SubmissionImportExternalPreviewComponent implements OnInit {
  /**
   * The external source entry
   */
  @Input() public externalSourceEntry: ExternalSourceEntry;
  /**
   * The entry metadata list
   */
  public metadataList: { key: string, value: MetadataValue }[];
  /**
   * The label prefix to use to generate the translation label
   */
  public labelPrefix: string;
  /**
   * The modal for the entry preview
   */
  modalRef: NgbModalRef;

  /**
   * Initialize the component variables.
   * @param {NgbActiveModal} activeModal
   * @param {SubmissionService} submissionService
   * @param {NgbModal} modalService
   * @param {Router} router
   * @param {NotificationsService} notificationService
   */
  constructor(
    private activeModal: NgbActiveModal,
    private submissionService: SubmissionService,
    private modalService: NgbModal,
    private router: Router,
    private notificationService: NotificationsService
  ) { }

  /**
   * Metadata initialization for HTML display.
   */
  ngOnInit(): void {
    this.metadataList = [];
    const metadataKeys = Object.keys(this.externalSourceEntry.metadata);
    metadataKeys.forEach((key) => {
      this.metadataList.push({
        key: key,
        value: Metadata.first(this.externalSourceEntry.metadata, key)
      });
    });
  }

  /**
   * Closes the modal.
   */
  public closeMetadataModal(): void {
    this.activeModal.dismiss(false);
  }

  /**
   * Start the import of an entry by opening up a collection choice modal window.
   */
  public import(): void {
    this.modalRef = this.modalService.open(SubmissionImportExternalCollectionComponent, {
      size: 'lg',
    });
    this.modalRef.componentInstance.entityType = this.labelPrefix;
    this.closeMetadataModal();

    this.modalRef.componentInstance.selectedEvent.pipe(
      mergeMap((collectionListEntry: CollectionListEntry) => {
        return this.submissionService.createSubmissionFromExternalSource(this.externalSourceEntry._links.self.href, collectionListEntry.collection.id);
      })
    ).subscribe((submissionObjects: SubmissionObject[]) => {
      let isValid = false;
      if (submissionObjects.length === 1) {
        if (submissionObjects[0] !== null) {
          isValid = true;
          this.router.navigateByUrl('/workspaceitems/' + submissionObjects[0].id + '/edit');
        }
      }
      if (!isValid) {
        this.notificationService.error('submission.import-external.preview.error.import.title', 'submission.import-external.preview.error.import.body');
      }
      this.modalRef.close();
    });
  }
}
