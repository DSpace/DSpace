import { ConfigObject } from '../config/models/config.model';
import { SubmissionObject } from '../submission/models/submission-object.model';

/**
 * Defines a type for submission request responses.
 */
export type SubmitDataResponseDefinitionObject
  = (SubmissionObject | ConfigObject | string)[];
