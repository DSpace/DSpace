import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for EPerson
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */

export const EPERSON = new ResourceType('eperson');
