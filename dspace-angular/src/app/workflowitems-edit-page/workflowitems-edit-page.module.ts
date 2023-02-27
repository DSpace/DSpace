import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { SharedModule } from '../shared/shared.module';
import { WorkflowItemsEditPageRoutingModule } from './workflowitems-edit-page-routing.module';
import { SubmissionModule } from '../submission/submission.module';
import { WorkflowItemDeleteComponent } from './workflow-item-delete/workflow-item-delete.component';
import { WorkflowItemSendBackComponent } from './workflow-item-send-back/workflow-item-send-back.component';
import { ThemedWorkflowItemDeleteComponent } from './workflow-item-delete/themed-workflow-item-delete.component';
import {
  ThemedWorkflowItemSendBackComponent
} from './workflow-item-send-back/themed-workflow-item-send-back.component';
import { StatisticsModule } from '../statistics/statistics.module';
import { ItemPageModule } from '../item-page/item-page.module';
import {
  AdvancedWorkflowActionsLoaderComponent
} from './advanced-workflow-action/advanced-workflow-actions-loader/advanced-workflow-actions-loader.component';
import {
  AdvancedWorkflowActionRatingComponent
} from './advanced-workflow-action/advanced-workflow-action-rating/advanced-workflow-action-rating.component';
import {
  AdvancedWorkflowActionSelectReviewerComponent
} from './advanced-workflow-action/advanced-workflow-action-select-reviewer/advanced-workflow-action-select-reviewer.component';
import {
  AdvancedWorkflowActionPageComponent
} from './advanced-workflow-action/advanced-workflow-action-page/advanced-workflow-action-page.component';
import {
  AdvancedWorkflowActionsDirective
} from './advanced-workflow-action/advanced-workflow-actions-loader/advanced-workflow-actions.directive';
import { AccessControlModule } from '../access-control/access-control.module';
import {
  ReviewersListComponent
} from './advanced-workflow-action/advanced-workflow-action-select-reviewer/reviewers-list/reviewers-list.component';
import { FormModule } from '../shared/form/form.module';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

@NgModule({
  imports: [
    WorkflowItemsEditPageRoutingModule,
    CommonModule,
    SharedModule,
    SubmissionModule,
    StatisticsModule,
    ItemPageModule,
    AccessControlModule,
    FormModule,
    NgbModule,
  ],
  declarations: [
    WorkflowItemDeleteComponent,
    ThemedWorkflowItemDeleteComponent,
    WorkflowItemSendBackComponent,
    ThemedWorkflowItemSendBackComponent,
    AdvancedWorkflowActionsLoaderComponent,
    AdvancedWorkflowActionRatingComponent,
    AdvancedWorkflowActionSelectReviewerComponent,
    AdvancedWorkflowActionPageComponent,
    AdvancedWorkflowActionsDirective,
    ReviewersListComponent,
  ]
})
/**
 * This module handles all modules that need to access the workflowitems edit page.
 */
export class WorkflowItemsEditPageModule {

}
