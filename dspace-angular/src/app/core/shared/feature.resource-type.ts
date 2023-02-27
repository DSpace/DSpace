import { ResourceType } from './resource-type';

/**
 * The resource type for Feature
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const FEATURE = new ResourceType('feature');
