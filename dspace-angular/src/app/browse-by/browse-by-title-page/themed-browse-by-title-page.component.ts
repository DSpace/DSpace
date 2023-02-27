import {Component} from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { BrowseByTitlePageComponent } from './browse-by-title-page.component';
import {BrowseByDataType, rendersBrowseBy} from '../browse-by-switcher/browse-by-decorator';

/**
 * Themed wrapper for BrowseByTitlePageComponent
 */
@Component({
  selector: 'ds-themed-browse-by-title-page',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})

@rendersBrowseBy(BrowseByDataType.Title)
export class ThemedBrowseByTitlePageComponent
  extends ThemedComponent<BrowseByTitlePageComponent> {
  protected getComponentName(): string {
    return 'BrowseByTitlePageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/browse-by/browse-by-title-page/browse-by-title-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./browse-by-title-page.component`);
  }
}
