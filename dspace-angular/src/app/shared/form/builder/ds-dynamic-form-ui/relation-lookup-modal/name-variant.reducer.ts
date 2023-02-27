/**
 * Represents the state of all lists containing name variants in the store
 */

import { NameVariantAction, NameVariantActionTypes, SetNameVariantAction } from './name-variant.actions';
import { hasValue } from '../../../../empty.util';

export interface NameVariantListsState {
  [listID: string]: NameVariantListState;
}

/**
 * Represents the state of a single list containing nameVariants in the store
 */
export interface NameVariantListState {
  [itemID: string]: string;
}

/**
 * Reducer that handles NameVariantAction to update the NameVariantListsState
 * @param {NameVariantListsState} state The initial NameVariantListsState
 * @param {NameVariantAction} action The Action to be performed on the state
 * @returns {NameVariantListsState} The new, reduced NameVariantListsState
 */
export function nameVariantReducer(state: NameVariantListsState = {}, action: NameVariantAction): NameVariantListsState {
  switch (action.type) {
    case NameVariantActionTypes.SET_NAME_VARIANT: {
      const listState: NameVariantListState = state[action.payload.listID] || {};
      const nameVariant = (action as SetNameVariantAction).payload.nameVariant;
      const newListState = setNameVariant(listState, action.payload.itemID, nameVariant);
      return Object.assign({}, state, { [action.payload.listID]: newListState });
    }
    case NameVariantActionTypes.REMOVE_NAME_VARIANT: {
      const listState: NameVariantListState = state[action.payload.listID];
      if (hasValue(listState) && hasValue(listState[action.payload.itemID])) {
        const newListState = setNameVariant(listState, action.payload.itemID, undefined);
        return Object.assign({}, state, { [action.payload.listID]: newListState });
      } else {
        return state;
      }
    }
    default: {
      return state;
    }
  }
}

function setNameVariant(state: NameVariantListState, itemID: string, nameVariant: string) {
  return Object.assign({}, state, { [itemID]: nameVariant });
}
