import { SubmissionSectionError } from './submission-section-error.model';

/**
 * An interface to represent section error
 */
export interface SubmissionError {
  [submissionId: string]: SubmissionSectionError[];
}
