import { Component, Inject } from '@angular/core';
import { TextMenuItemModel } from './models/text.model';
import { rendersMenuItemForType } from '../menu-item.decorator';
import { MenuItemType } from '../menu-item-type.model';

/**
 * Component that renders a menu section of type TEXT
 */
@Component({
  selector: 'ds-text-menu-item',
  templateUrl: './text-menu-item.component.html',
})
@rendersMenuItemForType(MenuItemType.TEXT)
export class TextMenuItemComponent {
  item: TextMenuItemModel;
  constructor(@Inject('itemModelProvider') item: TextMenuItemModel) {
    this.item = item;
  }
}
