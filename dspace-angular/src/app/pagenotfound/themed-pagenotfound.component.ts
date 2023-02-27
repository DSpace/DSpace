import { Component } from '@angular/core';
import { ThemedComponent } from '../shared/theme-support/themed.component';
import { PageNotFoundComponent } from './pagenotfound.component';

/**
 * Themed wrapper for PageNotFoundComponent
 */
@Component({
  selector: 'ds-themed-pagenotfound',
  styleUrls: [],
  templateUrl: '../shared/theme-support/themed.component.html',
})
export class ThemedPageNotFoundComponent extends ThemedComponent<PageNotFoundComponent> {

  protected getComponentName(): string {
    return 'PageNotFoundComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/pagenotfound/pagenotfound.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./pagenotfound.component`);
  }
}
