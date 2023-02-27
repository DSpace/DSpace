import { Component } from '@angular/core';
import { ThemedComponent } from '../shared/theme-support/themed.component';
import { LogoutPageComponent } from './logout-page.component';

/**
 * Themed wrapper for LogoutPageComponent
 */
@Component({
  selector: 'ds-themed-logout-page',
  styleUrls: [],
  templateUrl: './../shared/theme-support/themed.component.html'
})
export class ThemedLogoutPageComponent extends ThemedComponent<LogoutPageComponent> {
  protected getComponentName(): string {
    return 'LogoutPageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/logout-page/logout-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./logout-page.component`);
  }
}
