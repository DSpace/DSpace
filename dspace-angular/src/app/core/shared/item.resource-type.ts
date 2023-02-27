import { ResourceType } from './resource-type';

/**
 * The resource type for Item.
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const ITEM = new ResourceType('item');
