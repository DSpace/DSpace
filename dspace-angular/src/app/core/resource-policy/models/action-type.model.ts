/**
 * Enum representing the Action Type of a Resource Policy
 */
export enum ActionType {
  /**
   * Action of reading, viewing or downloading something
   */
  READ = 'READ',

  /**
   * Action of modifying something
   */
  WRITE = 'WRITE',

  /**
   * Action of deleting something
   */
  DELETE = 'DELETE',

  /**
   * Action of adding something to a container
   */
  ADD = 'ADD',

  /**
   * Action of removing something from a container
   */
  REMOVE = 'REMOVE',

  /**
   * Action of performing workflow step 1
   */
  WORKFLOW_STEP_1 = 5,

  /**
   * Action of performing workflow step 2
   */
  WORKFLOW_STEP_2 = 6,

  /**
   *  Action of performing workflow step 3
   */
  WORKFLOW_STEP_3 = 7,

  /**
   *  Action of performing a workflow abort
   */
  WORKFLOW_ABORT = 8,

  /**
   * Default Read policies for Bitstreams submitted to container
   */
  DEFAULT_BITSTREAM_READ = 'DEFAULT_BITSTREAM_READ',

  /**
   *  Default Read policies for Items submitted to container
   */
  DEFAULT_ITEM_READ = 'DEFAULT_ITEM_READ',

  /**
   * Administrative actions
   */
  ADMIN = 'ADMIN',

  /**
   * Action of withdrawn reading
   */
  WITHDRAWN_READ = 'WITHDRAWN_READ'
}
