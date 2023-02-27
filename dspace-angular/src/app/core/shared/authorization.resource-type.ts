import { ResourceType } from './resource-type';

/**
 * The resource type for Authorization
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const AUTHORIZATION = new ResourceType('authorization');
