/* eslint-disable max-classes-per-file */
import { type } from '../ngrx/type';
import { Action } from '@ngrx/store';

export const ObjectSelectionActionTypes = {
  INITIAL_DESELECT: type('dspace/object-select/INITIAL_DESELECT'),
  INITIAL_SELECT: type('dspace/object-select/INITIAL_SELECT'),
  SELECT: type('dspace/object-select/SELECT'),
  DESELECT: type('dspace/object-select/DESELECT'),
  SWITCH: type('dspace/object-select/SWITCH'),
  RESET: type('dspace/object-select/RESET')
};

export class ObjectSelectionAction implements Action {
  /**
   * Key of the list (of selections) for which the action should be performed
   */
  key: string;

  /**
   * UUID of the object a select action can be performed on
   */
  id: string;

  /**
   * Type of action that will be performed
   */
  type;

  /**
   * Initialize with the object's UUID
   * @param {string} key of the list
   * @param {string} id of the object
   */
  constructor(key: string, id: string) {
    this.key = key;
    this.id = id;
  }
}

/**
 * Used to set the initial state to deselected
 */
export class ObjectSelectionInitialDeselectAction extends ObjectSelectionAction {
  type = ObjectSelectionActionTypes.INITIAL_DESELECT;
}

/**
 * Used to set the initial state to selected
 */
export class ObjectSelectionInitialSelectAction extends ObjectSelectionAction {
  type = ObjectSelectionActionTypes.INITIAL_SELECT;
}

/**
 * Used to select an object
 */
export class ObjectSelectionSelectAction extends ObjectSelectionAction {
  type = ObjectSelectionActionTypes.SELECT;
}

/**
 * Used to deselect an object
 */
export class ObjectSelectionDeselectAction extends ObjectSelectionAction {
  type = ObjectSelectionActionTypes.DESELECT;
}

/**
 * Used to switch an object between selected and deselected
 */
export class ObjectSelectionSwitchAction extends ObjectSelectionAction {
  type = ObjectSelectionActionTypes.SWITCH;
}

/**
 * Used to reset all objects selected to be deselected
 */
export class ObjectSelectionResetAction extends ObjectSelectionAction {
  type = ObjectSelectionActionTypes.RESET;
}
