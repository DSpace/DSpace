import { CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';

import { By } from '@angular/platform-browser';
import { Store, StoreModule } from '@ngrx/store';

import { authReducer, AuthState } from '../../core/auth/auth.reducer';
import { EPersonMock } from '../testing/eperson.mock';
import { TranslateModule } from '@ngx-translate/core';
import { AppState } from '../../app.reducer';
import { AuthNavMenuComponent } from './auth-nav-menu.component';
import { HostWindowServiceStub } from '../testing/host-window-service.stub';
import { HostWindowService } from '../host-window.service';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AuthTokenInfo } from '../../core/auth/models/auth-token-info.model';
import { AuthService } from '../../core/auth/auth.service';
import { of } from 'rxjs';
import { BrowserOnlyMockPipe } from '../testing/browser-only-mock.pipe';

describe('AuthNavMenuComponent', () => {

  let component: AuthNavMenuComponent;
  let deNavMenu: DebugElement;
  let deNavMenuItem: DebugElement;
  let fixture: ComponentFixture<AuthNavMenuComponent>;

  let notAuthState: AuthState;
  let authState: AuthState;

  let authService: AuthService;

  let routerState = {
    url: '/home'
  };

  function serviceInit() {
    authService = jasmine.createSpyObj('authService', {
      getAuthenticatedUserFromStore: of(EPersonMock),
      setRedirectUrl: {}
    });
  }

  function init() {
    notAuthState = {
      authenticated: false,
      loaded: false,
      blocking: false,
      loading: false,
      idle: false
    };
    authState = {
      authenticated: true,
      loaded: true,
      blocking: false,
      loading: false,
      authToken: new AuthTokenInfo('test_token'),
      userId: EPersonMock.id,
      idle: false
    };
  }

  describe('when is a not mobile view', () => {

    beforeEach(waitForAsync(() => {
      const window = new HostWindowServiceStub(800);
      serviceInit();

      // refine the test module by declaring the test component
      TestBed.configureTestingModule({
        imports: [
          NoopAnimationsModule,
          StoreModule.forRoot(authReducer, {
            runtimeChecks: {
              strictStateImmutability: false,
              strictActionImmutability: false
            }
          }),
          TranslateModule.forRoot()
        ],
        declarations: [
          AuthNavMenuComponent,
          BrowserOnlyMockPipe
        ],
        providers: [
          { provide: HostWindowService, useValue: window },
          { provide: AuthService, useValue: authService }
        ],
        schemas: [
          CUSTOM_ELEMENTS_SCHEMA
        ]
      })
        .compileComponents();

    }));

    beforeEach(() => {
      init();
    });
    describe('when route is /login and user is not authenticated', () => {
      beforeEach(inject([Store], (store: Store<AppState>) => {
        routerState = {
          url: '/login'
        };
        store
          .subscribe((state) => {
            (state as any).router = Object.create({});
            (state as any).router.state = routerState;
            (state as any).core = Object.create({});
            (state as any).core.auth = notAuthState;
          });

        // create component and test fixture
        fixture = TestBed.createComponent(AuthNavMenuComponent);

        // get test component from the fixture
        component = fixture.componentInstance;

        fixture.detectChanges();

        const navMenuSelector = '.navbar-nav';
        deNavMenu = fixture.debugElement.query(By.css(navMenuSelector));

        const navMenuItemSelector = 'li';
        deNavMenuItem = deNavMenu.query(By.css(navMenuItemSelector));
      }));
      afterEach(() => {
        fixture.destroy();
      });
      it('should not render', () => {
        expect(component).toBeTruthy();
        expect(deNavMenu.nativeElement).toBeDefined();
        expect(deNavMenuItem).toBeNull();
      });

    });

    describe('when route is /logout and user is authenticated', () => {
      beforeEach(inject([Store], (store: Store<AppState>) => {
        routerState = {
          url: '/logout'
        };
        store
          .subscribe((state) => {
            (state as any).router = Object.create({});
            (state as any).router.state = routerState;
            (state as any).core = Object.create({});
            (state as any).core.auth = authState;
          });

        // create component and test fixture
        fixture = TestBed.createComponent(AuthNavMenuComponent);

        // get test component from the fixture
        component = fixture.componentInstance;

        fixture.detectChanges();

        const navMenuSelector = '.navbar-nav';
        deNavMenu = fixture.debugElement.query(By.css(navMenuSelector));

        const navMenuItemSelector = 'li';
        deNavMenuItem = deNavMenu.query(By.css(navMenuItemSelector));
      }));

      afterEach(() => {
        fixture.destroy();
      });

      it('should not render', () => {
        expect(component).toBeTruthy();
        expect(deNavMenu.nativeElement).toBeDefined();
        expect(deNavMenuItem).toBeNull();
      });

    });

    describe('when route is not /login neither /logout', () => {
      describe('when user is not authenticated', () => {

        beforeEach(inject([Store], (store: Store<AppState>) => {
          routerState = {
            url: '/home'
          };
          store
            .subscribe((state) => {
              (state as any).router = Object.create({});
              (state as any).router.state = routerState;
              (state as any).core = Object.create({});
              (state as any).core.auth = notAuthState;
            });

          // create component and test fixture
          fixture = TestBed.createComponent(AuthNavMenuComponent);

          // get test component from the fixture
          component = fixture.componentInstance;

          fixture.detectChanges();

          const navMenuSelector = '.navbar-nav';
          deNavMenu = fixture.debugElement.query(By.css(navMenuSelector));

          const navMenuItemSelector = 'li';
          deNavMenuItem = deNavMenu.query(By.css(navMenuItemSelector));
        }));

        afterEach(() => {
          fixture.destroy();
          component = null;
        });

        it('should render login dropdown menu', () => {
          const loginDropdownMenu = deNavMenuItem.query(By.css('div.loginDropdownMenu'));
          expect(loginDropdownMenu.nativeElement).toBeDefined();
        });
      });

      describe('when user is authenticated', () => {
        beforeEach(inject([Store], (store: Store<AppState>) => {
          routerState = {
            url: '/home'
          };
          store
            .subscribe((state) => {
              (state as any).router = Object.create({});
              (state as any).router.state = routerState;
              (state as any).core = Object.create({});
              (state as any).core.auth = authState;
            });

          // create component and test fixture
          fixture = TestBed.createComponent(AuthNavMenuComponent);

          // get test component from the fixture
          component = fixture.componentInstance;

          fixture.detectChanges();

          const navMenuSelector = '.navbar-nav';
          deNavMenu = fixture.debugElement.query(By.css(navMenuSelector));

          const navMenuItemSelector = 'li';
          deNavMenuItem = deNavMenu.query(By.css(navMenuItemSelector));
        }));

        afterEach(() => {
          fixture.destroy();
          component = null;
        });
        it('should render UserMenuComponent component', () => {
          const logoutDropdownMenu = deNavMenuItem.query(By.css('ds-user-menu'));
          expect(logoutDropdownMenu.nativeElement).toBeDefined();
        });
      });
    });
  });

  describe('when is a mobile view', () => {
    beforeEach(waitForAsync(() => {
      const window = new HostWindowServiceStub(300);
      serviceInit();

      // refine the test module by declaring the test component
      TestBed.configureTestingModule({
        imports: [
          NoopAnimationsModule,
          StoreModule.forRoot(authReducer, {
            runtimeChecks: {
              strictStateImmutability: false,
              strictActionImmutability: false
            }
          }),
          TranslateModule.forRoot()
        ],
        declarations: [
          AuthNavMenuComponent
        ],
        providers: [
          { provide: HostWindowService, useValue: window },
          { provide: AuthService, useValue: authService }
        ],
        schemas: [
          CUSTOM_ELEMENTS_SCHEMA
        ]
      })
        .compileComponents();

    }));

    beforeEach(() => {
      init();
    });
    describe('when user is not authenticated', () => {

      beforeEach(inject([Store], (store: Store<AppState>) => {
        store
          .subscribe((state) => {
            (state as any).router = Object.create({});
            (state as any).router.state = routerState;
            (state as any).core = Object.create({});
            (state as any).core.auth = notAuthState;
          });

        // create component and test fixture
        fixture = TestBed.createComponent(AuthNavMenuComponent);

        // get test component from the fixture
        component = fixture.componentInstance;

        fixture.detectChanges();

        const navMenuSelector = '.navbar-nav';
        deNavMenu = fixture.debugElement.query(By.css(navMenuSelector));

        const navMenuItemSelector = 'li';
        deNavMenuItem = deNavMenu.query(By.css(navMenuItemSelector));
      }));

      afterEach(() => {
        fixture.destroy();
        component = null;
      });

      it('should render login link', () => {
        const loginDropdownMenu = deNavMenuItem.query(By.css('.loginLink'));
        expect(loginDropdownMenu.nativeElement).toBeDefined();
      });
    });

    describe('when user is authenticated', () => {
      beforeEach(inject([Store], (store: Store<AppState>) => {
        store
          .subscribe((state) => {
            (state as any).router = Object.create({});
            (state as any).router.state = routerState;
            (state as any).core = Object.create({});
            (state as any).core.auth = authState;
          });

        // create component and test fixture
        fixture = TestBed.createComponent(AuthNavMenuComponent);

        // get test component from the fixture
        component = fixture.componentInstance;

        fixture.detectChanges();

        const navMenuSelector = '.navbar-nav';
        deNavMenu = fixture.debugElement.query(By.css(navMenuSelector));

        const navMenuItemSelector = 'li';
        deNavMenuItem = deNavMenu.query(By.css(navMenuItemSelector));
      }));

      afterEach(() => {
        fixture.destroy();
        component = null;
      });

      it('should render logout link', inject([Store], (store: Store<AppState>) => {
        const logoutDropdownMenu = deNavMenuItem.query(By.css('a[id=logoutLink]'));
        expect(logoutDropdownMenu.nativeElement).toBeDefined();
      }));
    });
  });
});
