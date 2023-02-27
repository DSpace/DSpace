import { ResourceType } from './resource-type';

/**
 * The resource type for Site
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const SITE = new ResourceType('site');
