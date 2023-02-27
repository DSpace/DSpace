import { Component, Input } from '@angular/core';
import { ComcolPageBrowseByComponent } from './comcol-page-browse-by.component';
import { ThemedComponent } from '../../theme-support/themed.component';

/**
 * Themed wrapper for ComcolPageBrowseByComponent
 */
@Component({
  selector: 'ds-themed-comcol-page-browse-by',
  styleUrls: [],
  templateUrl: '../../theme-support/themed.component.html',
})
export class ThemedComcolPageBrowseByComponent extends ThemedComponent<ComcolPageBrowseByComponent> {
  /**
   * The ID of the Community or Collection
   */
  @Input() id: string;
  @Input() contentType: string;

  inAndOutputNames: (keyof ComcolPageBrowseByComponent & keyof this)[] = ['id', 'contentType'];

  protected getComponentName(): string {
    return 'ComcolPageBrowseByComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../../themes/${themeName}/app/shared/comcol-page-browse-by/comcol-page-browse-by.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import('./comcol-page-browse-by.component');
  }
}
