/* eslint-disable max-classes-per-file */
import { type } from '../../shared/ngrx/type';
import { Action } from '@ngrx/store';

export const MetaTagTypes = {
  ADD: type('dspace/meta-tag/ADD'),
  CLEAR: type('dspace/meta-tag/CLEAR')
};

export class AddMetaTagAction implements Action {
  type = MetaTagTypes.ADD;
  payload: string;

  constructor(property: string) {
    this.payload = property;
  }
}

export class ClearMetaTagAction implements Action {
  type = MetaTagTypes.CLEAR;
}

export type MetaTagAction = AddMetaTagAction | ClearMetaTagAction;
