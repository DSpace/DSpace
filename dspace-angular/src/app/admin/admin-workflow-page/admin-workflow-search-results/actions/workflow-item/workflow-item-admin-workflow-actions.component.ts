import { Component, Input } from '@angular/core';

import { WorkflowItem } from '../../../../../core/submission/models/workflowitem.model';
import {
  getWorkflowItemDeleteRoute,
  getWorkflowItemSendBackRoute
} from '../../../../../workflowitems-edit-page/workflowitems-edit-page-routing-paths';

@Component({
  selector: 'ds-workflow-item-admin-workflow-actions-element',
  styleUrls: ['./workflow-item-admin-workflow-actions.component.scss'],
  templateUrl: './workflow-item-admin-workflow-actions.component.html'
})
/**
 * The component for displaying the actions for a list element for a workflow-item on the admin workflow search page
 */
export class WorkflowItemAdminWorkflowActionsComponent {

  /**
   * The workflow item to perform the actions on
   */
  @Input() public wfi: WorkflowItem;

  /**
   * Whether to use small buttons or not
   */
  @Input() public small: boolean;

  /**
   * Returns the path to the delete page of this workflow item
   */
  getDeleteRoute(): string {
    return getWorkflowItemDeleteRoute(this.wfi.id);
  }

  /**
   * Returns the path to the send back page of this workflow item
   */
  getSendBackRoute(): string {
    return getWorkflowItemSendBackRoute(this.wfi.id);
  }

}
