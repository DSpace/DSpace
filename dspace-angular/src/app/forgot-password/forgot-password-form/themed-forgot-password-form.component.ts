import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { ForgotPasswordFormComponent } from './forgot-password-form.component';

/**
 * Themed wrapper for ForgotPasswordFormComponent
 */
@Component({
  selector: 'ds-themed-forgot-password-form',
  styleUrls: [],
  templateUrl: './../../shared/theme-support/themed.component.html'
})
export class ThemedForgotPasswordFormComponent extends ThemedComponent<ForgotPasswordFormComponent> {
  protected getComponentName(): string {
    return 'ForgotPasswordFormComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/forgot-password/forgot-password-form/forgot-password-form.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./forgot-password-form.component`);
  }
}
