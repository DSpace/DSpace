import { ResourceType } from '../shared/resource-type';

/**
 * The resource type for MetadataField
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */

export const METADATA_FIELD = new ResourceType('metadatafield');
