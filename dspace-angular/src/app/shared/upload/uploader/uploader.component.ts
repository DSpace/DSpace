import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, HostListener, Input, Output, ViewEncapsulation, } from '@angular/core';

import { of as observableOf } from 'rxjs';
import { FileUploader } from 'ng2-file-upload';
import uniqueId from 'lodash/uniqueId';
import { ScrollToService } from '@nicky-lenaers/ngx-scroll-to';

import { UploaderOptions } from './uploader-options.model';
import { hasValue, isNotEmpty, isUndefined } from '../../empty.util';
import { UploaderProperties } from './uploader-properties.model';
import { HttpXsrfTokenExtractor } from '@angular/common/http';
import { XSRF_COOKIE, XSRF_REQUEST_HEADER, XSRF_RESPONSE_HEADER } from '../../../core/xsrf/xsrf.interceptor';
import { CookieService } from '../../../core/services/cookie.service';
import { DragService } from '../../../core/drag.service';

@Component({
  selector: 'ds-uploader',
  templateUrl: 'uploader.component.html',
  styleUrls: ['uploader.component.scss'],
  changeDetection: ChangeDetectionStrategy.Default,
  encapsulation: ViewEncapsulation.Emulated
})

export class UploaderComponent {

  /**
   * The message to show when drag files on the drop zone
   */
  @Input() dropMsg: string;

  /**
   * The message to show when drag files on the window document
   */
  @Input() dropOverDocumentMsg: string;

  /**
   * The message to show when drag files on the window document
   */
  @Input() enableDragOverDocument: boolean;

  /**
   * The function to call before an upload
   */
  @Input() onBeforeUpload: () => void;

  /**
   * Configuration for the ng2-file-upload component.
   */
  @Input() uploadFilesOptions: UploaderOptions;

  /**
   * Extra properties to be passed with the form-data of the upload
   */
  @Input() uploadProperties: UploaderProperties;

  /**
   * The function to call when upload is completed
   */
  @Output() onCompleteItem: EventEmitter<any> = new EventEmitter<any>();

  /**
   * The function to call on error occurred
   */
  @Output() onUploadError: EventEmitter<any> = new EventEmitter<any>();

  /**
   * The function to call when a file is selected
   */
  @Output() onFileSelected: EventEmitter<any> = new EventEmitter<any>();

  public uploader: FileUploader;
  public uploaderId: string;
  public isOverBaseDropZone = observableOf(false);
  public isOverDocumentDropZone = observableOf(false);

  @HostListener('window:dragover', ['$event'])
  onDragOver(event: any) {

    if (this.enableDragOverDocument && this.dragService.isAllowedDragOverPage()) {
      // Show drop area on the page
      event.preventDefault();
      if ((event.target as any).tagName !== 'HTML') {
        this.isOverDocumentDropZone = observableOf(true);
      }
    }
  }

  constructor(
    private cdr: ChangeDetectorRef,
    private scrollToService: ScrollToService,
    private dragService: DragService,
    private tokenExtractor: HttpXsrfTokenExtractor,
    private cookieService: CookieService
  ) {
  }

  /**
   * Method provided by Angular. Invoked after the constructor.
   */
  ngOnInit() {
    this.uploaderId = 'ds-drag-and-drop-uploader' + uniqueId();
    this.checkConfig(this.uploadFilesOptions);
    this.uploader = new FileUploader({
      url: this.uploadFilesOptions.url,
      authToken: this.uploadFilesOptions.authToken,
      disableMultipart: this.uploadFilesOptions.disableMultipart,
      itemAlias: this.uploadFilesOptions.itemAlias,
      removeAfterUpload: true,
      autoUpload: this.uploadFilesOptions.autoUpload,
      method: this.uploadFilesOptions.method,
      queueLimit: this.uploadFilesOptions.maxFileNumber,
    });

    if (isUndefined(this.enableDragOverDocument)) {
      this.enableDragOverDocument = false;
    }
    if (isUndefined(this.dropMsg)) {
      this.dropMsg = 'uploader.drag-message';
    }
    if (isUndefined(this.dropOverDocumentMsg)) {
      this.dropOverDocumentMsg = 'uploader.drag-message';
    }
  }

  ngAfterViewInit() {
    this.uploader.onAfterAddingAll = ((items) => {
      this.onFileSelected.emit(items);
    });
    if (isUndefined(this.onBeforeUpload)) {
      this.onBeforeUpload = () => {return;};
    }
    this.uploader.onBeforeUploadItem = (item) => {
      if (item.url !== this.uploader.options.url) {
        item.url = this.uploader.options.url;
      }
      // Ensure the current XSRF token is included in every upload request (token may change between items uploaded)
      this.uploader.options.headers = [{ name: XSRF_REQUEST_HEADER, value: this.tokenExtractor.getToken() }];
      this.onBeforeUpload();
      this.isOverDocumentDropZone = observableOf(false);
    };
    if (hasValue(this.uploadProperties)) {
      this.uploader.onBuildItemForm = (item, form) => {
        form.append('properties', JSON.stringify(this.uploadProperties));
      };
    }
    this.uploader.onCompleteItem = (item: any, response: any, status: any, headers: any) => {
      // Check for a changed XSRF token in response & save new token if found (to both cookie & header for next request)
      // NOTE: this is only necessary because ng2-file-upload doesn't use an Http service and therefore never
      // triggers our xsrf.interceptor.ts. See this bug: https://github.com/valor-software/ng2-file-upload/issues/950
      const token = headers[XSRF_RESPONSE_HEADER.toLowerCase()];
      if (isNotEmpty(token)) {
        this.saveXsrfToken(token);
        this.uploader.options.headers = [{ name: XSRF_REQUEST_HEADER, value: this.tokenExtractor.getToken() }];
      }

      if (isNotEmpty(response)) {
        const responsePath = JSON.parse(response);
        this.onCompleteItem.emit(responsePath);
      }
    };
    this.uploader.onErrorItem = (item: any, response: any, status: any, headers: any) => {
      // Check for a changed XSRF token in response & save new token if found (to both cookie & header for next request)
      // NOTE: this is only necessary because ng2-file-upload doesn't use an Http service and therefore never
      // triggers our xsrf.interceptor.ts. See this bug: https://github.com/valor-software/ng2-file-upload/issues/950
      const token = headers[XSRF_RESPONSE_HEADER.toLowerCase()];
      if (isNotEmpty(token)) {
        this.saveXsrfToken(token);
        this.uploader.options.headers = [{ name: XSRF_REQUEST_HEADER, value: this.tokenExtractor.getToken() }];
      }

      this.onUploadError.emit({ item: item, response: response, status: status, headers: headers });
      this.uploader.cancelAll();
    };
    this.uploader.onProgressAll = () => this.onProgress();
    this.uploader.onProgressItem = () => this.onProgress();
  }

  /**
   * Called when files are dragged on the base drop area.
   */
  public fileOverBase(isOver: boolean): void {
    this.isOverBaseDropZone = observableOf(isOver);
  }

  /**
   * Called when files are dragged on the window document drop area.
   */
  public fileOverDocument(isOver: boolean) {
    if (!isOver) {
      this.isOverDocumentDropZone = observableOf(isOver);
    }
  }

  private onProgress() {
    this.cdr.detectChanges();
  }

  /**
   * Ensure options passed contains the required properties.
   *
   * @param fileUploadOptions
   *    The upload-files options object.
   */
  private checkConfig(fileUploadOptions: any) {
    const required = ['url', 'authToken', 'disableMultipart', 'itemAlias'];
    const missing = required.filter((prop) => {
      return !((prop in fileUploadOptions) && fileUploadOptions[prop] !== '');
    });
    if (0 < missing.length) {
      throw new Error('UploadFiles: Argument is missing the following required properties: ' + missing.join(', '));
    }
  }

  /**
   * Save XSRF token found in response. This is a temporary copy of the method in xsrf.interceptor.ts
   * It can be removed once ng2-file-upload supports interceptors (see https://github.com/valor-software/ng2-file-upload/issues/950),
   * or we switch to a new upload library (see https://github.com/DSpace/dspace-angular/issues/820)
   * @param token token found
   */
  private saveXsrfToken(token: string) {
    // Save token value as a *new* value of our client-side XSRF-TOKEN cookie.
    // This is the cookie that is parsed by Angular's tokenExtractor(),
    // which we will send back in the X-XSRF-TOKEN header per Angular best practices.
    this.cookieService.remove(XSRF_COOKIE);
    this.cookieService.set(XSRF_COOKIE, token);
  }

}
