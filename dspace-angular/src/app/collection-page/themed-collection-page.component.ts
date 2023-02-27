import { Component } from '@angular/core';
import { ThemedComponent } from '../shared/theme-support/themed.component';
import { CollectionPageComponent } from './collection-page.component';

/**
 * Themed wrapper for CollectionPageComponent
 */
@Component({
  selector: 'ds-themed-collection-page',
  styleUrls: [],
  templateUrl: '../shared/theme-support/themed.component.html',
})
export class ThemedCollectionPageComponent extends ThemedComponent<CollectionPageComponent> {
  protected getComponentName(): string {
    return 'CollectionPageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/collection-page/collection-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./collection-page.component`);
  }

}
