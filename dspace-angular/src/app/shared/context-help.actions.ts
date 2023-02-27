/* eslint-disable max-classes-per-file */

import { Action } from '@ngrx/store';
import { type } from './ngrx/type';
import { ContextHelp } from './context-help.model';

export const ContextHelpActionTypes = {
  'CONTEXT_HELP_TOGGLE_ICONS': type('dspace/context-help/CONTEXT_HELP_TOGGLE_ICONS'),
  'CONTEXT_HELP_ADD': type('dspace/context-help/CONTEXT_HELP_ADD'),
  'CONTEXT_HELP_REMOVE': type('dspace/context-help/CONTEXT_HELP_REMOVE'),
  'CONTEXT_HELP_TOGGLE_TOOLTIP': type('dspace/context-help/CONTEXT_HELP_TOGGLE_TOOLTIP'),
  'CONTEXT_HELP_SHOW_TOOLTIP': type('dspace/context-help/CONTEXT_HELP_SHOW_TOOLTIP'),
  'CONTEXT_HELP_HIDE_TOOLTIP' : type('dspace/context-help/CONTEXT_HELP_HIDE_TOOLTIP'),
};

/**
 * Toggles the visibility of all context help icons.
 */
export class ContextHelpToggleIconsAction implements Action {
  type = ContextHelpActionTypes.CONTEXT_HELP_TOGGLE_ICONS;
}

/**
 * Registers a new context help icon to the store.
 */
export class ContextHelpAddAction implements Action {
  type = ContextHelpActionTypes.CONTEXT_HELP_ADD;
  model: ContextHelp;

  constructor (model: ContextHelp) {
    this.model = model;
  }
}

/**
 * Removes a context help icon from the store.
 */
export class ContextHelpRemoveAction implements Action {
  type = ContextHelpActionTypes.CONTEXT_HELP_REMOVE;
  id: string;

  constructor(id: string) {
    this.id = id;
  }
}

export abstract class ContextHelpTooltipAction implements Action {
  type;
  id: string;

  constructor(id: string) {
    this.id = id;
  }
}

/**
 * Toggles the tooltip of a single context help icon.
 */
export class ContextHelpToggleTooltipAction extends ContextHelpTooltipAction {
  type = ContextHelpActionTypes.CONTEXT_HELP_TOGGLE_TOOLTIP;
}

/**
 * Shows the tooltip of a single context help icon.
 */
export class ContextHelpShowTooltipAction extends ContextHelpTooltipAction {
  type = ContextHelpActionTypes.CONTEXT_HELP_SHOW_TOOLTIP;
}

/**
 * Hides the tooltip of a single context help icon.
 */
export class ContextHelpHideTooltipAction extends ContextHelpTooltipAction {
  type = ContextHelpActionTypes.CONTEXT_HELP_HIDE_TOOLTIP;
}

export type ContextHelpAction
  = ContextHelpToggleIconsAction
  | ContextHelpAddAction
  | ContextHelpRemoveAction
  | ContextHelpToggleTooltipAction
  | ContextHelpShowTooltipAction
  | ContextHelpHideTooltipAction;
