import { Action } from '@ngrx/store';
import { type } from '../shared/ngrx/type';
import { FlatNode } from './flat-node.model';

/**
 * All the action types of the community-list
 */

export const CommunityListActionTypes = {
  SAVE: type('dspace/community-list-page/SAVE')
};

/**
 * Community list SAVE action
 */
export class CommunityListSaveAction implements Action {

  type = CommunityListActionTypes.SAVE;

  payload: {
    expandedNodes: FlatNode[];
    loadingNode: FlatNode;
  };

  constructor(expandedNodes: FlatNode[], loadingNode: FlatNode) {
    this.payload = { expandedNodes, loadingNode };
  }
}

/**
 * Export a type alias of all actions in this action group
 * so that reducers can easily compose action types
 */

export type CommunityListActions = CommunityListSaveAction;
