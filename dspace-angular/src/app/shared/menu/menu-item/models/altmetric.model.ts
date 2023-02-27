import { MenuItemModel } from './menu-item.model';
import { MenuItemType } from '../../menu-item-type.model';

/**
 * Model representing an Altmetric Menu Section
 */
export class AltmetricMenuItemModel implements MenuItemModel {
  type = MenuItemType.ALTMETRIC;
  disabled: boolean;
  url: string;
}
