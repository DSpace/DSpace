import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { FeatureID } from '../feature-id';
import { Observable } from 'rxjs';
import { map} from 'rxjs/operators';
import { SomeFeatureAuthorizationGuard } from './some-feature-authorization.guard';

/**
 * Abstract Guard for preventing unauthorized activating and loading of routes when a user
 * doesn't have authorized rights on a specific feature and/or object.
 * Override the desired getters in the parent class for checking specific authorization on a feature and/or object.
 */
export abstract class SingleFeatureAuthorizationGuard extends SomeFeatureAuthorizationGuard {
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
