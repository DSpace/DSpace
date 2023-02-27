import { Component } from '@angular/core';
import { WorkflowItemDeleteComponent as BaseComponent } from '../../../../../app/workflowitems-edit-page/workflow-item-delete/workflow-item-delete.component';

@Component({
  selector: 'ds-workflow-item-delete',
  // styleUrls: ['workflow-item-delete.component.scss'],
  // templateUrl: './workflow-item-delete.component.html'
  templateUrl: '../../../../../app/workflowitems-edit-page/workflow-item-action-page.component.html'
})
/**
 * Component representing a page to delete a workflow item
 */
export class WorkflowItemDeleteComponent extends BaseComponent {
}
