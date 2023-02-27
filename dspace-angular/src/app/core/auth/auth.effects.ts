import { Injectable, NgZone } from '@angular/core';

import {
  asyncScheduler,
  combineLatest as observableCombineLatest,
  Observable,
  of as observableOf,
  queueScheduler,
  timer
} from 'rxjs';
import { catchError, filter, map, observeOn, switchMap, take, tap } from 'rxjs/operators';
// import @ngrx
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Action, select, Store } from '@ngrx/store';

// import services
import { AuthService } from './auth.service';
import { EPerson } from '../eperson/models/eperson.model';
import { AuthStatus } from './models/auth-status.model';
import { AuthTokenInfo } from './models/auth-token-info.model';
import { AppState } from '../../app.reducer';
import { isAuthenticated, isAuthenticatedLoaded } from './selectors';
import { StoreActionTypes } from '../../store.actions';
import { AuthMethod } from './models/auth.method';
// import actions
import {
  AuthActionTypes,
  AuthenticateAction,
  AuthenticatedAction,
  AuthenticatedErrorAction,
  AuthenticatedSuccessAction,
  AuthenticationErrorAction,
  AuthenticationSuccessAction,
  CheckAuthenticationTokenCookieAction,
  LogOutErrorAction,
  LogOutSuccessAction,
  RedirectAfterLoginSuccessAction,
  RefreshTokenAction,
  RefreshTokenErrorAction,
  RefreshTokenSuccessAction,
  RetrieveAuthenticatedEpersonAction,
  RetrieveAuthenticatedEpersonErrorAction,
  RetrieveAuthenticatedEpersonSuccessAction,
  RetrieveAuthMethodsAction,
  RetrieveAuthMethodsErrorAction,
  RetrieveAuthMethodsSuccessAction,
  RetrieveTokenAction,
  SetUserAsIdleAction
} from './auth.actions';
import { hasValue } from '../../shared/empty.util';
import { environment } from '../../../environments/environment';
import { RequestActionTypes } from '../data/request.actions';
import { NotificationsActionTypes } from '../../shared/notifications/notifications.actions';
import { LeaveZoneScheduler } from '../utilities/leave-zone.scheduler';
import { EnterZoneScheduler } from '../utilities/enter-zone.scheduler';
import { AuthorizationDataService } from '../data/feature-authorization/authorization-data.service';

// Action Types that do not break/prevent the user from an idle state
const IDLE_TIMER_IGNORE_TYPES: string[]
  = [...Object.values(AuthActionTypes).filter((t: string) => t !== AuthActionTypes.UNSET_USER_AS_IDLE),
  ...Object.values(RequestActionTypes), ...Object.values(NotificationsActionTypes)];

@Injectable()
export class AuthEffects {

  /**
   * Authenticate user.
   * @method authenticate
   */
  public authenticate$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(AuthActionTypes.AUTHENTICATE),
    switchMap((action: AuthenticateAction) => {
      return this.authService.authenticate(action.payload.email, action.payload.password).pipe(
        take(1),
        map((response: AuthStatus) => new AuthenticationSuccessAction(response.token)),
        catchError((error) => observableOf(new AuthenticationErrorAction(error)))
      );
    })
  ));

  public authenticateSuccess$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(AuthActionTypes.AUTHENTICATE_SUCCESS),
    map((action: AuthenticationSuccessAction) => new AuthenticatedAction(action.payload))
  ));

  public authenticated$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(AuthActionTypes.AUTHENTICATED),
    switchMap((action: AuthenticatedAction) => {
      return this.authService.authenticatedUser(action.payload).pipe(
        map((userHref: string) => new AuthenticatedSuccessAction((userHref !== null), action.payload, userHref)),
        catchError((error) => observableOf(new AuthenticatedErrorAction(error))),);
    })
  ));

  public authenticatedSuccess$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(AuthActionTypes.AUTHENTICATED_SUCCESS),
    tap((action: AuthenticatedSuccessAction) => this.authService.storeToken(action.payload.authToken)),
    switchMap((action: AuthenticatedSuccessAction) => this.authService.getRedirectUrl().pipe(
      take(1),
      map((redirectUrl: string) => [action, redirectUrl])
    )),
    map(([action, redirectUrl]: [AuthenticatedSuccessAction, string]) => {
      if (hasValue(redirectUrl)) {
        return new RedirectAfterLoginSuccessAction(redirectUrl);
      } else {
        return new RetrieveAuthenticatedEpersonAction(action.payload.userHref);
      }
    })
  ));

  public redirectAfterLoginSuccess$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(AuthActionTypes.REDIRECT_AFTER_LOGIN_SUCCESS),
    tap((action: RedirectAfterLoginSuccessAction) => {
      this.authService.clearRedirectUrl();
      this.authService.navigateToRedirectUrl(action.payload);
    })
  ), { dispatch: false });

  // It means "reacts to this action but don't send another"
  public authenticatedError$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(AuthActionTypes.AUTHENTICATED_ERROR),
    tap((action: LogOutSuccessAction) => this.authService.removeToken())
  ), { dispatch: false });

  public retrieveAuthenticatedEperson$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(AuthActionTypes.RETRIEVE_AUTHENTICATED_EPERSON),
    switchMap((action: RetrieveAuthenticatedEpersonAction) => {
      const impersonatedUserID = this.authService.getImpersonateID();
      let user$: Observable<EPerson>;
      if (hasValue(impersonatedUserID)) {
        user$ = this.authService.retrieveAuthenticatedUserById(impersonatedUserID);
      } else {
        user$ = this.authService.retrieveAuthenticatedUserByHref(action.payload);
      }
      return user$.pipe(
        map((user: EPerson) => new RetrieveAuthenticatedEpersonSuccessAction(user.id)),
        catchError((error) => observableOf(new RetrieveAuthenticatedEpersonErrorAction(error))));
    })
  ));

  public checkToken$: Observable<Action> = createEffect(() => this.actions$.pipe(ofType(AuthActionTypes.CHECK_AUTHENTICATION_TOKEN),
    switchMap(() => {
      return this.authService.hasValidAuthenticationToken().pipe(
        map((token: AuthTokenInfo) => new AuthenticatedAction(token)),
        catchError((error) => observableOf(new CheckAuthenticationTokenCookieAction()))
      );
    })
  ));

  public checkTokenCookie$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(AuthActionTypes.CHECK_AUTHENTICATION_TOKEN_COOKIE),
    switchMap(() => {
      return this.authService.checkAuthenticationCookie().pipe(
        map((response: AuthStatus) => {
          if (response.authenticated) {
            this.authorizationsService.invalidateAuthorizationsRequestCache();
            return new RetrieveTokenAction();
          } else {
            return new RetrieveAuthMethodsAction(response);
          }
        }),
        catchError((error) => observableOf(new AuthenticatedErrorAction(error)))
      );
    })
  ));

  public retrieveToken$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(AuthActionTypes.RETRIEVE_TOKEN),
    switchMap((action: AuthenticateAction) => {
      return this.authService.refreshAuthenticationToken(null).pipe(
        take(1),
        map((token: AuthTokenInfo) => new AuthenticationSuccessAction(token)),
        catchError((error) => observableOf(new AuthenticationErrorAction(error)))
      );
    })
  ));

  public refreshToken$: Observable<Action> = createEffect(() => this.actions$.pipe(ofType(AuthActionTypes.REFRESH_TOKEN),
    switchMap((action: RefreshTokenAction) => {
      return this.authService.refreshAuthenticationToken(action.payload).pipe(
        map((token: AuthTokenInfo) => new RefreshTokenSuccessAction(token)),
        catchError((error) => observableOf(new RefreshTokenErrorAction()))
      );
    })
  ));

  // It means "reacts to this action but don't send another"
  public refreshTokenSuccess$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(AuthActionTypes.REFRESH_TOKEN_SUCCESS),
    tap((action: RefreshTokenSuccessAction) => this.authService.replaceToken(action.payload))
  ), { dispatch: false });

  /**
   * When the store is rehydrated in the browser,
   * clear a possible invalid token or authentication errors
   */
  public clearInvalidTokenOnRehydrate$: Observable<any> = createEffect(() => this.actions$.pipe(
    ofType(StoreActionTypes.REHYDRATE),
    switchMap(() => {
      const isLoaded$ = this.store.pipe(select(isAuthenticatedLoaded));
      const authenticated$ = this.store.pipe(select(isAuthenticated));
      return observableCombineLatest([isLoaded$, authenticated$]).pipe(
        take(1),
        filter(([loaded, authenticated]) => loaded && !authenticated),
        tap(() => this.authService.removeToken()),
        tap(() => this.authService.resetAuthenticationError())
      );
    })), { dispatch: false });

  /**
   * When the store is rehydrated in the browser, invalidate all cache hits regarding the
   * authorizations endpoint, to be sure to have consistent responses after a login with external idp
   *
   */
   invalidateAuthorizationsRequestCache$ = createEffect(() => this.actions$
    .pipe(ofType(StoreActionTypes.REHYDRATE),
      tap(() => this.authorizationsService.invalidateAuthorizationsRequestCache())
    ), { dispatch: false });

  public logOut$: Observable<Action> = createEffect(() => this.actions$
    .pipe(
      ofType(AuthActionTypes.LOG_OUT),
      switchMap(() => {
        this.authService.stopImpersonating();
        return this.authService.logout().pipe(
          map((value) => new LogOutSuccessAction()),
          catchError((error) => observableOf(new LogOutErrorAction(error)))
        );
      })
    ));

  public logOutSuccess$: Observable<Action> = createEffect(() => this.actions$
    .pipe(ofType(AuthActionTypes.LOG_OUT_SUCCESS),
      tap(() => this.authService.removeToken()),
      tap(() => this.authService.clearRedirectUrl()),
      tap(() => this.authService.refreshAfterLogout())
    ), { dispatch: false });

  public redirectToLoginTokenExpired$: Observable<Action> = createEffect(() => this.actions$
    .pipe(
      ofType(AuthActionTypes.REDIRECT_TOKEN_EXPIRED),
      tap(() => this.authService.removeToken()),
      tap(() => this.authService.redirectToLoginWhenTokenExpired())
    ), { dispatch: false });

  public retrieveMethods$: Observable<Action> = createEffect(() => this.actions$
    .pipe(
      ofType(AuthActionTypes.RETRIEVE_AUTH_METHODS),
      switchMap((action: RetrieveAuthMethodsAction) => {
        return this.authService.retrieveAuthMethodsFromAuthStatus(action.payload)
          .pipe(
            map((authMethodModels: AuthMethod[]) => new RetrieveAuthMethodsSuccessAction(authMethodModels)),
            catchError((error) => observableOf(new RetrieveAuthMethodsErrorAction()))
          );
      })
    ));

  /**
   * For any action that is not in {@link IDLE_TIMER_IGNORE_TYPES} that comes in => Start the idleness timer
   * If the idleness timer runs out (so no un-ignored action come through for that amount of time)
   * => Return the action to set the user as idle ({@link SetUserAsIdleAction})
   * @method trackIdleness
   */
  public trackIdleness$: Observable<Action> = createEffect(() => this.actions$.pipe(
    filter((action: Action) => !IDLE_TIMER_IGNORE_TYPES.includes(action.type)),
    // Using switchMap the effect will stop subscribing to the previous timer if a new action comes
    // in, and start a new timer
    switchMap(() =>
      // Start a timer outside of Angular's zone
      timer(environment.auth.ui.timeUntilIdle, new LeaveZoneScheduler(this.zone, asyncScheduler))
    ),
    // Re-enter the zone to dispatch the action
    observeOn(new EnterZoneScheduler(this.zone, queueScheduler)),
    map(() => new SetUserAsIdleAction()),
  ));

  /**
   * @constructor
   * @param {Actions} actions$
   * @param {NgZone} zone
   * @param {AuthorizationDataService} authorizationsService
   * @param {AuthService} authService
   * @param {Store} store
   */
  constructor(private actions$: Actions,
              private zone: NgZone,
              private authorizationsService: AuthorizationDataService,
              private authService: AuthService,
              private store: Store<AppState>) {
  }
}
