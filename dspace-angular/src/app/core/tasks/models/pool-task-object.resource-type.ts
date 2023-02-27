import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for PoolTask
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const POOL_TASK = new ResourceType('pooltask');
