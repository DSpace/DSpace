import { ThemedComponent } from '../shared/theme-support/themed.component';
import { HomePageComponent } from './home-page.component';
import { Component } from '@angular/core';

@Component({
  selector: 'ds-themed-home-page',
  styleUrls: [],
  templateUrl: '../shared/theme-support/themed.component.html',
})
export class ThemedHomePageComponent extends ThemedComponent<HomePageComponent> {
  protected inAndOutputNames: (keyof HomePageComponent & keyof this)[];


  protected getComponentName(): string {
    return 'HomePageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/home-page/home-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./home-page.component`);
  }

}
