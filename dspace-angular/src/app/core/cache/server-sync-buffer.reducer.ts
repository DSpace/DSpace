import { hasNoValue, hasValue } from '../../shared/empty.util';
import {
  AddToSSBAction,
  EmptySSBAction,
  ServerSyncBufferAction,
  ServerSyncBufferActionTypes
} from './server-sync-buffer.actions';
import { RestRequestMethod } from '../data/rest-request-method';

/**
 * An entry in the ServerSyncBufferState
 * href: unique href of an ServerSyncBufferEntry
 * method: RestRequestMethod type
 */
export class ServerSyncBufferEntry {
  href: string;
  method: RestRequestMethod;
}

/**
 * The ServerSyncBuffer State
 *
 * Consists list of ServerSyncBufferState
 */
export interface ServerSyncBufferState {
  buffer: ServerSyncBufferEntry[];
}

const initialState: ServerSyncBufferState = { buffer: [] };

/**
 * The ServerSyncBuffer Reducer
 *
 * @param state
 *    the current state
 * @param action
 *    the action to perform on the state
 * @return ServerSyncBufferState
 *    the new state
 */
export function serverSyncBufferReducer(state = initialState, action: ServerSyncBufferAction): ServerSyncBufferState {
  switch (action.type) {

    case ServerSyncBufferActionTypes.ADD: {
      return addToServerSyncQueue(state, action as AddToSSBAction);
    }

    case ServerSyncBufferActionTypes.EMPTY: {
      return emptyServerSyncQueue(state, action as EmptySSBAction);
    }

    default: {
      return state;
    }
  }
}

/**
 * Add a new entry to the buffer with a specified method
 *
 * @param state
 *    the current state
 * @param action
 *    an AddToSSBAction
 * @return ServerSyncBufferState
 *    the new state, with a new entry added to the buffer
 */
function addToServerSyncQueue(state: ServerSyncBufferState, action: AddToSSBAction): ServerSyncBufferState {
  const actionEntry = action.payload as ServerSyncBufferEntry;
  if (hasNoValue(state.buffer.find((entry) => entry.href === actionEntry.href && entry.method === actionEntry.method))) {
    return Object.assign({}, state, { buffer: state.buffer.concat(actionEntry) });
  } else {
    return state;
  }
}

/**
 * Remove all ServerSyncBuffers entry from the buffer with a specified method
 * If no method is specified, empty the whole buffer
 *
 * @param state
 *    the current state
 * @param action
 *    an AddToSSBAction
 * @return ServerSyncBufferState
 *    the new state, with a new entry added to the buffer
 */
function emptyServerSyncQueue(state: ServerSyncBufferState, action: EmptySSBAction): ServerSyncBufferState {
    let newBuffer = [];
    if (hasValue(action.payload)) {
      newBuffer = state.buffer.filter((entry) => entry.method !== action.payload);
    }
    return Object.assign({}, state, { buffer: newBuffer });
}
