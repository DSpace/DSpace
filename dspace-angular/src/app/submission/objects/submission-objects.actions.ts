/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';

import { type } from '../../shared/ngrx/type';
import { WorkspaceitemSectionUploadFileObject } from '../../core/submission/models/workspaceitem-section-upload-file.model';
import {
  WorkspaceitemSectionDataType,
  WorkspaceitemSectionsObject
} from '../../core/submission/models/workspaceitem-sections.model';
import { SubmissionObject } from '../../core/submission/models/submission-object.model';
import { SubmissionDefinitionsModel } from '../../core/config/models/config-submission-definitions.model';
import { SectionsType } from '../sections/sections-type';
import { Item } from '../../core/shared/item.model';
import { SectionVisibility } from './section-visibility.model';
import { SubmissionError } from './submission-error.model';
import { SubmissionSectionError } from './submission-section-error.model';

/**
 * For each action type in an action group, make a simple
 * enum object for all of this group's action types.
 *
 * The 'type' utility function coerces strings into string
 * literal types and runs a simple check to guarantee all
 * action types in the application are unique.
 */
export const SubmissionObjectActionTypes = {
  // Section types
  INIT_SUBMISSION_FORM: type('dspace/submission/INIT_SUBMISSION_FORM'),
  RESET_SUBMISSION_FORM: type('dspace/submission/RESET_SUBMISSION_FORM'),
  CANCEL_SUBMISSION_FORM: type('dspace/submission/CANCEL_SUBMISSION_FORM'),
  COMPLETE_INIT_SUBMISSION_FORM: type('dspace/submission/COMPLETE_INIT_SUBMISSION_FORM'),
  SAVE_FOR_LATER_SUBMISSION_FORM: type('dspace/submission/SAVE_FOR_LATER_SUBMISSION_FORM'),
  SAVE_FOR_LATER_SUBMISSION_FORM_SUCCESS: type('dspace/submission/SAVE_FOR_LATER_SUBMISSION_FORM_SUCCESS'),
  SAVE_FOR_LATER_SUBMISSION_FORM_ERROR: type('dspace/submission/SAVE_FOR_LATER_SUBMISSION_FORM_ERROR'),
  SAVE_SUBMISSION_FORM: type('dspace/submission/SAVE_SUBMISSION_FORM'),
  SAVE_SUBMISSION_FORM_SUCCESS: type('dspace/submission/SAVE_SUBMISSION_FORM_SUCCESS'),
  SAVE_SUBMISSION_FORM_ERROR: type('dspace/submission/SAVE_SUBMISSION_FORM_ERROR'),
  SAVE_SUBMISSION_SECTION_FORM: type('dspace/submission/SAVE_SUBMISSION_SECTION_FORM'),
  SAVE_SUBMISSION_SECTION_FORM_SUCCESS: type('dspace/submission/SAVE_SUBMISSION_SECTION_FORM_SUCCESS'),
  SAVE_SUBMISSION_SECTION_FORM_ERROR: type('dspace/submission/SAVE_SUBMISSION_SECTION_FORM_ERROR'),
  CHANGE_SUBMISSION_COLLECTION: type('dspace/submission/CHANGE_SUBMISSION_COLLECTION'),
  SET_ACTIVE_SECTION: type('dspace/submission/SET_ACTIVE_SECTION'),
  INIT_SECTION: type('dspace/submission/INIT_SECTION'),
  ENABLE_SECTION: type('dspace/submission/ENABLE_SECTION'),
  DISABLE_SECTION: type('dspace/submission/DISABLE_SECTION'),
  SET_SECTION_FORM_ID: type('dspace/submission/SET_SECTION_FORM_ID'),
  SECTION_STATUS_CHANGE: type('dspace/submission/SECTION_STATUS_CHANGE'),
  SECTION_LOADING_STATUS_CHANGE: type('dspace/submission/SECTION_LOADING_STATUS_CHANGE'),
  UPDATE_SECTION_DATA: type('dspace/submission/UPDATE_SECTION_DATA'),
  UPDATE_SECTION_DATA_SUCCESS: type('dspace/submission/UPDATE_SECTION_DATA_SUCCESS'),
  SAVE_AND_DEPOSIT_SUBMISSION: type('dspace/submission/SAVE_AND_DEPOSIT_SUBMISSION'),
  DEPOSIT_SUBMISSION: type('dspace/submission/DEPOSIT_SUBMISSION'),
  DEPOSIT_SUBMISSION_SUCCESS: type('dspace/submission/DEPOSIT_SUBMISSION_SUCCESS'),
  DEPOSIT_SUBMISSION_ERROR: type('dspace/submission/DEPOSIT_SUBMISSION_ERROR'),
  DISCARD_SUBMISSION: type('dspace/submission/DISCARD_SUBMISSION'),
  DISCARD_SUBMISSION_SUCCESS: type('dspace/submission/DISCARD_SUBMISSION_SUCCESS'),
  DISCARD_SUBMISSION_ERROR: type('dspace/submission/DISCARD_SUBMISSION_ERROR'),

  // Upload file types
  NEW_FILE: type('dspace/submission/NEW_FILE'),
  EDIT_FILE_DATA: type('dspace/submission/EDIT_FILE_DATA'),
  DELETE_FILE: type('dspace/submission/DELETE_FILE'),

  // Errors
  ADD_SECTION_ERROR: type('dspace/submission/ADD_SECTION_ERROR'),
  DELETE_SECTION_ERROR: type('dspace/submission/DELETE_SECTION_ERROR'),
  REMOVE_SECTION_ERRORS: type('dspace/submission/REMOVE_SECTION_ERRORS'),
};


/**
 * Insert a new error of type SubmissionSectionError into the given section
 * @param {string} submissionId
 * @param {string} sectionId
 * @param {SubmissionSectionError} error
 */
export class InertSectionErrorsAction implements Action {
  type: string = SubmissionObjectActionTypes.ADD_SECTION_ERROR;
  payload: {
    submissionId: string;
    sectionId: string;
    error: SubmissionSectionError | SubmissionSectionError[];
  };

  constructor(submissionId: string, sectionId: string, error: SubmissionSectionError | SubmissionSectionError[]) {
    this.payload = { submissionId, sectionId, error };
  }
}

/**
 * Delete a SubmissionSectionError from the given section
 * @param {string} submissionId
 * @param {string} sectionId
 * @param {string | SubmissionSectionError} error
 */
export class DeleteSectionErrorsAction implements Action {
  type: string = SubmissionObjectActionTypes.DELETE_SECTION_ERROR;
  payload: {
    submissionId: string;
    sectionId: string;
    errors: SubmissionSectionError | SubmissionSectionError[];
  };

  constructor(submissionId: string, sectionId: string, errors: SubmissionSectionError | SubmissionSectionError[]) {
    this.payload = { submissionId, sectionId, errors };
  }
}

// Section actions

export class InitSectionAction implements Action {
  type = SubmissionObjectActionTypes.INIT_SECTION;
  payload: {
    submissionId: string;
    sectionId: string;
    header: string;
    config: string;
    mandatory: boolean;
    sectionType: SectionsType;
    visibility: SectionVisibility;
    enabled: boolean;
    data: WorkspaceitemSectionDataType;
    errors: SubmissionSectionError[];
  };

  /**
   * Create a new InitSectionAction
   *
   * @param submissionId
   *    the submission's ID to remove
   * @param sectionId
   *    the section's ID to add
   * @param header
   *    the section's header
   * @param config
   *    the section's config
   * @param mandatory
   *    the section's mandatory
   * @param sectionType
   *    the section's type
   * @param visibility
   *    the section's visibility
   * @param enabled
   *    the section's enabled state
   * @param data
   *    the section's data
   * @param errors
   *    the section's errors
   */
  constructor(submissionId: string,
              sectionId: string,
              header: string,
              config: string,
              mandatory: boolean,
              sectionType: SectionsType,
              visibility: SectionVisibility,
              enabled: boolean,
              data: WorkspaceitemSectionDataType,
              errors: SubmissionSectionError[]) {
    this.payload = { submissionId, sectionId, header, config, mandatory, sectionType, visibility, enabled, data, errors };
  }
}

export class EnableSectionAction implements Action {
  type = SubmissionObjectActionTypes.ENABLE_SECTION;
  payload: {
    submissionId: string;
    sectionId: string;
  };

  /**
   * Create a new EnableSectionAction
   *
   * @param submissionId
   *    the submission's ID to remove
   * @param sectionId
   *    the section's ID to add
   */
  constructor(submissionId: string,
              sectionId: string) {
    this.payload = { submissionId, sectionId };
  }
}

export class DisableSectionAction implements Action {
  type = SubmissionObjectActionTypes.DISABLE_SECTION;
  payload: {
    submissionId: string;
    sectionId: string;
  };

  /**
   * Create a new DisableSectionAction
   *
   * @param submissionId
   *    the submission's ID to remove
   * @param sectionId
   *    the section's ID to remove
   */
  constructor(submissionId: string, sectionId: string) {
    this.payload = { submissionId, sectionId };
  }
}

export class UpdateSectionDataAction implements Action {
  type = SubmissionObjectActionTypes.UPDATE_SECTION_DATA;
  payload: {
    submissionId: string;
    sectionId: string;
    data: WorkspaceitemSectionDataType;
    errorsToShow: SubmissionSectionError[];
    serverValidationErrors: SubmissionSectionError[];
    metadata: string[];
  };

  /**
   * Create a new EnableSectionAction
   *
   * @param submissionId
   *    the submission's ID to remove
   * @param sectionId
   *    the section's ID to add
   * @param data
   *    the section's data
   * @param errorsToShow
   *    the list of the section's errors to show
   * @param serverValidationErrors
   *    the list of the section errors detected by the server
   * @param metadata
   *    the section's metadata
   */
  constructor(submissionId: string,
              sectionId: string,
              data: WorkspaceitemSectionDataType,
              errorsToShow: SubmissionSectionError[],
              serverValidationErrors: SubmissionSectionError[],
              metadata?: string[]) {
    this.payload = { submissionId, sectionId, data, errorsToShow, serverValidationErrors, metadata };
  }
}

export class UpdateSectionDataSuccessAction implements Action {
  type = SubmissionObjectActionTypes.UPDATE_SECTION_DATA_SUCCESS;
}

export class RemoveSectionErrorsAction implements Action {
  type = SubmissionObjectActionTypes.REMOVE_SECTION_ERRORS;
  payload: {
    submissionId: string;
    sectionId: string;
  };

  /**
   * Create a new RemoveSectionErrorsAction
   *
   * @param submissionId
   *    the submission's ID to remove
   * @param sectionId
   *    the section's ID to add
   */
  constructor(submissionId: string, sectionId: string) {
    this.payload = { submissionId, sectionId };
  }
}

export class SetSectionFormId implements Action {
  type = SubmissionObjectActionTypes.SET_SECTION_FORM_ID;
  payload: {
    submissionId: string;
    sectionId: string;
    formId: string;
  };

  /**
   * Create a new SetSectionFormId
   *
   * @param submissionId
   *    the submission's ID
   * @param sectionId
   *    the section's ID
   * @param formId
   *    the section's formId
   */
  constructor(submissionId: string, sectionId: string, formId: string) {
    this.payload = { submissionId, sectionId, formId };
  }
}

// Submission actions

export class CompleteInitSubmissionFormAction implements Action {
  type = SubmissionObjectActionTypes.COMPLETE_INIT_SUBMISSION_FORM;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new CompleteInitSubmissionFormAction
   *
   * @param submissionId
   *    the submission's ID
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class InitSubmissionFormAction implements Action {
  type = SubmissionObjectActionTypes.INIT_SUBMISSION_FORM;
  payload: {
    collectionId: string;
    submissionId: string;
    selfUrl: string;
    submissionDefinition: SubmissionDefinitionsModel;
    sections: WorkspaceitemSectionsObject;
    item: Item;
    errors: SubmissionError;
  };

  /**
   * Create a new InitSubmissionFormAction
   *
   * @param collectionId
   *    the collection's Id where to deposit
   * @param submissionId
   *    the submission's ID
   * @param selfUrl
   *    the submission object url
   * @param submissionDefinition
   *    the submission's sections definition
   * @param sections
   *    the submission's sections
   * @param errors
   *    the submission's sections errors
   */
  constructor(collectionId: string,
              submissionId: string,
              selfUrl: string,
              submissionDefinition: SubmissionDefinitionsModel,
              sections: WorkspaceitemSectionsObject,
              item: Item,
              errors: SubmissionError) {
    this.payload = { collectionId, submissionId, selfUrl, submissionDefinition, sections, item, errors };
  }
}

export class SaveForLaterSubmissionFormAction implements Action {
  type = SubmissionObjectActionTypes.SAVE_FOR_LATER_SUBMISSION_FORM;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new SaveForLaterSubmissionFormAction
   *
   * @param submissionId
   *    the submission's ID
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class SaveForLaterSubmissionFormSuccessAction implements Action {
  type = SubmissionObjectActionTypes.SAVE_FOR_LATER_SUBMISSION_FORM_SUCCESS;
  payload: {
    submissionId: string;
    submissionObject: SubmissionObject[];
  };

  /**
   * Create a new SaveForLaterSubmissionFormSuccessAction
   *
   * @param submissionId
   *    the submission's ID
   * @param submissionObject
   *    the submission's Object
   */
  constructor(submissionId: string, submissionObject: SubmissionObject[]) {
    this.payload = { submissionId, submissionObject };
  }
}

export class SaveForLaterSubmissionFormErrorAction implements Action {
  type = SubmissionObjectActionTypes.SAVE_FOR_LATER_SUBMISSION_FORM_ERROR;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new SaveForLaterSubmissionFormErrorAction
   *
   * @param submissionId
   *    the submission's ID
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class SaveSubmissionFormAction implements Action {
  type = SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM;
  payload: {
    submissionId: string;
    isManual?: boolean;
  };

  /**
   * Create a new SaveSubmissionFormAction
   *
   * @param submissionId
   *    the submission's ID
   */
  constructor(submissionId: string, isManual: boolean = false) {
    this.payload = { submissionId, isManual };
  }
}

export class SaveSubmissionFormSuccessAction implements Action {
  type = SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM_SUCCESS;
  payload: {
    submissionId: string;
    submissionObject: SubmissionObject[];
    showNotifications?: boolean;
    showErrors?: boolean;
  };

  /**
   * Create a new SaveSubmissionFormSuccessAction
   *
   * @param submissionId
   *    the submission's ID
   * @param submissionObject
   *    the submission's Object
   * @param showNotifications
   *    a boolean representing if to show notifications on save
   * @param showErrors
   *    a boolean representing if to show errors on save
   */
  constructor(submissionId: string, submissionObject: SubmissionObject[], showNotifications?: boolean, showErrors?: boolean) {
    this.payload = { submissionId, submissionObject, showNotifications, showErrors };
  }
}

export class SaveSubmissionFormErrorAction implements Action {
  type = SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM_ERROR;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new SaveSubmissionFormErrorAction
   *
   * @param submissionId
   *    the submission's ID
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class SaveSubmissionSectionFormAction implements Action {
  type = SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM;
  payload: {
    submissionId: string;
    sectionId: string;
  };

  /**
   * Create a new SaveSubmissionSectionFormAction
   *
   * @param submissionId
   *    the submission's ID
   * @param sectionId
   *    the section's ID
   */
  constructor(submissionId: string, sectionId: string) {
    this.payload = { submissionId, sectionId };
  }
}

export class SaveSubmissionSectionFormSuccessAction implements Action {
  type = SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM_SUCCESS;
  payload: {
    submissionId: string;
    submissionObject: SubmissionObject[];
    notify?: boolean
  };

  /**
   * Create a new SaveSubmissionSectionFormSuccessAction
   *
   * @param submissionId
   *    the submission's ID
   * @param submissionObject
   *    the submission's Object
   */
  constructor(submissionId: string, submissionObject: SubmissionObject[], notify?: boolean) {
    this.payload = { submissionId, submissionObject, notify };
  }
}

export class SaveSubmissionSectionFormErrorAction implements Action {
  type = SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM_ERROR;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new SaveSubmissionFormErrorAction
   *
   * @param submissionId
   *    the submission's ID
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class ResetSubmissionFormAction implements Action {
  type = SubmissionObjectActionTypes.RESET_SUBMISSION_FORM;
  payload: {
    collectionId: string;
    submissionId: string;
    selfUrl: string;
    sections: WorkspaceitemSectionsObject;
    submissionDefinition: SubmissionDefinitionsModel;
    item: Item;
  };

  /**
   * Create a new ResetSubmissionFormAction
   *
   * @param collectionId
   *    the collection's Id where to deposit
   * @param submissionId
   *    the submission's ID
   * @param selfUrl
   *    the submission object url
   * @param sections
   *    the submission's sections
   * @param submissionDefinition
   *    the submission's form definition
   */
  constructor(collectionId: string, submissionId: string, selfUrl: string, sections: WorkspaceitemSectionsObject, submissionDefinition: SubmissionDefinitionsModel, item: Item) {
    this.payload = { collectionId, submissionId, selfUrl, sections, submissionDefinition, item };
  }
}

export class CancelSubmissionFormAction implements Action {
  type = SubmissionObjectActionTypes.CANCEL_SUBMISSION_FORM;
}

export class ChangeSubmissionCollectionAction implements Action {
  type = SubmissionObjectActionTypes.CHANGE_SUBMISSION_COLLECTION;
  payload: {
    submissionId: string;
    collectionId: string;
  };

  /**
   * Create a new ChangeSubmissionCollectionAction
   *
   * @param submissionId
   *    the submission's ID
   * @param collectionId
   *    the new collection's ID
   */
  constructor(submissionId: string, collectionId: string) {
    this.payload = { submissionId, collectionId };
  }
}

export class SaveAndDepositSubmissionAction implements Action {
  type = SubmissionObjectActionTypes.SAVE_AND_DEPOSIT_SUBMISSION;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new SaveAndDepositSubmissionAction
   *
   * @param submissionId
   *    the submission's ID to deposit
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class DepositSubmissionAction implements Action {
  type = SubmissionObjectActionTypes.DEPOSIT_SUBMISSION;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new DepositSubmissionAction
   *
   * @param submissionId
   *    the submission's ID to deposit
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class DepositSubmissionSuccessAction implements Action {
  type = SubmissionObjectActionTypes.DEPOSIT_SUBMISSION_SUCCESS;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new DepositSubmissionSuccessAction
   *
   * @param submissionId
   *    the submission's ID to deposit
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class DepositSubmissionErrorAction implements Action {
  type = SubmissionObjectActionTypes.DEPOSIT_SUBMISSION_ERROR;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new DepositSubmissionErrorAction
   *
   * @param submissionId
   *    the submission's ID to deposit
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class DiscardSubmissionAction implements Action {
  type = SubmissionObjectActionTypes.DISCARD_SUBMISSION;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new DiscardSubmissionAction
   *
   * @param submissionId
   *    the submission's ID to discard
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class DiscardSubmissionSuccessAction implements Action {
  type = SubmissionObjectActionTypes.DISCARD_SUBMISSION_SUCCESS;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new DiscardSubmissionSuccessAction
   *
   * @param submissionId
   *    the submission's ID to discard
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class DiscardSubmissionErrorAction implements Action {
  type = SubmissionObjectActionTypes.DISCARD_SUBMISSION_ERROR;
  payload: {
    submissionId: string;
  };

  /**
   * Create a new DiscardSubmissionErrorAction
   *
   * @param submissionId
   *    the submission's ID to discard
   */
  constructor(submissionId: string) {
    this.payload = { submissionId };
  }
}

export class SectionStatusChangeAction implements Action {
  type = SubmissionObjectActionTypes.SECTION_STATUS_CHANGE;
  payload: {
    submissionId: string;
    sectionId: string;
    status: boolean
  };

  /**
   * Change the section validity status
   *
   * @param submissionId
   *    the submission's ID
   * @param sectionId
   *    the section's ID to change
   * @param status
   *    the section validity status (true if is valid)
   */
  constructor(submissionId: string, sectionId: string, status: boolean) {
    this.payload = { submissionId, sectionId, status };
  }
}

export class SetActiveSectionAction implements Action {
  type = SubmissionObjectActionTypes.SET_ACTIVE_SECTION;
  payload: {
    submissionId: string;
    sectionId: string;
  };

  /**
   * Create a new SetActiveSectionAction
   *
   * @param submissionId
   *    the submission's ID
   * @param sectionId
   *    the section's ID to active
   */
  constructor(submissionId: string, sectionId: string) {
    this.payload = { submissionId, sectionId };
  }
}
// Upload file actions

export class NewUploadedFileAction implements Action {
  type = SubmissionObjectActionTypes.NEW_FILE;
  payload: {
    submissionId: string;
    sectionId: string;
    fileId: string;
    data: WorkspaceitemSectionUploadFileObject;
  };

  /**
   * Add a new uploaded file
   *
   * @param submissionId
   *    the submission's ID
   * @param sectionId
   *    the section's ID
   * @param fileId
   *    the file's ID
   * @param data
   *    the metadata of the new bitstream
   */
  constructor(submissionId: string, sectionId: string, fileId: string, data: WorkspaceitemSectionUploadFileObject) {
    this.payload = { submissionId, sectionId, fileId, data };
  }
}

export class EditFileDataAction implements Action {
  type = SubmissionObjectActionTypes.EDIT_FILE_DATA;
  payload: {
    submissionId: string;
    sectionId: string;
    fileId: string;
    data: WorkspaceitemSectionUploadFileObject;
  };

  /**
   * Edit a file data
   *
   * @param submissionId
   *    the submission's ID
   * @param sectionId
   *    the section's ID
   * @param fileId
   *    the file's ID
   * @param data
   *    the metadata of the new bitstream
   */
  constructor(submissionId: string, sectionId: string, fileId: string, data: WorkspaceitemSectionUploadFileObject) {
    this.payload = { submissionId, sectionId, fileId: fileId, data };
  }
}

export class DeleteUploadedFileAction implements Action {
  type = SubmissionObjectActionTypes.DELETE_FILE;
  payload: {
    submissionId: string;
    sectionId: string;
    fileId: string;
  };

  /**
   * Delete a uploaded file
   *
   * @param submissionId
   *    the submission's ID
   * @param sectionId
   *    the section's ID
   * @param fileId
   *    the file's ID
   */
  constructor(submissionId: string, sectionId: string, fileId: string) {
    this.payload = { submissionId, sectionId, fileId };
  }
}


/**
 * Export a type alias of all actions in this action group
 * so that reducers can easily compose action types
 */
export type SubmissionObjectAction = DisableSectionAction
  | InitSectionAction
  | SetSectionFormId
  | EnableSectionAction
  | InitSubmissionFormAction
  | ResetSubmissionFormAction
  | CancelSubmissionFormAction
  | CompleteInitSubmissionFormAction
  | ChangeSubmissionCollectionAction
  | SaveAndDepositSubmissionAction
  | DepositSubmissionAction
  | DepositSubmissionSuccessAction
  | DepositSubmissionErrorAction
  | DiscardSubmissionAction
  | DiscardSubmissionSuccessAction
  | DiscardSubmissionErrorAction
  | SectionStatusChangeAction
  | NewUploadedFileAction
  | EditFileDataAction
  | DeleteUploadedFileAction
  | InertSectionErrorsAction
  | DeleteSectionErrorsAction
  | UpdateSectionDataAction
  | RemoveSectionErrorsAction
  | SaveForLaterSubmissionFormAction
  | SaveForLaterSubmissionFormSuccessAction
  | SaveForLaterSubmissionFormErrorAction
  | SaveSubmissionFormAction
  | SaveSubmissionFormSuccessAction
  | SaveSubmissionFormErrorAction
  | SaveSubmissionSectionFormAction
  | SaveSubmissionSectionFormSuccessAction
  | SaveSubmissionSectionFormErrorAction
  | SetActiveSectionAction;
