import { MenuItemType } from '../../menu-item-type.model';

/**
 * Interface for models representing a Menu Section
 */
export interface MenuItemModel {
  type: MenuItemType;
  disabled: boolean;
}
