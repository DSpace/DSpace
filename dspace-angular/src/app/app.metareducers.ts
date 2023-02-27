import { StoreActionTypes } from './store.actions';

// fallback ngrx debugger
let actionCounter = 0;

export function debugMetaReducer(reducer) {
  return (state, action) => {
    actionCounter++;
    console.log('@ngrx action', actionCounter, action.type);
    console.log('state', JSON.stringify(state));
    console.log('action', JSON.stringify(action));
    console.log('------------------------------------');
    return reducer(state, action);
  };
}

export function universalMetaReducer(reducer) {
  return (state, action) => {
    switch (action.type) {
      case StoreActionTypes.REHYDRATE:
        state = Object.assign({}, state, action.payload);
        break;
      case StoreActionTypes.REPLAY:
      default:
        break;
    }
    return reducer(state, action);
  };
}

export const debugMetaReducers = [
  debugMetaReducer
];

export const appMetaReducers = [
  universalMetaReducer
];
