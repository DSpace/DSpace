/**
 * Represents all JSON Patch operations type.
 */
export enum JsonPatchOperationType {
  test = 'test',
  remove = 'remove',
  add = 'add',
  replace = 'replace',
  move = 'move',
  copy = 'copy',
}

/**
 * Represents a JSON Patch operations.
 */
export class JsonPatchOperationModel {
  op: JsonPatchOperationType;
  path: string;
  value: any;
}
