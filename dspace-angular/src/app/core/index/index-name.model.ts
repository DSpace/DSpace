/**
 * An enum containing all index names
 */
export enum IndexName {
  // Contains all objects in the object cache indexed by UUID
  OBJECT = 'object/uuid-to-self-link',

  // contains all requests in the request cache indexed by UUID
  REQUEST = 'get-request/href-to-uuid',

  /**
   * Contains the alternative link for an objects
   * Maps these link on to their matching self link in the object cache
   * Eg. /workspaceitems/12/item --> /items/12345
   */
  ALTERNATIVE_OBJECT_LINK = 'object/alt-link-to-self-link'
}
