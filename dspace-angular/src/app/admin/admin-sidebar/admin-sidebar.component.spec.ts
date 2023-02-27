import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { ChangeDetectionStrategy, Injector, NO_ERRORS_SCHEMA } from '@angular/core';
import { ScriptDataService } from '../../core/data/processes/script-data.service';
import { AdminSidebarComponent } from './admin-sidebar.component';
import { MenuService } from '../../shared/menu/menu.service';
import { MenuServiceStub } from '../../shared/testing/menu-service.stub';
import { CSSVariableService } from '../../shared/sass-helper/css-variable.service';
import { CSSVariableServiceStub } from '../../shared/testing/css-variable-service.stub';
import { AuthServiceStub } from '../../shared/testing/auth-service.stub';
import { AuthService } from '../../core/auth/auth.service';
import { of as observableOf } from 'rxjs';
import { By } from '@angular/platform-browser';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import createSpy = jasmine.createSpy;
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { Item } from '../../core/shared/item.model';
import { ThemeService } from '../../shared/theme-support/theme.service';
import { getMockThemeService } from '../../shared/mocks/theme-service.mock';

describe('AdminSidebarComponent', () => {
  let comp: AdminSidebarComponent;
  let fixture: ComponentFixture<AdminSidebarComponent>;
  const menuService = new MenuServiceStub();
  let authorizationService: AuthorizationDataService;
  let scriptService;


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
      isAuthorized: observableOf(true)
    });
    scriptService = jasmine.createSpyObj('scriptService', { scriptWithNameExistsAndCanExecute: observableOf(true) });
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), NoopAnimationsModule, RouterTestingModule],
      declarations: [AdminSidebarComponent],
      providers: [
        Injector,
        { provide: ThemeService, useValue: getMockThemeService() },
        { provide: MenuService, useValue: menuService },
        { provide: CSSVariableService, useClass: CSSVariableServiceStub },
        { provide: AuthService, useClass: AuthServiceStub },
        { provide: ActivatedRoute, useValue: {} },
        { provide: AuthorizationDataService, useValue: authorizationService },
        { provide: ScriptDataService, useValue: scriptService },
        { provide: ActivatedRoute, useValue: routeStub },
        {
          provide: NgbModal, useValue: {
            open: () => {/*comment*/
            }
          }
        }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(AdminSidebarComponent, {
      set: {
        changeDetection: ChangeDetectionStrategy.Default,
      }
    }).compileComponents();
  }));

  beforeEach(() => {
    spyOn(menuService, 'getMenuTopSections').and.returnValue(observableOf([]));
    fixture = TestBed.createComponent(AdminSidebarComponent);
    comp = fixture.componentInstance; // SearchPageComponent test instance
    comp.sections = observableOf([]);
    fixture.detectChanges();
  });

  describe('startSlide', () => {
    describe('when expanding', () => {
      beforeEach(() => {
        comp.sidebarClosed = true;
        comp.startSlide({ toState: 'expanded' } as any);
      });

      it('should set the sidebarClosed to false', () => {
        expect(comp.sidebarClosed).toBeFalsy();
      });
    });

    describe('when collapsing', () => {
      beforeEach(() => {
        comp.sidebarClosed = false;
        comp.startSlide({ toState: 'collapsed' } as any);
      });

      it('should set the sidebarOpen to false', () => {
        expect(comp.sidebarOpen).toBeFalsy();
      });
    });
  });

  describe('finishSlide', () => {
    describe('when expanding', () => {
      beforeEach(() => {
        comp.sidebarClosed = true;
        comp.startSlide({ fromState: 'expanded' } as any);
      });

      it('should set the sidebarClosed to true', () => {
        expect(comp.sidebarClosed).toBeTruthy();
      });
    });

    describe('when collapsing', () => {
      beforeEach(() => {
        comp.sidebarClosed = false;
        comp.startSlide({ fromState: 'collapsed' } as any);
      });

      it('should set the sidebarOpen to true', () => {
        expect(comp.sidebarOpen).toBeTruthy();
      });
    });
  });

  describe('when the collapse link is clicked', () => {
    beforeEach(() => {
      spyOn(menuService, 'toggleMenu');
      const sidebarToggler = fixture.debugElement.query(By.css('#sidebar-collapse-toggle > a'));
      sidebarToggler.triggerEventHandler('click', {
        preventDefault: () => {/**/
        }
      });
    });

    it('should call toggleMenu on the menuService', () => {
      expect(menuService.toggleMenu).toHaveBeenCalled();
    });
  });

  describe('when the the mouse enters the nav tag', () => {
    it('should call expandPreview on the menuService after 100ms', fakeAsync(() => {
      spyOn(menuService, 'expandMenuPreview');
      const sidebarToggler = fixture.debugElement.query(By.css('nav.navbar'));
      sidebarToggler.triggerEventHandler('mouseenter', {
        preventDefault: () => {/**/
        }
      });
      tick(99);
      expect(menuService.expandMenuPreview).not.toHaveBeenCalled();
      tick(1);
      expect(menuService.expandMenuPreview).toHaveBeenCalled();
    }));
  });

  describe('when the the mouse leaves the nav tag', () => {
    it('should call collapseMenuPreview on the menuService after 400ms', fakeAsync(() => {
      spyOn(menuService, 'collapseMenuPreview');
      const sidebarToggler = fixture.debugElement.query(By.css('nav.navbar'));
      sidebarToggler.triggerEventHandler('mouseleave', {
        preventDefault: () => {/**/
        }
      });
      tick(399);
      expect(menuService.collapseMenuPreview).not.toHaveBeenCalled();
      tick(1);
      expect(menuService.collapseMenuPreview).toHaveBeenCalled();
    }));
  });
});
