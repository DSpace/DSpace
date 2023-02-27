/**
 * Enum representing the Policy Type of a Resource Policy
 */
export enum PolicyType {
  /**
   * A policy in place during the submission
   */
  TYPE_SUBMISSION = 'TYPE_SUBMISSION',

  /**
   * A policy in place during the approval workflow
   */
  TYPE_WORKFLOW = 'TYPE_WORKFLOW',

  /**
   * A policy that has been inherited from a container (the collection)
   */
  TYPE_INHERITED = 'TYPE_INHERITED',

  /**
   * A policy defined by the user during the submission or workflow phase
   */
  TYPE_CUSTOM = 'TYPE_CUSTOM',

}
