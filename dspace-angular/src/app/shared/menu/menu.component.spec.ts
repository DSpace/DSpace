import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { ChangeDetectionStrategy, Injector, NO_ERRORS_SCHEMA } from '@angular/core';
import { MenuService } from './menu.service';
import { MenuComponent } from './menu.component';
import { MenuServiceStub } from '../testing/menu-service.stub';
import { of as observableOf } from 'rxjs';
import { Router, ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MenuSection } from './menu-section.model';
import { MenuID } from './menu-id.model';
import { Item } from '../../core/shared/item.model';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { createSuccessfulRemoteDataObject } from '../remote-data.utils';
import { ThemeService } from '../theme-support/theme.service';
import { getMockThemeService } from '../mocks/theme-service.mock';

describe('MenuComponent', () => {
  let comp: MenuComponent;
  let fixture: ComponentFixture<MenuComponent>;
  let menuService: MenuService;
  let router: any;

  const mockMenuID = 'mock-menuID' as MenuID;

  const mockStatisticSection = { 'id': 'statistics_site', 'active': true, 'visible': true, 'index': 2, 'type': 'statistics', 'model': { 'type': 1, 'text': 'menu.section.statistics', 'link': 'statistics' } };

  let authorizationService: AuthorizationDataService;

  const mockItem = Object.assign(new Item(), {
    id: 'fake-id',
    uuid: 'fake-id',
    handle: 'fake/handle',
    lastModified: '2018',
    _links: {
      self: {
        href: 'https://localhost:8000/items/fake-id'
      }
    }
  });


  const routeStub = {
    data: observableOf({
      dso: createSuccessfulRemoteDataObject(mockItem)
    }),
    children: []
  };

  beforeEach(waitForAsync(() => {

    authorizationService = jasmine.createSpyObj('authorizationService', {
      isAuthorized: observableOf(false)
    });

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), NoopAnimationsModule, RouterTestingModule],
      declarations: [MenuComponent],
      providers: [
        Injector,
        { provide: ThemeService, useValue: getMockThemeService() },
        { provide: MenuService, useClass: MenuServiceStub },
        { provide: AuthorizationDataService, useValue: authorizationService },
        { provide: ActivatedRoute, useValue: routeStub },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(MenuComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MenuComponent);
    comp = fixture.componentInstance; // SearchPageComponent test instance
    comp.menuID = mockMenuID;
    menuService = (comp as any).menuService;
    router = TestBed.inject(Router);
    spyOn(comp as any, 'getSectionDataInjector').and.returnValue(MenuSection);
    spyOn(comp as any, 'getSectionComponent').and.returnValue(observableOf({}));
    fixture.detectChanges();
  });

  describe('toggle', () => {
    beforeEach(() => {
      spyOn(menuService, 'toggleMenu');
      comp.toggle(new Event('click'));
    });
    it('should trigger the toggleMenu function on the menu service', () => {
      expect(menuService.toggleMenu).toHaveBeenCalledWith(comp.menuID);
    });
  });

  describe('expand', () => {
    beforeEach(() => {
      spyOn(menuService, 'expandMenu');
      comp.expand(new Event('click'));
    });
    it('should trigger the expandMenu function on the menu service', () => {
      expect(menuService.expandMenu).toHaveBeenCalledWith(comp.menuID);
    });
  });

  describe('collapse', () => {
    beforeEach(() => {
      spyOn(menuService, 'collapseMenu');
      comp.collapse(new Event('click'));
    });
    it('should trigger the collapseMenu function on the menu service', () => {
      expect(menuService.collapseMenu).toHaveBeenCalledWith(comp.menuID);
    });
  });

  describe('expandPreview', () => {
    it('should trigger the expandPreview function on the menu service after 100ms', fakeAsync(() => {
      spyOn(menuService, 'expandMenuPreview');
      comp.expandPreview(new Event('click'));
      tick(99);
      expect(menuService.expandMenuPreview).not.toHaveBeenCalled();
      tick(1);
      expect(menuService.expandMenuPreview).toHaveBeenCalledWith(comp.menuID);
    }));
  });

  describe('collapsePreview', () => {
    it('should trigger the collapsePreview function on the menu service after 400ms', fakeAsync(() => {
      spyOn(menuService, 'collapseMenuPreview');
      comp.collapsePreview(new Event('click'));
      tick(399);
      expect(menuService.collapseMenuPreview).not.toHaveBeenCalled();
      tick(1);
      expect(menuService.collapseMenuPreview).toHaveBeenCalledWith(comp.menuID);
    }));
  });

  describe('when unauthorized statistics', () => {

    beforeEach(() => {
      (authorizationService as any).isAuthorized.and.returnValue(observableOf(false));
      fixture.detectChanges();
    });

    it('should return observable of empty object', done => {
      comp.getAuthorizedStatistics(mockStatisticSection).subscribe((res) => {
        expect(res).toEqual({});
        done();
      });
    });
  });

  describe('get authorized statistics', () => {

    beforeEach(() => {
      (authorizationService as any).isAuthorized.and.returnValue(observableOf(true));
      fixture.detectChanges();
    });

    it('should return observable of statistics section menu', done => {
      comp.getAuthorizedStatistics(mockStatisticSection).subscribe((res) => {
        expect(res).toEqual(mockStatisticSection);
        done();
      });
    });
  });

});
