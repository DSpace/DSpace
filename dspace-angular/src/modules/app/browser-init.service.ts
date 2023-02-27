/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { InitService } from '../../app/init.service';
import { Store } from '@ngrx/store';
import { AppState } from '../../app/app.reducer';
import { TransferState } from '@angular/platform-browser';
import { APP_CONFIG, APP_CONFIG_STATE, AppConfig } from '../../config/app-config.interface';
import { DefaultAppConfig } from '../../config/default-app-config';
import { extendEnvironmentWithAppConfig } from '../../config/config.util';
import { environment } from '../../environments/environment';
import { CorrelationIdService } from '../../app/correlation-id/correlation-id.service';
import { Inject, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { LocaleService } from '../../app/core/locale/locale.service';
import { Angulartics2DSpace } from '../../app/statistics/angulartics/dspace-provider';
import { GoogleAnalyticsService } from '../../app/statistics/google-analytics.service';
import { MetadataService } from '../../app/core/metadata/metadata.service';
import { BreadcrumbsService } from '../../app/breadcrumbs/breadcrumbs.service';
import { KlaroService } from '../../app/shared/cookies/klaro.service';
import { AuthService } from '../../app/core/auth/auth.service';
import { ThemeService } from '../../app/shared/theme-support/theme.service';
import { StoreAction, StoreActionTypes } from '../../app/store.actions';
import { coreSelector } from '../../app/core/core.selectors';
import { find, map } from 'rxjs/operators';
import { isNotEmpty } from '../../app/shared/empty.util';
import { logStartupMessage } from '../../../startup-message';
import { MenuService } from '../../app/shared/menu/menu.service';

/**
 * Performs client-side initialization.
 */
@Injectable()
export class BrowserInitService extends InitService {
  constructor(
    protected store: Store<AppState>,
    protected correlationIdService: CorrelationIdService,
    protected transferState: TransferState,
    @Inject(APP_CONFIG) protected appConfig: AppConfig,
    protected translate: TranslateService,
    protected localeService: LocaleService,
    protected angulartics2DSpace: Angulartics2DSpace,
    protected googleAnalyticsService: GoogleAnalyticsService,
    protected metadata: MetadataService,
    protected breadcrumbsService: BreadcrumbsService,
    protected klaroService: KlaroService,
    protected authService: AuthService,
    protected themeService: ThemeService,
    protected menuService: MenuService,
  ) {
    super(
      store,
      correlationIdService,
      appConfig,
      translate,
      localeService,
      angulartics2DSpace,
      metadata,
      breadcrumbsService,
      themeService,
      menuService,
    );
  }

  protected static resolveAppConfig(
    transferState: TransferState,
  ) {
    if (transferState.hasKey<AppConfig>(APP_CONFIG_STATE)) {
      const appConfig = transferState.get<AppConfig>(APP_CONFIG_STATE, new DefaultAppConfig());
      // extend environment with app config for browser
      extendEnvironmentWithAppConfig(environment, appConfig);
    }
  }

  protected init(): () => Promise<boolean> {
    return async () => {
      await this.loadAppState();
      this.checkAuthenticationToken();
      this.initCorrelationId();

      this.checkEnvironment();
      logStartupMessage(environment);

      this.initI18n();
      this.initAngulartics();
      this.initGoogleAnalytics();
      this.initRouteListeners();
      this.themeService.listenForThemeChanges(true);
      this.trackAuthTokenExpiration();

      this.initKlaro();

      await this.authenticationReady$().toPromise();

      return true;
    };
  }

  // Browser-only initialization steps

  /**
   * Retrieve server-side application state from the {@link NGRX_STATE} key and rehydrate the store.
   * Resolves once the store is no longer empty.
   * @private
   */
  private async loadAppState(): Promise<boolean> {
    const state = this.transferState.get<any>(InitService.NGRX_STATE, null);
    this.transferState.remove(InitService.NGRX_STATE);
    this.store.dispatch(new StoreAction(StoreActionTypes.REHYDRATE, state));
    return this.store.select(coreSelector).pipe(
      find((core: any) => isNotEmpty(core)),
      map(() => true)
    ).toPromise();
  }

  private trackAuthTokenExpiration(): void {
    this.authService.trackTokenExpiration();
  }

  /**
   * Initialize Klaro (once authentication is resolved)
   * @protected
   */
  protected initKlaro() {
    this.authenticationReady$().subscribe(() => {
      this.klaroService.initialize();
    });
  }

  protected initGoogleAnalytics() {
    this.googleAnalyticsService.addTrackingIdToPage();
  }
}
