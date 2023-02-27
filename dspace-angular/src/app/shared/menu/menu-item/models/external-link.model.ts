import { MenuItemModel } from './menu-item.model';
import { MenuItemType } from '../../menu-item-type.model';

/**
 * Model representing a Link Menu Section for an external link
 */
export class ExternalLinkMenuItemModel implements MenuItemModel {
  type = MenuItemType.EXTERNAL;
  disabled: boolean;
  text: string;
  href: string;
}
