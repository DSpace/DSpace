import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { Component } from '@angular/core';
import { WorkflowItemSendBackComponent } from './workflow-item-send-back.component';

/**
 * Themed wrapper for WorkflowItemActionPageComponent
 */

@Component({
  selector: 'ds-themed-workflow-item-send-back',
  styleUrls: [],
  templateUrl: './../../shared/theme-support/themed.component.html'
})
export class ThemedWorkflowItemSendBackComponent extends ThemedComponent<WorkflowItemSendBackComponent> {
  protected getComponentName(): string {
    return 'WorkflowItemSendBackComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/workflowitems-edit-page/workflow-item-send-back/workflow-item-send-back.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./workflow-item-send-back.component`);
  }
}
