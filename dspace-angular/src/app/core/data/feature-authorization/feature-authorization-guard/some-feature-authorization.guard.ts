import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthorizationDataService } from '../authorization-data.service';
import { FeatureID } from '../feature-id';
import { combineLatest as observableCombineLatest, Observable, of as observableOf } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { AuthService } from '../../../auth/auth.service';
import { returnForbiddenUrlTreeOrLoginOnAllFalse } from '../../../shared/authorized.operators';

/**
 * Abstract Guard for preventing unauthorized activating and loading of routes when a user
 * doesn't have authorized rights on any of the specified features and/or object.
 * Override the desired getters in the parent class for checking specific authorization on a list of features and/or object.
 */
export abstract class SomeFeatureAuthorizationGuard implements CanActivate {
  constructor(protected authorizationService: AuthorizationDataService,
              protected router: Router,
              protected authService: AuthService) {
  }

  /**
   * True when user has authorization rights for the feature and object provided
   * Redirect the user to the unauthorized page when they are not authorized for the given feature
   */
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
    return observableCombineLatest(this.getFeatureIDs(route, state), this.getObjectUrl(route, state), this.getEPersonUuid(route, state)).pipe(
      switchMap(([featureIDs, objectUrl, ePersonUuid]) =>
        observableCombineLatest(...featureIDs.map((featureID) => this.authorizationService.isAuthorized(featureID, objectUrl, ePersonUuid)))
      ),
      returnForbiddenUrlTreeOrLoginOnAllFalse(this.router, this.authService, state.url)
    );
  }

  /**
   * The features to check authorization for
   * Override this method to define a list of features
   */
  abstract getFeatureIDs(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<FeatureID[]>;

  /**
   * The URL of the object to check if the user has authorized rights for
   * Override this method to define an object URL. If not provided, the {@link Site}'s URL will be used
   */
  getObjectUrl(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<string> {
    return observableOf(undefined);
  }

  /**
   * The UUID of the user to check authorization rights for
   * Override this method to define an {@link EPerson} UUID. If not provided, the authenticated user's UUID will be used.
   */
  getEPersonUuid(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<string> {
    return observableOf(undefined);
  }
}
