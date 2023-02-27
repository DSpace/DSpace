import { DSpaceObject } from '../../../shared/dspace-object.model';
import { DsoPageSomeFeatureGuard } from './dso-page-some-feature.guard';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { FeatureID } from '../feature-id';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

/**
 * Abstract Guard for preventing unauthorized access to {@link DSpaceObject} pages that require rights for a specific feature
 * This guard utilizes a resolver to retrieve the relevant object to check authorizations for
 */
export abstract class DsoPageSingleFeatureGuard<T extends DSpaceObject> extends DsoPageSomeFeatureGuard<T> {
  /**
   * The features to check authorization for
   */
  getFeatureIDs(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<FeatureID[]> {
    return this.getFeatureID(route, state).pipe(
      map((featureID) => [featureID]),
    );
  }

  /**
   * The type of feature to check authorization for
   * Override this method to define a feature
   */
  abstract getFeatureID(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<FeatureID>;
}
