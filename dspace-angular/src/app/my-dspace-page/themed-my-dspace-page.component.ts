import { Component } from '@angular/core';
import { ThemedComponent } from '../shared/theme-support/themed.component';
import { MyDSpacePageComponent } from './my-dspace-page.component';

/**
 * Themed wrapper for MyDSpacePageComponent
 */
@Component({
  selector: 'ds-themed-my-dspace-page',
  styleUrls: [],
  templateUrl: './../shared/theme-support/themed.component.html'
})
export class ThemedMyDSpacePageComponent extends ThemedComponent<MyDSpacePageComponent> {
  protected inAndOutputNames: (keyof MyDSpacePageComponent & keyof this)[];

  protected getComponentName(): string {
    return 'MyDSpacePageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/my-dspace-page/my-dspace-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./my-dspace-page.component`);
  }
}
