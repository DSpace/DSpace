import { SubmissionItemAccessConditionObject } from './submission-item-access-condition.model';

/**
 * An interface to represent the submission's item accesses condition.
 */
export interface WorkspaceitemSectionAccessesObject {
  /**
   * The access condition id
   */
  id: string;

  /**
   * Boolean that indicates whether the current item must be findable via search or browse.
   */
  discoverable: boolean;

  /**
   * A list of available item access conditions
   */
  accessConditions: SubmissionItemAccessConditionObject[];
}
