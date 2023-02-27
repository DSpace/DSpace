import { GroupMock } from '../../shared/testing/group-mock';
import { GroupRegistryCancelGroupAction, GroupRegistryEditGroupAction } from './group-registry.actions';
import { groupRegistryReducer, GroupRegistryState } from './group-registry.reducers';

const initialState: GroupRegistryState = {
  editGroup: null,
};

const editState: GroupRegistryState = {
  editGroup: GroupMock,
};

class NullAction extends GroupRegistryEditGroupAction {
  type = null;

  constructor() {
    super(undefined);
  }
}

describe('groupRegistryReducer', () => {

  it('should return the current state when no valid actions have been made', () => {
    const state = initialState;
    const action = new NullAction();
    const newState = groupRegistryReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with an initial state', () => {
    const state = initialState;
    const action = new NullAction();
    const initState = groupRegistryReducer(undefined, action);

    expect(initState).toEqual(state);
  });

  it('should update the current state to change the editGroup to a new group when GroupRegistryEditGroupAction is dispatched', () => {
    const state = editState;
    const action = new GroupRegistryEditGroupAction(GroupMock);
    const newState = groupRegistryReducer(state, action);

    expect(newState.editGroup).toEqual(GroupMock);
  });

  it('should update the current state to remove the editGroup from the state when GroupRegistryCancelGroupAction is dispatched', () => {
    const state = editState;
    const action = new GroupRegistryCancelGroupAction();
    const newState = groupRegistryReducer(state, action);

    expect(newState.editGroup).toEqual(null);
  });
});
