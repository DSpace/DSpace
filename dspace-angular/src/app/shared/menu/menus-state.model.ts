import { MenuState } from './menu-state.model';
import { MenuID } from './menu-id.model';

/**
 * Represents the state of all menus in the store
 */
export type MenusState = {
  [id in MenuID]: MenuState;
};
