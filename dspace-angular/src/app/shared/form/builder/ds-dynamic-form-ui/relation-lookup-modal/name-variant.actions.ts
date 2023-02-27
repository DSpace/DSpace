/* eslint-disable max-classes-per-file */
/**
 * The list of NameVariantAction type definitions
 */
import { type } from '../../../../ngrx/type';
import { Action } from '@ngrx/store';

export const NameVariantActionTypes = {
  SET_NAME_VARIANT: type('dspace/name-variant/SET_NAME_VARIANT'),
  REMOVE_NAME_VARIANT: type('dspace/name-variant/REMOVE_NAME_VARIANT'),
};

/**
 * Abstract class for actions that happen to name variants
 */
export abstract class NameVariantListAction implements Action {
  type;
  payload: {
    listID: string;
    itemID: string;
  };

  constructor(listID: string, itemID: string) {
    this.payload = { listID, itemID };
  }
}

/**
 * Action for setting a new name on an item in a certain list
 */
export class SetNameVariantAction extends NameVariantListAction {
  type = NameVariantActionTypes.SET_NAME_VARIANT;
  payload: {
    listID: string;
    itemID: string;
    nameVariant: string;
  };

  constructor(listID: string, itemID: string, nameVariant: string) {
    super(listID, itemID);
    this.payload.nameVariant = nameVariant;
  }
}

/**
 * Action for removing a name on an item in a certain list
 */
export class RemoveNameVariantAction extends NameVariantListAction {
  type = NameVariantActionTypes.REMOVE_NAME_VARIANT;
  constructor(listID: string, itemID: string) {
    super(listID, itemID);
  }
}

/**
 * A type to encompass all RelationshipActions
 */
export type NameVariantAction
  = SetNameVariantAction
  | RemoveNameVariantAction;
