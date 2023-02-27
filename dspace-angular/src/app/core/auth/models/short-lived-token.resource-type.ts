import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for ShortLivedToken
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const SHORT_LIVED_TOKEN = new ResourceType('shortlivedtoken');
