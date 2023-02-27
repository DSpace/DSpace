import { Component } from '@angular/core';
import { fadeIn } from '../../../../../app/shared/animations/fade';
import { SubmissionImportExternalComponent as BaseComponent } from '../../../../../app/submission/import-external/submission-import-external.component';

/**
 * This component allows to submit a new workspaceitem importing the data from an external source.
 */
@Component({
  selector: 'ds-submission-import-external',
  // styleUrls: ['./submission-import-external.component.scss'],
  styleUrls: ['../../../../../app/submission/import-external/submission-import-external.component.scss'],
  // templateUrl: './submission-import-external.component.html',
  templateUrl: '../../../../../app/submission/import-external/submission-import-external.component.html',
  animations: [fadeIn]
})
export class SubmissionImportExternalComponent extends BaseComponent {

}
