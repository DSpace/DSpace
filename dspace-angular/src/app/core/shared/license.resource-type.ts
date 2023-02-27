import { ResourceType } from './resource-type';

/**
 * The resource type for License
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const LICENSE = new ResourceType('license');
