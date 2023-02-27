import { ResourceType } from './resource-type';

/**
 * The resource type for TemplateItem.
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const ITEM_TEMPLATE = new ResourceType('itemtemplate');
