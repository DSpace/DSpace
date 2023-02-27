import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { CollectionStatisticsPageComponent } from './collection-statistics-page.component';

/**
 * Themed wrapper for CollectionStatisticsPageComponent
 */
@Component({
  selector: 'ds-themed-collection-statistics-page',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedCollectionStatisticsPageComponent extends ThemedComponent<CollectionStatisticsPageComponent> {
  protected getComponentName(): string {
    return 'CollectionStatisticsPageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/statistics-page/collection-statistics-page/collection-statistics-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./collection-statistics-page.component`);
  }

}
