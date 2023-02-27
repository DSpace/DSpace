import { MenuItemModel } from './menu-item.model';
import { MenuItemType } from '../../menu-item-type.model';
import { Params } from '@angular/router';

/**
 * Model representing an Link Menu Section
 */
export class LinkMenuItemModel implements MenuItemModel {
  type = MenuItemType.LINK;
  disabled: boolean;
  text: string;
  link: string;
  queryParams?: Params | null;
}
