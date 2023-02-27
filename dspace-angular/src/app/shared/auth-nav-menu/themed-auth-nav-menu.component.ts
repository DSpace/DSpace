import { Component } from '@angular/core';
import { ThemedComponent } from '../theme-support/themed.component';
import { AuthNavMenuComponent } from './auth-nav-menu.component';

/**
 * Themed wrapper for {@link AuthNavMenuComponent}
 */
@Component({
  selector: 'ds-themed-auth-nav-menu',
  styleUrls: [],
  templateUrl: '../theme-support/themed.component.html',
})
export class ThemedAuthNavMenuComponent extends ThemedComponent<AuthNavMenuComponent> {
  protected getComponentName(): string {
    return 'AuthNavMenuComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/shared/auth-nav-menu/auth-nav-menu.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./auth-nav-menu.component`);
  }
}
