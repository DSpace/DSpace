/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';

import { type } from '../ngrx/type';

/**
 * For each action type in an action group, make a simple
 * enum object for all of this group's action types.
 *
 * The 'type' utility function coerces strings into string
 * literal types and runs a simple check to guarantee all
 * action types in the application are unique.
 */
export const FormActionTypes = {
  FORM_INIT: type('dspace/form/FORM_INIT'),
  FORM_CHANGE: type('dspace/form/FORM_CHANGE'),
  FORM_ADD_TOUCHED: type('dspace/form/FORM_ADD_TOUCHED'),
  FORM_REMOVE: type('dspace/form/FORM_REMOVE'),
  FORM_STATUS_CHANGE: type('dspace/form/FORM_STATUS_CHANGE'),
  FORM_ADD_ERROR: type('dspace/form/FORM_ADD_ERROR'),
  FORM_REMOVE_ERROR: type('dspace/form/FORM_REMOVE_ERROR'),
  FORM_CLEAR_ERRORS: type('dspace/form/FORM_CLEAR_ERRORS'),
};

export class FormInitAction implements Action {
  type = FormActionTypes.FORM_INIT;
  payload: {
    formId: string;
    formData: any;
    valid: boolean;
  };

  /**
   * Create a new FormInitAction
   *
   * @param formId
   *    the Form's ID
   * @param formData
   *    the FormGroup Object
   * @param valid
   *    the Form validation status
   */
  constructor(formId: string, formData: any, valid: boolean) {
    this.payload = {formId, formData, valid};
  }
}

export class FormChangeAction implements Action {
  type = FormActionTypes.FORM_CHANGE;
  payload: {
    formId: string;
    formData: any;
  };

  /**
   * Create a new FormChangeAction
   *
   * @param formId
   *    the Form's ID
   * @param formData
   *    the FormGroup Object
   */
  constructor(formId: string, formData: any) {
    this.payload = {formId, formData};
  }
}

export class FormAddTouchedAction implements Action {
  type = FormActionTypes.FORM_ADD_TOUCHED;
  payload: {
    formId: string;
    touched: string[];
  };

  /**
   * Create a new FormAddTouchedAction
   *
   * @param formId
   *    the Form's ID
   * @param touched
   *    the array containing new touched fields
   */
  constructor(formId: string, touched: string[]) {
    this.payload = {formId, touched};
  }
}

export class FormRemoveAction implements Action {
  type = FormActionTypes.FORM_REMOVE;
  payload: {
    formId: string;
  };

  /**
   * Create a new FormRemoveAction
   *
   * @param formId
   *    the Form's ID
   */
  constructor(formId: string) {
    this.payload = {formId};
  }
}

export class FormStatusChangeAction implements Action {
  type = FormActionTypes.FORM_STATUS_CHANGE;
  payload: {
    formId: string;
    valid: boolean;
  };

  /**
   * Create a new FormInitAction
   *
   * @param formId
   *    the Form's ID
   * @param valid
   *    the Form validation status
   */
  constructor(formId: string, valid: boolean) {
    this.payload = {formId, valid};
  }
}

export class FormAddError implements Action {
  type = FormActionTypes.FORM_ADD_ERROR;
  payload: {
    formId: string,
    fieldId: string,
    fieldIndex: number,
    errorMessage: string,
  };

  constructor(formId: string, fieldId: string, fieldIndex: number, errorMessage: string) {
    this.payload = {formId, fieldId, fieldIndex, errorMessage};
  }
}

export class FormRemoveErrorAction implements Action {
  type = FormActionTypes.FORM_REMOVE_ERROR;
  payload: {
    formId: string,
    fieldId: string,
    fieldIndex: number,
  };

  constructor(formId: string, fieldId: string, fieldIndex: number,) {
    this.payload = {formId, fieldId, fieldIndex};
  }
}

export class FormClearErrorsAction implements Action {
  type = FormActionTypes.FORM_CLEAR_ERRORS;
  payload: {
    formId: string
  };

  constructor(formId: string) {
    this.payload = {formId};
  }
}


/**
 * Export a type alias of all actions in this action group
 * so that reducers can easily compose action types
 */
export type FormAction = FormInitAction
  | FormChangeAction
  | FormAddTouchedAction
  | FormRemoveAction
  | FormStatusChangeAction
  | FormAddError
  | FormClearErrorsAction
  | FormRemoveErrorAction;
