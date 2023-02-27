import { EPeopleRegistryCancelEPersonAction, EPeopleRegistryEditEPersonAction } from './epeople-registry.actions';
import { ePeopleRegistryReducer, EPeopleRegistryState } from './epeople-registry.reducers';
import { EPersonMock } from '../../shared/testing/eperson.mock';

const initialState: EPeopleRegistryState = {
  editEPerson: null,
};

const editState: EPeopleRegistryState = {
  editEPerson: EPersonMock,
};

class NullAction extends EPeopleRegistryEditEPersonAction {
  type = null;

  constructor() {
    super(undefined);
  }
}

describe('epeopleRegistryReducer', () => {

  it('should return the current state when no valid actions have been made', () => {
    const state = initialState;
    const action = new NullAction();
    const newState = ePeopleRegistryReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with an initial state', () => {
    const state = initialState;
    const action = new NullAction();
    const initState = ePeopleRegistryReducer(undefined, action);

    expect(initState).toEqual(state);
  });

  it('should update the current state to change the editEPerson to a new eperson when EPeopleRegistryEditEPersonAction is dispatched', () => {
    const state = editState;
    const action = new EPeopleRegistryEditEPersonAction(EPersonMock);
    const newState = ePeopleRegistryReducer(state, action);

    expect(newState.editEPerson).toEqual(EPersonMock);
  });

  it('should update the current state to remove the editEPerson from the state when EPeopleRegistryCancelEPersonAction is dispatched', () => {
    const state = editState;
    const action = new EPeopleRegistryCancelEPersonAction();
    const newState = ePeopleRegistryReducer(state, action);

    expect(newState.editEPerson).toEqual(null);
  });
});
