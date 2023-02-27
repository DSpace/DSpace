import { Component } from '@angular/core';
import { CreateProfileComponent } from './create-profile.component';
import { ThemedComponent } from '../../shared/theme-support/themed.component';

/**
 * Themed wrapper for CreateProfileComponent
 */
@Component({
  selector: 'ds-themed-create-profile',
  styleUrls: [],
  templateUrl: './../../shared/theme-support/themed.component.html'
})
export class ThemedCreateProfileComponent extends ThemedComponent<CreateProfileComponent> {
  protected getComponentName(): string {
    return 'CreateProfileComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/register-page/create-profile/create-profile.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./create-profile.component`);
  }
}
