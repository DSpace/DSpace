import { Injectable } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { CookieService } from '../services/cookie.service';
import { Observable, of as observableOf } from 'rxjs';
import { map, switchMap, take } from 'rxjs/operators';
import { hasValue } from '../../shared/empty.util';
import { EPersonDataService } from '../eperson/eperson-data.service';
import { getFirstCompletedRemoteData } from '../shared/operators';

export const END_USER_AGREEMENT_COOKIE = 'hasAgreedEndUser';
export const END_USER_AGREEMENT_METADATA_FIELD = 'dspace.agreements.end-user';

/**
 * Service for checking and managing the status of the current end user agreement
 */
@Injectable()
export class EndUserAgreementService {

  constructor(protected cookie: CookieService,
              protected authService: AuthService,
              protected ePersonService: EPersonDataService) {
  }

  /**
   * Whether or not either the cookie was accepted or the current user has accepted the End User Agreement
   * @param acceptedWhenAnonymous Whether or not the user agreement should be considered accepted if the user is
   *                              currently not authenticated (anonymous)
   */
  hasCurrentUserOrCookieAcceptedAgreement(acceptedWhenAnonymous: boolean): Observable<boolean> {
    if (this.isCookieAccepted()) {
      return observableOf(true);
    } else {
      return this.hasCurrentUserAcceptedAgreement(acceptedWhenAnonymous);
    }
  }

  /**
   * Whether or not the current user has accepted the End User Agreement
   * @param acceptedWhenAnonymous Whether or not the user agreement should be considered accepted if the user is
   *                              currently not authenticated (anonymous)
   */
  hasCurrentUserAcceptedAgreement(acceptedWhenAnonymous: boolean): Observable<boolean> {
    return this.authService.isAuthenticated().pipe(
      switchMap((authenticated) => {
        if (authenticated) {
          return this.authService.getAuthenticatedUserFromStore().pipe(
            map((user) => hasValue(user) && user.hasMetadata(END_USER_AGREEMENT_METADATA_FIELD) && user.firstMetadata(END_USER_AGREEMENT_METADATA_FIELD).value === 'true')
          );
        } else {
          return observableOf(acceptedWhenAnonymous);
        }
      })
    );
  }

  /**
   * Set the current user's accepted agreement status
   * When a user is authenticated, set their metadata to the provided value
   * When no user is authenticated, set the cookie to the provided value
   * @param accepted
   */
  setUserAcceptedAgreement(accepted: boolean): Observable<boolean> {
    return this.authService.isAuthenticated().pipe(
      switchMap((authenticated) => {
        if (authenticated) {
          return this.authService.getAuthenticatedUserFromStore().pipe(
            take(1),
            switchMap((user) => {
              const newValue = { value: String(accepted) };
              let operation;
              if (user.hasMetadata(END_USER_AGREEMENT_METADATA_FIELD)) {
                operation = { op: 'replace', path: `/metadata/${END_USER_AGREEMENT_METADATA_FIELD}/0`, value: newValue };
              } else {
                operation = { op: 'add', path: `/metadata/${END_USER_AGREEMENT_METADATA_FIELD}`, value: [ newValue ] };
              }
              return this.ePersonService.patch(user, [operation]);
            }),
            getFirstCompletedRemoteData(),
            map((response) => response.hasSucceeded)
          );
        } else {
          this.setCookieAccepted(accepted);
          return observableOf(true);
        }
      }),
      take(1)
    );
  }

  /**
   * Is the End User Agreement accepted in the cookie?
   */
  isCookieAccepted(): boolean {
    return this.cookie.get(END_USER_AGREEMENT_COOKIE) === true;
  }

  /**
   * Set the cookie's End User Agreement accepted state
   * @param accepted
   */
  setCookieAccepted(accepted: boolean) {
    this.cookie.set(END_USER_AGREEMENT_COOKIE, accepted);
  }

  /**
   * Remove the End User Agreement cookie
   */
  removeCookieAccepted() {
    this.cookie.remove(END_USER_AGREEMENT_COOKIE);
  }

}
