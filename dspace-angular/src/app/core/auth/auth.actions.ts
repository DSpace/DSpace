/* eslint-disable max-classes-per-file */
// import @ngrx
import { Action } from '@ngrx/store';
// import type function
import { type } from '../../shared/ngrx/type';
// import models
import { AuthTokenInfo } from './models/auth-token-info.model';
import { AuthMethod } from './models/auth.method';
import { AuthStatus } from './models/auth-status.model';

export const AuthActionTypes = {
  AUTHENTICATE: type('dspace/auth/AUTHENTICATE'),
  AUTHENTICATE_ERROR: type('dspace/auth/AUTHENTICATE_ERROR'),
  AUTHENTICATE_SUCCESS: type('dspace/auth/AUTHENTICATE_SUCCESS'),
  AUTHENTICATED: type('dspace/auth/AUTHENTICATED'),
  AUTHENTICATED_ERROR: type('dspace/auth/AUTHENTICATED_ERROR'),
  AUTHENTICATED_SUCCESS: type('dspace/auth/AUTHENTICATED_SUCCESS'),
  CHECK_AUTHENTICATION_TOKEN: type('dspace/auth/CHECK_AUTHENTICATION_TOKEN'),
  CHECK_AUTHENTICATION_TOKEN_COOKIE: type('dspace/auth/CHECK_AUTHENTICATION_TOKEN_COOKIE'),
  RETRIEVE_AUTH_METHODS: type('dspace/auth/RETRIEVE_AUTH_METHODS'),
  RETRIEVE_AUTH_METHODS_SUCCESS: type('dspace/auth/RETRIEVE_AUTH_METHODS_SUCCESS'),
  RETRIEVE_AUTH_METHODS_ERROR: type('dspace/auth/RETRIEVE_AUTH_METHODS_ERROR'),
  REDIRECT_TOKEN_EXPIRED: type('dspace/auth/REDIRECT_TOKEN_EXPIRED'),
  REDIRECT_AUTHENTICATION_REQUIRED: type('dspace/auth/REDIRECT_AUTHENTICATION_REQUIRED'),
  REFRESH_TOKEN: type('dspace/auth/REFRESH_TOKEN'),
  REFRESH_TOKEN_SUCCESS: type('dspace/auth/REFRESH_TOKEN_SUCCESS'),
  REFRESH_TOKEN_ERROR: type('dspace/auth/REFRESH_TOKEN_ERROR'),
  RETRIEVE_TOKEN: type('dspace/auth/RETRIEVE_TOKEN'),
  ADD_MESSAGE: type('dspace/auth/ADD_MESSAGE'),
  RESET_MESSAGES: type('dspace/auth/RESET_MESSAGES'),
  LOG_OUT: type('dspace/auth/LOG_OUT'),
  LOG_OUT_ERROR: type('dspace/auth/LOG_OUT_ERROR'),
  LOG_OUT_SUCCESS: type('dspace/auth/LOG_OUT_SUCCESS'),
  SET_REDIRECT_URL: type('dspace/auth/SET_REDIRECT_URL'),
  RETRIEVE_AUTHENTICATED_EPERSON: type('dspace/auth/RETRIEVE_AUTHENTICATED_EPERSON'),
  RETRIEVE_AUTHENTICATED_EPERSON_SUCCESS: type('dspace/auth/RETRIEVE_AUTHENTICATED_EPERSON_SUCCESS'),
  RETRIEVE_AUTHENTICATED_EPERSON_ERROR: type('dspace/auth/RETRIEVE_AUTHENTICATED_EPERSON_ERROR'),
  REDIRECT_AFTER_LOGIN_SUCCESS: type('dspace/auth/REDIRECT_AFTER_LOGIN_SUCCESS'),
  SET_USER_AS_IDLE: type('dspace/auth/SET_USER_AS_IDLE'),
  UNSET_USER_AS_IDLE: type('dspace/auth/UNSET_USER_AS_IDLE')
};


/**
 * Authenticate.
 * @class AuthenticateAction
 * @implements {Action}
 */
export class AuthenticateAction implements Action {
  public type: string = AuthActionTypes.AUTHENTICATE;
  payload: {
    email: string;
    password: string
  };

  constructor(email: string, password: string) {
    this.payload = { email, password };
  }
}

/**
 * Checks if user is authenticated.
 * @class AuthenticatedAction
 * @implements {Action}
 */
export class AuthenticatedAction implements Action {
  public type: string = AuthActionTypes.AUTHENTICATED;
  payload: AuthTokenInfo;

  constructor(token: AuthTokenInfo) {
    this.payload = token;
  }
}

/**
 * Authenticated check success.
 * @class AuthenticatedSuccessAction
 * @implements {Action}
 */
export class AuthenticatedSuccessAction implements Action {
  public type: string = AuthActionTypes.AUTHENTICATED_SUCCESS;
  payload: {
    authenticated: boolean;
    authToken: AuthTokenInfo;
    userHref: string
  };

  constructor(authenticated: boolean, authToken: AuthTokenInfo, userHref: string) {
    this.payload = { authenticated, authToken, userHref };
  }
}

/**
 * Authenticated check error.
 * @class AuthenticatedErrorAction
 * @implements {Action}
 */
export class AuthenticatedErrorAction implements Action {
  public type: string = AuthActionTypes.AUTHENTICATED_ERROR;
  payload: Error;

  constructor(payload: Error) {
    this.payload = payload;
  }
}

/**
 * Authentication error.
 * @class AuthenticationErrorAction
 * @implements {Action}
 */
export class AuthenticationErrorAction implements Action {
  public type: string = AuthActionTypes.AUTHENTICATE_ERROR;
  payload: Error;

  constructor(payload: Error) {
    this.payload = payload;
  }
}

/**
 * Authentication success.
 * @class AuthenticationSuccessAction
 * @implements {Action}
 */
export class AuthenticationSuccessAction implements Action {
  public type: string = AuthActionTypes.AUTHENTICATE_SUCCESS;
  payload: AuthTokenInfo;

  constructor(token: AuthTokenInfo) {
    this.payload = token;
  }
}

/**
 * Check if token is already present upon initial load.
 * @class CheckAuthenticationTokenAction
 * @implements {Action}
 */
export class CheckAuthenticationTokenAction implements Action {
  public type: string = AuthActionTypes.CHECK_AUTHENTICATION_TOKEN;
}

/**
 * Check Authentication Token Error.
 * @class CheckAuthenticationTokenCookieAction
 * @implements {Action}
 */
export class CheckAuthenticationTokenCookieAction implements Action {
  public type: string = AuthActionTypes.CHECK_AUTHENTICATION_TOKEN_COOKIE;
}

/**
 * Sign out.
 * @class LogOutAction
 * @implements {Action}
 */
export class LogOutAction implements Action {
  public type: string = AuthActionTypes.LOG_OUT;

  constructor(public payload?: any) {
  }
}

/**
 * Sign out error.
 * @class LogOutErrorAction
 * @implements {Action}
 */
export class LogOutErrorAction implements Action {
  public type: string = AuthActionTypes.LOG_OUT_ERROR;
  payload: Error;

  constructor(payload: Error) {
    this.payload = payload;
  }
}

/**
 * Sign out success.
 * @class LogOutSuccessAction
 * @implements {Action}
 */
export class LogOutSuccessAction implements Action {
  public type: string = AuthActionTypes.LOG_OUT_SUCCESS;

  constructor(public payload?: any) {
  }
}

/**
 * Redirect to login page when authentication is required.
 * @class RedirectWhenAuthenticationIsRequiredAction
 * @implements {Action}
 */
export class RedirectWhenAuthenticationIsRequiredAction implements Action {
  public type: string = AuthActionTypes.REDIRECT_AUTHENTICATION_REQUIRED;
  payload: string;

  constructor(message: string) {
    this.payload = message;
  }
}

/**
 * Redirect to login page when token is expired.
 * @class RedirectWhenTokenExpiredAction
 * @implements {Action}
 */
export class RedirectWhenTokenExpiredAction implements Action {
  public type: string = AuthActionTypes.REDIRECT_TOKEN_EXPIRED;
  payload: string;

  constructor(message: string) {
    this.payload = message;
  }
}

/**
 * Refresh authentication token.
 * @class RefreshTokenAction
 * @implements {Action}
 */
export class RefreshTokenAction implements Action {
  public type: string = AuthActionTypes.REFRESH_TOKEN;
  payload: AuthTokenInfo;

  constructor(token: AuthTokenInfo) {
    this.payload = token;
  }
}

/**
 * Refresh authentication token success.
 * @class RefreshTokenSuccessAction
 * @implements {Action}
 */
export class RefreshTokenSuccessAction implements Action {
  public type: string = AuthActionTypes.REFRESH_TOKEN_SUCCESS;
  payload: AuthTokenInfo;

  constructor(token: AuthTokenInfo) {
    this.payload = token;
  }
}

/**
 * Refresh authentication token error.
 * @class RefreshTokenErrorAction
 * @implements {Action}
 */
export class RefreshTokenErrorAction implements Action {
  public type: string = AuthActionTypes.REFRESH_TOKEN_ERROR;
}

/**
 * Retrieve authentication token.
 * @class RetrieveTokenAction
 * @implements {Action}
 */
export class RetrieveTokenAction implements Action {
  public type: string = AuthActionTypes.RETRIEVE_TOKEN;
}

/**
 * Add uthentication message.
 * @class AddAuthenticationMessageAction
 * @implements {Action}
 */
export class AddAuthenticationMessageAction implements Action {
  public type: string = AuthActionTypes.ADD_MESSAGE;
  payload: string;

  constructor(message: string) {
    this.payload = message;
  }
}

/**
 * Reset error.
 * @class ResetAuthenticationMessagesAction
 * @implements {Action}
 */
export class ResetAuthenticationMessagesAction implements Action {
  public type: string = AuthActionTypes.RESET_MESSAGES;
}

// // Next three Actions are used by dynamic login methods
/**
 * Action that triggers an effect fetching the authentication methods enabled ant the backend
 * @class  RetrieveAuthMethodsAction
 * @implements {Action}
 */
export class RetrieveAuthMethodsAction implements Action {
  public type: string = AuthActionTypes.RETRIEVE_AUTH_METHODS;

  payload: AuthStatus;

  constructor(authStatus: AuthStatus) {
    this.payload = authStatus;
  }
}

/**
 * Get Authentication methods enabled at the backend
 * @class RetrieveAuthMethodsSuccessAction
 * @implements {Action}
 */
export class RetrieveAuthMethodsSuccessAction implements Action {
  public type: string = AuthActionTypes.RETRIEVE_AUTH_METHODS_SUCCESS;
  payload: AuthMethod[];

  constructor(authMethods: AuthMethod[] ) {
    this.payload = authMethods;
  }
}

/**
 * Set password as default authentication method on error
 * @class RetrieveAuthMethodsErrorAction
 * @implements {Action}
 */
export class RetrieveAuthMethodsErrorAction implements Action {
  public type: string = AuthActionTypes.RETRIEVE_AUTH_METHODS_ERROR;
}

/**
 * Change the redirect url.
 * @class SetRedirectUrlAction
 * @implements {Action}
 */
export class SetRedirectUrlAction implements Action {
  public type: string = AuthActionTypes.SET_REDIRECT_URL;
  payload: string;

  constructor(url: string) {
    this.payload = url;
  }
}

/**
 * Start loading for a hard redirect
 * @class StartHardRedirectLoadingAction
 * @implements {Action}
 */
export class RedirectAfterLoginSuccessAction implements Action {
  public type: string = AuthActionTypes.REDIRECT_AFTER_LOGIN_SUCCESS;
  payload: string;

  constructor(url: string) {
    this.payload = url;
  }
}

/**
 * Retrieve the authenticated eperson.
 * @class RetrieveAuthenticatedEpersonAction
 * @implements {Action}
 */
export class RetrieveAuthenticatedEpersonAction implements Action {
  public type: string = AuthActionTypes.RETRIEVE_AUTHENTICATED_EPERSON;
  payload: string;

  constructor(user: string) {
    this.payload = user ;
  }
}

/**
 * Set the authenticated eperson in the state.
 * @class RetrieveAuthenticatedEpersonSuccessAction
 * @implements {Action}
 */
export class RetrieveAuthenticatedEpersonSuccessAction implements Action {
  public type: string = AuthActionTypes.RETRIEVE_AUTHENTICATED_EPERSON_SUCCESS;
  payload: string;

  constructor(userId: string) {
    this.payload = userId ;
  }
}

/**
 * Set the authenticated eperson in the state.
 * @class RetrieveAuthenticatedEpersonSuccessAction
 * @implements {Action}
 */
export class RetrieveAuthenticatedEpersonErrorAction implements Action {
  public type: string = AuthActionTypes.RETRIEVE_AUTHENTICATED_EPERSON_ERROR;
  payload: Error;

  constructor(payload: Error) {
    this.payload = payload ;
  }
}

/**
 * Set the current user as being idle.
 * @class SetUserAsIdleAction
 * @implements {Action}
 */
export class SetUserAsIdleAction implements Action {
  public type: string = AuthActionTypes.SET_USER_AS_IDLE;
}

/**
 * Unset the current user as being idle.
 * @class UnsetUserAsIdleAction
 * @implements {Action}
 */
export class UnsetUserAsIdleAction implements Action {
  public type: string = AuthActionTypes.UNSET_USER_AS_IDLE;
}

/**
 * Actions type.
 * @type {AuthActions}
 */
export type AuthActions
  = AuthenticateAction
  | AuthenticatedAction
  | AuthenticatedErrorAction
  | AuthenticatedSuccessAction
  | AuthenticationErrorAction
  | AuthenticationSuccessAction
  | CheckAuthenticationTokenAction
  | CheckAuthenticationTokenCookieAction
  | RedirectWhenAuthenticationIsRequiredAction
  | RedirectWhenTokenExpiredAction
  | AddAuthenticationMessageAction
  | RefreshTokenAction
  | RefreshTokenErrorAction
  | RefreshTokenSuccessAction
  | ResetAuthenticationMessagesAction
  | RetrieveAuthMethodsAction
  | RetrieveAuthMethodsSuccessAction
  | RetrieveAuthMethodsErrorAction
  | RetrieveTokenAction
  | RetrieveAuthenticatedEpersonAction
  | RetrieveAuthenticatedEpersonErrorAction
  | RetrieveAuthenticatedEpersonSuccessAction
  | SetRedirectUrlAction
  | RedirectAfterLoginSuccessAction
  | SetUserAsIdleAction
  | UnsetUserAsIdleAction;

