import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { AdminSidebarComponent } from './admin-sidebar.component';

/**
 * Themed wrapper for AdminSidebarComponent
 */
@Component({
  selector: 'ds-themed-admin-sidebar',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedAdminSidebarComponent extends ThemedComponent<AdminSidebarComponent> {
  protected getComponentName(): string {
    return 'AdminSidebarComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/admin/admin-sidebar/admin-sidebar.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import('./admin-sidebar.component');
  }
}
