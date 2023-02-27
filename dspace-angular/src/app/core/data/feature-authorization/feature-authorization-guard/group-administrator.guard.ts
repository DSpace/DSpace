import { Injectable } from '@angular/core';
import { SingleFeatureAuthorizationGuard } from './single-feature-authorization.guard';
import { AuthorizationDataService } from '../authorization-data.service';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { Observable, of as observableOf } from 'rxjs';
import { FeatureID } from '../feature-id';

/**
 * Prevent unauthorized activating and loading of routes when the current authenticated user doesn't have group
 * management rights
 */
@Injectable({
  providedIn: 'root'
})
export class GroupAdministratorGuard extends SingleFeatureAuthorizationGuard {
  constructor(protected authorizationService: AuthorizationDataService, protected router: Router, protected authService: AuthService) {
    super(authorizationService, router, authService);
  }

  /**
   * Check group management rights
   */
  getFeatureID(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<FeatureID> {
    return observableOf(FeatureID.CanManageGroups);
  }
}
