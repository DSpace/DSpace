// eslint-disable-next-line import/no-namespace
import * as deepFreeze from 'deep-freeze';
import {
  SearchFilterCollapseAction,
  SearchFilterDecrementPageAction,
  SearchFilterExpandAction,
  SearchFilterIncrementPageAction,
  SearchFilterInitializeAction,
  SearchFilterResetPageAction,
  SearchFilterToggleAction
} from './search-filter.actions';
import { filterReducer } from './search-filter.reducer';

const filterName1 = 'author';
const filterName2 = 'scope';

class NullAction extends SearchFilterCollapseAction {
  type = null;

  constructor() {
    super(undefined);
  }
}

describe('filterReducer', () => {

  it('should return the current state when no valid actions have been made', () => {
    const state = { author: { filterCollapsed: true, page: 1 } };
    const action = new NullAction();
    const newState = filterReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with an empty object', () => {
    const state = Object.create({});
    const action = new NullAction();
    const initialState = filterReducer(undefined, action);

    // The search filter starts collapsed
    expect(initialState).toEqual(state);
  });

  it('should set filterCollapsed to true in response to the COLLAPSE action', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: false, page: 1 };
    const action = new SearchFilterCollapseAction(filterName1);
    const newState = filterReducer(state, action);

    expect(newState[filterName1].filterCollapsed).toEqual(true);
  });

  it('should perform the COLLAPSE action without affecting the previous state', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: false, page: 1 };
    deepFreeze([state]);

    const action = new SearchFilterCollapseAction(filterName1);
    filterReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set filterCollapsed to false in response to the EXPAND action', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: true, page: 1 };
    const action = new SearchFilterExpandAction(filterName1);
    const newState = filterReducer(state, action);

    expect(newState[filterName1].filterCollapsed).toEqual(false);
  });

  it('should perform the EXPAND action without affecting the previous state', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: true, page: 1 };
    deepFreeze([state]);

    const action = new SearchFilterExpandAction(filterName1);
    filterReducer(state, action);
  });

  it('should flip the value of filterCollapsed in response to the TOGGLE action', () => {
    const state1 = {};
    state1[filterName1] = { filterCollapsed: true, page: 1 };
    const action = new SearchFilterToggleAction(filterName1);

    const state2 = filterReducer(state1, action);
    const state3 = filterReducer(state2, action);

    expect(state2[filterName1].filterCollapsed).toEqual(false);
    expect(state3[filterName1].filterCollapsed).toEqual(true);
  });

  it('should perform the TOGGLE action without affecting the previous state', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: true, page: 1 };
    deepFreeze([state]);

    const action = new SearchFilterToggleAction(filterName1);
    filterReducer(state, action);
  });

  it('should set filterCollapsed to true in response to the INITIALIZE action with isOpenByDefault to false when no state has been set for this filter', () => {
    const state = {};
    state[filterName2] = { filterCollapsed: false, page: 1 };
    const filterConfig = { isOpenByDefault: false, name: filterName1 } as any;
    const action = new SearchFilterInitializeAction(filterConfig);
    const newState = filterReducer(state, action);

    expect(newState[filterName1].filterCollapsed).toEqual(true);
  });

  it('should set filterCollapsed to false in response to the INITIALIZE action with isOpenByDefault to true when no state has been set for this filter', () => {
    const state = {};
    state[filterName2] = { filterCollapsed: true, page: 1 };
    const filterConfig = { isOpenByDefault: true, name: filterName1 } as any;
    const action = new SearchFilterInitializeAction(filterConfig);
    const newState = filterReducer(state, action);
    expect(newState[filterName1].filterCollapsed).toEqual(false);
  });

  it('should not change the state in response to  the INITIALIZE action with isOpenByDefault to false when the state has already been set for this filter', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: false, page: 1 };
    const filterConfig = { isOpenByDefault: true, name: filterName1 } as any;
    const action = new SearchFilterInitializeAction(filterConfig);
    const newState = filterReducer(state, action);
    expect(newState).toEqual(state);
  });

  it('should not change the state in response to  the INITIALIZE action with isOpenByDefault to true when the state has already been set for this filter', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: true, page: 1 };
    const filterConfig = { isOpenByDefault: false, name: filterName1 } as any;
    const action = new SearchFilterInitializeAction(filterConfig);
    const newState = filterReducer(state, action);
    expect(newState).toEqual(state);
  });

  it('should increment with 1 for the specified filter in response to the INCREMENT_PAGE action', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: true, page: 5 };
    const action = new SearchFilterIncrementPageAction(filterName1);
    const newState = filterReducer(state, action);
    expect(newState[filterName1].page).toEqual(6);
  });

  it('should decrement with 1 for the specified filter in response to the DECREMENT_PAGE action', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: true, page: 12 };
    const action = new SearchFilterDecrementPageAction(filterName1);
    const newState = filterReducer(state, action);
    expect(newState[filterName1].page).toEqual(11);
  });

  it('should not decrement when page is 1 for the specified filter in response to the DECREMENT_PAGE action', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: true, page: 1 };
    const action = new SearchFilterDecrementPageAction(filterName1);
    const newState = filterReducer(state, action);
    expect(newState[filterName1].page).toEqual(1);
  });

  it('should reset the page to 1 for the specified filter in response to the RESET_PAGE action', () => {
    const state = {};
    state[filterName1] = { filterCollapsed: true, page: 20 };
    const action = new SearchFilterResetPageAction(filterName1);
    const newState = filterReducer(state, action);
    expect(newState[filterName1].page).toEqual(1);
  });
});
