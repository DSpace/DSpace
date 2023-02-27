import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { ForgotEmailComponent } from './forgot-email.component';

/**
 * Themed wrapper for ForgotEmailComponent
 */
@Component({
  selector: 'ds-themed-forgot-email',
  styleUrls: [],
  templateUrl: './../../shared/theme-support/themed.component.html'
})
export class ThemedForgotEmailComponent extends ThemedComponent<ForgotEmailComponent> {
  protected getComponentName(): string {
    return 'ForgotEmailComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/forgot-password/forgot-password-email/forgot-email.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./forgot-email.component`);
  }

}
