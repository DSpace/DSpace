import {
  END_USER_AGREEMENT_COOKIE,
  END_USER_AGREEMENT_METADATA_FIELD,
  EndUserAgreementService
} from './end-user-agreement.service';
import { CookieServiceMock } from '../../shared/mocks/cookie.service.mock';
import { of as observableOf } from 'rxjs';
import { EPerson } from '../eperson/models/eperson.model';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';

describe('EndUserAgreementService', () => {
  let service: EndUserAgreementService;

  let userWithMetadata: EPerson;
  let userWithoutMetadata: EPerson;

  let cookie;
  let authService;
  let ePersonService;

  beforeEach(() => {
    userWithMetadata = Object.assign(new EPerson(), {
      metadata: {
        [END_USER_AGREEMENT_METADATA_FIELD]: [
          {
            value: 'true'
          }
        ]
      }
    });
    userWithoutMetadata = Object.assign(new EPerson());

    cookie = new CookieServiceMock();
    authService = jasmine.createSpyObj('authService', {
      isAuthenticated: observableOf(true),
      getAuthenticatedUserFromStore: observableOf(userWithMetadata)
    });
    ePersonService = jasmine.createSpyObj('ePersonService', {
      update: createSuccessfulRemoteDataObject$(userWithMetadata),
      patch: createSuccessfulRemoteDataObject$({})
    });

    service = new EndUserAgreementService(cookie, authService, ePersonService);
  });

  describe('when the cookie is set to true', () => {
    beforeEach(() => {
      cookie.set(END_USER_AGREEMENT_COOKIE, true);
    });

    it('hasCurrentUserOrCookieAcceptedAgreement should return true', (done) => {
      service.hasCurrentUserOrCookieAcceptedAgreement(false).subscribe((result) => {
        expect(result).toEqual(true);
        done();
      });
    });

    it('isCookieAccepted should return true', () => {
      expect(service.isCookieAccepted()).toEqual(true);
    });

    it('removeCookieAccepted should remove the cookie', () => {
      service.removeCookieAccepted();
      expect(cookie.get(END_USER_AGREEMENT_COOKIE)).toBeUndefined();
    });
  });

  describe('when the cookie isn\'t set', () => {
    describe('and the user is authenticated', () => {
      beforeEach(() => {
        (authService.isAuthenticated as jasmine.Spy).and.returnValue(observableOf(true));
      });

      describe('and the user contains agreement metadata', () => {
        beforeEach(() => {
          (authService.getAuthenticatedUserFromStore as jasmine.Spy).and.returnValue(observableOf(userWithMetadata));
        });

        it('hasCurrentUserOrCookieAcceptedAgreement should return true', (done) => {
          service.hasCurrentUserOrCookieAcceptedAgreement(false).subscribe((result) => {
            expect(result).toEqual(true);
            done();
          });
        });
      });

      describe('and the user doesn\'t contain agreement metadata', () => {
        beforeEach(() => {
          (authService.getAuthenticatedUserFromStore as jasmine.Spy).and.returnValue(observableOf(userWithoutMetadata));
        });

        it('hasCurrentUserOrCookieAcceptedAgreement should return false', (done) => {
          service.hasCurrentUserOrCookieAcceptedAgreement(false).subscribe((result) => {
            expect(result).toEqual(false);
            done();
          });
        });
      });

      it('setUserAcceptedAgreement should update the user with new metadata', (done) => {
        service.setUserAcceptedAgreement(true).subscribe(() => {
          expect(ePersonService.patch).toHaveBeenCalled();
          done();
        });
      });
    });

    describe('and the user is not authenticated', () => {
      beforeEach(() => {
        (authService.isAuthenticated as jasmine.Spy).and.returnValue(observableOf(false));
      });

      it('hasCurrentUserOrCookieAcceptedAgreement should return false', (done) => {
        service.hasCurrentUserOrCookieAcceptedAgreement(false).subscribe((result) => {
          expect(result).toEqual(false);
          done();
        });
      });

      it('setUserAcceptedAgreement should set the cookie to true', (done) => {
        service.setUserAcceptedAgreement(true).subscribe(() => {
          expect(cookie.get(END_USER_AGREEMENT_COOKIE)).toEqual(true);
          done();
        });
      });
    });

    it('isCookieAccepted should return false', () => {
      expect(service.isCookieAccepted()).toEqual(false);
    });

    it('setCookieAccepted should set the cookie', () => {
      service.setCookieAccepted(true);
      expect(cookie.get(END_USER_AGREEMENT_COOKIE)).toEqual(true);
    });
  });
});
