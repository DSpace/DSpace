import { MenuItemModel } from './menu-item.model';
import { MenuItemType } from '../../menu-item-type.model';

/**
 * Model representing an OnClick Menu Section
 */
export class OnClickMenuItemModel implements MenuItemModel {
  type = MenuItemType.ONCLICK;
  disabled: boolean;
  text: string;
  function: () => {};
}
