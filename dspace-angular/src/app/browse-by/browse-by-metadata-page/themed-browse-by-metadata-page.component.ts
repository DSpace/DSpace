import {Component} from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { BrowseByMetadataPageComponent } from './browse-by-metadata-page.component';
import {BrowseByDataType, rendersBrowseBy} from '../browse-by-switcher/browse-by-decorator';

/**
 * Themed wrapper for BrowseByMetadataPageComponent
 **/
@Component({
  selector: 'ds-themed-browse-by-metadata-page',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})

@rendersBrowseBy(BrowseByDataType.Metadata)
export class ThemedBrowseByMetadataPageComponent
  extends ThemedComponent<BrowseByMetadataPageComponent> {
  protected getComponentName(): string {
    return 'BrowseByMetadataPageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/browse-by/browse-by-metadata-page/browse-by-metadata-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./browse-by-metadata-page.component`);
  }
}
