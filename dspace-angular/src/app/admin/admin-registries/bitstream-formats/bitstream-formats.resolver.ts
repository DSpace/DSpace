import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { RemoteData } from '../../../core/data/remote-data';
import { BitstreamFormat } from '../../../core/shared/bitstream-format.model';
import { BitstreamFormatDataService } from '../../../core/data/bitstream-format-data.service';
import { getFirstCompletedRemoteData } from '../../../core/shared/operators';

/**
 * This class represents a resolver that requests a specific bitstreamFormat before the route is activated
 */
@Injectable()
export class BitstreamFormatsResolver implements Resolve<RemoteData<BitstreamFormat>> {
  constructor(private bitstreamFormatDataService: BitstreamFormatDataService) {
  }

  /**
   * Method for resolving an bitstreamFormat based on the parameters in the current route
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns Observable<<RemoteData<BitstreamFormat>> Emits the found bitstreamFormat based on the parameters in the current route,
   * or an error if something went wrong
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<RemoteData<BitstreamFormat>> {
    return this.bitstreamFormatDataService.findById(route.params.id)
      .pipe(
        getFirstCompletedRemoteData()
      );
  }
}
