/* eslint-disable max-classes-per-file */
/**
 * The list of RelationshipAction type definitions
 */
import { type } from '../../../../ngrx/type';
import { Action } from '@ngrx/store';
import { Item } from '../../../../../core/shared/item.model';
import { Relationship } from '../../../../../core/shared/item-relationships/relationship.model';

export const RelationshipActionTypes = {
  ADD_RELATIONSHIP: type('dspace/relationship/ADD_RELATIONSHIP'),
  REMOVE_RELATIONSHIP: type('dspace/relationship/REMOVE_RELATIONSHIP'),
  UPDATE_NAME_VARIANT: type('dspace/relationship/UPDATE_NAME_VARIANT'),
  UPDATE_RELATIONSHIP: type('dspace/relationship/UPDATE_RELATIONSHIP'),
};

/**
 * An ngrx action to create a new relationship
 */
export class AddRelationshipAction implements Action {
  type = RelationshipActionTypes.ADD_RELATIONSHIP;

  payload: {
    item1: Item;
    item2: Item;
    relationshipType: string;
    submissionId: string;
    nameVariant: string;
  };

  /**
   * Create a new AddRelationshipAction
   *
   * @param item1 The first item in the relationship
   * @param item2 The second item in the relationship
   * @param relationshipType The label of the relationshipType
   * @param submissionId The current submissionId
   * @param nameVariant The nameVariant of the relationshipType
   */
  constructor(
    item1: Item,
    item2: Item,
    relationshipType: string,
    submissionId: string,
    nameVariant?: string
  ) {
    this.payload = { item1, item2, relationshipType, submissionId, nameVariant };
  }
}

export class UpdateRelationshipNameVariantAction implements Action {
  type = RelationshipActionTypes.UPDATE_NAME_VARIANT;

  payload: {
    item1: Item;
    item2: Item;
    relationshipType: string;
    submissionId: string;
    nameVariant: string;
  };

  /**
   * Create a new UpdateRelationshipNameVariantAction
   *
   * @param item1 The first item in the relationship
   * @param item2 The second item in the relationship
   * @param relationshipType The label of the relationshipType
   * @param submissionId The current submissionId
   * @param nameVariant The nameVariant of the relationshipType
   */
  constructor(
    item1: Item,
    item2: Item,
    relationshipType: string,
    submissionId: string,
    nameVariant?: string
  ) {
    this.payload = { item1, item2, relationshipType, submissionId, nameVariant };
  }
}

export class UpdateRelationshipAction implements Action {
  type = RelationshipActionTypes.UPDATE_RELATIONSHIP;

  payload: {
    relationship: Relationship;
    submissionId: string;
  };

  /**
   * Create a new UpdateRelationshipAction
   *
   * @param relationship The relationship
   * @param submissionId The current submissionId
   */
  constructor(
    relationship: Relationship,
    submissionId: string,
  ) {
    this.payload = { relationship, submissionId };
  }
}

/**
 * An ngrx action to remove an existing relationship
 */
export class RemoveRelationshipAction implements Action {
  type = RelationshipActionTypes.REMOVE_RELATIONSHIP;

  payload: {
    item1: Item;
    item2: Item;
    relationshipType: string;
    submissionId: string;
  };

  /**
   * Create a new RemoveRelationshipAction
   *
   * @param item1 The first item in the relationship
   * @param item2 The second item in the relationship
   * @param relationshipType The label of the relationshipType
   * @param submissionId The current submissionId
   */
  constructor(
    item1: Item,
    item2: Item,
    relationshipType: string,
    submissionId: string
  ) {
    this.payload = { item1, item2, relationshipType, submissionId };
  }
}


/**
 * A type to encompass all RelationshipActions
 */
export type RelationshipAction
  = AddRelationshipAction
  | RemoveRelationshipAction;
