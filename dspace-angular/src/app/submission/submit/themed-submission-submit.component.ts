import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { SubmissionSubmitComponent } from './submission-submit.component';

/**
 * Themed wrapper for SubmissionSubmitComponent
 */
@Component({
  selector: 'ds-themed-submission-submit',
  styleUrls: [],
  templateUrl: './../../shared/theme-support/themed.component.html'
})
export class ThemedSubmissionSubmitComponent extends ThemedComponent<SubmissionSubmitComponent> {
  protected getComponentName(): string {
    return 'SubmissionSubmitComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/submission/submit/submission-submit.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./submission-submit.component`);
  }
}
