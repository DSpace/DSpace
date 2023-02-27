import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { RegisterEmailComponent } from './register-email.component';

/**
 * Themed wrapper for RegisterEmailComponent
 */
@Component({
  selector: 'ds-themed-register-email',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedRegisterEmailComponent extends ThemedComponent<RegisterEmailComponent> {
  protected getComponentName(): string {
    return 'RegisterEmailComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/register-page/register-email/register-email.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import('./register-email.component');
  }
}
