import { Store, StoreModule } from '@ngrx/store';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

// Load the implementations that should be tested
import { AppComponent } from './app.component';
import { HostWindowState } from './shared/search/host-window.reducer';
import { HostWindowResizeAction } from './shared/host-window.actions';
import { MetadataService } from './core/metadata/metadata.service';

import { NativeWindowRef, NativeWindowService } from './core/services/window.service';
import { TranslateLoaderMock } from './shared/mocks/translate-loader.mock';
import { MetadataServiceMock } from './shared/mocks/metadata-service.mock';
import { AngularticsProviderMock } from './shared/mocks/angulartics-provider.service.mock';
import { AuthServiceMock } from './shared/mocks/auth.service.mock';
import { AuthService } from './core/auth/auth.service';
import { MenuService } from './shared/menu/menu.service';
import { CSSVariableService } from './shared/sass-helper/css-variable.service';
import { CSSVariableServiceStub } from './shared/testing/css-variable-service.stub';
import { MenuServiceStub } from './shared/testing/menu-service.stub';
import { HostWindowService } from './shared/host-window.service';
import { HostWindowServiceStub } from './shared/testing/host-window-service.stub';
import { RouteService } from './core/services/route.service';
import { MockActivatedRoute } from './shared/mocks/active-router.mock';
import { RouterMock } from './shared/mocks/router.mock';
import { Angulartics2DSpace } from './statistics/angulartics/dspace-provider';
import { storeModuleConfig } from './app.reducer';
import { LocaleService } from './core/locale/locale.service';
import { authReducer } from './core/auth/auth.reducer';
import { provideMockStore } from '@ngrx/store/testing';
import { ThemeService } from './shared/theme-support/theme.service';
import { getMockThemeService } from './shared/mocks/theme-service.mock';
import { BreadcrumbsService } from './breadcrumbs/breadcrumbs.service';
import { APP_CONFIG } from '../config/app-config.interface';
import { environment } from '../environments/environment';

let comp: AppComponent;
let fixture: ComponentFixture<AppComponent>;
const menuService = new MenuServiceStub();
const initialState = {
  core: { auth: { loading: false } }
};

export function getMockLocaleService(): LocaleService {
  return jasmine.createSpyObj('LocaleService', {
    setCurrentLanguageCode: jasmine.createSpy('setCurrentLanguageCode')
  });
}

describe('App component', () => {

  let breadcrumbsServiceSpy;

  const getDefaultTestBedConf = () => {
    breadcrumbsServiceSpy = jasmine.createSpyObj(['listenForRouteChanges']);

    return {
      imports: [
        CommonModule,
        StoreModule.forRoot(authReducer, storeModuleConfig),
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
      ],
      declarations: [AppComponent], // declare the test component
      providers: [
        { provide: NativeWindowService, useValue: new NativeWindowRef() },
        { provide: MetadataService, useValue: new MetadataServiceMock() },
        { provide: Angulartics2DSpace, useValue: new AngularticsProviderMock() },
        { provide: AuthService, useValue: new AuthServiceMock() },
        { provide: Router, useValue: new RouterMock() },
        { provide: ActivatedRoute, useValue: new MockActivatedRoute() },
        { provide: MenuService, useValue: menuService },
        { provide: CSSVariableService, useClass: CSSVariableServiceStub },
        { provide: HostWindowService, useValue: new HostWindowServiceStub(800) },
        { provide: LocaleService, useValue: getMockLocaleService() },
        { provide: ThemeService, useValue: getMockThemeService() },
        { provide: BreadcrumbsService, useValue: breadcrumbsServiceSpy },
        { provide: APP_CONFIG, useValue: environment },
        provideMockStore({ initialState }),
        AppComponent,
        RouteService
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    };
  };

  // waitForAsync beforeEach
  beforeEach(waitForAsync(() => {
    return TestBed.configureTestingModule(getDefaultTestBedConf());
  }));

  // synchronous beforeEach
  beforeEach(() => {
    fixture = TestBed.createComponent(AppComponent);
    comp = fixture.componentInstance; // component test instance
    fixture.detectChanges();
  });

  it('should create component', inject([AppComponent], (app: AppComponent) => {
    // Perform test using fixture and service
    expect(app).toBeTruthy();
  }));

  describe('when the window is resized', () => {
    let width: number;
    let height: number;
    let store: Store<HostWindowState>;

    beforeEach(() => {
      store = fixture.debugElement.injector.get(Store) as Store<HostWindowState>;
      spyOn(store, 'dispatch');

      window.dispatchEvent(new Event('resize'));
      width = window.innerWidth;
      height = window.innerHeight;
    });

    it('should dispatch a HostWindowResizeAction with the width and height of the window as its payload', () => {
      expect(store.dispatch).toHaveBeenCalledWith(new HostWindowResizeAction(width, height));
    });

  });
});
