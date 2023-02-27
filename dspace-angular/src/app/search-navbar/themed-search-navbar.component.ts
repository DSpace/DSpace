import { ThemedComponent } from '../shared/theme-support/themed.component';
import { SearchNavbarComponent } from './search-navbar.component';
import { Component } from '@angular/core';

@Component({
  selector: 'ds-themed-search-navbar',
  styleUrls: [],
  templateUrl: '../shared/theme-support/themed.component.html',
})
export class ThemedSearchNavbarComponent extends ThemedComponent<SearchNavbarComponent> {

  protected getComponentName(): string {
    return 'SearchNavbarComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/search-navbar/search-navbar.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./search-navbar.component`);
  }

}
