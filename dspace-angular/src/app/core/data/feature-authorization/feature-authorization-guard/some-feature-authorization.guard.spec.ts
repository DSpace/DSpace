import { AuthorizationDataService } from '../authorization-data.service';
import { FeatureID } from '../feature-id';
import { Observable, of as observableOf } from 'rxjs';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { SomeFeatureAuthorizationGuard } from './some-feature-authorization.guard';

/**
 * Test implementation of abstract class SomeFeatureAuthorizationGuard
 * Provide the return values of the overwritten getters as constructor arguments
 */
class SomeFeatureAuthorizationGuardImpl extends SomeFeatureAuthorizationGuard {
  constructor(protected authorizationService: AuthorizationDataService,
              protected router: Router,
              protected authService: AuthService,
              protected featureIds: FeatureID[],
              protected objectUrl: string,
              protected ePersonUuid: string) {
    super(authorizationService, router, authService);
  }

  getFeatureIDs(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<FeatureID[]> {
    return observableOf(this.featureIds);
  }

  getObjectUrl(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<string> {
    return observableOf(this.objectUrl);
  }

  getEPersonUuid(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<string> {
    return observableOf(this.ePersonUuid);
  }
}

describe('SomeFeatureAuthorizationGuard', () => {
  let guard: SomeFeatureAuthorizationGuard;
  let authorizationService: AuthorizationDataService;
  let router: Router;
  let authService: AuthService;

  let featureIds: FeatureID[];
  let authorizedFeatureIds: FeatureID[];
  let objectUrl: string;
  let ePersonUuid: string;

  function init() {
    featureIds = [FeatureID.LoginOnBehalfOf, FeatureID.CanDelete];
    authorizedFeatureIds = [];
    objectUrl = 'fake-object-url';
    ePersonUuid = 'fake-eperson-uuid';

    authorizationService = Object.assign({
      isAuthorized(featureId?: FeatureID): Observable<boolean> {
        return observableOf(authorizedFeatureIds.indexOf(featureId) > -1);
      }
    });
    router = jasmine.createSpyObj('router', {
      parseUrl: {}
    });
    authService = jasmine.createSpyObj('authService', {
      isAuthenticated: observableOf(true)
    });
    guard = new SomeFeatureAuthorizationGuardImpl(authorizationService, router, authService, featureIds, objectUrl, ePersonUuid);
  }

  beforeEach(() => {
    init();
  });

  describe('canActivate', () => {
    describe('when the user isn\'t authorized', () => {
      beforeEach(() => {
        authorizedFeatureIds = [];
      });

      it('should not return true', (done) => {
        guard.canActivate(undefined, { url: 'current-url' } as any).subscribe((result) => {
          expect(result).not.toEqual(true);
          done();
        });
      });
    });

    describe('when the user is authorized for at least one of the guard\'s features', () => {
      beforeEach(() => {
        authorizedFeatureIds = [featureIds[0]];
      });

      it('should return true', (done) => {
        guard.canActivate(undefined, { url: 'current-url' } as any).subscribe((result) => {
          expect(result).toEqual(true);
          done();
        });
      });
    });

    describe('when the user is authorized for all of the guard\'s features', () => {
      beforeEach(() => {
        authorizedFeatureIds = featureIds;
      });

      it('should return true', (done) => {
        guard.canActivate(undefined, { url: 'current-url' } as any).subscribe((result) => {
          expect(result).toEqual(true);
          done();
        });
      });
    });
  });
});
