import { ResourceType } from '../../../../core/shared/resource-type';

/**
 * The resource type for SearchResult
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const SEARCH_RESULT = new ResourceType('searchresult');
