import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { Community } from '../core/shared/community.model';
import { CommunityPageResolver } from './community-page.resolver';
import { AuthorizationDataService } from '../core/data/feature-authorization/authorization-data.service';
import { Observable, of as observableOf } from 'rxjs';
import { DsoPageSingleFeatureGuard } from '../core/data/feature-authorization/feature-authorization-guard/dso-page-single-feature.guard';
import { FeatureID } from '../core/data/feature-authorization/feature-id';
import { AuthService } from '../core/auth/auth.service';

@Injectable({
  providedIn: 'root'
})
/**
 * Guard for preventing unauthorized access to certain {@link Community} pages requiring administrator rights
 */
export class CommunityPageAdministratorGuard extends DsoPageSingleFeatureGuard<Community> {
  constructor(protected resolver: CommunityPageResolver,
              protected authorizationService: AuthorizationDataService,
              protected router: Router,
              protected authService: AuthService) {
    super(resolver, authorizationService, router, authService);
  }

  /**
   * Check administrator authorization rights
   */
  getFeatureID(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<FeatureID> {
    return observableOf(FeatureID.AdministratorOf);
  }
}
