/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';
import { type } from '../../shared/ngrx/type';
import { Params } from '@angular/router';

/**
 * The list of HrefIndexAction type definitions
 */
export const RouteActionTypes = {
  SET_QUERY_PARAMETERS: type('dspace/core/route/SET_QUERY_PARAMETERS'),
  SET_PARAMETERS: type('dspace/core/route/SET_PARAMETERS'),
  ADD_QUERY_PARAMETER: type('dspace/core/route/ADD_QUERY_PARAMETER'),
  ADD_PARAMETER: type('dspace/core/route/ADD_PARAMETER'),
  SET_QUERY_PARAMETER: type('dspace/core/route/SET_QUERY_PARAMETER'),
  SET_PARAMETER: type('dspace/core/route/SET_PARAMETER'),
  RESET: type('dspace/core/route/RESET'),
};

/**
 * An ngrx action to set the query parameters
 */
export class SetQueryParametersAction implements Action {
  type = RouteActionTypes.SET_QUERY_PARAMETERS;
  payload: Params;

  /**
   * Create a new SetQueryParametersAction
   *
   * @param parameters
   *    the query parameters
   */
  constructor(parameters: Params) {
    this.payload = parameters;
  }
}

/**
 * An ngrx action to set the parameters
 */
export class SetParametersAction implements Action {
  type = RouteActionTypes.SET_PARAMETERS;
  payload: Params;

  /**
   * Create a new SetParametersAction
   *
   * @param parameters
   *    the parameters
   */
  constructor(parameters: Params) {
    this.payload = parameters;
  }
}

/**
 * An ngrx action to add a query parameter
 */
export class AddQueryParameterAction implements Action {
  type = RouteActionTypes.ADD_QUERY_PARAMETER;
  payload: {
    key: string;
    value: string;
  };

  /**
   * Create a new AddQueryParameterAction
   *
   * @param key
   *    the key to add
   * @param value
   *    the value of this key
   */
  constructor(key: string, value: string) {
    this.payload = { key, value };
  }
}

/**
 * An ngrx action to add a parameter
 */
export class AddParameterAction implements Action {
  type = RouteActionTypes.ADD_PARAMETER;
  payload: {
    key: string;
    value: string;
  };

  /**
   * Create a new AddParameterAction
   *
   * @param key
   *    the key to add
   * @param value
   *    the value of this key
   */
  constructor(key: string, value: string) {
    this.payload = { key, value };
  }
}

/**
 * An ngrx action to set a query parameter
 */
export class SetQueryParameterAction implements Action {
  type = RouteActionTypes.SET_QUERY_PARAMETER;
  payload: {
    key: string;
    value: string;
  };

  /**
   * Create a new SetQueryParameterAction
   *
   * @param key
   *    the key to set
   * @param value
   *    the value of this key
   */
  constructor(key: string, value: string) {
    this.payload = { key, value };
  }
}

/**
 * An ngrx action to set a parameter
 */
export class SetParameterAction implements Action {
  type = RouteActionTypes.SET_PARAMETER;
  payload: {
    key: string;
    value: string;
  };

  /**
   * Create a new SetParameterAction
   *
   * @param key
   *    the key to set
   * @param value
   *    the value of this key
   */
  constructor(key: string, value: string) {
    this.payload = { key, value };
  }
}

/**
 * An ngrx action to reset the route state
 */
export class ResetRouteStateAction implements Action {
  type = RouteActionTypes.RESET;
}


/**
 * A type to encompass all RouteActions
 */
export type RouteActions =
  SetQueryParametersAction
  | SetParametersAction
  | AddQueryParameterAction
  | AddParameterAction
  | ResetRouteStateAction
  | SetParameterAction
  | SetQueryParameterAction;
