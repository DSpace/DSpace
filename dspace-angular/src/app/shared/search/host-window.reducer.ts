import { HostWindowAction, HostWindowActionTypes } from '../host-window.actions';

export interface HostWindowState {
  width: number;
  height: number;
}

const initialState: HostWindowState = {
  width: null,
  height: null
};

export function hostWindowReducer(state = initialState, action: HostWindowAction): HostWindowState {
  switch (action.type) {

    case HostWindowActionTypes.RESIZE: {
      return Object.assign({}, state, action.payload);
    }

    default: {
      return state;
    }
  }
}
