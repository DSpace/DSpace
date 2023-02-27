import { ListableObject } from '../../object-collection/shared/listable-object.model';
import {
  SelectableListAction,
  SelectableListActionTypes,
  SelectableListSelectAction,
  SelectableListSelectSingleAction,
  SelectableListDeselectAction,
  SelectableListDeselectSingleAction, SelectableListSetSelectionAction
} from './selectable-list.actions';
import { hasNoValue } from '../../empty.util';

/**
 * Represents the state of all selectable lists in the store
 */
export interface SelectableListsState {
  [id: string]: SelectableListState;
}

/**
 * Represents the state of a single selectable list in the store
 */
export interface SelectableListState {
  id: string;
  selection: ListableObject[];
}

/**
 * Reducer that handles SelectableListAction to update the SelectableListsState
 * @param {SelectableListsState} state The initial SelectableListsState
 * @param {SelectableListAction} action The Action to be performed on the state
 * @returns {SelectableListsState} The new, reducer SelectableListsState
 */
export function selectableListReducer(state: SelectableListsState = {}, action: SelectableListAction): SelectableListsState {
  const listState: SelectableListState = state[action.id] || clearSelection(action.id);
  switch (action.type) {
    case SelectableListActionTypes.SELECT: {
      const newListState = select(listState, action as SelectableListSelectAction);
      return Object.assign({}, state, { [action.id]: newListState });
    }
    case SelectableListActionTypes.SELECT_SINGLE: {
      const newListState = selectSingle(listState, action as SelectableListSelectSingleAction);
      return Object.assign({}, state, { [action.id]: newListState });
    }
    case SelectableListActionTypes.DESELECT: {
      const newListState = deselect(listState, action as SelectableListDeselectAction);
      return Object.assign({}, state, { [action.id]: newListState });
    }
    case SelectableListActionTypes.DESELECT_SINGLE: {
      const newListState = deselectSingle(listState, action as SelectableListDeselectSingleAction);
      return Object.assign({}, state, { [action.id]: newListState });
    }
    case SelectableListActionTypes.SET_SELECTION: {
      const newListState = setList(listState, action as SelectableListSetSelectionAction);
      return Object.assign({}, state, { [action.id]: newListState });
    }
    case SelectableListActionTypes.DESELECT_ALL: {
      const newListState = clearSelection(action.id);
      return Object.assign({}, state, { [action.id]: newListState });
    }
    default: {
      return state;
    }
  }
}

/**
 * Adds multiple objects to the existing selection state
 * @param state The current state
 * @param action The action to perform
 */
function select(state: SelectableListState, action: SelectableListSelectAction) {
  const filteredNewObjects = action.payload.filter((object) => !isObjectInSelection(state.selection, object));
  const newSelection = [...state.selection, ...filteredNewObjects];
  return Object.assign({}, state, { selection: newSelection });
}

/**
 * Adds a single object to the existing selection state
 * @param state The current state
 * @param action The action to perform
 */
function selectSingle(state: SelectableListState, action: SelectableListSelectSingleAction) {
  let newSelection = state.selection;
  if (!isObjectInSelection(state.selection, action.payload.object)) {
    newSelection = [...state.selection, action.payload.object];
  }
  return Object.assign({}, state, { selection: newSelection });
}

/**
 * Removes multiple objects in the existing selection state
 * @param state The current state
 * @param action The action to perform
 */
function deselect(state: SelectableListState, action: SelectableListDeselectAction) {
  const newSelection = state.selection.filter((selected) => hasNoValue(action.payload.find((object) => object.equals(selected))));
  return Object.assign({}, state, { selection: newSelection });
}

/** Removes a single object from the existing selection state
 *
 * @param state The current state
 * @param action The action to perform
 */
function deselectSingle(state: SelectableListState, action: SelectableListDeselectSingleAction) {
  const newSelection = state.selection.filter((selected) => {
    return !selected.equals(action.payload);
  });
  return Object.assign({}, state, { selection: newSelection });
}

/**
 * Sets the selection state of the list
 * @param state The current state
 * @param action The action to perform
 */
function setList(state: SelectableListState, action: SelectableListSetSelectionAction) {
  return Object.assign({}, state, { selection: action.payload });
}

/**
 * Clears the selection
 * @param state The current state
 * @param action The action to perform
 */
function clearSelection(id: string) {
  return { id: id, selection: [] };
}

/**
 * Checks whether the object is in currently in the selection
 * @param state The current state
 * @param action The action to perform
 */
function isObjectInSelection(selection: ListableObject[], object: ListableObject) {
  return selection.findIndex((selected) => selected.equals(object)) >= 0;
}
