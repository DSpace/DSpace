import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for OrcidQueue
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const ORCID_QUEUE = new ResourceType('orcidqueue');
