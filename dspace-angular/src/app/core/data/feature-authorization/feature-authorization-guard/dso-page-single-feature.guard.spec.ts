import { AuthorizationDataService } from '../authorization-data.service';
import { ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router';
import { RemoteData } from '../../remote-data';
import { Observable, of as observableOf } from 'rxjs';
import { createSuccessfulRemoteDataObject$ } from '../../../../shared/remote-data.utils';
import { DSpaceObject } from '../../../shared/dspace-object.model';
import { DsoPageSingleFeatureGuard } from './dso-page-single-feature.guard';
import { FeatureID } from '../feature-id';
import { AuthService } from '../../../auth/auth.service';

/**
 * Test implementation of abstract class DsoPageSingleFeatureGuard
 */
class DsoPageSingleFeatureGuardImpl extends DsoPageSingleFeatureGuard<any> {
  constructor(protected resolver: Resolve<RemoteData<any>>,
              protected authorizationService: AuthorizationDataService,
              protected router: Router,
              protected authService: AuthService,
              protected featureID: FeatureID) {
    super(resolver, authorizationService, router, authService);
  }

  getFeatureID(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<FeatureID> {
    return observableOf(this.featureID);
  }
}

describe('DsoPageSingleFeatureGuard', () => {
  let guard: DsoPageSingleFeatureGuard<any>;
  let authorizationService: AuthorizationDataService;
  let router: Router;
  let authService: AuthService;
  let resolver: Resolve<RemoteData<any>>;
  let object: DSpaceObject;
  let route;
  let parentRoute;

  function init() {
    object = {
      self: 'test-selflink'
    } as DSpaceObject;

    authorizationService = jasmine.createSpyObj('authorizationService', {
      isAuthorized: observableOf(true)
    });
    router = jasmine.createSpyObj('router', {
      parseUrl: {}
    });
    resolver = jasmine.createSpyObj('resolver', {
      resolve: createSuccessfulRemoteDataObject$(object)
    });
    authService = jasmine.createSpyObj('authService', {
      isAuthenticated: observableOf(true)
    });
    parentRoute = {
      params: {
        id: '3e1a5327-dabb-41ff-af93-e6cab9d032f0'
      }
    };
    route = {
      params: {
      },
      parent: parentRoute
    };
    guard = new DsoPageSingleFeatureGuardImpl(resolver, authorizationService, router, authService, undefined);
  }

  beforeEach(() => {
    init();
  });

  describe('getObjectUrl', () => {
    it('should return the resolved object\'s selflink', (done) => {
      guard.getObjectUrl(route, undefined).subscribe((selflink) => {
        expect(selflink).toEqual(object.self);
        done();
      });
    });
  });

  describe('getRouteWithDSOId', () => {
    it('should return the route that has the UUID of the DSO', () => {
      const foundRoute = (guard as any).getRouteWithDSOId(route);
      expect(foundRoute).toBe(parentRoute);
    });
  });
});
