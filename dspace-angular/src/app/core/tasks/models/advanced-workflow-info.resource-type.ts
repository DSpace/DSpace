import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for {@link RatingAdvancedWorkflowInfo}
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const RATING_ADVANCED_WORKFLOW_INFO = new ResourceType('ratingrevieweraction');

/**
 * The resource type for {@link SelectReviewerAdvancedWorkflowInfo}
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const SELECT_REVIEWER_ADVANCED_WORKFLOW_INFO = new ResourceType('selectrevieweraction');
