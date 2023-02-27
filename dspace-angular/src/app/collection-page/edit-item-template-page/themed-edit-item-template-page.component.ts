import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { EditItemTemplatePageComponent } from './edit-item-template-page.component';

@Component({
  selector: 'ds-themed-edit-item-template-page',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
/**
 * Component for editing the item template of a collection
 */
export class ThemedEditItemTemplatePageComponent extends ThemedComponent<EditItemTemplatePageComponent> {
  protected getComponentName(): string {
    return 'EditItemTemplatePageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/collection-page/edit-item-template-page/edit-item-template-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import('./edit-item-template-page.component');
  }
}
