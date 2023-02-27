// import actions
import {
  AddAuthenticationMessageAction,
  AuthActions,
  AuthActionTypes,
  AuthenticatedSuccessAction,
  AuthenticationErrorAction,
  LogOutErrorAction,
  RedirectWhenAuthenticationIsRequiredAction,
  RedirectWhenTokenExpiredAction,
  RefreshTokenSuccessAction,
  RetrieveAuthenticatedEpersonSuccessAction,
  RetrieveAuthMethodsSuccessAction,
  SetRedirectUrlAction
} from './auth.actions';
// import models
import { AuthTokenInfo } from './models/auth-token-info.model';
import { AuthMethod } from './models/auth.method';
import { AuthMethodType } from './models/auth.method-type';
import { StoreActionTypes } from '../../store.actions';

/**
 * The auth state.
 * @interface State
 */
export interface AuthState {

  // boolean if user is authenticated
  authenticated: boolean;

  // the authentication token
  authToken?: AuthTokenInfo;

  // error message
  error?: string;

  // true if we have attempted existing auth session
  loaded: boolean;

  // true when loading
  loading: boolean;

  // true when everything else should wait for authorization
  // to complete
  blocking: boolean;

  // info message
  info?: string;

  // redirect url after login
  redirectUrl?: string;

  // true when refreshing token
  refreshing?: boolean;

  // the authenticated user's id
  userId?: string;

  // all authentication Methods enabled at the backend
  authMethods?: AuthMethod[];

  // true when the current user is idle
  idle: boolean;

}

/**
 * The initial state.
 */
const initialState: AuthState = {
  authenticated: false,
  loaded: false,
  blocking: true,
  loading: false,
  authMethods: [],
  idle: false
};

/**
 * The reducer function.
 * @function reducer
 * @param {State} state Current state
 * @param {AuthActions} action Incoming action
 */
export function authReducer(state: any = initialState, action: AuthActions): AuthState {

  switch (action.type) {
    case AuthActionTypes.AUTHENTICATE:
      return Object.assign({}, state, {
        error: undefined,
        loading: true,
        info: undefined
      });

    case AuthActionTypes.AUTHENTICATED:
      return Object.assign({}, state, {
        loading: true,
        blocking: true
      });

    case AuthActionTypes.CHECK_AUTHENTICATION_TOKEN:
    case AuthActionTypes.CHECK_AUTHENTICATION_TOKEN_COOKIE:
      return Object.assign({}, state, {
        loading: true,
      });

    case AuthActionTypes.AUTHENTICATED_ERROR:
    case AuthActionTypes.RETRIEVE_AUTHENTICATED_EPERSON_ERROR:
      return Object.assign({}, state, {
        authenticated: false,
        authToken: undefined,
        error: (action as AuthenticationErrorAction).payload.message,
        loaded: true,
        blocking: false,
        loading: false
      });

    case AuthActionTypes.AUTHENTICATED_SUCCESS:
      return Object.assign({}, state, {
        authenticated: true,
        authToken: (action as AuthenticatedSuccessAction).payload.authToken
      });

    case AuthActionTypes.RETRIEVE_AUTHENTICATED_EPERSON_SUCCESS:
      return Object.assign({}, state, {
        loaded: true,
        error: undefined,
        loading: false,
        blocking: false,
        info: undefined,
        userId: (action as RetrieveAuthenticatedEpersonSuccessAction).payload
      });

    case AuthActionTypes.AUTHENTICATE_ERROR:
      return Object.assign({}, state, {
        authenticated: false,
        authToken: undefined,
        error: (action as AuthenticationErrorAction).payload.message,
        blocking: false,
        loading: false
      });

    case AuthActionTypes.AUTHENTICATE_SUCCESS:
    case AuthActionTypes.LOG_OUT:
      return state;

    case AuthActionTypes.LOG_OUT_ERROR:
      return Object.assign({}, state, {
        authenticated: true,
        error: (action as LogOutErrorAction).payload.message
      });

    case AuthActionTypes.REFRESH_TOKEN_ERROR:
      return Object.assign({}, state, {
        authenticated: false,
        authToken: undefined,
        error: undefined,
        loaded: false,
        blocking: false,
        loading: false,
        info: undefined,
        refreshing: false,
        userId: undefined
      });

    case AuthActionTypes.LOG_OUT_SUCCESS:
      return Object.assign({}, state, {
        authenticated: false,
        authToken: undefined,
        error: undefined,
        loaded: false,
        blocking: true,
        loading: true,
        info: undefined,
        refreshing: false,
        userId: undefined
      });

    case AuthActionTypes.REDIRECT_AUTHENTICATION_REQUIRED:
    case AuthActionTypes.REDIRECT_TOKEN_EXPIRED:
      return Object.assign({}, state, {
        authenticated: false,
        authToken: undefined,
        loaded: false,
        blocking: false,
        loading: false,
        info: (action as RedirectWhenTokenExpiredAction as RedirectWhenAuthenticationIsRequiredAction).payload,
        userId: undefined
      });

    case AuthActionTypes.REFRESH_TOKEN:
      return Object.assign({}, state, {
        refreshing: true,
      });

    case AuthActionTypes.REFRESH_TOKEN_SUCCESS:
      return Object.assign({}, state, {
        authToken: (action as RefreshTokenSuccessAction).payload,
        refreshing: false,
        blocking: false
      });

    case AuthActionTypes.ADD_MESSAGE:
      return Object.assign({}, state, {
        info: (action as AddAuthenticationMessageAction).payload,
      });

    case AuthActionTypes.RESET_MESSAGES:
      return Object.assign({}, state, {
        error: undefined,
        info: undefined,
      });

    // next three cases are used by dynamic rendering of login methods
    case AuthActionTypes.RETRIEVE_AUTH_METHODS:
      return Object.assign({}, state, {
        loading: true,
      });

    case AuthActionTypes.RETRIEVE_AUTH_METHODS_SUCCESS:
      return Object.assign({}, state, {
        loading: false,
        blocking: false,
        authMethods: (action as RetrieveAuthMethodsSuccessAction).payload
      });

    case AuthActionTypes.RETRIEVE_AUTH_METHODS_ERROR:
      return Object.assign({}, state, {
        loading: false,
        blocking: false,
        authMethods: [new AuthMethod(AuthMethodType.Password)]
      });

    case AuthActionTypes.SET_REDIRECT_URL:
      return Object.assign({}, state, {
        redirectUrl: (action as SetRedirectUrlAction).payload,
      });

    case AuthActionTypes.REDIRECT_AFTER_LOGIN_SUCCESS:
      return Object.assign({}, state, {
        loading: true,
        blocking: true,
      });

    case AuthActionTypes.SET_USER_AS_IDLE:
      return Object.assign({}, state, {
        idle: true,
      });

    case AuthActionTypes.UNSET_USER_AS_IDLE:
      return Object.assign({}, state, {
        idle: false,
      });

    case StoreActionTypes.REHYDRATE:
      return Object.assign({}, state, {
        blocking: true,
      });

    default:
      return state;
  }
}
