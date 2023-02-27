import { Component } from '@angular/core';
import { SubmissionEditComponent as BaseComponent } from '../../../../../app/submission/edit/submission-edit.component';

/**
 * This component allows to edit an existing workspaceitem/workflowitem.
 */
@Component({
  selector: 'ds-submission-edit',
  // styleUrls: ['./submission-edit.component.scss'],
  styleUrls: ['../../../../../app/submission/edit/submission-edit.component.scss'],
  // templateUrl: './submission-edit.component.html'
  templateUrl: '../../../../../app/submission/edit/submission-edit.component.html'
})
export class SubmissionEditComponent extends BaseComponent {
}
