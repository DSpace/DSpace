import { createSelector } from '@ngrx/store';

/**
 * Every reducer module's default export is the reducer function itself. In
 * addition, each module should export a type or interface that describes
 * the state of the reducer plus any selector functions. The `* as`
 * notation packages up all of the exports into a single object.
 */
import { AuthState } from './auth.reducer';
import { CoreState } from '../core-state.model';
import { coreSelector } from '../core.selectors';

/**
 * Returns the user state.
 * @function getUserState
 * @param {AppState} state Top level state.
 * @return {AuthState}
 */
export const getAuthState = createSelector(coreSelector, (state: CoreState) => state.auth);

/**
 * Returns true if the user is authenticated.
 * @function _isAuthenticated
 * @param {State} state
 * @returns {boolean}
 */
const _isAuthenticated = (state: AuthState) => state.authenticated;

/**
 * Returns true if the authenticated has loaded.
 * @function _isAuthenticatedLoaded
 * @param {State} state
 * @returns {boolean}
 */
const _isAuthenticatedLoaded = (state: AuthState) => state.loaded;

/**
 * Return the users state
 * @function _getAuthenticatedUserId
 * @param {State} state
 * @returns {string} User ID
 */
const _getAuthenticatedUserId = (state: AuthState) => state.userId;

/**
 * Returns the authentication error.
 * @function _getAuthenticationError
 * @param {State} state
 * @returns {string}
 */
const _getAuthenticationError = (state: AuthState) => state.error;

/**
 * Returns the authentication info message.
 * @function _getAuthenticationInfo
 * @param {State} state
 * @returns {string}
 */
const _getAuthenticationInfo = (state: AuthState) => state.info;

/**
 * Returns true if request is in progress.
 * @function _isLoading
 * @param {State} state
 * @returns {boolean}
 */
const _isLoading = (state: AuthState) => state.loading;

/**
 * Returns true if everything else should wait for authentication.
 * @function _isBlocking
 * @param {State} state
 * @returns {boolean}
 */
const _isBlocking = (state: AuthState) => state.blocking;

/**
 * Returns true if a refresh token request is in progress.
 * @function _isRefreshing
 * @param {State} state
 * @returns {boolean}
 */
const _isRefreshing = (state: AuthState) => state.refreshing;

/**
 * Returns the authentication token.
 * @function _getAuthenticationToken
 * @param {State} state
 * @returns {AuthToken}
 */
const _getAuthenticationToken = (state: AuthState) => state.authToken;

/**
 * Returns the sign out error.
 * @function _getLogOutError
 * @param {State} state
 * @returns {string}
 */
const _getLogOutError = (state: AuthState) => state.error;

/**
 * Returns the sign up error.
 * @function _getRegistrationError
 * @param {State} state
 * @returns {string}
 */
const _getRegistrationError = (state: AuthState) => state.error;

/**
 * Returns the redirect url.
 * @function _getRedirectUrl
 * @param {State} state
 * @returns {string}
 */
const _getRedirectUrl = (state: AuthState) => state.redirectUrl;

const _getAuthenticationMethods = (state: AuthState) => state.authMethods;

/**
 * Returns true if the user is idle.
 * @function _isIdle
 * @param {State} state
 * @returns {boolean}
 */
const _isIdle = (state: AuthState) => state.idle;

/**
 * Returns the authentication methods enabled at the backend
 * @function getAuthenticationMethods
 * @param {AuthState} state
 * @param {any} props
 * @return {any}
 */
export const getAuthenticationMethods = createSelector(getAuthState, _getAuthenticationMethods);

/**
 * Returns the authenticated user id
 * @function getAuthenticatedUserId
 * @param {AuthState} state
 * @param {any} props
 * @return {string} User ID
 */
export const getAuthenticatedUserId = createSelector(getAuthState, _getAuthenticatedUserId);

/**
 * Returns the authentication error.
 * @function getAuthenticationError
 * @param {AuthState} state
 * @param {any} props
 * @return {Error}
 */
export const getAuthenticationError = createSelector(getAuthState, _getAuthenticationError);

/**
 * Returns the authentication info message.
 * @function getAuthenticationInfo
 * @param {AuthState} state
 * @param {any} props
 * @return {string}
 */
export const getAuthenticationInfo = createSelector(getAuthState, _getAuthenticationInfo);

/**
 * Returns true if the user is authenticated
 * @function isAuthenticated
 * @param {AuthState} state
 * @param {any} props
 * @return {boolean}
 */
export const isAuthenticated = createSelector(getAuthState, _isAuthenticated);

/**
 * Returns true if the user is authenticated
 * @function isAuthenticated
 * @param {AuthState} state
 * @param {any} props
 * @return {boolean}
 */
export const isAuthenticatedLoaded = createSelector(getAuthState, _isAuthenticatedLoaded);

/**
 * Returns true if the authentication request is loading.
 * @function isAuthenticationLoading
 * @param {AuthState} state
 * @param {any} props
 * @return {boolean}
 */
export const isAuthenticationLoading = createSelector(getAuthState, _isLoading);

/**
 * Returns true if the authentication should block everything else
 *
 * @function isAuthenticationBlocking
 * @param {AuthState} state
 * @param {any} props
 * @return {boolean}
 */
export const isAuthenticationBlocking = createSelector(getAuthState, _isBlocking);

/**
 * Returns true if the refresh token request is loading.
 * @function isTokenRefreshing
 * @param {AuthState} state
 * @param {any} props
 * @return {boolean}
 */
export const isTokenRefreshing = createSelector(getAuthState, _isRefreshing);

/**
 * Returns the authentication token.
 * @function getAuthenticationToken
 * @param {State} state
 * @returns {AuthToken}
 */
export const getAuthenticationToken = createSelector(getAuthState, _getAuthenticationToken);

/**
 * Returns the log out error.
 * @function getLogOutError
 * @param {AuthState} state
 * @param {any} props
 * @return {Error}
 */
export const getLogOutError = createSelector(getAuthState, _getLogOutError);

/**
 * Returns the registration error.
 * @function getRegistrationError
 * @param {AuthState} state
 * @param {any} props
 * @return {Error}
 */
export const getRegistrationError = createSelector(getAuthState, _getRegistrationError);

/**
 * Returns the redirect url.
 * @function getRedirectUrl
 * @param {AuthState} state
 * @param {any} props
 * @return {string}
 */
export const getRedirectUrl = createSelector(getAuthState, _getRedirectUrl);

/**
 * Returns true if the user is idle
 * @function isIdle
 * @param {AuthState} state
 * @param {any} props
 * @return {boolean}
 */
export const isIdle = createSelector(getAuthState, _isIdle);
