import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { HomeNewsComponent } from './home-news.component';

@Component({
  selector: 'ds-themed-home-news',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})

/**
 * Component to render the news section on the home page
 */
export class ThemedHomeNewsComponent extends ThemedComponent<HomeNewsComponent> {
  protected getComponentName(): string {
    return 'HomeNewsComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/home-page/home-news/home-news.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./home-news.component`);
  }

}
