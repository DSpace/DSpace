import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for SupervisionOrder
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const SUPERVISION_ORDER = new ResourceType('supervisionorder');
