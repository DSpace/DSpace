import { Params } from '@angular/router';
import {
  AddParameterAction,
  AddQueryParameterAction,
  RouteActions,
  RouteActionTypes,
  SetParameterAction,
  SetParametersAction,
  SetQueryParameterAction,
  SetQueryParametersAction
} from './route.actions';
import { isNotEmpty } from '../../shared/empty.util';

/**
 * Interface to represent the parameter state of a current route in the store
 */
export interface RouteState {
  queryParams: Params;
  params: Params;
}

/**
 * The initial route state
 */
const initialState: RouteState = {
  queryParams: {},
  params: {}
};

/**
 * Reducer function to save the current route parameters and query parameters in the store
 * @param state The current or initial state
 * @param action The action to perform on the state
 */
export function routeReducer(state = initialState, action: RouteActions): RouteState {
  switch (action.type) {
    case RouteActionTypes.RESET: {
      return initialState;
    }
    case RouteActionTypes.SET_PARAMETERS: {
      return setParameters(state, action as SetParametersAction, 'params');
    }
    case RouteActionTypes.SET_QUERY_PARAMETERS: {
      return setParameters(state, action as SetQueryParametersAction, 'queryParams');
    }
    case RouteActionTypes.ADD_PARAMETER: {
      return addParameter(state, action as AddParameterAction, 'params');
    }
    case RouteActionTypes.ADD_QUERY_PARAMETER: {
      return addParameter(state, action as AddQueryParameterAction, 'queryParams');
    }
    case RouteActionTypes.SET_PARAMETER: {
      return setParameter(state, action as SetParameterAction, 'params');
    }
    case RouteActionTypes.SET_QUERY_PARAMETER: {
      return setParameter(state, action as SetQueryParameterAction, 'queryParams');
    }
    default: {
      return state;
    }
  }
}

/**
 * Add a route or query parameter in the store
 * @param state The current state
 * @param action The add action to perform on the current state
 * @param paramType The type of parameter to add: route or query parameter
 */
function addParameter(state: RouteState, action: AddParameterAction | AddQueryParameterAction, paramType: string): RouteState {
  const subState = state[paramType];
  const existingValues = subState[action.payload.key] || [];
  const newValues = [...existingValues, action.payload.value];
  const newSubstate = Object.assign({}, subState, { [action.payload.key]: newValues });
  return Object.assign({}, state, { [paramType]: newSubstate });
}

/**
 * Set a route or query parameter in the store
 * @param state The current state
 * @param action The set action to perform on the current state
 * @param paramType The type of parameter to set: route or query parameter
 */
function setParameters(state: RouteState, action: SetParametersAction | SetQueryParametersAction, paramType: string): RouteState {
  const param = isNotEmpty(action.payload) ? { [paramType]: { [action.payload.key]: action.payload.value } } : {};
  return Object.assign({}, state, param);
}

/**
 * Set a route or query parameter in the store
 * @param state The current state
 * @param action The set action to perform on the current state
 * @param paramType The type of parameter to set: route or query parameter
 */
function setParameter(state: RouteState, action: SetParameterAction | SetQueryParameterAction, paramType: string): RouteState {
  const subState = state[paramType];
  const newSubstate = Object.assign({}, subState, { [action.payload.key]: action.payload.value });
  return Object.assign({}, state, { [paramType]: newSubstate });
}
