import { Injectable } from '@angular/core';
import { Community } from '../core/shared/community.model';
import { DSpaceObjectDataService } from '../core/data/dspace-object-data.service';
import { DSOBreadcrumbsService } from '../core/breadcrumbs/dso-breadcrumbs.service';
import { Collection } from '../core/shared/collection.model';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { BreadcrumbConfig } from '../breadcrumbs/breadcrumb/breadcrumb-config.model';
import { Observable } from 'rxjs';
import { getRemoteDataPayload, getFirstSucceededRemoteData } from '../core/shared/operators';
import { map } from 'rxjs/operators';
import { hasValue } from '../shared/empty.util';
import { getDSORoute } from '../app-routing-paths';

/**
 * The class that resolves the BreadcrumbConfig object for a DSpaceObject on a browse by page
 */
@Injectable()
export class BrowseByDSOBreadcrumbResolver {
  constructor(protected breadcrumbService: DSOBreadcrumbsService, protected dataService: DSpaceObjectDataService) {
  }

  /**
   * Method for resolving a breadcrumb config object
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns BreadcrumbConfig object
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<BreadcrumbConfig<Community | Collection>> {
    const uuid = route.queryParams.scope;
    if (hasValue(uuid)) {
      return this.dataService.findById(uuid).pipe(
        getFirstSucceededRemoteData(),
        getRemoteDataPayload(),
        map((object: Community | Collection) => {
          return { provider: this.breadcrumbService, key: object, url: getDSORoute(object) };
        })
      );
    }
    return undefined;
  }
}
