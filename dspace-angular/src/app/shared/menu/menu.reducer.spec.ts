// eslint-disable-next-line import/no-namespace
import * as deepFreeze from 'deep-freeze';
import {
  ActivateMenuSectionAction,
  AddMenuSectionAction,
  CollapseMenuAction,
  CollapseMenuPreviewAction,
  DeactivateMenuSectionAction,
  ExpandMenuAction,
  ExpandMenuPreviewAction,
  HideMenuAction,
  HideMenuSectionAction,
  RemoveMenuSectionAction,
  ShowMenuAction,
  ShowMenuSectionAction,
  ToggleActiveMenuSectionAction,
  ToggleMenuAction
} from './menu.actions';
import { menusReducer } from './menu.reducer';
import { initialMenusState} from './initial-menus-state';
import { MenuSectionIndex } from './menu-section-Index.model';
import { MenuID } from './menu-id.model';

let visibleSection1;
let dummyState;
const menuID = MenuID.ADMIN;
const topSectionID = 'new';

class NullAction extends CollapseMenuAction {
  type = null;

  constructor() {
    super(undefined);
  }
}

describe('menusReducer', () => {
  beforeEach(() => {
    visibleSection1 = {
      id: 'section',
      parentID: 'new',
      visible: true,
      active: false,
      index: -1,
    };

    dummyState = {
      [MenuID.ADMIN]: {
        id: MenuID.ADMIN,
        collapsed: true,
        previewCollapsed: true,
        visible: true,
        sections: {
          [topSectionID]: {
            id: topSectionID,
            active: false,
            visible: true,
            model: {
              type: 0,
              text: 'menu.section.new'
            },
            icon: 'plus-circle',
            index: 0
          },
          new_item: {
            id: 'new_item',
            parentID: 'new',
            active: false,
            visible: true,
            model: {
              type: 1,
              text: 'menu.section.new_item',
              link: '/items/submission'
            }
          },
          new_community: {
            id: 'new_community',
            parentID: 'new',
            active: false,
            visible: true,
            model: {
              type: 1,
              text: 'menu.section.new_community',
              link: '/communities/submission'
            }
          },
          access_control: {
            id: 'access_control',
            active: false,
            visible: true,
            model: {
              type: 0,
              text: 'menu.section.access_control'
            },
            icon: 'key',
            index: 4
          },
          access_control_people: {
            id: 'access_control_people',
            parentID: 'access_control',
            active: false,
            visible: true,
            model: {
              type: 1,
              text: 'menu.section.access_control_people',
              link: '#'
            }
          },
          access_control_groups: {
            id: 'access_control_groups',
            parentID: 'access_control',
            active: false,
            visible: true,
            model: {
              type: 1,
              text: 'menu.section.access_control_groups',
              link: '#'
            }
          },
          new_collection: {
            id: 'new_collection',
            parentID: 'new',
            active: false,
            visible: true,
            model: {
              type: 1,
              text: 'menu.section.new_collection',
              link: '/collections/submission'
            }
          }
        },
        sectionToSubsectionIndex: {
          access_control: [
            'access_control_people',
            'access_control_groups',
          ],
          new: [
            'new_collection',
            'new_item',
            'new_community'
          ]
        }
      }
    };
  });

  it('should return the current state when no valid actions have been made', () => {
    const state = dummyState;
    const action = new NullAction();
    const newState = menusReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with the initialMenusState', () => {
    const state = initialMenusState;
    const action = new NullAction();
    const initialState = menusReducer(undefined, action);

    // The search filter starts collapsed
    expect(initialState).toEqual(state);
  });

  it('should set collapsed to true for the correct menu in response to the COLLAPSE_MENU action', () => {
    dummyState[MenuID.ADMIN].collapsed = false;
    const state = dummyState;
    const action = new CollapseMenuAction(menuID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].collapsed).toEqual(true);
  });

  it('should perform the COLLAPSE_MENU action without affecting the previous state', () => {
    dummyState[MenuID.ADMIN].collapsed = false;
    const state = dummyState;
    deepFreeze([state]);

    const action = new CollapseMenuAction(menuID);
    menusReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set collapsed to false for the correct menu in response to the EXPAND_MENU action', () => {
    dummyState[MenuID.ADMIN].collapsed = true;
    const state = dummyState;
    const action = new ExpandMenuAction(menuID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].collapsed).toEqual(false);
  });

  it('should perform the EXPAND_MENU action without affecting the previous state', () => {
    dummyState[MenuID.ADMIN].collapsed = true;
    const state = dummyState;
    deepFreeze([state]);

    const action = new ExpandMenuAction(menuID);
    menusReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set collapsed to false for the correct menu in response to the TOGGLE_MENU action when collapsed is true', () => {
    dummyState[MenuID.ADMIN].collapsed = true;
    const state = dummyState;
    const action = new ToggleMenuAction(menuID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].collapsed).toEqual(false);
  });

  it('should set collapsed to true for the correct menu in response to the TOGGLE_MENU action when collapsed is false', () => {
    dummyState[MenuID.ADMIN].collapsed = true;
    const state = dummyState;
    const action = new ToggleMenuAction(menuID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].collapsed).toEqual(false);
  });

  it('should perform the TOGGLE_MENU action without affecting the previous state', () => {
    dummyState[MenuID.ADMIN].collapsed = true;
    const state = dummyState;
    deepFreeze([state]);

    const action = new ToggleMenuAction(menuID);
    menusReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set previewCollapsed to true for the correct menu in response to the COLLAPSE_MENU_PREVIEW action', () => {
    dummyState[MenuID.ADMIN].previewCollapsed = false;
    const state = dummyState;
    const action = new CollapseMenuPreviewAction(menuID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].previewCollapsed).toEqual(true);
  });

  it('should perform the COLLAPSE_MENU_PREVIEW action without affecting the previous state', () => {
    dummyState[MenuID.ADMIN].previewCollapsed = false;
    const state = dummyState;
    deepFreeze([state]);

    const action = new CollapseMenuPreviewAction(menuID);
    menusReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set previewCollapsed to false for the correct menu in response to the EXPAND_MENU_PREVIEW action', () => {
    dummyState[MenuID.ADMIN].previewCollapsed = true;
    const state = dummyState;
    const action = new ExpandMenuPreviewAction(menuID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].previewCollapsed).toEqual(false);
  });

  it('should perform the EXPAND_MENU_PREVIEW action without affecting the previous state', () => {
    dummyState[MenuID.ADMIN].previewCollapsed = true;
    const state = dummyState;
    deepFreeze([state]);

    const action = new ExpandMenuPreviewAction(menuID);
    menusReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set visible to true for the correct menu in response to the SHOW_MENU action', () => {
    dummyState[MenuID.ADMIN].visible = false;
    const state = dummyState;
    const action = new ShowMenuAction(menuID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].visible).toEqual(true);
  });

  it('should perform the SHOW_MENU action without affecting the previous state', () => {
    dummyState[MenuID.ADMIN].visible = false;
    const state = dummyState;
    deepFreeze([state]);

    const action = new ShowMenuAction(menuID);
    menusReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set previewCollapsed to false for the correct menu in response to the HIDE_MENU action', () => {
    dummyState[MenuID.ADMIN].visible = true;
    const state = dummyState;
    const action = new HideMenuAction(menuID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].visible).toEqual(false);
  });

  it('should perform the HIDE_MENU action without affecting the previous state', () => {
    dummyState[MenuID.ADMIN].visible = true;
    const state = dummyState;
    deepFreeze([state]);

    const action = new HideMenuAction(menuID);
    menusReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set add a new section for the correct menu in response to the ADD_SECTION action', () => {
    const state = dummyState;
    const action = new AddMenuSectionAction(menuID, visibleSection1);
    const newState = menusReducer(state, action);
    expect(Object.values(newState[menuID].sections)).toContain(visibleSection1);
  });

  it('should set add a new section in the right place according to the index for the correct menu in response to the ADD_SECTION action', () => {
    const state = dummyState;
    const action = new AddMenuSectionAction(menuID, visibleSection1);
    const newState = menusReducer(state, action);
    expect(Object.values(newState[menuID].sections)[0]).toEqual(visibleSection1);
  });

  it('should add the new section to the sectionToSubsectionIndex when it has a parentID in response to the ADD_SECTION action', () => {
    const state = dummyState;
    const action = new AddMenuSectionAction(menuID, visibleSection1);
    const newState = menusReducer(state, action);
    expect(newState[menuID].sectionToSubsectionIndex[visibleSection1.parentID]).toContain(visibleSection1.id);
  });

  it('should perform the ADD_SECTION action without affecting the previous state', () => {
    const state = dummyState;
    deepFreeze([state]);

    const action = new AddMenuSectionAction(menuID, visibleSection1);
    menusReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should remove a section for the correct menu in response to the REMOVE_SECTION action', () => {
    const sectionID = Object.keys(dummyState[menuID].sections)[0];
    const state = dummyState;
    const action = new RemoveMenuSectionAction(menuID, sectionID);
    const newState = menusReducer(state, action);
    expect(Object.keys(newState[menuID].sections)).not.toContain(sectionID);
  });

  it('should remove a section for the correct menu from the sectionToSubsectionIndex in response to the REMOVE_SECTION action', () => {
    const index: MenuSectionIndex = dummyState[menuID].sectionToSubsectionIndex;
    const parentID: string = Object.keys(index)[0];
    const childID: string = index[parentID][0];
    const state = dummyState;
    const action = new RemoveMenuSectionAction(menuID, childID);
    const newState = menusReducer(state, action);
    expect(newState[menuID].sectionToSubsectionIndex[parentID]).not.toContain(childID);
  });

  it('should set active to true for the correct menu section in response to the ACTIVATE_SECTION action', () => {
    dummyState[menuID].sections[topSectionID].active = false;
    const state = dummyState;
    const action = new ActivateMenuSectionAction(menuID, topSectionID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].sections[topSectionID].active).toEqual(true);
  });

  it('should perform the ACTIVATE_SECTION action without affecting the previous state', () => {
    dummyState[menuID].sections[topSectionID].active = false;
    const state = dummyState;
    deepFreeze([state]);

    const action = new ActivateMenuSectionAction(menuID, topSectionID);
    menusReducer(state, action);
  });

  it('should set active to false for the correct menu section in response to the DEACTIVATE_SECTION action', () => {
    dummyState[menuID].sections[topSectionID].active = true;
    const state = dummyState;
    const action = new DeactivateMenuSectionAction(menuID, topSectionID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].sections[topSectionID].active).toEqual(false);
  });

  it('should perform the DEACTIVATE_SECTION action without affecting the previous state', () => {
    dummyState[MenuID.ADMIN].sections[topSectionID].active = false;
    const state = dummyState;
    deepFreeze([state]);

    const action = new DeactivateMenuSectionAction(menuID, topSectionID);
    menusReducer(state, action);
  });

  it('should set active to false for the correct menu in response to the TOGGLE_ACTIVE_SECTION action when active is true', () => {
    dummyState[menuID].sections[topSectionID].active = true;
    const state = dummyState;
    const action = new ToggleActiveMenuSectionAction(menuID, topSectionID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].sections[topSectionID].active).toEqual(false);
  });

  it('should set collapsed to true for the correct menu in response to the TOGGLE_ACTIVE_SECTION action when active is false', () => {
    dummyState[menuID].sections[topSectionID].active = false;
    const state = dummyState;
    const action = new ToggleActiveMenuSectionAction(menuID, topSectionID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].sections[topSectionID].active).toEqual(true);
  });

  it('should perform the TOGGLE_ACTIVE_SECTION action without affecting the previous state', () => {
    dummyState[menuID].sections[topSectionID].active = true;
    const state = dummyState;
    const action = new ToggleActiveMenuSectionAction(menuID, topSectionID);
    deepFreeze([state]);
    menusReducer(state, action);

    // no expect required, deepFreeze will ensure an exception is thrown if the state
    // is mutated, and any uncaught exception will cause the test to fail
  });

  it('should set visible to true for the correct menu section in response to the SHOW_SECTION action', () => {
    dummyState[menuID].sections[topSectionID].visible = false;
    const state = dummyState;
    const action = new ShowMenuSectionAction(menuID, topSectionID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].sections[topSectionID].visible).toEqual(true);
  });

  it('should perform the SHOW_SECTION action without affecting the previous state', () => {
    dummyState[menuID].sections[topSectionID].visible = false;
    const state = dummyState;
    deepFreeze([state]);

    const action = new ShowMenuSectionAction(menuID, topSectionID);
    menusReducer(state, action);
  });

  it('should set visible to false for the correct menu section in response to the HIDE_SECTION action', () => {
    dummyState[menuID].sections[topSectionID].visible = true;
    const state = dummyState;
    const action = new HideMenuSectionAction(menuID, topSectionID);
    const newState = menusReducer(state, action);

    expect(newState[menuID].sections[topSectionID].visible).toEqual(false);
  });

  it('should perform the HIDE_SECTION action without affecting the previous state', () => {
    dummyState[MenuID.ADMIN].sections[topSectionID].visible = false;
    const state = dummyState;
    deepFreeze([state]);

    const action = new HideMenuSectionAction(menuID, topSectionID);
    menusReducer(state, action);
  });
});
