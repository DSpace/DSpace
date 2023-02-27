import { AddUrlToHistoryAction, HistoryAction, HistoryActionTypes } from './history.actions';

/**
 * The auth state.
 */
// eslint-disable-next-line @typescript-eslint/no-empty-interface
export interface HistoryState extends Array<string> {

}

/**
 * The initial state.
 */
const initialState: HistoryState = [];

export function historyReducer(state = initialState, action: HistoryAction): HistoryState {
  switch (action.type) {

    case HistoryActionTypes.ADD_TO_HISTORY: {
      return [...state, (action as AddUrlToHistoryAction).payload.url];
    }

    default: {
      return state;
    }
  }
}
