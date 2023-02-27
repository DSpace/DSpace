import { MenusState } from './menus-state.model';
import { MenuID } from './menu-id.model';

/**
 * The initial state of the menus
 */
export const initialMenusState: MenusState = {
  [MenuID.ADMIN]:
    {
      id: MenuID.ADMIN,
      collapsed: true,
      previewCollapsed: true,
      visible: false,
      sections: {},
      sectionToSubsectionIndex: {}
    },
  [MenuID.PUBLIC]:
    {
      id: MenuID.PUBLIC,
      collapsed: true,
      previewCollapsed: true,
      visible: true,
      sections: {},
      sectionToSubsectionIndex: {}
    },
  [MenuID.DSO_EDIT]:
    {
      id: MenuID.DSO_EDIT,
      collapsed: true,
      previewCollapsed: true,
      visible: false,
      sections: {},
      sectionToSubsectionIndex: {}
    },
};
