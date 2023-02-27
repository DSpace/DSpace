import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for License
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const USAGE_REPORT = new ResourceType('usagereport');
