/**
 * Model class for an Access Condition
 */
export class AccessConditionOption {

  /**
   * The name for this Access Condition
   */
  name: string;

  /**
   * A boolean representing if this Access Condition has a start date
   */
  hasStartDate: boolean;

  /**
   * A boolean representing if this Access Condition has an end date
   */
  hasEndDate: boolean;

  /**
   * Maximum value of the start date
   */
  maxStartDate: string;

  /**
   * Maximum value of the end date
   */
  maxEndDate: string;
}
