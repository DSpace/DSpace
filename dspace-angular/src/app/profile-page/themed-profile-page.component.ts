import { Component } from '@angular/core';
import { ThemedComponent } from '../shared/theme-support/themed.component';
import { ProfilePageComponent } from './profile-page.component';

/**
 * Themed wrapper for ProfilePageComponent
 */
@Component({
  selector: 'ds-themed-profile-page',
  styleUrls: [],
  templateUrl: './../shared/theme-support/themed.component.html'
})
export class ThemedProfilePageComponent extends ThemedComponent<ProfilePageComponent> {
  protected getComponentName(): string {
    return 'ProfilePageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/profile-page/profile-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./profile-page.component`);
  }
}
