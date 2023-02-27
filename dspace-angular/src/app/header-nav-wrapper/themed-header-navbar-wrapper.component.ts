import { Component } from '@angular/core';
import { ThemedComponent } from '../shared/theme-support/themed.component';
import { HeaderNavbarWrapperComponent } from './header-navbar-wrapper.component';

/**
 * Themed wrapper for BreadcrumbsComponent
 */
@Component({
  selector: 'ds-themed-header-navbar-wrapper',
  styleUrls: ['./themed-header-navbar-wrapper.component.scss'],
  templateUrl: '../shared/theme-support/themed.component.html',
})
export class ThemedHeaderNavbarWrapperComponent extends ThemedComponent<HeaderNavbarWrapperComponent> {
  protected getComponentName(): string {
    return 'HeaderNavbarWrapperComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/header-nav-wrapper/header-navbar-wrapper.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./header-navbar-wrapper.component`);
  }
}
