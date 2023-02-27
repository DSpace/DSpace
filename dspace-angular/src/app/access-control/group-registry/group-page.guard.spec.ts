import { GroupPageGuard } from './group-page.guard';
import { HALEndpointService } from '../../core/shared/hal-endpoint.service';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of as observableOf } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { FeatureID } from '../../core/data/feature-authorization/feature-id';

describe('GroupPageGuard', () => {
  const groupsEndpointUrl = 'https://test.org/api/eperson/groups';
  const groupUuid = '0d6f89df-f95a-4829-943c-f21f434fb892';
  const groupEndpointUrl = `${groupsEndpointUrl}/${groupUuid}`;
  const routeSnapshotWithGroupId = {
    params: {
      groupId: groupUuid,
    }
  } as unknown as ActivatedRouteSnapshot;

  let guard: GroupPageGuard;
  let halEndpointService: HALEndpointService;
  let authorizationService: AuthorizationDataService;
  let router: Router;
  let authService: AuthService;

  beforeEach(() => {
    halEndpointService = jasmine.createSpyObj(['getEndpoint']);
    (halEndpointService as any).getEndpoint.and.returnValue(observableOf(groupsEndpointUrl));

    authorizationService = jasmine.createSpyObj(['isAuthorized']);
    // NOTE: value is set in beforeEach

    router = jasmine.createSpyObj(['parseUrl']);
    (router as any).parseUrl.and.returnValue = {};

    authService = jasmine.createSpyObj(['isAuthenticated']);
    (authService as any).isAuthenticated.and.returnValue(observableOf(true));

    guard = new GroupPageGuard(halEndpointService, authorizationService, router, authService);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  describe('canActivate', () => {
    describe('when the current user can manage the group', () => {
      beforeEach(() => {
        (authorizationService as any).isAuthorized.and.returnValue(observableOf(true));
      });

      it('should return true', (done) => {
        guard.canActivate(
          routeSnapshotWithGroupId, { url: 'current-url'} as any
        ).subscribe((result) => {
          expect(authorizationService.isAuthorized).toHaveBeenCalledWith(
            FeatureID.CanManageGroup, groupEndpointUrl, undefined
          );
          expect(result).toBeTrue();
          done();
        });
      });
    });

    describe('when the current user can not manage the group', () => {
      beforeEach(() => {
        (authorizationService as any).isAuthorized.and.returnValue(observableOf(false));
      });

      it('should not return true', (done) => {
        guard.canActivate(
          routeSnapshotWithGroupId, { url: 'current-url'} as any
        ).subscribe((result) => {
          expect(authorizationService.isAuthorized).toHaveBeenCalledWith(
            FeatureID.CanManageGroup, groupEndpointUrl, undefined
          );
          expect(result).not.toBeTrue();
          done();
        });
      });
    });
  });

});
