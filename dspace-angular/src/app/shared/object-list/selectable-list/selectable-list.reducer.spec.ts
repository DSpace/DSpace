/* eslint-disable max-classes-per-file */
import {
  SelectableListAction,
  SelectableListDeselectAction,
  SelectableListDeselectAllAction,
  SelectableListDeselectSingleAction,
  SelectableListSelectAction,
  SelectableListSelectSingleAction,
  SelectableListSetSelectionAction
} from './selectable-list.actions';
import { selectableListReducer } from './selectable-list.reducer';
import { ListableObject } from '../../object-collection/shared/listable-object.model';
import { hasValue } from '../../empty.util';

class SelectableObject extends ListableObject {
  constructor(private value: string) {
    super();
  }

  equals(other: SelectableObject): boolean {
    return hasValue(this.value) && hasValue(other.value) && this.value === other.value;
  }

  getRenderTypes() {
    return ['selectable'];
  }
}

class NullAction extends SelectableListAction {
  type = null;

  constructor() {
    super(undefined, undefined);
  }
}

const listID1 = 'id1';
const listID2 = 'id2';
const value1 = 'Selected object';
const value2 = 'Another selected object';
const value3 = 'Selection';
const value4 = 'Selected object numero 4';

const selected1 = new SelectableObject(value1);
const selected2 = new SelectableObject(value2);
const selected3 = new SelectableObject(value3);
const selected4 = new SelectableObject(value4);
const testState = { [listID1]: { id: listID1, selection: [selected1, selected2] } };

describe('selectableListReducer', () => {

  it('should return the current state when no valid actions have been made', () => {
    const state = {};
    state[listID1] = {};
    state[listID1] = { id: listID1, selection: [selected1, selected2] };
    const action = new NullAction();
    const newState = selectableListReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with an empty object', () => {
    const state = {};
    const action = new NullAction();
    const newState = selectableListReducer(undefined, action);

    expect(newState).toEqual(state);
  });

  it('should add the payload to the existing list in response to the SELECT action for the given id', () => {
    const action = new SelectableListSelectAction(listID1, [selected3, selected4]);
    const newState = selectableListReducer(testState, action);

    expect(newState[listID1].selection).toEqual([selected1, selected2, selected3, selected4]);
  });

  it('should add the payload to the existing list in response to the SELECT_SINGLE action for the given id', () => {
    const action = new SelectableListSelectSingleAction(listID1, selected4);
    const newState = selectableListReducer(testState, action);

    expect(newState[listID1].selection).toEqual([selected1, selected2, selected4]);
  });

  it('should remove the payload from the existing list in response to the DESELECT action for the given id', () => {
    const action = new SelectableListDeselectAction(listID1, [selected1, selected2]);
    const newState = selectableListReducer(testState, action);

    expect(newState[listID1].selection).toEqual([]);
  });

  it('should remove the payload from the existing list in response to the DESELECT_SINGLE action for the given id', () => {
    const action = new SelectableListDeselectSingleAction(listID1, selected2);
    const newState = selectableListReducer(testState, action);

    expect(newState[listID1].selection).toEqual([selected1]);
  });

  it('should set the list to the payload in response to the SET_SELECTION action for the given id', () => {
    const action = new SelectableListSetSelectionAction(listID2, [selected2, selected4]);
    const newState = selectableListReducer(testState, action);

    expect(newState[listID1].selection).toEqual(testState[listID1].selection);
    expect(newState[listID2].selection).toEqual([selected2, selected4]);
  });

  it('should remove the payload from the existing list in response to the DESELECT action for the given id', () => {
    const action = new SelectableListDeselectAllAction(listID1);
    const newState = selectableListReducer(testState, action);

    expect(newState[listID1].selection).toEqual([]);
  });
});
