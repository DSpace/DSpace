/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { type } from '../../shared/ngrx/type';

/**
 * For each action type in an action group, make a simple
 * enum object for all of this group's action types.
 *
 * The 'type' utility function coerces strings into string
 * literal types and runs a simple check to guarantee all
 * action types in the application are unique.
 */
export const EPeopleRegistryActionTypes = {

  EDIT_EPERSON: type('dspace/epeople-registry/EDIT_EPERSON'),
  CANCEL_EDIT_EPERSON: type('dspace/epeople-registry/CANCEL_EDIT_EPERSON'),
};

/**
 * Used to edit an EPerson in the EPeople registry
 */
export class EPeopleRegistryEditEPersonAction implements Action {
  type = EPeopleRegistryActionTypes.EDIT_EPERSON;

  eperson: EPerson;

  constructor(eperson: EPerson) {
    this.eperson = eperson;
  }
}

/**
 * Used to cancel the editing of an EPerson in the EPeople registry
 */
export class EPeopleRegistryCancelEPersonAction implements Action {
  type = EPeopleRegistryActionTypes.CANCEL_EDIT_EPERSON;
}


/**
 * Export a type alias of all actions in this action group
 * so that reducers can easily compose action types
 * These are all the actions to perform on the EPeople registry state
 */
export type EPeopleRegistryAction
  = EPeopleRegistryEditEPersonAction
  | EPeopleRegistryCancelEPersonAction;
