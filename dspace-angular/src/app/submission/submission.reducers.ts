import { ActionReducerMap, createFeatureSelector } from '@ngrx/store';

import {
  submissionObjectReducer,
  SubmissionObjectState
} from './objects/submission-objects.reducer';

/**
 * The Submission State
 */
export interface SubmissionState {
  'objects': SubmissionObjectState;
}

export const submissionReducers: ActionReducerMap<SubmissionState> = {
  objects: submissionObjectReducer,
};

export const submissionSelector = createFeatureSelector<SubmissionState>('submission');
