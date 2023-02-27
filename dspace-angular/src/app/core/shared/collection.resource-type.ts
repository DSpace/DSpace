import { ResourceType } from './resource-type';

/**
 * The resource type for Collection
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const COLLECTION = new ResourceType('collection');
