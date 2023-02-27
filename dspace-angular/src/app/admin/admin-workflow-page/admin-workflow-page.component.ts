import { Component } from '@angular/core';
import { Context } from '../../core/shared/context.model';

@Component({
  selector: 'ds-admin-workflow-page',
  templateUrl: './admin-workflow-page.component.html',
  styleUrls: ['./admin-workflow-page.component.scss']
})

/**
 * Component that represents a workflow item search page for administrators
 */
export class AdminWorkflowPageComponent {
  /**
   * The context of this page
   */
  context: Context = Context.AdminWorkflowSearch;
}
