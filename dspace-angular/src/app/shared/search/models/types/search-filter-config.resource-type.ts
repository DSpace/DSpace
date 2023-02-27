import { ResourceType } from '../../../../core/shared/resource-type';

/**
 * The resource type for SearchFilterConfig
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const SEARCH_FILTER_CONFIG = new ResourceType('discovery-filter');
