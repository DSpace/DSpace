import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { Observable, of as observableOf } from 'rxjs';
import { FeatureID } from '../../core/data/feature-authorization/feature-id';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { AuthService } from '../../core/auth/auth.service';
import { SomeFeatureAuthorizationGuard } from '../../core/data/feature-authorization/feature-authorization-guard/some-feature-authorization.guard';
import { HALEndpointService } from '../../core/shared/hal-endpoint.service';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class GroupPageGuard extends SomeFeatureAuthorizationGuard {

  protected groupsEndpoint = 'groups';

  constructor(protected halEndpointService: HALEndpointService,
              protected authorizationService: AuthorizationDataService,
              protected router: Router,
              protected authService: AuthService) {
    super(authorizationService, router, authService);
  }

  getFeatureIDs(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<FeatureID[]> {
    return observableOf([FeatureID.CanManageGroup]);
  }

  getObjectUrl(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<string> {
    return this.halEndpointService.getEndpoint(this.groupsEndpoint).pipe(
      map(groupsUrl => `${groupsUrl}/${route?.params?.groupId}`)
    );
  }

}
