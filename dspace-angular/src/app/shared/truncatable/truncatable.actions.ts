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
export const TruncatableActionTypes = {
  TOGGLE: type('dspace/truncatable/TOGGLE'),
  COLLAPSE: type('dspace/truncatable/COLLAPSE'),
  EXPAND: type('dspace/truncatable/EXPAND'),
};

export class TruncatableAction implements Action {
  /**
   * UUID of the truncatable component the action is performed on, used to identify the filter
   */
  id: string;

  /**
   * Type of action that will be performed
   */
  type;

  /**
   * Initialize with the truncatable component's UUID
   * @param {string} id of the filter
   */
  constructor(id: string) {
    this.id = id;
  }
}

/**
 * Used to collapse a truncatable component when it's expanded and expand it when it's collapsed
 */
export class TruncatableToggleAction extends TruncatableAction {
  type = TruncatableActionTypes.TOGGLE;
}

/**
 * Used to collapse a truncatable component
 */
export class TruncatableCollapseAction extends TruncatableAction {
  type = TruncatableActionTypes.COLLAPSE;
}

/**
 * Used to expand a truncatable component
 */
export class TruncatableExpandAction extends TruncatableAction {
  type = TruncatableActionTypes.EXPAND;
}

