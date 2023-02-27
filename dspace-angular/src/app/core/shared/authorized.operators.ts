import { Router, UrlTree } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { combineLatest as observableCombineLatest, Observable } from 'rxjs';
import { filter, map, withLatestFrom } from 'rxjs/operators';
import { InjectionToken } from '@angular/core';
import { RemoteData } from '../data/remote-data';
import { getEndUserAgreementPath } from '../../info/info-routing-paths';
import { getForbiddenRoute, getPageNotFoundRoute } from '../../app-routing-paths';

export const REDIRECT_ON_4XX = new InjectionToken<<T>(router: Router, authService: AuthService) => (source: Observable<RemoteData<T>>) => Observable<RemoteData<T>>>('redirectOn4xx', {
  providedIn: 'root',
  factory: () => redirectOn4xx
});
/**
 * Operator that checks if a remote data object returned a 4xx error
 * When it does contain such an error, it will redirect the user to the related error page, without
 * altering the current URL
 *
 * @param router The router used to navigate to a new page
 * @param authService Service to check if the user is authenticated
 */
export const redirectOn4xx = <T>(router: Router, authService: AuthService) =>
  (source: Observable<RemoteData<T>>): Observable<RemoteData<T>> =>
    source.pipe(
      withLatestFrom(authService.isAuthenticated()),
      filter(([rd, isAuthenticated]: [RemoteData<T>, boolean]) => {
        if (rd.hasFailed) {
          if (rd.statusCode === 404 || rd.statusCode === 422) {
            router.navigateByUrl(getPageNotFoundRoute(), { skipLocationChange: true });
            return false;
          } else if (rd.statusCode === 403 || rd.statusCode === 401) {
            if (isAuthenticated) {
              router.navigateByUrl(getForbiddenRoute(), { skipLocationChange: true });
              return false;
            } else {
              authService.setRedirectUrl(router.url);
              router.navigateByUrl('login');
              return false;
            }
          }
        }
        return true;
      }),
      map(([rd,]: [RemoteData<T>, boolean]) => rd)
    );
/**
 * Operator that returns a UrlTree to a forbidden page or the login page when the boolean received is false
 * @param router      The router used to navigate to a forbidden page
 * @param authService The AuthService used to determine whether or not the user is logged in
 * @param redirectUrl The URL to redirect back to after logging in
 */
export const returnForbiddenUrlTreeOrLoginOnFalse = (router: Router, authService: AuthService, redirectUrl: string) =>
  (source: Observable<boolean>): Observable<boolean | UrlTree> =>
    source.pipe(
      map((authorized) => [authorized]),
      returnForbiddenUrlTreeOrLoginOnAllFalse(router, authService, redirectUrl),
    );
/**
 * Operator that returns a UrlTree to a forbidden page or the login page when the booleans received are all false
 * @param router      The router used to navigate to a forbidden page
 * @param authService The AuthService used to determine whether or not the user is logged in
 * @param redirectUrl The URL to redirect back to after logging in
 */
export const returnForbiddenUrlTreeOrLoginOnAllFalse = (router: Router, authService: AuthService, redirectUrl: string) =>
  (source: Observable<boolean[]>): Observable<boolean | UrlTree> =>
    observableCombineLatest(source, authService.isAuthenticated()).pipe(
      map(([authorizedList, authenticated]: [boolean[], boolean]) => {
        if (authorizedList.some((b: boolean) => b === true)) {
          return true;
        } else {
          if (authenticated) {
            return router.parseUrl(getForbiddenRoute());
          } else {
            authService.setRedirectUrl(redirectUrl);
            return router.parseUrl('login');
          }
        }
      }));
/**
 * Operator that returns a UrlTree to the unauthorized page when the boolean received is false
 * @param router    Router
 * @param redirect  Redirect URL to add to the UrlTree. This is used to redirect back to the original route after the
 *                  user accepts the agreement.
 */
export const returnEndUserAgreementUrlTreeOnFalse = (router: Router, redirect: string) =>
  (source: Observable<boolean>): Observable<boolean | UrlTree> =>
    source.pipe(
      map((hasAgreed: boolean) => {
        const queryParams = { redirect: encodeURIComponent(redirect) };
        return hasAgreed ? hasAgreed : router.createUrlTree([getEndUserAgreementPath()], { queryParams });
      }));
