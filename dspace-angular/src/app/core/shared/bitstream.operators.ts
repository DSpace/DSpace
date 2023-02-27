import { AuthorizationDataService } from '../data/feature-authorization/authorization-data.service';
import { Observable, of as observableOf } from 'rxjs';
import { Bitstream } from './bitstream.model';
import { map, switchMap } from 'rxjs/operators';
import { hasValue } from '../../shared/empty.util';
import { FeatureID } from '../data/feature-authorization/feature-id';

/**
 * Operator to check if the given bitstream is downloadable
 */
export const getDownloadableBitstream = (authService: AuthorizationDataService) =>
  (source: Observable<Bitstream>): Observable<Bitstream | null> =>
    source.pipe(
      switchMap((bit: Bitstream) => {
        if (hasValue(bit)) {
          return authService.isAuthorized(FeatureID.CanDownload, bit.self).pipe(
            map((canDownload: boolean) => {
              return canDownload ? bit : null;
            }));
        } else {
          return observableOf(null);
        }
      })
    );
