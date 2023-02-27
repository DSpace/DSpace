import { Component, Input, OnInit } from '@angular/core';

import { WorkspaceitemSectionUploadFileObject } from '../../../../../core/submission/models/workspaceitem-section-upload-file.model';
import { isNotEmpty } from '../../../../../shared/empty.util';
import { Metadata } from '../../../../../core/shared/metadata.utils';
import { MetadataMap, MetadataValue } from '../../../../../core/shared/metadata.models';

/**
 * This component allow to show bitstream's metadata
 */
@Component({
  selector: 'ds-submission-section-upload-file-view',
  templateUrl: './section-upload-file-view.component.html',
})
export class SubmissionSectionUploadFileViewComponent implements OnInit {

  /**
   * The bitstream's metadata data
   * @type {WorkspaceitemSectionUploadFileObject}
   */
  @Input() fileData: WorkspaceitemSectionUploadFileObject;

  /**
   * The [[MetadataMap]] object
   * @type {MetadataMap}
   */
  public metadata: MetadataMap = Object.create({});

  /**
   * The bitstream's title key
   * @type {string}
   */
  public fileTitleKey = 'Title';

  /**
   * The bitstream's description key
   * @type {string}
   */
  public fileDescrKey = 'Description';

  /**
   * Initialize instance variables
   */
  ngOnInit() {
    if (isNotEmpty(this.fileData.metadata)) {
      this.metadata[this.fileTitleKey] = Metadata.all(this.fileData.metadata, 'dc.title');
      this.metadata[this.fileDescrKey] = Metadata.all(this.fileData.metadata, 'dc.description');
    }
  }

  /**
   * Gets all matching metadata in the map(s)
   *
   * @param metadataKey
   *    The metadata key(s) in scope
   * @returns {MetadataValue[]}
   *    The matching values
   */
  getAllMetadataValue(metadataKey: string): MetadataValue[] {
    return Metadata.all(this.metadata, metadataKey);
  }
}
