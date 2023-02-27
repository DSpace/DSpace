import { InitService } from './init.service';
import { APP_CONFIG } from 'src/config/app-config.interface';
import { APP_INITIALIZER, Injectable } from '@angular/core';
import { inject, TestBed, waitForAsync } from '@angular/core/testing';
import { MetadataService } from './core/metadata/metadata.service';
import { BreadcrumbsService } from './breadcrumbs/breadcrumbs.service';
import { CommonModule } from '@angular/common';
import { Store, StoreModule } from '@ngrx/store';
import { authReducer } from './core/auth/auth.reducer';
import { storeModuleConfig } from './app.reducer';
import { AngularticsProviderMock } from './shared/mocks/angulartics-provider.service.mock';
import { Angulartics2DSpace } from './statistics/angulartics/dspace-provider';
import { AuthService } from './core/auth/auth.service';
import { AuthServiceMock } from './shared/mocks/auth.service.mock';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterMock } from './shared/mocks/router.mock';
import { MockActivatedRoute } from './shared/mocks/active-router.mock';
import { MenuService } from './shared/menu/menu.service';
import { LocaleService } from './core/locale/locale.service';
import { environment } from '../environments/environment';
import { provideMockStore } from '@ngrx/store/testing';
import { AppComponent } from './app.component';
import { RouteService } from './core/services/route.service';
import { getMockLocaleService } from './app.component.spec';
import { CorrelationIdService } from './correlation-id/correlation-id.service';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateLoaderMock } from './shared/mocks/translate-loader.mock';
import { ThemeService } from './shared/theme-support/theme.service';
import { getMockThemeService } from './shared/mocks/theme-service.mock';
import objectContaining = jasmine.objectContaining;
import createSpyObj = jasmine.createSpyObj;
import SpyObj = jasmine.SpyObj;
import { getTestScheduler } from 'jasmine-marbles';

let spy: SpyObj<any>;

@Injectable()
export class ConcreteInitServiceMock extends InitService {
  protected static resolveAppConfig() {
    spy.resolveAppConfig();
  }

  protected init(): () => Promise<boolean> {
    spy.init();
    return async () => true;
  }
}

const initialState = {
  core: {
    auth: {
      loading: false,
      blocking: true,
    }
  }
};


describe('InitService', () => {
  describe('providers', () => {
    beforeEach(() => {
      spy = createSpyObj('ConcreteInitServiceMock', {
        resolveAppConfig: null,
        init: null,
      });
    });

    it('should throw error when called on abstract InitService', () => {
      expect(() => InitService.providers()).toThrow();
    });

    it('should correctly set up provider dependencies', () => {
      const providers = ConcreteInitServiceMock.providers();

      expect(providers).toContain(objectContaining({
        provide: InitService,
        useClass: ConcreteInitServiceMock
      }));

      expect(providers).toContain(objectContaining({
        provide: APP_CONFIG,
      }));

      expect(providers).toContain(objectContaining({
        provide: APP_INITIALIZER,
        deps: [ InitService ],
        multi: true,
      }));
    });

    it('should call resolveAppConfig() in APP_CONFIG factory', () => {
      const factory = (
        ConcreteInitServiceMock.providers()
                               .find((p: any) => p.provide === APP_CONFIG) as any
      ).useFactory;

      // this factory is called _before_ InitService is instantiated
      factory();
      expect(spy.resolveAppConfig).toHaveBeenCalled();
      expect(spy.init).not.toHaveBeenCalled();
    });

    it('should defer to init() in APP_INITIALIZER factory', () => {
      const factory = (
        ConcreteInitServiceMock.providers()
                               .find((p: any) => p.provide === APP_INITIALIZER) as any
      ).useFactory;

      // we don't care about the dependencies here
      // @ts-ignore
      const instance = new ConcreteInitServiceMock(null, null, null);

      // provider ensures that the right concrete instance is passed to the factory
      factory(instance);
      expect(spy.resolveAppConfig).not.toHaveBeenCalled();
      expect(spy.init).toHaveBeenCalled();
    });
  });

  describe('common initialization steps', () => {
    let correlationIdServiceSpy;
    let dspaceTransferStateSpy;
    let transferStateSpy;
    let metadataServiceSpy;
    let breadcrumbsServiceSpy;
    let menuServiceSpy;

    const BLOCKING = {
      t: {  core: { auth: { blocking: true } } },
      f: {  core: { auth: { blocking: false } } },
    };
    const BOOLEAN = {
      t: true,
      f: false,
    };

    beforeEach(waitForAsync(() => {
      correlationIdServiceSpy = jasmine.createSpyObj('correlationIdServiceSpy', [
        'initCorrelationId',
      ]);
      dspaceTransferStateSpy = jasmine.createSpyObj('dspaceTransferStateSpy', [
        'transfer',
      ]);
      transferStateSpy = jasmine.createSpyObj('dspaceTransferStateSpy', [
        'get', 'hasKey'
      ]);
      breadcrumbsServiceSpy = jasmine.createSpyObj('breadcrumbsServiceSpy', [
        'listenForRouteChanges',
      ]);
      metadataServiceSpy = jasmine.createSpyObj('metadataService', [
        'listenForRouteChange',
      ]);
      menuServiceSpy = jasmine.createSpyObj('menuServiceSpy', [
        'listenForRouteChanges',
      ]);


      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
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
        providers: [
          { provide: InitService, useClass: ConcreteInitServiceMock },
          { provide: CorrelationIdService, useValue: correlationIdServiceSpy },
          { provide: APP_CONFIG, useValue: environment },
          { provide: LocaleService, useValue: getMockLocaleService() },
          { provide: Angulartics2DSpace, useValue: new AngularticsProviderMock() },
          { provide: MetadataService, useValue: metadataServiceSpy },
          { provide: BreadcrumbsService, useValue: breadcrumbsServiceSpy },
          { provide: AuthService, useValue: new AuthServiceMock() },
          { provide: Router, useValue: new RouterMock() },
          { provide: ActivatedRoute, useValue: new MockActivatedRoute() },
          { provide: MenuService, useValue: menuServiceSpy },
          { provide: ThemeService, useValue: getMockThemeService() },
          provideMockStore({ initialState }),
          AppComponent,
          RouteService,
        ]
      });
    }));

    describe('initRouteListeners', () => {
      it('should call listenForRouteChanges', inject([InitService], (service) => {
        // @ts-ignore
        service.initRouteListeners();
        expect(metadataServiceSpy.listenForRouteChange).toHaveBeenCalledTimes(1);
        expect(breadcrumbsServiceSpy.listenForRouteChanges).toHaveBeenCalledTimes(1);
        expect(breadcrumbsServiceSpy.listenForRouteChanges).toHaveBeenCalledTimes(1);
      }));
    });

    describe('authenticationReady', () => {
      it('should emit & complete the first time auth is unblocked', () => {
        getTestScheduler().run(({ cold, expectObservable }) => {
          TestBed.overrideProvider(Store, { useValue: cold('t--t--f--t--f--', BLOCKING) });
          const service = TestBed.inject(InitService);

          // @ts-ignore
          expectObservable(service.authenticationReady$()).toBe('------(f|)', BOOLEAN);
        });
      });
    });
  });
});

