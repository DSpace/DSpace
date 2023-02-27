import { ResourceType } from './resource-type';

/**
 * The resource type for Bundle
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const BUNDLE = new ResourceType('bundle');
