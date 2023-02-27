import { ResourceType } from './resource-type';

/**
 * The resource type for BrowseEntry
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const BROWSE_ENTRY = new ResourceType('browseEntry');
