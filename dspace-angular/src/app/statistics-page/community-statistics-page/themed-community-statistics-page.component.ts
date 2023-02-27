import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { CommunityStatisticsPageComponent } from './community-statistics-page.component';

/**
 * Themed wrapper for CommunityStatisticsPageComponent
 */
@Component({
  selector: 'ds-themed-collection-statistics-page',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedCommunityStatisticsPageComponent extends ThemedComponent<CommunityStatisticsPageComponent> {
  protected getComponentName(): string {
    return 'CommunityStatisticsPageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/statistics-page/community-statistics-page/community-statistics-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./community-statistics-page.component`);
  }

}
