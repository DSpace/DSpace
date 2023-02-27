import { Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';
import uniqueId from 'lodash/uniqueId';
import { FileUploader } from 'ng2-file-upload';
import { Observable, of as observableOf } from 'rxjs';
import { UploaderOptions } from '../uploader/uploader-options.model';

/**
 * Component to have a file dropzone without that dropping/choosing a file in browse automatically triggers
 * the uploader, instead an event is emitted with the file that was added.
 *
 * Here only one file is allowed to be selected, so if one is selected/dropped the message changes to a
 * replace message.
 */
@Component({
  selector: 'ds-file-dropzone-no-uploader',
  templateUrl: './file-dropzone-no-uploader.component.html',
  styleUrls: ['./file-dropzone-no-uploader.scss']
})
export class FileDropzoneNoUploaderComponent implements OnInit {

  public isOverDocumentDropZone: Observable<boolean>;
  public uploader: FileUploader;
  public uploaderId: string;

  @Input() dropMessageLabel: string;
  @Input() dropMessageLabelReplacement: string;

  /**
   * The function to call when file is added
   */
  @Output() onFileAdded: EventEmitter<any> = new EventEmitter<any>();

  /**
   * The uploader configuration options
   * @type {UploaderOptions}
   */
  uploadFilesOptions: UploaderOptions = Object.assign(new UploaderOptions(), {
    // URL needs to contain something to not produce any errors. We are using onFileDrop; not the uploader
    url: 'placeholder',
  });

  /**
   * The current value of the file
   */
  fileObject: File;

  /**
   * Method provided by Angular. Invoked after the constructor.
   */
  ngOnInit() {
    this.uploaderId = 'ds-drag-and-drop-uploader' + uniqueId();
    this.isOverDocumentDropZone = observableOf(false);
    this.uploader = new FileUploader({
      // required, but using onFileDrop, not uploader
      url: 'placeholder',
    });
  }

  @HostListener('window:drop', ['$event'])
  onDrop(event: any) {
    event.preventDefault();
  }

  @HostListener('window:dragover', ['$event'])
  onDragOver(event: any) {
    // Show drop area on the page
    event.preventDefault();
    if ((event.target as any).tagName !== 'HTML') {
      this.isOverDocumentDropZone = observableOf(true);
    }
  }

  /**
   * Called when files are dragged on the window document drop area.
   */
  public fileOverDocument(isOver: boolean) {
    if (!isOver) {
      this.isOverDocumentDropZone = observableOf(isOver);
    }
  }

  /**
   * Set file
   * @param files
   */
  setFile(files) {
    this.fileObject = files.length > 0 ? files[0] : undefined;
    this.onFileAdded.emit(this.fileObject);
  }

}
