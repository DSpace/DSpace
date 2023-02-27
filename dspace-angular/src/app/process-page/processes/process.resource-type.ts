/**
 * The resource type for Process
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
import { ResourceType } from '../../core/shared/resource-type';

export const PROCESS = new ResourceType('process');
