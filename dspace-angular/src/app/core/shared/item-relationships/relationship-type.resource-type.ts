import { ResourceType } from '../resource-type';

/**
 * The resource type for RelationshipType
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */

export const RELATIONSHIP_TYPE = new ResourceType('relationshiptype');
