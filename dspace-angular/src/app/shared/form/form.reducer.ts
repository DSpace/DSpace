import {
  FormAction,
  FormActionTypes,
  FormAddError,
  FormAddTouchedAction,
  FormChangeAction,
  FormClearErrorsAction,
  FormInitAction,
  FormRemoveAction,
  FormRemoveErrorAction,
  FormStatusChangeAction
} from './form.actions';
import { hasValue } from '../empty.util';
import isEqual from 'lodash/isEqual';
import uniqWith from 'lodash/uniqWith';

export interface FormError {
  message: string;
  fieldId: string;
  fieldIndex: number;
}

export interface FormTouchedState {
  [key: string]: boolean;
}

export interface FormEntry {
  data: any;
  valid: boolean;
  errors: FormError[];
  touched: FormTouchedState;
}

export interface FormState {
  [formId: string]: FormEntry;
}

const initialState: FormState = Object.create(null);

export function formReducer(state = initialState, action: FormAction): FormState {
  switch (action.type) {

    case FormActionTypes.FORM_INIT: {
      return initForm(state, action as FormInitAction);
    }

    case FormActionTypes.FORM_CHANGE: {
      return changeDataForm(state, action as FormChangeAction);
    }

    case FormActionTypes.FORM_ADD_TOUCHED: {
      return changeTouchedState(state, action as FormAddTouchedAction);
    }

    case FormActionTypes.FORM_REMOVE: {
      return removeForm(state, action as FormRemoveAction);
    }

    case FormActionTypes.FORM_STATUS_CHANGE: {
      return changeStatusForm(state, action as FormStatusChangeAction);
    }

    case FormActionTypes.FORM_ADD_ERROR: {
      return addFormErrors(state, action as FormAddError);
    }

    case FormActionTypes.FORM_REMOVE_ERROR: {
      return removeFormError(state, action as FormRemoveErrorAction);
    }

    case FormActionTypes.FORM_CLEAR_ERRORS: {
      return clearsFormErrors(state, action as FormClearErrorsAction);
    }

    default: {
      return state;
    }
  }
}

function addFormErrors(state: FormState, action: FormAddError) {
  const formId = action.payload.formId;
  if (hasValue(state[formId])) {
    const error: FormError = {
      fieldId: action.payload.fieldId,
      fieldIndex: action.payload.fieldIndex,
      message: action.payload.errorMessage
    };
    const metadata = action.payload.fieldId.replace(/\_/g, '.');
    const touched = Object.assign({}, state[formId].touched, {
      [metadata]: true
    });
    return Object.assign({}, state, {
      [formId]: {
        data: state[formId].data,
        valid: state[formId].valid,
        errors: state[formId].errors ? uniqWith(state[formId].errors.concat(error), isEqual) : [].concat(error),
        touched
      }
    });
  } else {
    return state;
  }
}

function removeFormError(state: FormState, action: FormRemoveErrorAction) {
  const formId = action.payload.formId;
  const fieldId = action.payload.fieldId;
  const fieldIndex = action.payload.fieldIndex;
  if (hasValue(state[formId])) {
    const errors = state[formId].errors.filter((error) => error.fieldId !== fieldId || error.fieldIndex !== fieldIndex);
    const newState = Object.assign({}, state);
    newState[formId] = Object.assign({}, state[formId], {errors});
    return newState;
  } else {
    return state;
  }
}

function clearsFormErrors(state: FormState, action: FormClearErrorsAction) {
  const formId = action.payload.formId;
  if (hasValue(state[formId])) {
    const errors = [];
    const newState = Object.assign({}, state);
    newState[formId] = Object.assign({}, state[formId], {errors});
    return newState;
  } else {
    return state;
  }
}

/**
 * Init form state.
 *
 * @param state
 *    the current state
 * @param action
 *    an FormInitAction
 * @return FormState
 *    the new state, with the form initialized.
 */
function initForm(state: FormState, action: FormInitAction): FormState {
  const formState = {
    data: action.payload.formData,
    valid: action.payload.valid,
    touched: {},
    errors: [],
  };
  if (!hasValue(state[action.payload.formId])) {
    return Object.assign({}, state, {
      [action.payload.formId]: formState
    });
  } else {
    const newState = Object.assign({}, state);
    newState[action.payload.formId] = Object.assign({}, newState[action.payload.formId], formState);
    return newState;
  }
}

/**
 * Set form data.
 *
 * @param state
 *    the current state
 * @param action
 *    an FormChangeAction
 * @return FormState
 *    the new state, with the data changed.
 */
function changeDataForm(state: FormState, action: FormChangeAction): FormState {
  if (hasValue(state[action.payload.formId])) {
    const newState = Object.assign({}, state);
    newState[action.payload.formId] = Object.assign({}, newState[action.payload.formId], {
        data: action.payload.formData,
        valid: state[action.payload.formId].valid
      }
    );
    return newState;
  } else {
    return state;
  }
}

/**
 * Set form status.
 *
 * @param state
 *    the current state
 * @param action
 *    an FormStatusChangeAction
 * @return FormState
 *    the new state, with the status changed.
 */
function changeStatusForm(state: FormState, action: FormStatusChangeAction): FormState {
  if (!hasValue(state[action.payload.formId])) {
    return Object.assign({}, state, {
      [action.payload.formId]: {
        data: state[action.payload.formId].data,
        valid: action.payload.valid
      }
    });
  } else {
    const newState = Object.assign({}, state);
    newState[action.payload.formId] = Object.assign({}, newState[action.payload.formId], {
        data: state[action.payload.formId].data,
        valid: action.payload.valid
      }
    );
    return newState;
  }
}

/**
 * Remove form state.
 *
 * @param state
 *    the current state
 * @param action
 *    an FormRemoveAction
 * @return FormState
 *    the new state, with the form initialized.
 */
function removeForm(state: FormState, action: FormRemoveAction): FormState {
  if (hasValue(state[action.payload.formId])) {
    const newState = Object.assign({}, state);
    delete newState[action.payload.formId];
    return newState;
  } else {
    return state;
  }
}

/**
 * Compute the touched state of the form. New touched fields are merged with the previous ones.
 * @param state
 * @param action
 */
function changeTouchedState(state: FormState, action: FormAddTouchedAction): FormState {
  if (hasValue(state[action.payload.formId])) {
    const newState = Object.assign({}, state);

    const newForm = Object.assign({}, newState[action.payload.formId]);
    newState[action.payload.formId] = newForm;

    newForm.touched = { ... newForm.touched};
    action.payload.touched.forEach((field) => newForm.touched[field] = true);

    return newState;
  } else {
    return state;
  }
}
