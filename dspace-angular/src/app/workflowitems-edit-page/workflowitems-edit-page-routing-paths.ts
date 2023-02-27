import { URLCombiner } from '../core/url-combiner/url-combiner';
import { getWorkflowItemModuleRoute } from '../app-routing-paths';

export function getWorkflowItemPageRoute(wfiId: string) {
  return new URLCombiner(getWorkflowItemModuleRoute(), wfiId).toString();
}

export function getWorkflowItemEditRoute(wfiId: string) {
  return new URLCombiner(getWorkflowItemModuleRoute(), wfiId, WORKFLOW_ITEM_EDIT_PATH).toString();
}
export function getWorkflowItemViewRoute(wfiId: string) {
  return new URLCombiner(getWorkflowItemModuleRoute(), wfiId, WORKFLOW_ITEM_VIEW_PATH).toString();
}

export function getWorkflowItemDeleteRoute(wfiId: string) {
  return new URLCombiner(getWorkflowItemModuleRoute(), wfiId, WORKFLOW_ITEM_DELETE_PATH).toString();
}

export function getWorkflowItemSendBackRoute(wfiId: string) {
  return new URLCombiner(getWorkflowItemModuleRoute(), wfiId, WORKFLOW_ITEM_SEND_BACK_PATH).toString();
}

export function getAdvancedWorkflowRoute(wfiId: string) {
  return new URLCombiner(getWorkflowItemModuleRoute(), wfiId, ADVANCED_WORKFLOW_PATH).toString();
}

export const WORKFLOW_ITEM_EDIT_PATH = 'edit';
export const WORKFLOW_ITEM_DELETE_PATH = 'delete';
export const WORKFLOW_ITEM_VIEW_PATH = 'view';
export const WORKFLOW_ITEM_SEND_BACK_PATH = 'sendback';
export const ADVANCED_WORKFLOW_PATH = 'advanced';
