import { CSSVariableAction, CSSVariableActionTypes } from './css-variable.actions';
import { KeyValuePair } from '../key-value-pair.model';

export interface CSSVariablesState {
  [name: string]: string;
}

const initialState: CSSVariablesState = Object.create({});

/**
 * Reducer that handles the state of CSS variables in the store
 * @param state The current state of the store
 * @param action The action to apply onto the current state of the store
 */
export function cssVariablesReducer(state = initialState, action: CSSVariableAction): CSSVariablesState {
  switch (action.type) {
    case CSSVariableActionTypes.ADD: {
      const variable = action.payload;
      return Object.assign({}, state, { [variable.name]: variable.value });
    } case CSSVariableActionTypes.ADD_ALL: {
      const variables = action.payload;
      return Object.assign({}, state, ...variables.map(({ key, value }: KeyValuePair<string, string>) => {return {[key]: value};}));
    } case CSSVariableActionTypes.CLEAR: {
      return initialState;
    }
    default: {
      return state;
    }
  }
}
