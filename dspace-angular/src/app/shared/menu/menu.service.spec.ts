import { TestBed, waitForAsync } from '@angular/core/testing';

import { of as observableOf } from 'rxjs';
import { Store, StoreModule } from '@ngrx/store';
import { provideMockStore } from '@ngrx/store/testing';
import { cold } from 'jasmine-marbles';

import { MenuService } from './menu.service';
import {
  ActivateMenuSectionAction,
  AddMenuSectionAction,
  CollapseMenuAction,
  CollapseMenuPreviewAction,
  DeactivateMenuSectionAction,
  ExpandMenuAction,
  ExpandMenuPreviewAction,
  HideMenuAction,
  RemoveMenuSectionAction,
  ShowMenuAction,
  ToggleActiveMenuSectionAction,
  ToggleMenuAction
} from './menu.actions';
import { menusReducer } from './menu.reducer';
import { storeModuleConfig } from '../../app.reducer';
import { MenuSection } from './menu-section.model';
import { MenuID } from './menu-id.model';
import { MenuItemType } from './menu-item-type.model';
import { LinkMenuItemModel } from './menu-item/models/link.model';
import { NavigationEnd } from '@angular/router';

describe('MenuService', () => {
  let service: MenuService;
  let store: any;
  let fakeMenu;
  let visibleSection1;
  let visibleSection2;
  let hiddenSection3;
  let subSection4;
  let topSections;
  let initialState;
  let routeDataMenuSection: MenuSection;
  let routeDataMenuSectionResolved: MenuSection;
  let routeDataMenuChildSection: MenuSection;
  let toBeRemovedMenuSection: MenuSection;
  let alreadyPresentMenuSection: MenuSection;
  let route;
  let router;


  function init() {

    visibleSection1 = {
      id: 'section',
      visible: true,
      active: false
    };
    visibleSection2 = {
      id: 'section_2',
      visible: true
    };
    hiddenSection3 = {
      id: 'section_3',
      visible: false
    };
    subSection4 = {
      id: 'section_4',
      visible: true,
      parentID: 'section'
    };
    subSection4 = {
      id: 'section_4',
      visible: true,
      parentID: 'section'
    };
    topSections = {
      section: visibleSection1,
      section_2: visibleSection2,
      section_3: hiddenSection3,
      section_4: subSection4
    };

    fakeMenu = {
      id: MenuID.ADMIN,
      collapsed: true,
      visible: false,
      sections: topSections,
      previewCollapsed: true,
      sectionToSubsectionIndex: {
        'section': ['section_4']
      }
    } as any;

    initialState = {
      menus: {
        'admin-sidebar': fakeMenu
      }
    };

    routeDataMenuSection = {
      id: 'mockSection_:idparam',
      active: false,
      visible: true,
      model: {
        type: MenuItemType.LINK,
        text: 'menu.section.mockSection',
        link: 'path/:linkparam'
      } as LinkMenuItemModel
    };
    routeDataMenuSectionResolved = {
      id: 'mockSection_id_param_resolved',
      active: false,
      visible: true,
      model: {
        type: MenuItemType.LINK,
        text: 'menu.section.mockSection',
        link: 'path/link_param_resolved'
      } as LinkMenuItemModel
    };
    routeDataMenuChildSection = {
      id: 'mockChildSection',
      parentID: 'mockSection',
      active: false,
      visible: true,
      model: {
        type: MenuItemType.LINK,
        text: 'menu.section.mockChildSection',
        link: ''
      } as LinkMenuItemModel
    };
    toBeRemovedMenuSection = {
      id: 'toBeRemovedSection',
      active: false,
      visible: true,
      model: {
        type: MenuItemType.LINK,
        text: 'menu.section.toBeRemovedSection',
        link: ''
      } as LinkMenuItemModel
    };
    alreadyPresentMenuSection = {
      id: 'alreadyPresentSection',
      active: false,
      visible: true,
      model: {
        type: MenuItemType.LINK,
        text: 'menu.section.alreadyPresentSection',
        link: ''
      } as LinkMenuItemModel
    };
    route = {
      root: {
        snapshot: {
          data: {
            menu: {
              [MenuID.PUBLIC]: [routeDataMenuSection, alreadyPresentMenuSection]
            }
          },
          params: {
            idparam: 'id_param_resolved',
            linkparam: 'link_param_resolved',
          }
        },
        firstChild: {
          snapshot: {
            data: {
              menu: {
                [MenuID.PUBLIC]: routeDataMenuChildSection
              }
            }
          }
        }
      }
    };

    router = {
      events: observableOf(new NavigationEnd(1, 'test-url', 'test-url'))
    };
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot({menus: menusReducer}, storeModuleConfig)
      ],
      providers: [
        provideMockStore({initialState}),
        {provide: MenuService, useValue: service}
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    store = TestBed.inject(Store);
    service = new MenuService(store, route, router);
    spyOn(store, 'dispatch');
  });

  describe('getMenu', () => {
    it('should return the menu', () => {

      const result = service.getMenu(MenuID.ADMIN);
      const expected = cold('b', {
        b: fakeMenu
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getMenuTopSections', () => {

    it('should return only the visible top MenuSections when mustBeVisible is true', () => {

      const result = service.getMenuTopSections(MenuID.ADMIN);
      const expected = cold('b', {
        b: [visibleSection1, visibleSection2]
      });

      expect(result).toBeObservable(expected);
    });

    it('should return only the all top MenuSections when mustBeVisible is false', () => {

      const result = service.getMenuTopSections(MenuID.ADMIN, false);
      const expected = cold('b', {
        b: [visibleSection1, visibleSection2, hiddenSection3]
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getSubSectionsByParentID', () => {
    describe('when the subsection list is not empty', () => {
      it('should return the MenuSections with the given parentID', () => {

        const result = service.getSubSectionsByParentID(MenuID.ADMIN, 'section');
        const expected = cold('b', {
          b: [subSection4]
        });

        expect(result).toBeObservable(expected);
      });
    });
    describe('when the subsection list is undefined', () => {
      it('should return an observable that emits nothing', () => {

        const result = service.getSubSectionsByParentID(MenuID.ADMIN, 'fakeId');
        const expected = cold('');

        expect(result).toBeObservable(expected);
      });
    });
  });

  describe('hasSubSections', () => {
    describe('when the subsection list is not empty', () => {

      it('should return true', () => {

        const result = service.hasSubSections(MenuID.ADMIN, 'section');
        const expected = cold('b', {
          b: true
        });

        expect(result).toBeObservable(expected);
      });
    });

    describe('when the subsection list is empty', () => {
      it('should return false', () => {

        const result = service.hasSubSections(MenuID.ADMIN, 'fakeId');
        const expected = cold('b', {
          b: false
        });

        expect(result).toBeObservable(expected);
      });
    });
  });

  describe('getMenuSection', () => {
    it('should return menu section', () => {

      const result = service.getMenuSection(MenuID.ADMIN, 'section_3');
      const expected = cold('b', {
        b: hiddenSection3
      });

      expect(result).toBeObservable(expected);
    });

    it('should return undefined', () => {

      const result = service.getMenuSection(MenuID.ADMIN, 'fake');
      const expected = cold('b', {
        b: undefined
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('isMenuCollapsed', () => {
    beforeEach(() => {
      spyOn(service, 'getMenu').and.returnValue(observableOf(fakeMenu));
    });
    it('should return true when the menu is collapsed', () => {

      const result = service.isMenuCollapsed(MenuID.ADMIN);
      const expected = cold('(b|)', {
        b: fakeMenu.collapsed
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('isMenuPreviewCollapsed', () => {
    beforeEach(() => {
      spyOn(service, 'getMenu').and.returnValue(observableOf(fakeMenu));
    });
    it('should return true when the menu\'s preview is collapsed', () => {

      const result = service.isMenuPreviewCollapsed(MenuID.ADMIN);
      const expected = cold('(b|)', {
        b: fakeMenu.previewCollapsed
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('isMenuVisibleWithVisibleSections', () => {
    it('should return false when the menu is empty', () => {
      const testMenu = {
        id: MenuID.ADMIN,
        collapsed: false,
        visible: true,
        sections: {},
        previewCollapsed: false,
        sectionToSubsectionIndex: {}
      } as any;
      spyOn(service, 'getMenu').and.returnValue(observableOf(testMenu));

      const result = service.isMenuVisibleWithVisibleSections(MenuID.ADMIN);
      const expected = cold('(b|)', {
        b: false
      });

      expect(result).toBeObservable(expected);
    });
    it('should return false when no top-level sections are visible', () => {
      const noTopLevelVisibleSections = {
        section: {id: 's1', visible: false},
        section_2: {id: 's2', visible: false},
        section_3: {id: 's3', visible: false},
        section_4: {id: 's1_1', visible: true, parentID: 's1'},
        section_5: {id: 's2_1', visible: true, parentID: 's2'},
      };
      const testMenu = {
        id: MenuID.ADMIN,
        collapsed: false,
        visible: true,
        sections: noTopLevelVisibleSections,
        previewCollapsed: false,
        sectionToSubsectionIndex: {
          'section': ['section_4'],
          'section_2': ['section_5'],
        }
      } as any;
      spyOn(service, 'getMenu').and.returnValue(observableOf(testMenu));

      const result = service.isMenuVisibleWithVisibleSections(MenuID.ADMIN);
      const expected = cold('(b|)', {
        b: false
      });

      expect(result).toBeObservable(expected);
    });

    it('should return true when any top-level section is visible', () => {
      const noTopLevelVisibleSections = {
        section: {id: 's1', visible: false},
        section_2: {id: 's2', visible: true},
        section_3: {id: 's3', visible: false},
        section_4: {id: 's1_1', visible: true, parentID: 's1'},
        section_5: {id: 's2_1', visible: true, parentID: 's2'},
      };
      const testMenu = {
        id: MenuID.ADMIN,
        collapsed: false,
        visible: true,
        sections: noTopLevelVisibleSections,
        previewCollapsed: false,
        sectionToSubsectionIndex: {
          'section': ['section_4'],
          'section_2': ['section_5'],
        }
      } as any;
      spyOn(service, 'getMenu').and.returnValue(observableOf(testMenu));

      const result = service.isMenuVisibleWithVisibleSections(MenuID.ADMIN);
      const expected = cold('(b|)', {
        b: true
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('isMenuVisible', () => {
    beforeEach(() => {
      spyOn(service, 'getMenu').and.returnValue(observableOf(fakeMenu));

    });
    it('should return false when the menu is hidden', () => {

      const result = service.isMenuVisible(MenuID.ADMIN);
      const expected = cold('(b|)', {
        b: fakeMenu.visible
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('isSectionActive', () => {
    beforeEach(() => {
      spyOn(service, 'getMenuSection').and.returnValue(observableOf(visibleSection1 as MenuSection));
    });

    it('should return false when the section is not active', () => {
      const result = service.isSectionActive(MenuID.ADMIN, 'fakeID');
      const expected = cold('(b|)', {
        b: visibleSection1.active
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('isSectionVisible', () => {
    beforeEach(() => {
      spyOn(service, 'getMenuSection').and.returnValue(observableOf(hiddenSection3 as MenuSection));
    });

    it('should return false when the section is hidden', () => {
      const result = service.isSectionVisible(MenuID.ADMIN, 'fakeID');
      const expected = cold('(b|)', {
        b: hiddenSection3.visible
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('addSection', () => {
    it('should dispatch an AddMenuSectionAction with the correct arguments', () => {
      service.addSection(MenuID.ADMIN, visibleSection1 as any);
      expect(store.dispatch).toHaveBeenCalledWith(new AddMenuSectionAction(MenuID.ADMIN, visibleSection1 as any));
    });
  });

  describe('removeSection', () => {
    it('should dispatch an RemoveMenuSectionAction with the correct arguments', () => {
      service.removeSection(MenuID.ADMIN, 'fakeID');
      expect(store.dispatch).toHaveBeenCalledWith(new RemoveMenuSectionAction(MenuID.ADMIN, 'fakeID'));
    });
  });

  describe('expandMenu', () => {
    it('should dispatch an ExpandMenuAction with the correct arguments', () => {
      service.expandMenu(MenuID.ADMIN);
      expect(store.dispatch).toHaveBeenCalledWith(new ExpandMenuAction(MenuID.ADMIN));
    });
  });

  describe('collapseMenu', () => {
    it('should dispatch an CollapseMenuAction with the correct arguments', () => {
      service.collapseMenu(MenuID.ADMIN);
      expect(store.dispatch).toHaveBeenCalledWith(new CollapseMenuAction(MenuID.ADMIN));
    });
  });

  describe('expandMenuPreview', () => {
    it('should dispatch an ExpandMenuPreviewAction with the correct arguments', () => {
      service.expandMenuPreview(MenuID.ADMIN);
      expect(store.dispatch).toHaveBeenCalledWith(new ExpandMenuPreviewAction(MenuID.ADMIN));
    });
  });

  describe('collapseMenuPreview', () => {
    it('should dispatch an CollapseMenuPreviewAction with the correct arguments', () => {
      service.collapseMenuPreview(MenuID.ADMIN);
      expect(store.dispatch).toHaveBeenCalledWith(new CollapseMenuPreviewAction(MenuID.ADMIN));
    });
  });

  describe('toggleMenu', () => {
    it('should dispatch an ToggleMenuAction with the correct arguments', () => {
      service.toggleMenu(MenuID.ADMIN);
      expect(store.dispatch).toHaveBeenCalledWith(new ToggleMenuAction(MenuID.ADMIN));
    });
  });

  describe('showMenu', () => {
    it('should dispatch an ShowMenuAction with the correct arguments', () => {
      service.showMenu(MenuID.ADMIN);
      expect(store.dispatch).toHaveBeenCalledWith(new ShowMenuAction(MenuID.ADMIN));
    });
  });

  describe('hideMenu', () => {
    it('should dispatch an HideMenuAction with the correct arguments', () => {
      service.hideMenu(MenuID.ADMIN);
      expect(store.dispatch).toHaveBeenCalledWith(new HideMenuAction(MenuID.ADMIN));
    });
  });

  describe('toggleActiveSection', () => {
    it('should dispatch an ToggleActiveMenuSectionAction with the correct arguments', () => {
      service.toggleActiveSection(MenuID.ADMIN, 'fakeID');
      expect(store.dispatch).toHaveBeenCalledWith(new ToggleActiveMenuSectionAction(MenuID.ADMIN, 'fakeID'));
    });
  });

  describe('activateSection', () => {
    it('should dispatch an ActivateMenuSectionAction with the correct arguments', () => {
      service.activateSection(MenuID.ADMIN, 'fakeID');
      expect(store.dispatch).toHaveBeenCalledWith(new ActivateMenuSectionAction(MenuID.ADMIN, 'fakeID'));
    });
  });

  describe('deactivateSection', () => {
    it('should dispatch an DeactivateMenuSectionAction with the correct arguments', () => {
      service.deactivateSection(MenuID.ADMIN, 'fakeID');
      expect(store.dispatch).toHaveBeenCalledWith(new DeactivateMenuSectionAction(MenuID.ADMIN, 'fakeID'));
    });
  });

  describe('buildRouteMenuSections', () => {
    it('should add and remove menu sections depending on the current route', () => {
      spyOn(service, 'addSection');
      spyOn(service, 'removeSection');

      spyOn(service, 'getNonPersistentMenuSections').and.returnValue(observableOf([toBeRemovedMenuSection, alreadyPresentMenuSection]));

      service.buildRouteMenuSections(MenuID.PUBLIC);

      expect(service.addSection).toHaveBeenCalledWith(MenuID.PUBLIC, routeDataMenuSectionResolved);
      expect(service.addSection).toHaveBeenCalledWith(MenuID.PUBLIC, routeDataMenuChildSection);
      expect(service.addSection).not.toHaveBeenCalledWith(MenuID.PUBLIC, alreadyPresentMenuSection);
      expect(service.removeSection).toHaveBeenCalledWith(MenuID.PUBLIC, toBeRemovedMenuSection.id);
    });
  });

  describe('listenForRouteChanges', () => {
    it('should build the menu sections on NavigationEnd event', () => {
      spyOn(service, 'buildRouteMenuSections');

      service.listenForRouteChanges();

      expect(service.buildRouteMenuSections).toHaveBeenCalledWith(MenuID.ADMIN);
      expect(service.buildRouteMenuSections).toHaveBeenCalledWith(MenuID.PUBLIC);
    });
  });

});
