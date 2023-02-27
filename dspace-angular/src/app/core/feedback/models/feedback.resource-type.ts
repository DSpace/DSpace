import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for Feedback
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const FEEDBACK = new ResourceType('feedback');
