import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { SharedModule } from '../shared/shared.module';
import { WorkspaceitemsEditPageRoutingModule } from './workspaceitems-edit-page-routing.module';
import { SubmissionModule } from '../submission/submission.module';

@NgModule({
  imports: [
    WorkspaceitemsEditPageRoutingModule,
    CommonModule,
    SharedModule,
    SubmissionModule,
  ],
  declarations: []
})
/**
 * This module handles all modules that need to access the workspaceitems edit page.
 */
export class WorkspaceitemsEditPageModule {

}
