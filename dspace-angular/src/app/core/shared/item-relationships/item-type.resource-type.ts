import { ResourceType } from '../resource-type';

/**
 * The resource type for ItemType
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const ITEM_TYPE = new ResourceType('entitytype');

/**
 * The unset entity type
 */
export const NONE_ENTITY_TYPE = 'none';
