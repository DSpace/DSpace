import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for TaskObject
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const TASK_OBJECT = new ResourceType('taskobject');
