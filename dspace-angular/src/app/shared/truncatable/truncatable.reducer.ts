import { TruncatableAction, TruncatableActionTypes } from './truncatable.actions';

/**
 * Interface that represents the state of a single truncatable
 */
export interface TruncatableState {
  collapsed: boolean;
}

/**
 * Interface that represents the state of all truncatable
 */
export interface TruncatablesState {
  [id: string]: TruncatableState;
}

const initialState: TruncatablesState = Object.create(null);

/**
 * Performs a truncatable action on the current state
 * @param {TruncatablesState} state The state before the action is performed
 * @param {TruncatableAction} action The action that should be performed
 * @returns {TruncatablesState} The state after the action is performed
 */
export function truncatableReducer(state = initialState, action: TruncatableAction): TruncatablesState {

  switch (action.type) {

    case TruncatableActionTypes.COLLAPSE: {
      return Object.assign({}, state, {
        [action.id]: {
          collapsed: true,
        }
      });
    } case TruncatableActionTypes.EXPAND: {
      return Object.assign({}, state, {
        [action.id]: {
          collapsed: false,
        }
      });
    } case TruncatableActionTypes.TOGGLE: {
      if (!state[action.id]) {
        state[action.id] = {collapsed: false};
      }
      return Object.assign({}, state, {
        [action.id]: {
          collapsed: !state[action.id].collapsed,
        }
      });
    }
    default: {
      return state;
    }
  }
}
