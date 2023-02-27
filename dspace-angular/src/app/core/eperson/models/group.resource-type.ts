import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for Group
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */

export const GROUP = new ResourceType('group');
