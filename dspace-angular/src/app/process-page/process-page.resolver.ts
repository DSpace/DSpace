import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { RemoteData } from '../core/data/remote-data';
import { Observable } from 'rxjs';
import { Process } from './processes/process.model';
import { followLink } from '../shared/utils/follow-link-config.model';
import { ProcessDataService } from '../core/data/processes/process-data.service';
import { getFirstCompletedRemoteData } from '../core/shared/operators';

/**
 * This class represents a resolver that requests a specific process before the route is activated
 */
@Injectable()
export class ProcessPageResolver implements Resolve<RemoteData<Process>> {
  constructor(private processService: ProcessDataService) {
  }

  /**
   * Method for resolving a process based on the parameters in the current route
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns Observable<<RemoteData<Process>> Emits the found process based on the parameters in the current route,
   * or an error if something went wrong
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<RemoteData<Process>> {
    return this.processService.findById(route.params.id, false, true, followLink('script')).pipe(
      getFirstCompletedRemoteData(),
    );
  }
}
