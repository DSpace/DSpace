import { ResourceType } from './resource-type';

/**
 * The resource type for VersionHistory
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const VERSION_HISTORY = new ResourceType('versionhistory');
