/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';
import { type } from '../ngrx/type';
import { MenuSection } from './menu-section.model';
import { MenuID } from './menu-id.model';

/**
 * For each action type in an action group, make a simple
 * enum object for all of this group's action types.
 *
 * The 'type' utility function coerces strings into string
 * literal types and runs a simple check to guarantee all
 * action types in the application are unique.
 */
export const MenuActionTypes = {
  COLLAPSE_MENU: type('dspace/menu/COLLAPSE_MENU'),
  TOGGLE_MENU: type('dspace/menu/TOGGLE_MENU'),
  EXPAND_MENU: type('dspace/menu/EXPAND_MENU'),
  SHOW_MENU: type('dspace/menu/SHOW_MENU'),
  HIDE_MENU: type('dspace/menu/HIDE_MENU'),
  COLLAPSE_MENU_PREVIEW: type('dspace/menu/COLLAPSE_MENU_PREVIEW'),
  EXPAND_MENU_PREVIEW: type('dspace/menu/EXPAND_MENU_PREVIEW'),
  ADD_SECTION: type('dspace/menu-section/ADD_SECTION'),
  REMOVE_SECTION: type('dspace/menu-section/REMOVE_SECTION'),
  SHOW_SECTION: type('dspace/menu-section/SHOW_SECTION'),
  HIDE_SECTION: type('dspace/menu-section/HIDE_SECTION'),
  ACTIVATE_SECTION: type('dspace/menu-section/ACTIVATE_SECTION'),
  DEACTIVATE_SECTION: type('dspace/menu-section/DEACTIVATE_SECTION'),
  TOGGLE_ACTIVE_SECTION: type('dspace/menu-section/TOGGLE_ACTIVE_SECTION'),
};


// MENU STATE ACTIONS
/**
 * Action used to collapse a single menu
 */
export class CollapseMenuAction implements Action {
  type = MenuActionTypes.COLLAPSE_MENU;
  menuID: MenuID;

  constructor(menuID: MenuID) {
    this.menuID = menuID;
  }
}

/**
 * Action used to expand a single menu
 */
export class ExpandMenuAction implements Action {
  type = MenuActionTypes.EXPAND_MENU;
  menuID: MenuID;

  constructor(menuID: MenuID) {
    this.menuID = menuID;
  }
}

/**
 * Action used to collapse a single menu when it's expanded and expanded it when it's collapse
 */
export class ToggleMenuAction implements Action {
  type = MenuActionTypes.TOGGLE_MENU;
  menuID: MenuID;

  constructor(menuID: MenuID) {
    this.menuID = menuID;
  }
}

/**
 * Action used to show a single menu
 */
export class ShowMenuAction implements Action {
  type = MenuActionTypes.SHOW_MENU;
  menuID: MenuID;

  constructor(menuID: MenuID) {
    this.menuID = menuID;
  }
}

/**
 * Action used to hide a single menu
 */
export class HideMenuAction implements Action {
  type = MenuActionTypes.HIDE_MENU;
  menuID: MenuID;

  constructor(menuID: MenuID) {
    this.menuID = menuID;
  }
}

/**
 * Action used to collapse a single menu's preview
 */
export class CollapseMenuPreviewAction implements Action {
  type = MenuActionTypes.COLLAPSE_MENU_PREVIEW;
  menuID: MenuID;

  constructor(menuID: MenuID) {
    this.menuID = menuID;
  }
}

/**
 * Action used to expand a single menu's preview
 */
export class ExpandMenuPreviewAction implements Action {
  type = MenuActionTypes.EXPAND_MENU_PREVIEW;
  menuID: MenuID;

  constructor(menuID: MenuID) {
    this.menuID = menuID;
  }
}

// MENU SECTION ACTIONS
/**
 * Action used to perform state changes for a section of a certain menu
 */
export abstract class MenuSectionAction implements Action {
  type;
  menuID: MenuID;
  id: string;

  constructor(menuID: MenuID, id: string) {
    this.menuID = menuID;
    this.id = id;
  }
}

/**
 * Action used to add a section to a certain menu
 */
export class AddMenuSectionAction extends MenuSectionAction {
  type = MenuActionTypes.ADD_SECTION;
  section: MenuSection;

  constructor(menuID: MenuID, section: MenuSection) {
    super(menuID, section.id);
    this.section = section;
  }
}

/**
 * Action used to remove a section from a certain menu
 */
export class RemoveMenuSectionAction extends MenuSectionAction {
  type = MenuActionTypes.REMOVE_SECTION;

  constructor(menuID: MenuID, id: string) {
    super(menuID, id);

  }
}

/**
 * Action used to hide a section of a certain menu
 */
export class HideMenuSectionAction extends MenuSectionAction {
  type = MenuActionTypes.HIDE_SECTION;

  constructor(menuID: MenuID, id: string) {
    super(menuID, id);
  }
}

/**
 * Action used to show a section of a certain menu
 */
export class ShowMenuSectionAction extends MenuSectionAction {
  type = MenuActionTypes.SHOW_SECTION;

  constructor(menuID: MenuID, id: string) {
    super(menuID, id);
  }
}

/**
 * Action used to make a section of a certain menu active
 */
export class ActivateMenuSectionAction extends MenuSectionAction {
  type = MenuActionTypes.ACTIVATE_SECTION;

  constructor(menuID: MenuID, id: string) {
    super(menuID, id);
  }
}

/**
 * Action used to make a section of a certain menu inactive
 */
export class DeactivateMenuSectionAction extends MenuSectionAction {
  type = MenuActionTypes.DEACTIVATE_SECTION;

  constructor(menuID: MenuID, id: string) {
    super(menuID, id);
  }
}

/**
 * Action used to make an active section of a certain menu inactive or an inactive section of a certain menu active
 */
export class ToggleActiveMenuSectionAction extends MenuSectionAction {
  type = MenuActionTypes.TOGGLE_ACTIVE_SECTION;

  constructor(menuID: MenuID, id: string) {
    super(menuID, id);
  }
}

export type MenuAction =
  CollapseMenuAction
  | ExpandMenuAction
  | ToggleMenuAction
  | ShowMenuAction
  | HideMenuAction
  | AddMenuSectionAction
  | RemoveMenuSectionAction
  | ShowMenuSectionAction
  | HideMenuSectionAction
  | ActivateMenuSectionAction
  | DeactivateMenuSectionAction
  | ToggleActiveMenuSectionAction
  | CollapseMenuPreviewAction
  | ExpandMenuPreviewAction;
