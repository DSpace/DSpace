import {
  ObjectSelectionDeselectAction,
  ObjectSelectionInitialDeselectAction,
  ObjectSelectionInitialSelectAction,
  ObjectSelectionResetAction,
  ObjectSelectionSelectAction,
  ObjectSelectionSwitchAction
} from './object-select.actions';
import { objectSelectionReducer } from './object-select.reducer';

const key = 'key';
const objectId1 = 'id1';
const objectId2 = 'id2';

class NullAction extends ObjectSelectionSelectAction {
  type = null;

  constructor() {
    super(undefined, undefined);
  }
}

describe('objectSelectionReducer', () => {

  it('should return the current state when no valid actions have been made', () => {
    const state = {};
    state[key] = {};
    state[key][objectId1] = { checked: true };
    const action = new NullAction();
    const newState = objectSelectionReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with an empty object', () => {
    const state = {};
    const action = new NullAction();
    const newState = objectSelectionReducer(undefined, action);

    expect(newState).toEqual(state);
  });

  it('should set checked to true in response to the INITIAL_SELECT action', () => {
    const action = new ObjectSelectionInitialSelectAction(key, objectId1);
    const newState = objectSelectionReducer(undefined, action);

    expect(newState[key][objectId1].checked).toBeTruthy();
  });

  it('should set checked to true in response to the INITIAL_DESELECT action', () => {
    const action = new ObjectSelectionInitialDeselectAction(key, objectId1);
    const newState = objectSelectionReducer(undefined, action);

    expect(newState[key][objectId1].checked).toBeFalsy();
  });

  it('should set checked to true in response to the SELECT action', () => {
    const state = {};
    state[key] = {};
    state[key][objectId1] = { checked: false };
    const action = new ObjectSelectionSelectAction(key, objectId1);
    const newState = objectSelectionReducer(state, action);

    expect(newState[key][objectId1].checked).toBeTruthy();
  });

  it('should set checked to false in response to the DESELECT action', () => {
    const state = {};
    state[key] = {};
    state[key][objectId1] = { checked: true };
    const action = new ObjectSelectionDeselectAction(key, objectId1);
    const newState = objectSelectionReducer(state, action);

    expect(newState[key][objectId1].checked).toBeFalsy();
  });

  it('should set checked from false to true in response to the SWITCH action', () => {
    const state = {};
    state[key] = {};
    state[key][objectId1] = { checked: false };
    const action = new ObjectSelectionSwitchAction(key, objectId1);
    const newState = objectSelectionReducer(state, action);

    expect(newState[key][objectId1].checked).toBeTruthy();
  });

  it('should set checked from true to false in response to the SWITCH action', () => {
    const state = {};
    state[key] = {};
    state[key][objectId1] = { checked: true };
    const action = new ObjectSelectionSwitchAction(key, objectId1);
    const newState = objectSelectionReducer(state, action);

    expect(newState[key][objectId1].checked).toBeFalsy();
  });

  it('should reset the state in response to the RESET action', () => {
    const state = {};
    state[key] = {};
    state[key][objectId1] = { checked: true };
    state[key][objectId2] = { checked: false };
    const action = new ObjectSelectionResetAction(key, undefined);
    const newState = objectSelectionReducer(state, action);

    expect(newState[key]).toEqual({});
  });

});
