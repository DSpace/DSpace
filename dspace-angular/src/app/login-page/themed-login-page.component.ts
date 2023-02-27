import { Component } from '@angular/core';
import { ThemedComponent } from '../shared/theme-support/themed.component';
import { LoginPageComponent } from './login-page.component';

/**
 * Themed wrapper for LoginPageComponent
 */
@Component({
  selector: 'ds-themed-login-page',
  styleUrls: [],
  templateUrl: './../shared/theme-support/themed.component.html'
})
export class ThemedLoginPageComponent extends ThemedComponent<LoginPageComponent> {
  protected getComponentName(): string {
    return 'LoginPageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/login-page/login-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./login-page.component`);
  }
}
