import { EPerson } from '../../core/eperson/models/eperson.model';
import {
  EPeopleRegistryAction,
  EPeopleRegistryActionTypes,
  EPeopleRegistryEditEPersonAction
} from './epeople-registry.actions';

/**
 * The EPeople registry state.
 * @interface EPeopleRegistryState
 */
export interface EPeopleRegistryState {
  editEPerson: EPerson;
}

/**
 * The initial state.
 */
const initialState: EPeopleRegistryState = {
  editEPerson: null,
};

/**
 * Reducer that handles EPeopleRegistryActions to modify EPeople
 * @param state   The current EPeopleRegistryState
 * @param action  The EPeopleRegistryAction to perform on the state
 */
export function ePeopleRegistryReducer(state = initialState, action: EPeopleRegistryAction): EPeopleRegistryState {
  switch (action.type) {

    case EPeopleRegistryActionTypes.EDIT_EPERSON: {
      return Object.assign({}, state, {
        editEPerson: (action as EPeopleRegistryEditEPersonAction).eperson
      });
    }

    case EPeopleRegistryActionTypes.CANCEL_EDIT_EPERSON: {
      return Object.assign({}, state, {
        editEPerson: null
      });
    }

    default:
      return state;
  }
}
