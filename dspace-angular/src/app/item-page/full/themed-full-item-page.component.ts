import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { FullItemPageComponent } from './full-item-page.component';

/**
 * Themed wrapper for FullItemPageComponent
 */
@Component({
  selector: 'ds-themed-full-item-page',
  styleUrls: [],
  templateUrl: './../../shared/theme-support/themed.component.html',
})
export class ThemedFullItemPageComponent extends ThemedComponent<FullItemPageComponent> {
  protected getComponentName(): string {
    return 'FullItemPageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/item-page/full/full-item-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./full-item-page.component`);
  }
}
