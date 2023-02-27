import { MenuItemModel } from './menu-item/models/menu-item.model';

/**
 * Represents the state of a single menu section in the store
 */
export class MenuSection {
  id: string;
  parentID?: string;
  visible: boolean;
  active: boolean;
  model: MenuItemModel;
  index?: number;
  icon?: string;
  shouldPersistOnRouteChange? = false;
}
