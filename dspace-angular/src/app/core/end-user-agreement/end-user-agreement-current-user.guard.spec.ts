import { EndUserAgreementCurrentUserGuard } from './end-user-agreement-current-user.guard';
import { EndUserAgreementService } from './end-user-agreement.service';
import { Router, UrlTree } from '@angular/router';
import { of as observableOf } from 'rxjs';
import { environment } from '../../../environments/environment.test';

describe('EndUserAgreementGuard', () => {
  let guard: EndUserAgreementCurrentUserGuard;

  let endUserAgreementService: EndUserAgreementService;
  let router: Router;

  beforeEach(() => {
    endUserAgreementService = jasmine.createSpyObj('endUserAgreementService', {
      hasCurrentUserAcceptedAgreement: observableOf(true)
    });
    router = jasmine.createSpyObj('router', {
      navigateByUrl: {},
      parseUrl: new UrlTree(),
      createUrlTree: new UrlTree()
    });

    guard = new EndUserAgreementCurrentUserGuard(endUserAgreementService, router);
  });

  describe('canActivate', () => {
    describe('when the user has accepted the agreement', () => {
      it('should return true', (done) => {
        guard.canActivate(undefined, Object.assign({ url: 'redirect' })).subscribe((result) => {
          expect(result).toEqual(true);
          done();
        });
      });
    });

    describe('when the user hasn\'t accepted the agreement', () => {
      beforeEach(() => {
        (endUserAgreementService.hasCurrentUserAcceptedAgreement as jasmine.Spy).and.returnValue(observableOf(false));
      });

      it('should return a UrlTree', (done) => {
        guard.canActivate(undefined, Object.assign({ url: 'redirect' })).subscribe((result) => {
          expect(result).toEqual(jasmine.any(UrlTree));
          done();
        });
      });
    });

    describe('when the end user agreement is disabled', () => {
      it('should return true', (done) => {
        environment.info.enableEndUserAgreement = false;
        guard.canActivate(undefined, Object.assign({ url: 'redirect' })).subscribe((result) => {
          expect(result).toEqual(true);
          done();
        });
      });

      it('should not resolve to the end user agreement page', (done) => {
        environment.info.enableEndUserAgreement = false;
        guard.canActivate(undefined, Object.assign({ url: 'redirect' })).subscribe((result) => {
          expect(router.navigateByUrl).not.toHaveBeenCalled();
          done();
        });
      });
    });
  });
});
