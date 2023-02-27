import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { ItemStatisticsPageComponent } from './item-statistics-page.component';

/**
 * Themed wrapper for ItemStatisticsPageComponent
 */
@Component({
  selector: 'ds-themed-item-statistics-page',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedItemStatisticsPageComponent extends ThemedComponent<ItemStatisticsPageComponent> {
  protected getComponentName(): string {
    return 'ItemStatisticsPageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/statistics-page/item-statistics-page/item-statistics-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./item-statistics-page.component`);
  }

}
