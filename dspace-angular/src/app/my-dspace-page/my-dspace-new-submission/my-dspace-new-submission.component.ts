import { ChangeDetectorRef, Component, EventEmitter, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';

import { Subscription } from 'rxjs';
import { first } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { AuthService } from '../../core/auth/auth.service';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { UploaderOptions } from '../../shared/upload/uploader/uploader-options.model';
import { HALEndpointService } from '../../core/shared/hal-endpoint.service';
import { hasValue } from '../../shared/empty.util';
import { SearchResult } from '../../shared/search/models/search-result.model';
import { CollectionSelectorComponent } from '../collection-selector/collection-selector.component';
import { UploaderComponent } from '../../shared/upload/uploader/uploader.component';
import { UploaderError } from '../../shared/upload/uploader/uploader-error.model';
import { Router } from '@angular/router';

/**
 * This component represents the whole mydspace page header
 */
@Component({
  selector: 'ds-my-dspace-new-submission',
  styleUrls: ['./my-dspace-new-submission.component.scss'],
  templateUrl: './my-dspace-new-submission.component.html'
})
export class MyDSpaceNewSubmissionComponent implements OnDestroy, OnInit {
  /**
   * Output that emits the workspace item when the upload has completed
   */
  @Output() uploadEnd = new EventEmitter<SearchResult<DSpaceObject>[]>();

  /**
   * The UploaderOptions object
   */
  public uploadFilesOptions: UploaderOptions = new UploaderOptions();

  /**
   * Subscription to unsubscribe from
   */
  private sub: Subscription;

  /**
   * Reference to uploaderComponent
   */
  @ViewChild(UploaderComponent) uploaderComponent: UploaderComponent;

  /**
   * Initialize instance variables
   *
   * @param {AuthService} authService
   * @param {ChangeDetectorRef} changeDetectorRef
   * @param {HALEndpointService} halService
   * @param {NotificationsService} notificationsService
   * @param {TranslateService} translate
   * @param {NgbModal} modalService
   * @param {Router} router
   */
  constructor(private authService: AuthService,
              private changeDetectorRef: ChangeDetectorRef,
              private halService: HALEndpointService,
              private notificationsService: NotificationsService,
              private translate: TranslateService,
              private modalService: NgbModal,
              private router: Router) {
  }

  /**
   * Initialize url and Bearer token
   */
  ngOnInit() {
    this.uploadFilesOptions.autoUpload = false;
    this.sub = this.halService.getEndpoint('workspaceitems').pipe(first()).subscribe((url) => {
        this.uploadFilesOptions.url = url;
        this.uploadFilesOptions.authToken = this.authService.buildAuthHeader();
        this.changeDetectorRef.detectChanges();
      }
    );
  }

  /**
   * Method called when file upload is completed to notify upload status
   */
  public onCompleteItem(res) {
    if (res && res._embedded && res._embedded.workspaceitems && res._embedded.workspaceitems.length > 0) {
      const workspaceitems = res._embedded.workspaceitems;
      this.uploadEnd.emit(workspaceitems);

      if (workspaceitems.length === 1) {
        const link = '/workspaceitems/' + workspaceitems[0].id + '/edit';
        // To avoid confusion and ambiguity, redirect the user on the publication page.
        this.router.navigateByUrl(link);
      } else if (workspaceitems.length > 1) {
        this.notificationsService.success(null, this.translate.get('mydspace.upload.upload-multiple-successful', {qty: workspaceitems.length}));
      }

    } else {
      this.notificationsService.error(null, this.translate.get('mydspace.upload.upload-failed'));
    }
  }

  /**
   * Method called on file upload error
   */
  public onUploadError(error: UploaderError) {
    let errorMessageKey = 'mydspace.upload.upload-failed';
    if (hasValue(error.status) && error.status === 422) {
      errorMessageKey = 'mydspace.upload.upload-failed-manyentries';
    }
    this.notificationsService.error(null, this.translate.get(errorMessageKey));
  }

  /**
   * Method invoked after all file are loaded from upload plugin
   */
  afterFileLoaded(items) {
    const uploader = this.uploaderComponent.uploader;
    if (hasValue(items) && items.length > 1) {
      this.notificationsService.error(null, this.translate.get('mydspace.upload.upload-failed-moreonefile'));
      uploader.clearQueue();
      this.changeDetectorRef.detectChanges();
    } else {
      const modalRef = this.modalService.open(CollectionSelectorComponent);
      // When the dialog are closes its takes the collection selected and
      // uploads choosed file after adds owningCollection parameter
      modalRef.result.then( (result) => {
        uploader.onBuildItemForm = (fileItem: any, form: any) => {
          form.append('owningCollection', result.uuid);
        };
        uploader.uploadAll();
      });
    }
  }

  /**
   * Unsubscribe from the subscription
   */
  ngOnDestroy(): void {
    if (hasValue(this.sub)) {
      this.sub.unsubscribe();
    }
  }
}
