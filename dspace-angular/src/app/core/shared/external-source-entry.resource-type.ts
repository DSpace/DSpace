import { ResourceType } from './resource-type';

/**
 * The resource type for ResourceType
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const EXTERNAL_SOURCE_ENTRY = new ResourceType('externalSourceEntry');
