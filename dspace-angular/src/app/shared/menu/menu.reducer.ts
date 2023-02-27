import {
  ActivateMenuSectionAction,
  AddMenuSectionAction,
  DeactivateMenuSectionAction,
  HideMenuSectionAction,
  MenuAction,
  MenuActionTypes,
  MenuSectionAction,
  RemoveMenuSectionAction,
  ShowMenuSectionAction,
  ToggleActiveMenuSectionAction
} from './menu.actions';
import { initialMenusState} from './initial-menus-state';
import { hasValue } from '../empty.util';
import { MenusState } from './menus-state.model';
import { MenuState } from './menu-state.model';
import { MenuSectionIndex } from './menu-section-Index.model';
import { MenuSections } from './menu-sections.model';
import { MenuSection } from './menu-section.model';
import { MenuID } from './menu-id.model';

/**
 * Reducer that handles MenuActions to update the MenusState
 * @param {MenusState} state The initial MenusState
 * @param {MenuAction} action The Action to be performed on the state
 * @returns {MenusState} The new, reducer MenusState
 */
export function menusReducer(state: MenusState = initialMenusState, action: MenuAction): MenusState {
  const menuState: MenuState = state[action.menuID];
  switch (action.type) {
    case MenuActionTypes.COLLAPSE_MENU: {
      const newMenuState = Object.assign({}, menuState, { collapsed: true });
      return Object.assign({}, state, { [action.menuID]: newMenuState });
    }
    case MenuActionTypes.EXPAND_MENU: {
      const newMenuState = Object.assign({}, menuState, { collapsed: false });
      return Object.assign({}, state, { [action.menuID]: newMenuState });
    }
    case MenuActionTypes.COLLAPSE_MENU_PREVIEW: {
      const newMenuState = Object.assign({}, menuState, { previewCollapsed: true });
      return Object.assign({}, state, { [action.menuID]: newMenuState });
    }
    case MenuActionTypes.EXPAND_MENU_PREVIEW: {
      const newMenuState = Object.assign({}, menuState, { previewCollapsed: false });
      return Object.assign({}, state, { [action.menuID]: newMenuState });
    }
    case MenuActionTypes.TOGGLE_MENU: {
      const newMenuState = Object.assign({}, menuState, { collapsed: !menuState.collapsed });
      return Object.assign({}, state, { [action.menuID]: newMenuState });
    }
    case MenuActionTypes.SHOW_MENU: {
      const newMenuState = Object.assign({}, menuState, { visible: true });
      return Object.assign({}, state, { [action.menuID]: newMenuState });
    }
    case MenuActionTypes.HIDE_MENU: {
      const newMenuState = Object.assign({}, menuState, { visible: false });
      return Object.assign({}, state, { [action.menuID]: newMenuState });
    }
    case MenuActionTypes.ADD_SECTION: {
      return addSection(state, action as AddMenuSectionAction);
    }
    case MenuActionTypes.REMOVE_SECTION: {
      return removeSection(state, action as RemoveMenuSectionAction);
    }
    case MenuActionTypes.ACTIVATE_SECTION: {
      return activateSection(state, action as ActivateMenuSectionAction);
    }
    case MenuActionTypes.DEACTIVATE_SECTION: {
      return deactivateSection(state, action as DeactivateMenuSectionAction);
    }
    case MenuActionTypes.TOGGLE_ACTIVE_SECTION: {
      return toggleActiveSection(state, action as ToggleActiveMenuSectionAction);
    }
    case MenuActionTypes.HIDE_SECTION: {
      return hideSection(state, action as HideMenuSectionAction);
    }
    case MenuActionTypes.SHOW_SECTION: {
      return showSection(state, action as ShowMenuSectionAction);
    }

    default: {
      return state;
    }
  }
}

/**
 * Add a section the a certain menu
 * @param {MenusState} state The initial state
 * @param {AddMenuSectionAction} action Action containing the new section and the menu's ID
 * @returns {MenusState} The new reduced state
 */
function addSection(state: MenusState, action: AddMenuSectionAction) {
  // let newState = addToIndex(state, action.section, action.menuID);
  const newState = putSectionState(state, action, action.section);
  return reorderSections(newState, action);
}

/**
 * Reorder all sections based on their index field
 * @param {MenusState} state The initial state
 * @param {MenuSectionAction} action Action containing the menu ID of the menu that is to be reordered
 * @returns {MenusState} The new reduced state
 */
function reorderSections(state: MenusState, action: MenuSectionAction) {
  const menuState: MenuState = state[action.menuID];
  const newSectionState: MenuSections = {};
  const newSectionIndexState: MenuSectionIndex = {};

  Object.values(menuState.sections).sort((sectionA: MenuSection, sectionB: MenuSection) => {
    const indexA = sectionA.index || 0;
    const indexB = sectionB.index || 0;
    return indexA - indexB;
  }).forEach((section: MenuSection) => {
    newSectionState[section.id] = section;
    if (hasValue(section.parentID)) {
      const parentIndex = hasValue(newSectionIndexState[section.parentID]) ? newSectionIndexState[section.parentID] : [];
      newSectionIndexState[section.parentID] = [...parentIndex, section.id];
    }
  });
  const newMenuState = Object.assign({}, menuState, {
    sections: newSectionState,
    sectionToSubsectionIndex: newSectionIndexState
  });
  return Object.assign({}, state, { [action.menuID]: newMenuState });
}

/**
 * Remove a section from a certain menu
 * @param {MenusState} state The initial state
 * @param {RemoveMenuSectionAction} action Action containing the section ID and menu ID of the section that should be removed
 * @returns {MenusState} The new reduced state
 */
function removeSection(state: MenusState, action: RemoveMenuSectionAction) {
  const menuState: MenuState = state[action.menuID];
  const id = action.id;
  const newState = removeFromIndex(state, menuState.sections[action.id], action.menuID);
  const newMenuState = Object.assign({}, newState[action.menuID], {
    sections: Object.assign({}, newState[action.menuID].sections)
  });
  delete newMenuState.sections[id];
  return Object.assign({}, newState, { [action.menuID]: newMenuState });
}

/**
 * Remove a section from the index of a certain menu
 * @param {MenusState} state The initial state
 * @param {MenuSection} action The MenuSection of which the ID should be removed from the index
 * @param {MenuID} action The Menu ID to which the section belonged
 * @returns {MenusState} The new reduced state
 */
function removeFromIndex(state: MenusState, section: MenuSection, menuID: MenuID) {
  const sectionID = section.id;
  const parentID = section.parentID;
  if (hasValue(parentID)) {
    const menuState: MenuState = state[menuID];
    const index = menuState.sectionToSubsectionIndex;
    const parentIndex = hasValue(index[parentID]) ? index[parentID] : [];
    const newIndex = Object.assign({}, index, { [parentID]: parentIndex.filter((id) => id !== sectionID) });
    const newMenuState = Object.assign({}, menuState, { sectionToSubsectionIndex: newIndex });
    return Object.assign({}, state, { [menuID]: newMenuState });
  }
  return state;
}

/**
 * Hide a certain section
 * @param {MenusState} state The initial state
 * @param {HideMenuSectionAction} action Action containing data to identify the section to be updated
 * @returns {MenusState} The new reduced state
 */
function hideSection(state: MenusState, action: HideMenuSectionAction) {
  return updateSectionState(state, action, { visible: false });
}

/**
 * Show a certain section
 * @param {MenusState} state The initial state
 * @param {ShowMenuSectionAction} action Action containing data to identify the section to be updated
 * @returns {MenusState} The new reduced state
 */
function showSection(state: MenusState, action: ShowMenuSectionAction) {
  return updateSectionState(state, action, { visible: true });
}

/**
 * Deactivate a certain section
 * @param {MenusState} state The initial state
 * @param {DeactivateMenuSectionAction} action Action containing data to identify the section to be updated
 * @returns {MenusState} The new reduced state
 */
function deactivateSection(state: MenusState, action: DeactivateMenuSectionAction) {
  const sectionState: MenuSection = state[action.menuID].sections[action.id];
  if (hasValue(sectionState)) {
    return updateSectionState(state, action, { active: false });
  }
}

/**
 * Activate a certain section
 * @param {MenusState} state The initial state
 * @param {DeactivateMenuSectionAction} action Action containing data to identify the section to be updated
 * @returns {MenusState} The new reduced state
 */
function activateSection(state: MenusState, action: ActivateMenuSectionAction) {
  const sectionState: MenuSection = state[action.menuID].sections[action.id];
  if (hasValue(sectionState)) {
    return updateSectionState(state, action, { active: true });
  }
}

/**
 * Deactivate a certain section when it's currently active, activate a certain section when it's currently inactive
 * @param {MenusState} state The initial state
 * @param {DeactivateMenuSectionAction} action Action containing data to identify the section to be updated
 * @returns {MenusState} The new reduced state
 */
function toggleActiveSection(state: MenusState, action: ToggleActiveMenuSectionAction) {
  const sectionState: MenuSection = state[action.menuID].sections[action.id];
  if (hasValue(sectionState)) {
    return updateSectionState(state, action, { active: !sectionState.active });
  }
  return state;
}

/**
 * Add or replace a section in the state
 * @param {MenusState} state The initial state
 * @param {MenuAction} action The action which contains the menu ID of the menu of which the state is to be updated
 * @param {MenuSection} section The section that will be added or replaced in the state
 * @returns {MenusState} The new reduced state
 */
function putSectionState(state: MenusState, action: MenuAction, section: MenuSection): MenusState {
  const menuState: MenuState = state[action.menuID];
  const newSections = Object.assign({}, menuState.sections, {
    [section.id]: section
  });
  const newMenuState = Object.assign({}, menuState, {
    sections: newSections
  });
  return Object.assign({}, state, { [action.menuID]: newMenuState });
}

/**
 * Update a section
 * @param {MenusState} state The initial state
 * @param {MenuSectionAction} action The action containing the menu ID and section ID
 * @param {any} update A partial section that represents the part that should be updated in an existing section
 * @returns {MenusState} The new reduced state
 */
function updateSectionState(state: MenusState, action: MenuSectionAction, update: any): MenusState {
  const menuState: MenuState = state[action.menuID];
  const sectionState = menuState.sections[action.id];
  if (hasValue(sectionState)) {
    const newTopSection = Object.assign({}, sectionState, update);
    return putSectionState(state, action, newTopSection);

  }
  return state;
}
