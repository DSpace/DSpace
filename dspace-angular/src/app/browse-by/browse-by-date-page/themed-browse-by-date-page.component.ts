import {Component} from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { BrowseByDatePageComponent } from './browse-by-date-page.component';
import {BrowseByDataType, rendersBrowseBy} from '../browse-by-switcher/browse-by-decorator';

/**
 * Themed wrapper for BrowseByDatePageComponent
 * */
@Component({
  selector: 'ds-themed-browse-by-metadata-page',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})

@rendersBrowseBy(BrowseByDataType.Date)
export class ThemedBrowseByDatePageComponent
  extends ThemedComponent<BrowseByDatePageComponent> {
  protected getComponentName(): string {
    return 'BrowseByDatePageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/browse-by/browse-by-date-page/browse-by-date-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./browse-by-date-page.component`);
  }
}
