import { SingleFeatureAuthorizationGuard } from './single-feature-authorization.guard';
import { AuthorizationDataService } from '../authorization-data.service';
import { FeatureID } from '../feature-id';
import { Observable, of as observableOf } from 'rxjs';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';

/**
 * Test implementation of abstract class SingleFeatureAuthorizationGuard
 * Provide the return values of the overwritten getters as constructor arguments
 */
class SingleFeatureAuthorizationGuardImpl extends SingleFeatureAuthorizationGuard {
  constructor(protected authorizationService: AuthorizationDataService,
              protected router: Router,
              protected authService: AuthService,
              protected featureId: FeatureID,
              protected objectUrl: string,
              protected ePersonUuid: string) {
    super(authorizationService, router, authService);
  }

  getFeatureID(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<FeatureID> {
    return observableOf(this.featureId);
  }

  getObjectUrl(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<string> {
    return observableOf(this.objectUrl);
  }

  getEPersonUuid(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<string> {
    return observableOf(this.ePersonUuid);
  }
}

describe('SingleFeatureAuthorizationGuard', () => {
  let guard: SingleFeatureAuthorizationGuard;
  let authorizationService: AuthorizationDataService;
  let router: Router;
  let authService: AuthService;

  let featureId: FeatureID;
  let objectUrl: string;
  let ePersonUuid: string;

  function init() {
    featureId = FeatureID.LoginOnBehalfOf;
    objectUrl = 'fake-object-url';
    ePersonUuid = 'fake-eperson-uuid';

    authorizationService = jasmine.createSpyObj('authorizationService', {
      isAuthorized: observableOf(true)
    });
    router = jasmine.createSpyObj('router', {
      parseUrl: {}
    });
    authService = jasmine.createSpyObj('authService', {
      isAuthenticated: observableOf(true)
    });
    guard = new SingleFeatureAuthorizationGuardImpl(authorizationService, router, authService, featureId, objectUrl, ePersonUuid);
  }

  beforeEach(() => {
    init();
  });

  describe('canActivate', () => {
    it('should call authorizationService.isAuthenticated with the appropriate arguments', () => {
      guard.canActivate(undefined, { url: 'current-url' } as any).subscribe();
      expect(authorizationService.isAuthorized).toHaveBeenCalledWith(featureId, objectUrl, ePersonUuid);
    });
  });
});
