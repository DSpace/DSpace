/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';
import { Group } from '../../core/eperson/models/group.model';
import { type } from '../../shared/ngrx/type';

/**
 * For each action type in an action group, make a simple
 * enum object for all of this group's action types.
 *
 * The 'type' utility function coerces strings into string
 * literal types and runs a simple check to guarantee all
 * action types in the application are unique.
 */
export const GroupRegistryActionTypes = {

  EDIT_GROUP: type('dspace/epeople-registry/EDIT_GROUP'),
  CANCEL_EDIT_GROUP: type('dspace/epeople-registry/CANCEL_EDIT_GROUP'),
};

/**
 * Used to edit a Group in the Group registry
 */
export class GroupRegistryEditGroupAction implements Action {
  type = GroupRegistryActionTypes.EDIT_GROUP;

  group: Group;

  constructor(group: Group) {
    this.group = group;
  }
}

/**
 * Used to cancel the editing of a Group in the Group registry
 */
export class GroupRegistryCancelGroupAction implements Action {
  type = GroupRegistryActionTypes.CANCEL_EDIT_GROUP;
}


/**
 * Export a type alias of all actions in this action group
 * so that reducers can easily compose action types
 * These are all the actions to perform on the EPeople registry state
 */
export type GroupRegistryAction
  = GroupRegistryEditGroupAction
  | GroupRegistryCancelGroupAction;
