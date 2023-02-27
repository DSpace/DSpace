/**
 * Themed wrapper for SubmissionEditComponent
 */
import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { SubmissionEditComponent } from './submission-edit.component';

@Component({
  selector: 'ds-themed-submission-edit',
  styleUrls: [],
  templateUrl: './../../shared/theme-support/themed.component.html'
})
export class ThemedSubmissionEditComponent extends ThemedComponent<SubmissionEditComponent> {
  protected getComponentName(): string {
    return 'SubmissionEditComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/submission/edit/submission-edit.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./submission-edit.component`);
  }
}
