/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';
import { type } from '../ngrx/type';
import { KeyValuePair } from '../key-value-pair.model';

/**
 * For each action type in an action group, make a simple
 * enum object for all of this group's action types.
 *
 * The 'type' utility function coerces strings into string
 * literal types and runs a simple check to guarantee all
 * action types in the application are unique.
 */
export const CSSVariableActionTypes = {
  ADD: type('dspace/css-variables/ADD'),
  ADD_ALL: type('dspace/css-variables/ADD_ALL'),
  CLEAR: type('dspace/css-variables/CLEAR'),
};

export class AddCSSVariableAction implements Action {
  type = CSSVariableActionTypes.ADD;
  payload: {
    name: string,
    value: string
  };

  constructor(name: string, value: string) {
    this.payload = {name, value};
  }
}
export class AddAllCSSVariablesAction implements Action {
  type = CSSVariableActionTypes.ADD_ALL;
  payload: KeyValuePair<string, string>[];

  constructor(variables: KeyValuePair<string, string>[]) {
    this.payload = variables;
  }
}

export class ClearCSSVariablesAction implements Action {
  type = CSSVariableActionTypes.CLEAR;
}

export type CSSVariableAction = AddCSSVariableAction | AddAllCSSVariablesAction | ClearCSSVariablesAction;
