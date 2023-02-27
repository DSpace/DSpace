import { isEmpty } from '../empty.util';
import { ObjectSelectionAction, ObjectSelectionActionTypes } from './object-select.actions';

/**
 * Interface that represents the state for a single selection of an object
 */
export interface ObjectSelectionState {
  checked: boolean;
}

/**
 * Interface that represents the state for all selected items within a certain category defined by a key
 */
export interface ObjectSelectionsState {
  [id: string]: ObjectSelectionState;
}

/**
 * Interface that represents the state for all selected items
 */
export interface ObjectSelectionListState {
  [key: string]: ObjectSelectionsState;
}

const initialState: ObjectSelectionListState = Object.create(null);

/**
 * Performs a selection action on the current state
 * @param {ObjectSelectionListState} state The state before the action is performed
 * @param {ObjectSelectionAction} action The action that should be performed
 * @returns {ObjectSelectionListState} The state after the action is performed
 */
export function objectSelectionReducer(state = initialState, action: ObjectSelectionAction): ObjectSelectionListState {

  switch (action.type) {

    case ObjectSelectionActionTypes.INITIAL_SELECT: {
      if (isEmpty(state) || isEmpty(state[action.key]) || isEmpty(state[action.key][action.id])) {
        return Object.assign({}, state, {
          [action.key]: Object.assign({}, state[action.key], {
            [action.id]: {
              checked: true
            }
          })
        });
      }
      return state;
    }

    case ObjectSelectionActionTypes.INITIAL_DESELECT: {
      if (isEmpty(state) || isEmpty(state[action.key]) || isEmpty(state[action.key][action.id])) {
        return Object.assign({}, state, {
          [action.key]: Object.assign({}, state[action.key], {
            [action.id]: {
              checked: false
            }
          })
        });
      }
      return state;
    }

    case ObjectSelectionActionTypes.SELECT: {
      return Object.assign({}, state, {
        [action.key]: Object.assign({}, state[action.key], {
          [action.id]: {
            checked: true
          }
        })
      });
    }

    case ObjectSelectionActionTypes.DESELECT: {
      return Object.assign({}, state, {
        [action.key]: Object.assign({}, state[action.key], {
          [action.id]: {
            checked: false
          }
        })
      });
    }

    case ObjectSelectionActionTypes.SWITCH: {
      return Object.assign({}, state, {
        [action.key]: Object.assign({}, state[action.key], {
          [action.id]: {
            checked: (isEmpty(state) || isEmpty(state[action.key]) || isEmpty(state[action.key][action.id])) ? true : !state[action.key][action.id].checked
          }
        })
      });
    }

    case ObjectSelectionActionTypes.RESET: {
      if (isEmpty(action.key)) {
        return {};
      } else {
        return Object.assign({}, state, {
          [action.key]: {}
        });
      }
    }

    default: {
      return state;
    }
  }
}
