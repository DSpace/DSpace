import {ResourceType} from '../../resource-type';

/**
 * The resource type for SearchConfig
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const SEARCH_CONFIG = new ResourceType('discover');
