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
export const SidebarActionTypes = {
  COLLAPSE: type('dspace/sidebar/COLLAPSE'),
  EXPAND: type('dspace/sidebar/EXPAND'),
  TOGGLE: type('dspace/sidebar/TOGGLE')
};

/**
 * Used to collapse the sidebar
 */
export class SidebarCollapseAction implements Action {
  type = SidebarActionTypes.COLLAPSE;
}

/**
 * Used to expand the sidebar
 */
export class SidebarExpandAction implements Action {
  type = SidebarActionTypes.EXPAND;
}

/**
 * Used to collapse the sidebar when it's expanded and expand it when it's collapsed
 */
export class SidebarToggleAction implements Action {
  type = SidebarActionTypes.TOGGLE;
}

/**
 * Export a type alias of all actions in this action group
 * so that reducers can easily compose action types
 */
export type SidebarAction
  = SidebarCollapseAction
  | SidebarExpandAction
  | SidebarToggleAction;
