import { DEFAULT_THEME } from '../object-collection/shared/listable-object/listable-object.decorator';
import { MenuID } from './menu-id.model';
import { hasValue } from '../empty.util';

const menuComponentMap = new Map();

/**
 * Decorator function to render a MenuSection for a menu
 * @param {MenuID} menuID The ID of the Menu in which the section is rendered
 * @param {boolean} expandable True when the section should be expandable, false when if should not
 * @returns {(menuSectionWrapperComponent: GenericConstructor) => void}
 */
export function rendersSectionForMenu(menuID: MenuID, expandable: boolean, theme = DEFAULT_THEME) {
  return function decorator(menuSectionWrapperComponent: any) {
    if (!menuSectionWrapperComponent) {
      return;
    }
    if (!menuComponentMap.get(menuID)) {
      menuComponentMap.set(menuID, new Map());
    }
    if (!menuComponentMap.get(menuID).get(expandable)) {
        menuComponentMap.get(menuID).set(expandable, new Map());
    }
    menuComponentMap.get(menuID).get(expandable).set(theme, menuSectionWrapperComponent);
  };
}

/**
 * Retrieves the component matching the given MenuID and whether or not it should be expandable
 * @param {MenuID} menuID The ID of the Menu in which the section is rendered
 * @param {boolean} expandable True when the section should be expandable, false when if should not
 * @returns {GenericConstructor} The constructor of the matching Component
 */
export function getComponentForMenu(menuID: MenuID, expandable: boolean, theme: string) {
  const comp = menuComponentMap.get(menuID).get(expandable).get(theme);
  if (hasValue(comp)) {
    return comp;
  } else {
    return menuComponentMap.get(menuID).get(expandable).get(DEFAULT_THEME);
  }
}
