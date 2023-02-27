import { Component } from '@angular/core';
import { ThemedComponent } from '../shared/theme-support/themed.component';
import { BreadcrumbsComponent } from './breadcrumbs.component';

/**
 * Themed wrapper for BreadcrumbsComponent
 */
@Component({
  selector: 'ds-themed-breadcrumbs',
  styleUrls: [],
  templateUrl: '../shared/theme-support/themed.component.html',
})
export class ThemedBreadcrumbsComponent extends ThemedComponent<BreadcrumbsComponent> {
  protected getComponentName(): string {
    return 'BreadcrumbsComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/breadcrumbs/breadcrumbs.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./breadcrumbs.component`);
  }
}
