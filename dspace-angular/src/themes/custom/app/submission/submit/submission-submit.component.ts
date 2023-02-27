import { Component } from '@angular/core';
import { SubmissionSubmitComponent as BaseComponent } from '../../../../../app/submission/submit/submission-submit.component';

/**
 * This component allows to submit a new workspaceitem.
 */
@Component({
  selector: 'ds-submission-submit',
  // styleUrls: ['./submission-submit.component.scss'],
  styleUrls: ['../../../../../app/submission/submit/submission-submit.component.scss'],
  // templateUrl: './submission-submit.component.html'
  templateUrl: '../../../../../app/submission/submit/submission-submit.component.html'
})
export class SubmissionSubmitComponent extends BaseComponent {
}
