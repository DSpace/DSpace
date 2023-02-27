import { ResourceType } from './resource-type';

/**
 * The resource type for DSpaceObject
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const DSPACE_OBJECT = new ResourceType('dspaceobject');
