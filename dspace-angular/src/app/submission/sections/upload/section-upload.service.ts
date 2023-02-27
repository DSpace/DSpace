import { Injectable } from '@angular/core';

import { Observable } from 'rxjs';
import { distinctUntilChanged, filter, map } from 'rxjs/operators';
import { Store } from '@ngrx/store';

import { SubmissionState } from '../../submission.reducers';
import {
  DeleteUploadedFileAction,
  EditFileDataAction,
  NewUploadedFileAction
} from '../../objects/submission-objects.actions';
import { submissionUploadedFileFromUuidSelector, submissionUploadedFilesFromIdSelector } from '../../selectors';
import { isUndefined } from '../../../shared/empty.util';
import { WorkspaceitemSectionUploadFileObject } from '../../../core/submission/models/workspaceitem-section-upload-file.model';

/**
 * A service that provides methods to handle submission's bitstream state.
 */
@Injectable()
export class SectionUploadService {

  /**
   * Initialize service variables
   *
   * @param {Store<SubmissionState>} store
   */
  constructor(private store: Store<SubmissionState>) {}

  /**
   * Return submission's bitstream list from state
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @returns {Array}
   *    Returns submission's bitstream list
   */
  public getUploadedFileList(submissionId: string, sectionId: string): Observable<any> {
    return this.store.select(submissionUploadedFilesFromIdSelector(submissionId, sectionId)).pipe(
      map((state) => state),
      distinctUntilChanged());
  }

  /**
   * Return bitstream's metadata
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param fileUUID
   *    The bitstream UUID
   * @returns {Observable}
   *    Emits bitstream's metadata
   */
  public getFileData(submissionId: string, sectionId: string, fileUUID: string): Observable<any> {
    return this.store.select(submissionUploadedFilesFromIdSelector(submissionId, sectionId)).pipe(
      filter((state) => !isUndefined(state)),
      map((state) => {
        let fileState;
        Object.keys(state)
          .filter((key) => state[key].uuid === fileUUID)
          .forEach((key) => fileState = state[key]);
        return fileState;
      }),
      distinctUntilChanged());
  }

  /**
   * Return bitstream's default policies
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param fileUUID
   *    The bitstream UUID
   * @returns {Observable}
   *    Emits bitstream's default policies
   */
  public getDefaultPolicies(submissionId: string, sectionId: string, fileUUID: string): Observable<any> {
    return this.store.select(submissionUploadedFileFromUuidSelector(submissionId, sectionId, fileUUID)).pipe(
      map((state) => state),
      distinctUntilChanged());
  }

  /**
   * Add a new bitstream to the state
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param fileUUID
   *    The bitstream UUID
   * @param data
   *    The [[WorkspaceitemSectionUploadFileObject]] object
   */
  public addUploadedFile(submissionId: string, sectionId: string, fileUUID: string, data: WorkspaceitemSectionUploadFileObject) {
    this.store.dispatch(
      new NewUploadedFileAction(submissionId, sectionId, fileUUID, data)
    );
  }

  /**
   * Update bitstream metadata into the state
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param fileUUID
   *    The bitstream UUID
   * @param data
   *    The [[WorkspaceitemSectionUploadFileObject]] object
   */
  public updateFileData(submissionId: string, sectionId: string, fileUUID: string, data: WorkspaceitemSectionUploadFileObject) {
    this.store.dispatch(
      new EditFileDataAction(submissionId, sectionId, fileUUID, data)
    );
  }

  /**
   * Remove bitstream from the state
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param fileUUID
   *    The bitstream UUID
   */
  public removeUploadedFile(submissionId: string, sectionId: string, fileUUID: string) {
    this.store.dispatch(
      new DeleteUploadedFileAction(submissionId, sectionId, fileUUID)
    );
  }
}
