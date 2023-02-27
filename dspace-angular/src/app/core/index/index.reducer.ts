import { AddToIndexAction, IndexAction, IndexActionTypes, RemoveFromIndexBySubstringAction, RemoveFromIndexByValueAction } from './index.actions';
import { IndexName } from './index-name.model';

/**
 * The state of a single index
 */
export interface IndexState {
  [key: string]: any;
}

/**
 * The state that contains all indices
 */
export type MetaIndexState = {
  [name in IndexName]: IndexState
};

// Object.create(null) ensures the object has no default js properties (e.g. `__proto__`)
const initialState: MetaIndexState = Object.create(null);

/**
 * The Index Reducer
 *
 * @param state
 *    the current state
 * @param action
 *    the action to perform on the state
 * @return MetaIndexState
 *    the new state
 */
export function indexReducer(state = initialState, action: IndexAction): MetaIndexState {
  switch (action.type) {

    case IndexActionTypes.ADD: {
      return addToIndex(state, action as AddToIndexAction);
    }

    case IndexActionTypes.REMOVE_BY_VALUE: {
      return removeFromIndexByValue(state, action as RemoveFromIndexByValueAction);
    }

    case IndexActionTypes.REMOVE_BY_SUBSTRING: {
      return removeFromIndexBySubstring(state, action as RemoveFromIndexBySubstringAction);
    }

    default: {
      return state;
    }
  }
}

/**
 * Add an entry to a given index
 *
 * @param state
 *    The MetaIndexState that contains all indices
 * @param action
 *    The AddToIndexAction containing the value to add, and the index to add it to
 * @return MetaIndexState
 *    the new state
 */
function addToIndex(state: MetaIndexState, action: AddToIndexAction): MetaIndexState {
  const subState = state[action.payload.name];
  const newSubState = Object.assign({}, subState, {
    [action.payload.key]: action.payload.value
  });
  const obs = Object.assign({}, state, {
    [action.payload.name]: newSubState
  });
  return obs;
}

/**
 * Remove a entries that contain a given value from a given index
 *
 * @param state
 *    The MetaIndexState that contains all indices
 * @param action
 *    The RemoveFromIndexByValueAction containing the value to remove, and the index to remove it from
 * @return MetaIndexState
 *    the new state
 */
function removeFromIndexByValue(state: MetaIndexState, action: RemoveFromIndexByValueAction): MetaIndexState {
  const subState = state[action.payload.name];
  const newSubState = Object.create(null);
  for (const value in subState) {
    if (subState[value] !== action.payload.value) {
      newSubState[value] = subState[value];
    }
  }
  return Object.assign({}, state, {
    [action.payload.name]: newSubState
  });
}

/**
 * Remove entries that contain a given substring from a given index
 *
 * @param state
 *    The MetaIndexState that contains all indices
 * @param action
 *    The RemoveFromIndexByValueAction the substring to remove, and the index to remove it from
 * @return MetaIndexState
 *    the new state
 */
function removeFromIndexBySubstring(state: MetaIndexState, action: RemoveFromIndexByValueAction | RemoveFromIndexBySubstringAction): MetaIndexState {
  const subState = state[action.payload.name];
  const newSubState = Object.create(null);
  for (const value in subState) {
    if (value.indexOf(action.payload.value) < 0) {
      newSubState[value] = subState[value];
    }
  }
  return Object.assign({}, state, {
    [action.payload.name]: newSubState
  });
}
