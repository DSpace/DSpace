import { SidebarAction, SidebarActionTypes } from './sidebar.actions';

/**
 * Interface that represents the state of the sidebar
 */
export interface SidebarState {
  sidebarCollapsed: boolean;
}

const initialState: SidebarState = {
  sidebarCollapsed: true
};

/**
 * Performs a sidebar action on the current state
 * @param {SidebarState} state The state before the action is performed
 * @param {SidebarAction} action The action that should be performed
 * @returns {SidebarState} The state after the action is performed
 */
export function sidebarReducer(state = initialState, action: SidebarAction): SidebarState {
  switch (action.type) {

    case SidebarActionTypes.COLLAPSE: {
      return Object.assign({}, state, {
        sidebarCollapsed: true
      });
    }

    case SidebarActionTypes.EXPAND: {
      return Object.assign({}, state, {
        sidebarCollapsed: false
      });

    }

    case SidebarActionTypes.TOGGLE: {
      return Object.assign({}, state, {
        sidebarCollapsed: !state.sidebarCollapsed
      });

    }

    default: {
      return state;
    }
  }
}
