import { Inject, Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AppConfig, APP_CONFIG } from '../../../config/app-config.interface';
import { isNotEmpty } from '../../shared/empty.util';

/**
 * A guard redirecting the user to the URL provided in the route's query params
 * When no redirect url is found, the user is redirected to the homepage
 */
@Injectable()
export class ReloadGuard implements CanActivate {
  constructor(
    private router: Router,
    @Inject(APP_CONFIG) private appConfig: AppConfig,
  ) {
  }

  /**
   * Get the UrlTree of the URL to redirect to
   * @param route
   * @param state
   */
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): UrlTree {
    if (isNotEmpty(route.queryParams.redirect)) {
      const url = route.queryParams.redirect.startsWith(this.appConfig.ui.nameSpace)
        ? route.queryParams.redirect.substring(this.appConfig.ui.nameSpace.length)
        : route.queryParams.redirect;
      return this.router.parseUrl(url);
    } else {
      return this.router.createUrlTree(['home']);
    }
  }
}
