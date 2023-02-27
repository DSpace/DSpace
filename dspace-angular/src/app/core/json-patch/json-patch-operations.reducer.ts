import { hasValue, isNotEmpty, isNotUndefined, isNull } from '../../shared/empty.util';

import {
  FlushPatchOperationsAction,
  PatchOperationsActions,
  JsonPatchOperationsActionTypes,
  NewPatchAddOperationAction,
  NewPatchCopyOperationAction,
  NewPatchMoveOperationAction,
  NewPatchRemoveOperationAction,
  NewPatchReplaceOperationAction,
  CommitPatchOperationsAction,
  StartTransactionPatchOperationsAction,
  RollbacktPatchOperationsAction,
  DeletePendingJsonPatchOperationsAction
} from './json-patch-operations.actions';
import { JsonPatchOperationModel, JsonPatchOperationType } from './json-patch.model';

/**
 * An interface to represent JSON-PATCH Operation objects to execute
 */
export interface JsonPatchOperationObject {
  operation: JsonPatchOperationModel;
  timeCompleted: number;
}

/**
 * An interface to represent the body containing a list of JsonPatchOperationObject
 */
export interface JsonPatchOperationsEntry {
  body: JsonPatchOperationObject[];
}

/**
 * Interface used to represent a JSON-PATCH path member
 * in JsonPatchOperationsState
 */
export interface JsonPatchOperationsResourceEntry {
  children: { [resourceId: string]: JsonPatchOperationsEntry };
  transactionStartTime: number;
  commitPending: boolean;
}

/**
 * The JSON patch operations State
 *
 * Consists of a map with a namespace as key,
 * and an array of JsonPatchOperationModel as values
 */
export interface JsonPatchOperationsState {
  [resourceType: string]: JsonPatchOperationsResourceEntry;
}

const initialState: JsonPatchOperationsState = Object.create(null);

/**
 * The JSON-PATCH operations Reducer
 *
 * @param state
 *    the current state
 * @param action
 *    the action to perform on the state
 * @return JsonPatchOperationsState
 *    the new state
 */
export function jsonPatchOperationsReducer(state = initialState, action: PatchOperationsActions): JsonPatchOperationsState {
  switch (action.type) {

    case JsonPatchOperationsActionTypes.COMMIT_JSON_PATCH_OPERATIONS: {
      return commitOperations(state, action as CommitPatchOperationsAction);
    }

    case JsonPatchOperationsActionTypes.FLUSH_JSON_PATCH_OPERATIONS: {
      return flushOperation(state, action as FlushPatchOperationsAction);
    }

    case JsonPatchOperationsActionTypes.NEW_JSON_PATCH_ADD_OPERATION: {
      return newOperation(state, action as NewPatchAddOperationAction);
    }

    case JsonPatchOperationsActionTypes.NEW_JSON_PATCH_COPY_OPERATION: {
      return newOperation(state, action as NewPatchCopyOperationAction);
    }

    case JsonPatchOperationsActionTypes.NEW_JSON_PATCH_MOVE_OPERATION: {
      return newOperation(state, action as NewPatchMoveOperationAction);
    }

    case JsonPatchOperationsActionTypes.NEW_JSON_PATCH_REMOVE_OPERATION: {
      return newOperation(state, action as NewPatchRemoveOperationAction);
    }

    case JsonPatchOperationsActionTypes.NEW_JSON_PATCH_REPLACE_OPERATION: {
      return newOperation(state, action as NewPatchReplaceOperationAction);
    }

    case JsonPatchOperationsActionTypes.ROLLBACK_JSON_PATCH_OPERATIONS: {
      return rollbackOperations(state, action as RollbacktPatchOperationsAction);
    }

    case JsonPatchOperationsActionTypes.START_TRANSACTION_JSON_PATCH_OPERATIONS: {
      return startTransactionPatchOperations(state, action as StartTransactionPatchOperationsAction);
    }

    case JsonPatchOperationsActionTypes.DELETE_PENDING_JSON_PATCH_OPERATIONS: {
      return deletePendingOperations(state, action as DeletePendingJsonPatchOperationsAction);
    }

    default: {
      return state;
    }
  }
}

/**
 * Set the transaction start time.
 *
 * @param state
 *    the current state
 * @param action
 *    an StartTransactionPatchOperationsAction
 * @return JsonPatchOperationsState
 *    the new state.
 */
function startTransactionPatchOperations(state: JsonPatchOperationsState, action: StartTransactionPatchOperationsAction): JsonPatchOperationsState {
  if (hasValue(state[ action.payload.resourceType ])
    && isNull(state[ action.payload.resourceType ].transactionStartTime)) {
    return Object.assign({}, state, {
      [action.payload.resourceType]: Object.assign({}, state[ action.payload.resourceType ], {
        transactionStartTime: action.payload.startTime,
        commitPending: true
      })
    });
  } else {
    return state;
  }
}

/**
 * Set commit pending state.
 *
 * @param state
 *    the current state
 * @param action
 *    an CommitPatchOperationsAction
 * @return JsonPatchOperationsState
 *    the new state, with the section new validity status.
 */
function commitOperations(state: JsonPatchOperationsState, action: CommitPatchOperationsAction): JsonPatchOperationsState {
  if (hasValue(state[ action.payload.resourceType ])
    && state[ action.payload.resourceType ].commitPending) {
    return Object.assign({}, state, {
      [action.payload.resourceType]: Object.assign({}, state[ action.payload.resourceType ], {
        commitPending: false
      })
    });
  } else {
    return state;
  }
}

/**
 * Set commit pending state.
 *
 * @param state
 *    the current state
 * @param action
 *    an RollbacktPatchOperationsAction
 * @return JsonPatchOperationsState
 *    the new state.
 */
function rollbackOperations(state: JsonPatchOperationsState, action: RollbacktPatchOperationsAction): JsonPatchOperationsState {
  if (hasValue(state[ action.payload.resourceType ])
    && state[ action.payload.resourceType ].commitPending) {
    return Object.assign({}, state, {
      [action.payload.resourceType]: Object.assign({}, state[ action.payload.resourceType ], {
        transactionStartTime: null,
        commitPending: false
      })
    });
  } else {
    return state;
  }
}

/**
 * Set the JsonPatchOperationsState to its initial value.
 *
 * @param state
 *    the current state
 * @param action
 *    an DeletePendingJsonPatchOperationsAction
 * @return JsonPatchOperationsState
 *    the new state.
 */
function deletePendingOperations(state: JsonPatchOperationsState, action: DeletePendingJsonPatchOperationsAction): JsonPatchOperationsState {
  return null;
}

/**
 * Add new JSON patch operation list.
 *
 * @param state
 *    the current state
 * @param action
 *    an NewPatchAddOperationAction
 * @return JsonPatchOperationsState
 *    the new state, with the section new validity status.
 */
function newOperation(state: JsonPatchOperationsState, action): JsonPatchOperationsState {
  const newState = Object.assign({}, state);
  const body: any[] = hasValidBody(newState, action.payload.resourceType, action.payload.resourceId)
    ? newState[ action.payload.resourceType ].children[ action.payload.resourceId ].body : Array.of();
  const newBody = addOperationToList(
    body,
    action.type,
    action.payload.path,
    hasValue(action.payload.value) ? action.payload.value : null,
    hasValue(action.payload.from) ? action.payload.from : null);

  if (hasValue(newState[ action.payload.resourceType ])
    && hasValue(newState[ action.payload.resourceType ].children)) {
    return Object.assign({}, state, {
      [action.payload.resourceType]: Object.assign({}, state[ action.payload.resourceType ], {
        children: Object.assign({}, state[ action.payload.resourceType ].children, {
          [action.payload.resourceId]: {
            body: newBody,
          }
        }),
        commitPending: isNotUndefined(state[ action.payload.resourceType ].commitPending) ? state[ action.payload.resourceType ].commitPending : false
      })
    });
  } else {
    return Object.assign({}, state, {
      [action.payload.resourceType]: Object.assign({}, {
        children: {
          [action.payload.resourceId]: {
            body: newBody,
          }
        },
        transactionStartTime: null,
        commitPending: false
      })
    });
  }
}

/**
 * Check if state has a valid body.
 *
 * @param state
 *    the current state
 * @param resourceType
 *    an resource type
 * @param resourceId
 *    an resource ID
 * @return boolean
 */
function hasValidBody(state: JsonPatchOperationsState, resourceType: any, resourceId: any): boolean {
  return (hasValue(state[ resourceType ])
    && hasValue(state[ resourceType ].children)
    && hasValue(state[ resourceType ].children[ resourceId ])
    && isNotEmpty(state[ resourceType ].children[ resourceId ].body));
}

/**
 * Set the section validity.
 *
 * @param state
 *    the current state
 * @param action
 *    an FlushPatchOperationsAction
 * @return SubmissionObjectState
 *    the new state, with the section new validity status.
 */
function flushOperation(state: JsonPatchOperationsState, action: FlushPatchOperationsAction): JsonPatchOperationsState {
  if (hasValue(state[ action.payload.resourceType ])) {
    let newChildren;
    if (isNotUndefined(action.payload.resourceId)) {
      // flush only specified child's operations
      if (hasValue(state[ action.payload.resourceType ].children)
        && hasValue(state[ action.payload.resourceType ].children[ action.payload.resourceId ])) {
        newChildren = Object.assign({}, state[ action.payload.resourceType ].children, {
          [action.payload.resourceId]: {
            body: state[ action.payload.resourceType ].children[ action.payload.resourceId ].body
              .filter((entry) => entry.timeCompleted > state[ action.payload.resourceType ].transactionStartTime)
          }
        });
      } else {
        newChildren = state[ action.payload.resourceType ].children;
      }
    } else {
      // flush all children's operations
      newChildren = state[ action.payload.resourceType ].children;
      Object.keys(newChildren)
        .forEach((resourceId) => {
          newChildren = Object.assign({}, newChildren, {
            [resourceId]: {
              body: newChildren[ resourceId ].body
                .filter((entry) => entry.timeCompleted > state[ action.payload.resourceType ].transactionStartTime)
            }
          });
        });
    }
    return Object.assign({}, state, {
      [action.payload.resourceType]: Object.assign({}, state[ action.payload.resourceType ], {
        children: newChildren,
        transactionStartTime: null,
      })
    });
  } else {
    return state;
  }
}

/**
 * Add a new operation to a patch
 *
 * @param body
 *    The current patch
 * @param actionType
 *    The type of operation to add
 * @param targetPath
 *    The path for the operation
 * @param value
 *    The new value
 * @param fromPath
 *    The previous path (in case of a move operation)
 */
function addOperationToList(body: JsonPatchOperationObject[], actionType, targetPath, value?, fromPath?) {
  const newBody = Array.from(body);
  switch (actionType) {
    case JsonPatchOperationsActionTypes.NEW_JSON_PATCH_ADD_OPERATION:
      newBody.push(makeOperationEntry({
        op: JsonPatchOperationType.add,
        path: targetPath,
        value: value
      }));
      break;
    case JsonPatchOperationsActionTypes.NEW_JSON_PATCH_REPLACE_OPERATION:
      newBody.push(makeOperationEntry({
        op: JsonPatchOperationType.replace,
        path: targetPath,
        value: value
      }));
      break;
    case JsonPatchOperationsActionTypes.NEW_JSON_PATCH_REMOVE_OPERATION:
      newBody.push(makeOperationEntry({ op: JsonPatchOperationType.remove, path: targetPath }));
      break;
    case JsonPatchOperationsActionTypes.NEW_JSON_PATCH_MOVE_OPERATION:
      newBody.push(makeOperationEntry({ op: JsonPatchOperationType.move, from: fromPath, path: targetPath }));
      break;
  }
  return newBody;
}

function makeOperationEntry(operation) {
  return { operation: operation, timeCompleted: new Date().getTime() };
}
