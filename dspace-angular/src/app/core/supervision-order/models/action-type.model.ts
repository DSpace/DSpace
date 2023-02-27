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
   * None Type of Supervision Order
   */
  NONE = 'NONE',

  /**
   * Editor Type of Supervision Order
   */
  EDITOR = 'EDITOR',

  /**
   * Observer Type of Supervision Order
   */
  OBSERVER = 'OBSERVER',
}
