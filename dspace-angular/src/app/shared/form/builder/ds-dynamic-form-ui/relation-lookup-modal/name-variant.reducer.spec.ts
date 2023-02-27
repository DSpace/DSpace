import { RemoveNameVariantAction, SetNameVariantAction } from './name-variant.actions';
import { Action } from '@ngrx/store';
import { nameVariantReducer } from './name-variant.reducer';

class NullAction implements Action {
  type = null;
}

let listID1;
let listID2;
let itemID1;
let itemID2;
let variantList1Item1;
let variantList1Item1Update;
let variantList1Item2;

function init() {
  listID1 = 'dbfb81de-2930-4de6-ba2e-ea21c8534ee9';
  listID2 = 'd7f2c48d-e1e2-4996-ab8d-e271cabec78a';
  itemID1 = 'd1c81d4f-6b05-4844-986b-372d2e39c6aa';
  itemID2 = 'fe4ca421-d897-417f-9436-9724262d5c69';
  variantList1Item1 = 'Test Name Variant 1';
  variantList1Item1Update = 'Test Name Variant 1 Update';
  variantList1Item2 = 'Test Name Variant 2';
}

describe('nameVariantReducer', () => {
  beforeEach(() => {
    init();
  });

  it('should return the current state when no valid actions have been made', () => {
    const state = { [listID1]: { [itemID1]: variantList1Item1 } };
    const action = new NullAction() as any;
    const newState = nameVariantReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with an empty object', () => {
    const state = Object.create({});
    const action = new NullAction() as any;
    const initialState = nameVariantReducer(undefined, action);

    // The search filter starts collapsed
    expect(initialState).toEqual(state);
  });

  it('should set add a new name variant in response to the SET_NAME_VARIANT' +
    ' action with a combination of list and item ID that does not exist yet', () => {
    const state = {};
    state[listID1] = { [itemID1]: variantList1Item1 };
    const action = new SetNameVariantAction(listID1, itemID2, variantList1Item2);
    const newState = nameVariantReducer(state, action);

    expect(newState[listID1][itemID1]).toEqual(variantList1Item1);
    expect(newState[listID1][itemID2]).toEqual(variantList1Item2);
  });

  it('should set a name variant in response to the SET_NAME_VARIANT' +
    ' action with a combination of list and item ID that already exists', () => {
    const state = {};
    state[listID1] = { [itemID1]: variantList1Item1 };
    const action = new SetNameVariantAction(listID1, itemID1, variantList1Item1Update);
    const newState = nameVariantReducer(state, action);

    expect(newState[listID1][itemID1]).toEqual(variantList1Item1Update);
  });

  it('should remove a name variant in response to the REMOVE_NAME_VARIANT' +
    ' action with a combination of list and item ID that already exists', () => {
    const state = {};
    state[listID1] = { [itemID1]: variantList1Item1 };
    expect(state[listID1][itemID1]).toEqual(variantList1Item1);

    const action = new RemoveNameVariantAction(listID1, itemID1);
    const newState = nameVariantReducer(state, action);

    expect(newState[listID1][itemID1]).toBeUndefined();
  });

  it('should do nothing in response to the REMOVE_NAME_VARIANT' +
    ' action with a combination of list and item ID that does not exists', () => {
    const state = {};
    state[listID1] = { [itemID1]: variantList1Item1 };

    const action = new RemoveNameVariantAction(listID2, itemID1);
    const newState = nameVariantReducer(state, action);

    expect(newState).toEqual(state);
  });
});
