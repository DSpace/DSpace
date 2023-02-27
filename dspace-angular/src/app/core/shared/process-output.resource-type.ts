import { ResourceType } from './resource-type';

/**
 * The resource type for ProcessOutput
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const PROCESS_OUTPUT_TYPE = new ResourceType('processOutput');
