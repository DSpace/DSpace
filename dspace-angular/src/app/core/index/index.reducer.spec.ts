// eslint-disable-next-line import/no-namespace
import * as deepFreeze from 'deep-freeze';

import { indexReducer, MetaIndexState } from './index.reducer';
import {
  AddToIndexAction,
  RemoveFromIndexBySubstringAction,
  RemoveFromIndexByValueAction
} from './index.actions';
import { IndexName } from './index-name.model';

class NullAction extends AddToIndexAction {
  type = null;
  payload = null;

  constructor() {
    super(null, null, null);
  }
}

describe('requestReducer', () => {
  const key1 = '567a639f-f5ff-4126-807c-b7d0910808c8';
  const key2 = '1911e8a4-6939-490c-b58b-a5d70f8d91fb';
  const val1 = 'https://dspace7.4science.it/dspace-spring-rest/api/core/items/567a639f-f5ff-4126-807c-b7d0910808c8';
  const val2 = 'https://dspace7.4science.it/dspace-spring-rest/api/core/items/1911e8a4-6939-490c-b58b-a5d70f8d91fb';
  const testState: MetaIndexState = {
    [IndexName.OBJECT]: {
      [key1]: val1
    }, [IndexName.ALTERNATIVE_OBJECT_LINK]: {
      [key1]: val1
    }, [IndexName.REQUEST]: {
      [key1]: val1
    }
  };
  deepFreeze(testState);

  it('should return the current state when no valid actions have been made', () => {
    const action = new NullAction();
    const newState = indexReducer(testState, action);

    expect(newState).toEqual(testState);
  });

  it('should start with an empty state', () => {
    const action = new NullAction();
    const initialState = indexReducer(undefined, action);

    expect(initialState).toEqual(Object.create(null));
  });

  it('should add the \'key\' with the corresponding \'value\' to the correct substate, in response to an ADD action', () => {
    const state = testState;

    const action = new AddToIndexAction(IndexName.REQUEST, key2, val2);
    const newState = indexReducer(state, action);

    expect(newState[IndexName.REQUEST][key2]).toEqual(val2);
  });

  it('should remove the given \'value\' from its corresponding \'key\' in the correct substate, in response to a REMOVE_BY_VALUE action', () => {
    const state = testState;

    const action = new RemoveFromIndexByValueAction(IndexName.OBJECT, val1);
    const newState = indexReducer(state, action);

    expect(newState[IndexName.OBJECT][key1]).toBeUndefined();
  });

  it('should remove the given \'value\' from its corresponding \'key\' in the correct substate, in response to a REMOVE_BY_SUBSTRING action', () => {
    const state = testState;

    const action = new RemoveFromIndexBySubstringAction(IndexName.OBJECT, key1);
    const newState = indexReducer(state, action);

    expect(newState[IndexName.OBJECT][key1]).toBeUndefined();
  });
});
