/**
 * Model class for an Item Access Condition
 */
export class AccessesConditionOption {

    /**
     * The name for this Access Condition
     */
    name: string;

    /**
     * The groupName for this Access Condition
     */
    groupName: string;

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
    endDateLimit?: string;

    /**
     * Maximum value of the end date
     */
    startDateLimit?: string;

    /**
     * Maximum value of the start date
     */
    maxStartDate?: string;

    /**
     * Maximum value of the end date
     */
    maxEndDate?: string;
}
