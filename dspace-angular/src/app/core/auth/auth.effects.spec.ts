import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { cold, hot } from 'jasmine-marbles';
import { Observable, of as observableOf, throwError as observableThrow } from 'rxjs';

import { AuthEffects } from './auth.effects';
import {
  AuthActionTypes,
  AuthenticatedAction,
  AuthenticatedErrorAction,
  AuthenticatedSuccessAction,
  AuthenticationErrorAction,
  AuthenticationSuccessAction,
  CheckAuthenticationTokenCookieAction,
  LogOutErrorAction,
  LogOutSuccessAction,
  RefreshTokenErrorAction,
  RefreshTokenSuccessAction,
  RetrieveAuthenticatedEpersonAction,
  RetrieveAuthenticatedEpersonErrorAction,
  RetrieveAuthenticatedEpersonSuccessAction,
  RetrieveAuthMethodsAction,
  RetrieveAuthMethodsErrorAction,
  RetrieveAuthMethodsSuccessAction,
  RetrieveTokenAction
} from './auth.actions';
import { authMethodsMock, AuthServiceStub } from '../../shared/testing/auth-service.stub';
import { AuthService } from './auth.service';
import { authReducer } from './auth.reducer';
import { AuthStatus } from './models/auth-status.model';
import { EPersonMock } from '../../shared/testing/eperson.mock';
import { AppState, storeModuleConfig } from '../../app.reducer';
import { StoreActionTypes } from '../../store.actions';
import { isAuthenticated, isAuthenticatedLoaded } from './selectors';
import { AuthorizationDataService } from '../data/feature-authorization/authorization-data.service';

describe('AuthEffects', () => {
  let authEffects: AuthEffects;
  let actions: Observable<any>;
  let authServiceStub;
  let initialState;
  let token;
  let store: MockStore<AppState>;

  const authorizationService = jasmine.createSpyObj(['invalidateAuthorizationsRequestCache']);

  function init() {
    authServiceStub = new AuthServiceStub();
    token = authServiceStub.getToken();
    initialState = {
      core: {
        auth: {
          authenticated: false,
          loaded: false,
          loading: false,
          authMethods: []
        }
      }
    };
  }

  beforeEach(() => {
    init();
    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot({ auth: authReducer }, storeModuleConfig)
      ],
      providers: [
        AuthEffects,
        provideMockStore({ initialState }),
        { provide: AuthorizationDataService, useValue: authorizationService },
        { provide: AuthService, useValue: authServiceStub },
        provideMockActions(() => actions),
        // other providers
      ],
    });

    authEffects = TestBed.inject(AuthEffects);
    store = TestBed.inject(Store as any);
  });

  describe('authenticate$', () => {
    describe('when credentials are correct', () => {
      it('should return a AUTHENTICATE_SUCCESS action in response to a AUTHENTICATE action', () => {
        actions = hot('--a-', {
          a: {
            type: AuthActionTypes.AUTHENTICATE,
            payload: { email: 'user', password: 'password' }
          }
        });

        const expected = cold('--b-', { b: new AuthenticationSuccessAction(token) });

        expect(authEffects.authenticate$).toBeObservable(expected);
      });
    });

    describe('when credentials are wrong', () => {
      it('should return a AUTHENTICATE_ERROR action in response to a AUTHENTICATE action', () => {
        spyOn((authEffects as any).authService, 'authenticate').and.returnValue(observableThrow(new Error('Message Error test')));

        actions = hot('--a-', {
          a: {
            type: AuthActionTypes.AUTHENTICATE,
            payload: { email: 'user', password: 'wrongpassword' }
          }
        });

        const expected = cold('--b-', { b: new AuthenticationErrorAction(new Error('Message Error test')) });

        expect(authEffects.authenticate$).toBeObservable(expected);
      });
    });
  });

  describe('authenticateSuccess$', () => {

    it('should return a AUTHENTICATED action in response to a AUTHENTICATE_SUCCESS action', () => {
      actions = hot('--a-', { a: { type: AuthActionTypes.AUTHENTICATE_SUCCESS, payload: token } });

      const expected = cold('--b-', { b: new AuthenticatedAction(token) });

      expect(authEffects.authenticateSuccess$).toBeObservable(expected);
    });
  });

  describe('authenticated$', () => {

    describe('when token is valid', () => {
      it('should return a AUTHENTICATED_SUCCESS action in response to a AUTHENTICATED action', () => {
        actions = hot('--a-', { a: { type: AuthActionTypes.AUTHENTICATED, payload: token } });

        const expected = cold('--b-', { b: new AuthenticatedSuccessAction(true, token, EPersonMock._links.self.href) });

        expect(authEffects.authenticated$).toBeObservable(expected);
      });
    });

    describe('when token is not valid', () => {
      it('should return a AUTHENTICATED_ERROR action in response to a AUTHENTICATED action', () => {
        spyOn((authEffects as any).authService, 'authenticatedUser').and.returnValue(observableThrow(new Error('Message Error test')));

        actions = hot('--a-', { a: { type: AuthActionTypes.AUTHENTICATED, payload: token } });

        const expected = cold('--b-', { b: new AuthenticatedErrorAction(new Error('Message Error test')) });

        expect(authEffects.authenticated$).toBeObservable(expected);
      });
    });
  });

  describe('authenticatedSuccess$', () => {

    it('should return a RETRIEVE_AUTHENTICATED_EPERSON action in response to a AUTHENTICATED_SUCCESS action', (done) => {
      spyOn((authEffects as any).authService, 'storeToken');
      actions = hot('--a-', {
        a: {
          type: AuthActionTypes.AUTHENTICATED_SUCCESS, payload: {
            authenticated: true,
            authToken: token,
            userHref: EPersonMock._links.self.href
          }
        }
      });

      const expected = cold('--b-', { b: new RetrieveAuthenticatedEpersonAction(EPersonMock._links.self.href) });

      authEffects.authenticatedSuccess$.subscribe(() => {
        expect(authServiceStub.storeToken).toHaveBeenCalledWith(token);
      });

      expect(authEffects.authenticatedSuccess$).toBeObservable(expected);
      done();
    });

  });

  describe('checkToken$', () => {

    describe('when check token succeeded', () => {
      it('should return a AUTHENTICATED action in response to a CHECK_AUTHENTICATION_TOKEN action', () => {

        actions = hot('--a-', { a: { type: AuthActionTypes.CHECK_AUTHENTICATION_TOKEN } });

        const expected = cold('--b-', { b: new AuthenticatedAction(token) });

        expect(authEffects.checkToken$).toBeObservable(expected);
      });
    });

    describe('when check token failed', () => {
      it('should return a CHECK_AUTHENTICATION_TOKEN_ERROR action in response to a CHECK_AUTHENTICATION_TOKEN action', () => {
        spyOn((authEffects as any).authService, 'hasValidAuthenticationToken').and.returnValue(observableThrow(''));

        actions = hot('--a-', { a: { type: AuthActionTypes.CHECK_AUTHENTICATION_TOKEN, payload: token } });

        const expected = cold('--b-', { b: new CheckAuthenticationTokenCookieAction() });

        expect(authEffects.checkToken$).toBeObservable(expected);
      });
    });
  });

  describe('checkTokenCookie$', () => {

    describe('when check token succeeded', () => {
      it('should return a RETRIEVE_TOKEN action in response to a CHECK_AUTHENTICATION_TOKEN_COOKIE action when authenticated is true', () => {
        spyOn((authEffects as any).authService, 'checkAuthenticationCookie').and.returnValue(
          observableOf(
            {
              authenticated: true
            })
        );
        actions = hot('--a-', { a: { type: AuthActionTypes.CHECK_AUTHENTICATION_TOKEN_COOKIE } });

        const expected = cold('--b-', { b: new RetrieveTokenAction() });

        expect(authEffects.checkTokenCookie$).toBeObservable(expected);
        authEffects.checkTokenCookie$.subscribe(() => {
          expect((authEffects as any).authorizationsService.invalidateAuthorizationsRequestCache).toHaveBeenCalled();
        });
      });

      it('should return a RETRIEVE_AUTH_METHODS action in response to a CHECK_AUTHENTICATION_TOKEN_COOKIE action when authenticated is false', () => {
        spyOn((authEffects as any).authService, 'checkAuthenticationCookie').and.returnValue(
          observableOf(
            { authenticated: false })
        );
        actions = hot('--a-', { a: { type: AuthActionTypes.CHECK_AUTHENTICATION_TOKEN_COOKIE } });

        const expected = cold('--b-', { b: new RetrieveAuthMethodsAction({ authenticated: false } as AuthStatus) });

        expect(authEffects.checkTokenCookie$).toBeObservable(expected);
      });
    });

    describe('when check token failed', () => {
      it('should return a AUTHENTICATED_ERROR action in response to a CHECK_AUTHENTICATION_TOKEN_COOKIE action', () => {
        spyOn((authEffects as any).authService, 'checkAuthenticationCookie').and.returnValue(observableThrow(new Error('Message Error test')));

        actions = hot('--a-', { a: { type: AuthActionTypes.CHECK_AUTHENTICATION_TOKEN_COOKIE, payload: token } });

        const expected = cold('--b-', { b: new AuthenticatedErrorAction(new Error('Message Error test')) });

        expect(authEffects.checkTokenCookie$).toBeObservable(expected);
      });
    });
  });

  describe('retrieveAuthenticatedEperson$', () => {

    describe('when request is successful', () => {
      it('should return a RETRIEVE_AUTHENTICATED_EPERSON_SUCCESS action in response to a RETRIEVE_AUTHENTICATED_EPERSON action', () => {
        actions = hot('--a-', {
          a: {
            type: AuthActionTypes.RETRIEVE_AUTHENTICATED_EPERSON,
            payload: EPersonMock._links.self.href
          }
        });

        const expected = cold('--b-', { b: new RetrieveAuthenticatedEpersonSuccessAction(EPersonMock.id) });

        expect(authEffects.retrieveAuthenticatedEperson$).toBeObservable(expected);
      });
    });

    describe('when request is not successful', () => {
      it('should return a RETRIEVE_AUTHENTICATED_EPERSON_ERROR action in response to a RETRIEVE_AUTHENTICATED_EPERSON action', () => {
        spyOn((authEffects as any).authService, 'retrieveAuthenticatedUserByHref').and.returnValue(observableThrow(new Error('Message Error test')));

        actions = hot('--a-', { a: { type: AuthActionTypes.RETRIEVE_AUTHENTICATED_EPERSON, payload: token } });

        const expected = cold('--b-', { b: new RetrieveAuthenticatedEpersonErrorAction(new Error('Message Error test')) });

        expect(authEffects.retrieveAuthenticatedEperson$).toBeObservable(expected);
      });
    });
  });

  describe('refreshToken$', () => {

    describe('when refresh token succeeded', () => {
      it('should return a REFRESH_TOKEN_SUCCESS action in response to a REFRESH_TOKEN action', () => {

        actions = hot('--a-', { a: { type: AuthActionTypes.REFRESH_TOKEN } });

        const expected = cold('--b-', { b: new RefreshTokenSuccessAction(token) });

        expect(authEffects.refreshToken$).toBeObservable(expected);
      });
    });

    describe('when refresh token failed', () => {
      it('should return a REFRESH_TOKEN_ERROR action in response to a REFRESH_TOKEN action', () => {
        spyOn((authEffects as any).authService, 'refreshAuthenticationToken').and.returnValue(observableThrow(''));

        actions = hot('--a-', { a: { type: AuthActionTypes.REFRESH_TOKEN, payload: token } });

        const expected = cold('--b-', { b: new RefreshTokenErrorAction() });

        expect(authEffects.refreshToken$).toBeObservable(expected);
      });
    });
  });

  describe('retrieveToken$', () => {
    describe('when user is authenticated', () => {
      it('should return a AUTHENTICATE_SUCCESS action in response to a RETRIEVE_TOKEN action', () => {
        actions = hot('--a-', {
          a: {
            type: AuthActionTypes.RETRIEVE_TOKEN
          }
        });

        const expected = cold('--b-', { b: new AuthenticationSuccessAction(token) });

        expect(authEffects.retrieveToken$).toBeObservable(expected);
      });
    });

    describe('when user is not authenticated', () => {
      it('should return a AUTHENTICATE_ERROR action in response to a RETRIEVE_TOKEN action', () => {
        spyOn((authEffects as any).authService, 'refreshAuthenticationToken').and.returnValue(observableThrow(new Error('Message Error test')));

        actions = hot('--a-', {
          a: {
            type: AuthActionTypes.RETRIEVE_TOKEN
          }
        });

        const expected = cold('--b-', { b: new AuthenticationErrorAction(new Error('Message Error test')) });

        expect(authEffects.retrieveToken$).toBeObservable(expected);
      });
    });
  });

  describe('logOut$', () => {

    describe('when refresh token succeeded', () => {
      it('should return a LOG_OUT_SUCCESS action in response to a LOG_OUT action', () => {

        actions = hot('--a-', { a: { type: AuthActionTypes.LOG_OUT } });

        const expected = cold('--b-', { b: new LogOutSuccessAction() });

        expect(authEffects.logOut$).toBeObservable(expected);
      });
    });

    describe('when refresh token failed', () => {
      it('should return a REFRESH_TOKEN_ERROR action in response to a LOG_OUT action', () => {
        spyOn((authEffects as any).authService, 'logout').and.returnValue(observableThrow(new Error('Message Error test')));

        actions = hot('--a-', { a: { type: AuthActionTypes.LOG_OUT, payload: token } });

        const expected = cold('--b-', { b: new LogOutErrorAction(new Error('Message Error test')) });

        expect(authEffects.logOut$).toBeObservable(expected);
      });
    });
  });

  describe('retrieveMethods$', () => {

    describe('when retrieve authentication methods succeeded', () => {
      it('should return a RETRIEVE_AUTH_METHODS_SUCCESS action in response to a RETRIEVE_AUTH_METHODS action', () => {
        actions = hot('--a-', { a: { type: AuthActionTypes.RETRIEVE_AUTH_METHODS } });

        const expected = cold('--b-', { b: new RetrieveAuthMethodsSuccessAction(authMethodsMock) });

        expect(authEffects.retrieveMethods$).toBeObservable(expected);
      });
    });

    describe('when retrieve authentication methods failed', () => {
      it('should return a RETRIEVE_AUTH_METHODS_ERROR action in response to a RETRIEVE_AUTH_METHODS action', () => {
        spyOn((authEffects as any).authService, 'retrieveAuthMethodsFromAuthStatus').and.returnValue(observableThrow(''));

        actions = hot('--a-', { a: { type: AuthActionTypes.RETRIEVE_AUTH_METHODS } });

        const expected = cold('--b-', { b: new RetrieveAuthMethodsErrorAction() });

        expect(authEffects.retrieveMethods$).toBeObservable(expected);
      });
    });
  });

  describe('clearInvalidTokenOnRehydrate$', () => {

    beforeEach(() => {
      store.overrideSelector(isAuthenticated, false);
    });

    describe('when auth loaded is false', () => {
      it('should not call removeToken method', fakeAsync(() => {
        store.overrideSelector(isAuthenticatedLoaded, false);
        actions = observableOf({ type: StoreActionTypes.REHYDRATE });
        spyOn(authServiceStub, 'removeToken');

        authEffects.clearInvalidTokenOnRehydrate$.subscribe(() => {
          expect(false).toBeTrue();  // subscribe to trigger taps, fail if the effect emits (we don't expect it to)
        });
        tick(1000);
        expect(authServiceStub.removeToken).not.toHaveBeenCalled();
      }));
    });

    describe('when auth loaded is true', () => {
      it('should call removeToken method', (done) => {
        spyOn(console, 'log').and.callThrough();

        store.overrideSelector(isAuthenticatedLoaded, true);
        actions = observableOf({ type: StoreActionTypes.REHYDRATE });
        spyOn(authServiceStub, 'removeToken');

        authEffects.clearInvalidTokenOnRehydrate$.subscribe(() => {
          expect(authServiceStub.removeToken).toHaveBeenCalled();
          done();
        });
      });
    });
  });

  describe('invalidateAuthorizationsRequestCache$', () => {
    it('should call invalidateAuthorizationsRequestCache method in response to a REHYDRATE action', (done) => {
      actions = observableOf({ type: StoreActionTypes.REHYDRATE });

      authEffects.invalidateAuthorizationsRequestCache$.subscribe(() => {
        expect((authEffects as  any).authorizationsService.invalidateAuthorizationsRequestCache).toHaveBeenCalled();
        done();
      });
    });
  });
});
