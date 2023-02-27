import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { I18nBreadcrumbsService } from '../core/breadcrumbs/i18n-breadcrumbs.service';
import { BreadcrumbConfig } from '../breadcrumbs/breadcrumb/breadcrumb-config.model';
import { I18nBreadcrumbResolver } from '../core/breadcrumbs/i18n-breadcrumb.resolver';

/**
 * This class resolves a BreadcrumbConfig object with an i18n key string for a route
 * It adds the metadata field of the current browse-by page
 */
@Injectable()
export class BrowseByI18nBreadcrumbResolver extends I18nBreadcrumbResolver {
  constructor(protected breadcrumbService: I18nBreadcrumbsService) {
    super(breadcrumbService);
  }

  /**
   * Method for resolving a browse-by i18n breadcrumb configuration object
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns BreadcrumbConfig object for a browse-by page
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): BreadcrumbConfig<string> {
    const extendedBreadcrumbKey = route.data.breadcrumbKey + '.' + route.params.id;
    route.data = Object.assign({}, route.data, { breadcrumbKey: extendedBreadcrumbKey });
    return super.resolve(route, state);
  }
}
