import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Process } from './processes/process.model';
import { followLink } from '../shared/utils/follow-link-config.model';
import { ProcessDataService } from '../core/data/processes/process-data.service';
import { BreadcrumbConfig } from '../breadcrumbs/breadcrumb/breadcrumb-config.model';
import { getRemoteDataPayload, getFirstSucceededRemoteData } from '../core/shared/operators';
import { ProcessBreadcrumbsService } from './process-breadcrumbs.service';

/**
 * This class represents a resolver that requests a specific process before the route is activated
 */
@Injectable()
export class ProcessBreadcrumbResolver implements Resolve<BreadcrumbConfig<Process>> {
  constructor(protected breadcrumbService: ProcessBreadcrumbsService, private processService: ProcessDataService) {
  }

  /**
   * Method for resolving a process based on the parameters in the current route
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns Observable<<RemoteData<Process>> Emits the found process based on the parameters in the current route,
   * or an error if something went wrong
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<BreadcrumbConfig<Process>> {
    const id = route.params.id;

    return this.processService.findById(route.params.id, true, false, followLink('script')).pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
      map((object: Process) => {
        const fullPath = state.url;
        const url = fullPath.substr(0, fullPath.indexOf(id)) + id;
        return { provider: this.breadcrumbService, key: object, url: url };
      })
    );
  }
}
