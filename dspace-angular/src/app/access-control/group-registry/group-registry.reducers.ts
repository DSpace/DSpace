import { Group } from '../../core/eperson/models/group.model';
import { GroupRegistryAction, GroupRegistryActionTypes, GroupRegistryEditGroupAction } from './group-registry.actions';

/**
 * The metadata registry state.
 * @interface GroupRegistryState
 */
export interface GroupRegistryState {
  editGroup: Group;
}

/**
 * The initial state.
 */
const initialState: GroupRegistryState = {
  editGroup: null,
};

/**
 * Reducer that handles GroupRegistryActions to modify Groups
 * @param state   The current GroupRegistryState
 * @param action  The GroupRegistryAction to perform on the state
 */
export function groupRegistryReducer(state = initialState, action: GroupRegistryAction): GroupRegistryState {

  switch (action.type) {

    case GroupRegistryActionTypes.EDIT_GROUP: {
      return Object.assign({}, state, {
        editGroup: (action as GroupRegistryEditGroupAction).group
      });
    }

    case GroupRegistryActionTypes.CANCEL_EDIT_GROUP: {
      return Object.assign({}, state, {
        editGroup: null
      });
    }

    default:
      return state;
  }
}
