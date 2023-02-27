// eslint-disable-next-line import/no-namespace
import * as deepFreeze from 'deep-freeze';

import { sidebarReducer } from './sidebar.reducer';
import { SidebarCollapseAction, SidebarExpandAction, SidebarToggleAction } from './sidebar.actions';

class NullAction extends SidebarCollapseAction {
  type = null;

  constructor() {
    super();
  }
}

describe('sidebarReducer', () => {

  it('should return the current state when no valid actions have been made', () => {
    const state = { sidebarCollapsed: false };
    const action = new NullAction();
    const newState = sidebarReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with sidebarCollapsed = true', () => {
    const action = new NullAction();
    const initialState = sidebarReducer(undefined, action);

    // The search sidebar starts collapsed
    expect(initialState.sidebarCollapsed).toEqual(true);
  });

  it('should set sidebarCollapsed to true in response to the COLLAPSE action', () => {
    const state = { sidebarCollapsed: false };
    const action = new SidebarCollapseAction();
    const newState = sidebarReducer(state, action);

    expect(newState.sidebarCollapsed).toEqual(true);
  });

  it('should perform the COLLAPSE action without affecting the previous state', () => {
    const state = { sidebarCollapsed: false };
    deepFreeze([state]);

    const action = new SidebarCollapseAction();
    sidebarReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set sidebarCollapsed to false in response to the EXPAND action', () => {
    const state = { sidebarCollapsed: true };
    const action = new SidebarExpandAction();
    const newState = sidebarReducer(state, action);

    expect(newState.sidebarCollapsed).toEqual(false);
  });

  it('should perform the EXPAND action without affecting the previous state', () => {
    const state = { sidebarCollapsed: true };
    deepFreeze([state]);

    const action = new SidebarExpandAction();
    sidebarReducer(state, action);
  });

  it('should flip the value of sidebarCollapsed in response to the TOGGLE action', () => {
    const state1 = { sidebarCollapsed: true };
    const action = new SidebarToggleAction();

    const state2 = sidebarReducer(state1, action);
    const state3 = sidebarReducer(state2, action);

    expect(state2.sidebarCollapsed).toEqual(false);
    expect(state3.sidebarCollapsed).toEqual(true);
  });

  it('should perform the TOGGLE action without affecting the previous state', () => {
    const state = { sidebarCollapsed: true };
    deepFreeze([state]);

    const action = new SidebarToggleAction();
    sidebarReducer(state, action);
  });

});
