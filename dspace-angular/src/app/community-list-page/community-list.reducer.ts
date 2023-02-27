import { CommunityListActions, CommunityListActionTypes, CommunityListSaveAction } from './community-list.actions';
import { FlatNode } from './flat-node.model';

/**
 * States we wish to put in store concerning the community list
 */
export interface CommunityListState {
  expandedNodes: FlatNode[];
  loadingNode: FlatNode;
}

/**
 * Initial starting state of the list of expandedNodes and the current loading node of the community list
 */
const initialState: CommunityListState = {
  expandedNodes: [],
  loadingNode: null,
};

/**
 * Reducer to interact with store concerning objects for the community list
 * @constructor
 */
export function CommunityListReducer(state = initialState, action: CommunityListActions) {
  switch (action.type) {
    case CommunityListActionTypes.SAVE: {
      return Object.assign({}, state, {
        expandedNodes: (action as CommunityListSaveAction).payload.expandedNodes,
        loadingNode: (action as CommunityListSaveAction).payload.loadingNode,
      });
    }
    default: {
      return state;
    }
  }
}
