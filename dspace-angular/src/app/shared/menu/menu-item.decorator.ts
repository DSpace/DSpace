import { MenuItemType } from './menu-item-type.model';

const menuMenuItemComponentMap = new Map();

/**
 * Decorator function to link a MenuItemType to a Component
 * @param {MenuItemType} type The MenuItemType of the MenuSection's model
 * @returns {(sectionComponent: GenericContructor) => void}
 */
export function rendersMenuItemForType(type: MenuItemType) {
  return function decorator(sectionComponent: any) {
    if (!sectionComponent) {
      return;
    }
    menuMenuItemComponentMap.set(type, sectionComponent);
  };
}

/**
 * Retrieves the Component matching a given MenuItemType
 * @param {MenuItemType} type The given MenuItemType
 * @returns {GenericConstructor} The constructor of the Component that matches the MenuItemType
 */
export function getComponentForMenuItemType(type: MenuItemType) {
  return menuMenuItemComponentMap.get(type);
}
