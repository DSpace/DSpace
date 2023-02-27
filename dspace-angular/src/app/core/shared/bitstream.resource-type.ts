import { ResourceType } from './resource-type';

/**
 * The resource type for Bitstream
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const BITSTREAM = new ResourceType('bitstream');
