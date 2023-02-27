import { ResourceType } from './resource-type';

/**
 * The resource type for ExternalSource
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const EXTERNAL_SOURCE = new ResourceType('externalsource');
