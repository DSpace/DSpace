import { ResourceType } from './resource-type';

/**
 * The resource type for Version
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const VERSION = new ResourceType('version');
