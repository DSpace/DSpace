import { WorkflowItemDeleteComponent } from './workflow-item-delete.component';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { Component } from '@angular/core';

/**
 * Themed wrapper for WorkflowItemDeleteComponent
 */

@Component({
  selector: 'ds-themed-workflow-item-delete',
  styleUrls: [],
  templateUrl: './../../shared/theme-support/themed.component.html'
})
export class ThemedWorkflowItemDeleteComponent extends ThemedComponent<WorkflowItemDeleteComponent> {
  protected getComponentName(): string {
    return 'WorkflowItemDeleteComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/workflowitems-edit-page/workflow-item-delete/workflow-item-delete.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./workflow-item-delete.component`);
  }
}
