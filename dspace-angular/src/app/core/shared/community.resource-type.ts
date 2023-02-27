import { ResourceType } from './resource-type';

/**
 * The resource type for Community
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const COMMUNITY = new ResourceType('community');
