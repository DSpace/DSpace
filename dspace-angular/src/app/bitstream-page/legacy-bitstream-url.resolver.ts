import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { RemoteData } from '../core/data/remote-data';
import { Bitstream } from '../core/shared/bitstream.model';
import { getFirstCompletedRemoteData } from '../core/shared/operators';
import { hasNoValue } from '../shared/empty.util';
import { BitstreamDataService } from '../core/data/bitstream-data.service';

/**
 * This class resolves a bitstream based on the DSpace 6 XMLUI or JSPUI bitstream download URLs
 */
@Injectable({
  providedIn: 'root'
})
export class LegacyBitstreamUrlResolver implements Resolve<RemoteData<Bitstream>> {
  constructor(protected bitstreamDataService: BitstreamDataService) {
  }

  /**
   * Resolve a bitstream based on the handle of the item, and the sequence id or the filename of the
   * bitstream
   *
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns Observable<<RemoteData<Item>> Emits the found bitstream based on the parameters in
   * current route, or an error if something went wrong
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot):
    Observable<RemoteData<Bitstream>> {
      const prefix = route.params.prefix;
      const suffix = route.params.suffix;
      const filename = route.params.filename;

      let sequenceId = route.params.sequence_id;
      if (hasNoValue(sequenceId)) {
        sequenceId = route.queryParams.sequenceId;
      }

      return this.bitstreamDataService.findByItemHandle(
        `${prefix}/${suffix}`,
        sequenceId,
        filename,
      ).pipe(
        getFirstCompletedRemoteData()
      );
  }
}
