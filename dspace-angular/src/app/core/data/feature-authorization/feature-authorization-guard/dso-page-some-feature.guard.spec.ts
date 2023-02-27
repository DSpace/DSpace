import { AuthorizationDataService } from '../authorization-data.service';
import { ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router';
import { RemoteData } from '../../remote-data';
import { Observable, of as observableOf } from 'rxjs';
import { createSuccessfulRemoteDataObject$ } from '../../../../shared/remote-data.utils';
import { DSpaceObject } from '../../../shared/dspace-object.model';
import { FeatureID } from '../feature-id';
import { AuthService } from '../../../auth/auth.service';
import { DsoPageSomeFeatureGuard } from './dso-page-some-feature.guard';

/**
 * Test implementation of abstract class DsoPageSomeFeatureGuard
 */
class DsoPageSomeFeatureGuardImpl extends DsoPageSomeFeatureGuard<any> {
  constructor(protected resolver: Resolve<RemoteData<any>>,
              protected authorizationService: AuthorizationDataService,
              protected router: Router,
              protected authService: AuthService,
              protected featureIDs: FeatureID[]) {
    super(resolver, authorizationService, router, authService);
  }

  getFeatureIDs(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<FeatureID[]> {
    return observableOf(this.featureIDs);
  }
}

describe('DsoPageSomeFeatureGuard', () => {
  let guard: DsoPageSomeFeatureGuard<any>;
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
    guard = new DsoPageSomeFeatureGuardImpl(resolver, authorizationService, router, authService, []);
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
