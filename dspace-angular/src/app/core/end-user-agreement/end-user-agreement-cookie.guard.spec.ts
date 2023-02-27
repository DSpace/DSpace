import { EndUserAgreementService } from './end-user-agreement.service';
import { Router, UrlTree } from '@angular/router';
import { EndUserAgreementCookieGuard } from './end-user-agreement-cookie.guard';

describe('EndUserAgreementCookieGuard', () => {
  let guard: EndUserAgreementCookieGuard;

  let endUserAgreementService: EndUserAgreementService;
  let router: Router;

  beforeEach(() => {
    endUserAgreementService = jasmine.createSpyObj('endUserAgreementService', {
      isCookieAccepted: true
    });
    router = jasmine.createSpyObj('router', {
      navigateByUrl: {},
      parseUrl: new UrlTree(),
      createUrlTree: new UrlTree()
    });

    guard = new EndUserAgreementCookieGuard(endUserAgreementService, router);
  });

  describe('canActivate', () => {
    describe('when the cookie has been accepted', () => {
      it('should return true', (done) => {
        guard.canActivate(undefined, { url: Object.assign({ url: 'redirect' }) } as any).subscribe((result) => {
          expect(result).toEqual(true);
          done();
        });
      });
    });

    describe('when the cookie hasn\'t been accepted', () => {
      beforeEach(() => {
        (endUserAgreementService.isCookieAccepted as jasmine.Spy).and.returnValue(false);
      });

      it('should return a UrlTree', (done) => {
        guard.canActivate(undefined, Object.assign({ url: 'redirect' })).subscribe((result) => {
          expect(result).toEqual(jasmine.any(UrlTree));
          done();
        });
      });
    });
  });
});
