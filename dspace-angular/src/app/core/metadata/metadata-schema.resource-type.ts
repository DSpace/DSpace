import { ResourceType } from '../shared/resource-type';

/**
 * The resource type for MetadataSchema
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */

export const METADATA_SCHEMA = new ResourceType('metadataschema');
