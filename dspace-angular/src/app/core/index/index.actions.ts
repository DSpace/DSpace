/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';

import { type } from '../../shared/ngrx/type';
import { IndexName } from './index-name.model';

/**
 * The list of HrefIndexAction type definitions
 */
export const IndexActionTypes = {
  ADD: type('dspace/core/index/ADD'),
  REMOVE_BY_VALUE: type('dspace/core/index/REMOVE_BY_VALUE'),
  REMOVE_BY_SUBSTRING: type('dspace/core/index/REMOVE_BY_SUBSTRING')
};

/**
 * An ngrx action to add a value to the index
 */
export class AddToIndexAction implements Action {
  type = IndexActionTypes.ADD;
  payload: {
    name: IndexName;
    value: string;
    key: string;
  };

  /**
   * Create a new AddToIndexAction
   *
   * @param name
   *    the name of the index to add to
   * @param key
   *    the key to add
   * @param value
   *    the self link of the resource the key belongs to
   */
  constructor(name: IndexName, key: string, value: string) {
    this.payload = { name, key, value };
  }
}

/**
 * An ngrx action to remove a value from the index
 */
export class RemoveFromIndexByValueAction implements Action {
  type = IndexActionTypes.REMOVE_BY_VALUE;
  payload: {
    name: IndexName,
    value: any
  };

  /**
   * Create a new RemoveFromIndexByValueAction
   *
   * @param name
   *    the name of the index to remove from
   * @param value
   *    the value to remove the UUID for
   */
  constructor(name: IndexName, value: any) {
    this.payload = { name, value };
  }

}

/**
 * An ngrx action to remove multiple values from the index by substring
 */
export class RemoveFromIndexBySubstringAction implements Action {
  type = IndexActionTypes.REMOVE_BY_SUBSTRING;
  payload: {
    name: IndexName,
    value: string
  };

  /**
   * Create a new RemoveFromIndexByValueAction
   *
   * @param name
   *    the name of the index to remove from
   * @param value
   *    the value to remove the UUID for
   */
  constructor(name: IndexName, value: string) {
    this.payload = { name, value };
  }

}

/**
 * A type to encompass all HrefIndexActions
 */
export type IndexAction = AddToIndexAction | RemoveFromIndexByValueAction | RemoveFromIndexBySubstringAction;
