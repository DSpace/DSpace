import { CollectionDropdownComponent, CollectionListEntry } from './collection-dropdown.component';
import { ThemedComponent } from '../theme-support/themed.component';
import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'ds-themed-collection-dropdown',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedCollectionDropdownComponent extends ThemedComponent<CollectionDropdownComponent> {

  @Input() entityType: string;

  @Output() searchComplete = new EventEmitter<any>();

  @Output() theOnlySelectable = new EventEmitter<CollectionListEntry>();

  @Output() selectionChange = new EventEmitter<CollectionListEntry>();

  protected inAndOutputNames: (keyof CollectionDropdownComponent & keyof this)[] = ['entityType', 'searchComplete', 'theOnlySelectable', 'selectionChange'];

  protected getComponentName(): string {
    return 'CollectionDropdownComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/shared/collection-dropdown/collection-dropdown.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./collection-dropdown.component`);
  }
}
