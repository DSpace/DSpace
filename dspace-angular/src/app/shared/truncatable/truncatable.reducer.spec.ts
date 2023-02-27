// eslint-disable-next-line import/no-namespace
import * as deepFreeze from 'deep-freeze';

import { truncatableReducer } from './truncatable.reducer';
import { TruncatableCollapseAction, TruncatableExpandAction, TruncatableToggleAction } from './truncatable.actions';

const id1 = '123';
const id2 = '456';

class NullAction extends TruncatableCollapseAction {
  type = null;

  constructor() {
    super(undefined);
  }
}

describe('truncatableReducer', () => {

  it('should return the current state when no valid actions have been made', () => {
    const state = { 123: { collapsed: true, page: 1 } };
    const action = new NullAction();
    const newState = truncatableReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with an empty object', () => {
    const state = Object.create({});
    const action = new NullAction();
    const initialState = truncatableReducer(undefined, action);

    // The search filter starts collapsed
    expect(initialState).toEqual(state);
  });

  it('should set collapsed to true in response to the COLLAPSE action', () => {
    const state = {};
    state[id1] = { collapsed: false };
    const action = new TruncatableCollapseAction(id1);
    const newState = truncatableReducer(state, action);

    expect(newState[id1].collapsed).toEqual(true);
  });

  it('should perform the COLLAPSE action without affecting the previous state', () => {
    const state = {};
    state[id1] = { collapsed: false };
    deepFreeze([state]);

    const action = new TruncatableCollapseAction(id1);
    truncatableReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set filterCollapsed to false in response to the EXPAND action', () => {
    const state = {};
    state[id1] = { collapsed: true };
    const action = new TruncatableExpandAction(id1);
    const newState = truncatableReducer(state, action);

    expect(newState[id1].collapsed).toEqual(false);
  });

  it('should perform the EXPAND action without affecting the previous state', () => {
    const state = {};
    state[id1] = { collapsed: true };
    deepFreeze([state]);

    const action = new TruncatableExpandAction(id1);
    truncatableReducer(state, action);
  });

  it('should flip the value of filterCollapsed in response to the TOGGLE action', () => {
    const state1 = {};
    state1[id1] = { collapsed: true };
    const action = new TruncatableToggleAction(id1);

    const state2 = truncatableReducer(state1, action);
    const state3 = truncatableReducer(state2, action);

    expect(state2[id1].collapsed).toEqual(false);
    expect(state3[id1].collapsed).toEqual(true);
  });

  it('should perform the TOGGLE action without affecting the previous state', () => {
    const state = {};
    state[id2] = { collapsed: true };
    deepFreeze([state]);

    const action = new TruncatableToggleAction(id2);
    truncatableReducer(state, action);
  });
});
