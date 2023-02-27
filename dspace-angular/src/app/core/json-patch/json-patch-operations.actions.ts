/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';

import { type } from '../../shared/ngrx/type';

/**
 * For each action type in an action group, make a simple
 * enum object for all of this group's action types.
 *
 * The 'type' utility function coerces strings into string
 * literal types and runs a simple check to guarantee all
 * action types in the application are unique.
 */
export const JsonPatchOperationsActionTypes = {
  NEW_JSON_PATCH_ADD_OPERATION: type('dspace/core/patch/NEW_JSON_PATCH_ADD_OPERATION'),
  NEW_JSON_PATCH_COPY_OPERATION: type('dspace/core/patch/NEW_JSON_PATCH_COPY_OPERATION'),
  NEW_JSON_PATCH_MOVE_OPERATION: type('dspace/core/patch/NEW_JSON_PATCH_MOVE_OPERATION'),
  NEW_JSON_PATCH_REMOVE_OPERATION: type('dspace/core/patch/NEW_JSON_PATCH_REMOVE_OPERATION'),
  NEW_JSON_PATCH_REPLACE_OPERATION: type('dspace/core/patch/NEW_JSON_PATCH_REPLACE_OPERATION'),
  COMMIT_JSON_PATCH_OPERATIONS: type('dspace/core/patch/COMMIT_JSON_PATCH_OPERATIONS'),
  ROLLBACK_JSON_PATCH_OPERATIONS: type('dspace/core/patch/ROLLBACK_JSON_PATCH_OPERATIONS'),
  FLUSH_JSON_PATCH_OPERATIONS: type('dspace/core/patch/FLUSH_JSON_PATCH_OPERATIONS'),
  START_TRANSACTION_JSON_PATCH_OPERATIONS: type('dspace/core/patch/START_TRANSACTION_JSON_PATCH_OPERATIONS'),
  DELETE_PENDING_JSON_PATCH_OPERATIONS: type('dspace/core/patch/DELETE_PENDING_JSON_PATCH_OPERATIONS'),
};


/**
 * An ngrx action to commit the current transaction
 */
export class CommitPatchOperationsAction implements Action {
  type = JsonPatchOperationsActionTypes.COMMIT_JSON_PATCH_OPERATIONS;
  payload: {
    resourceType: string;
    resourceId: string;
  };

  /**
   * Create a new CommitPatchOperationsAction
   *
   * @param resourceType
   *    the resource's type
   * @param resourceId
   *    the resource's ID
   */
  constructor(resourceType: string, resourceId: string) {
    this.payload = { resourceType, resourceId };
  }
}

/**
 * An ngrx action to rollback the current transaction
 */
export class RollbacktPatchOperationsAction implements Action {
  type = JsonPatchOperationsActionTypes.ROLLBACK_JSON_PATCH_OPERATIONS;
  payload: {
    resourceType: string;
    resourceId: string;
  };

  /**
   * Create a new CommitPatchOperationsAction
   *
   * @param resourceType
   *    the resource's type
   * @param resourceId
   *    the resource's ID
   */
  constructor(resourceType: string, resourceId: string) {
    this.payload = { resourceType, resourceId };
  }
}

/**
 * An ngrx action to initiate a transaction block
 */
export class StartTransactionPatchOperationsAction implements Action {
  type = JsonPatchOperationsActionTypes.START_TRANSACTION_JSON_PATCH_OPERATIONS;
  payload: {
    resourceType: string;
    resourceId: string;
    startTime: number;
  };

  /**
   * Create a new CommitPatchOperationsAction
   *
   * @param resourceType
   *    the resource's type
   * @param resourceId
   *    the resource's ID
   * @param startTime
   *    the start timestamp
   */
  constructor(resourceType: string, resourceId: string, startTime: number) {
    this.payload = { resourceType, resourceId, startTime };
  }
}

/**
 * An ngrx action to flush list of the JSON Patch operations
 */
export class FlushPatchOperationsAction implements Action {
  type = JsonPatchOperationsActionTypes.FLUSH_JSON_PATCH_OPERATIONS;
  payload: {
    resourceType: string;
    resourceId: string;
  };

  /**
   * Create a new FlushPatchOperationsAction
   *
   * @param resourceType
   *    the resource's type
   * @param resourceId
   *    the resource's ID
   */
  constructor(resourceType: string, resourceId: string) {
    this.payload = { resourceType, resourceId };
  }
}

/**
 * An ngrx action to Add new HTTP/PATCH ADD operations to state
 */
export class NewPatchAddOperationAction implements Action {
  type = JsonPatchOperationsActionTypes.NEW_JSON_PATCH_ADD_OPERATION;
  payload: {
    resourceType: string;
    resourceId: string;
    path: string;
    value: any
  };

  /**
   * Create a new NewPatchAddOperationAction
   *
   * @param resourceType
   *    the resource's type where to add operation
   * @param resourceId
   *    the resource's ID
   * @param path
   *    the path of the operation
   * @param value
   *    the operation's payload
   */
  constructor(resourceType: string, resourceId: string, path: string, value: any) {
    this.payload = { resourceType, resourceId, path, value };
  }
}

/**
 * An ngrx action to add new JSON Patch COPY operation to state
 */
export class NewPatchCopyOperationAction implements Action {
  type = JsonPatchOperationsActionTypes.NEW_JSON_PATCH_COPY_OPERATION;
  payload: {
    resourceType: string;
    resourceId: string;
    from: string;
    path: string;
  };

  /**
   * Create a new NewPatchCopyOperationAction
   *
   * @param resourceType
   *    the resource's type
   * @param resourceId
   *    the resource's ID
   * @param from
   *    the path to copy the value from
   * @param path
   *    the path where to copy the value
   */
  constructor(resourceType: string, resourceId: string, from: string, path: string) {
    this.payload = { resourceType, resourceId, from, path };
  }
}

/**
 * An ngrx action to Add new JSON Patch MOVE operation to state
 */
export class NewPatchMoveOperationAction implements Action {
  type = JsonPatchOperationsActionTypes.NEW_JSON_PATCH_MOVE_OPERATION;
  payload: {
    resourceType: string;
    resourceId: string;
    from: string;
    path: string;
  };

  /**
   * Create a new NewPatchMoveOperationAction
   *
   * @param resourceType
   *    the resource's type
   * @param resourceId
   *    the resource's ID
   * @param from
   *    the path to move the value from
   * @param path
   *    the path where to move the value
   */
  constructor(resourceType: string, resourceId: string, from: string, path: string) {
    this.payload = { resourceType, resourceId, from, path };
  }
}

/**
 * An ngrx action to Add new JSON Patch REMOVE operation to state
 */
export class NewPatchRemoveOperationAction implements Action {
  type = JsonPatchOperationsActionTypes.NEW_JSON_PATCH_REMOVE_OPERATION;
  payload: {
    resourceType: string;
    resourceId: string;
    path: string;
  };

  /**
   * Create a new NewPatchRemoveOperationAction
   *
   * @param resourceType
   *    the resource's type
   * @param resourceId
   *    the resource's ID
   * @param path
   *    the path of the operation
   */
  constructor(resourceType: string, resourceId: string, path: string) {
    this.payload = { resourceType, resourceId, path };
  }
}

/**
 * An ngrx action to add new JSON Patch REPLACE operation to state
 */
export class NewPatchReplaceOperationAction implements Action {
  type = JsonPatchOperationsActionTypes.NEW_JSON_PATCH_REPLACE_OPERATION;
  payload: {
    resourceType: string;
    resourceId: string;
    path: string;
    value: any
  };

  /**
   * Create a new NewPatchReplaceOperationAction
   *
   * @param resourceType
   *    the resource's type
   * @param resourceId
   *    the resource's ID
   * @param path
   *    the path of the operation
   * @param value
   *    the operation's payload
   */
  constructor(resourceType: string, resourceId: string, path: string, value: any) {
    this.payload = { resourceType, resourceId, path, value };
  }
}

/**
 * An ngrx action to delete all pending JSON Patch Operations.
 */
export class DeletePendingJsonPatchOperationsAction implements Action {
  type = JsonPatchOperationsActionTypes.DELETE_PENDING_JSON_PATCH_OPERATIONS;
}


/**
 * Export a type alias of all actions in this action group
 * so that reducers can easily compose action types
 */
export type PatchOperationsActions
  = CommitPatchOperationsAction
  | FlushPatchOperationsAction
  | NewPatchAddOperationAction
  | NewPatchCopyOperationAction
  | NewPatchMoveOperationAction
  | NewPatchRemoveOperationAction
  | NewPatchReplaceOperationAction
  | RollbacktPatchOperationsAction
  | StartTransactionPatchOperationsAction
  | DeletePendingJsonPatchOperationsAction;
