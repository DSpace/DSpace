import { ResourceType } from '../../shared/resource-type';

/**
 * The resource type for WorkflowAction
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const WORKFLOW_ACTION = new ResourceType('workflowaction');
