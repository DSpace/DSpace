import { Observable, of as observableOf } from 'rxjs';
import { AuthStatus } from '../../core/auth/models/auth-status.model';
import { AuthTokenInfo } from '../../core/auth/models/auth-token-info.model';
import { EPersonMock } from './eperson.mock';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { createSuccessfulRemoteDataObject$ } from '../remote-data.utils';
import { AuthMethod } from '../../core/auth/models/auth.method';
import { hasValue } from '../empty.util';

export const authMethodsMock = [
  new AuthMethod('password'),
  new AuthMethod('shibboleth', 'dspace.test/shibboleth')
];

export class AuthServiceStub {

  token: AuthTokenInfo = new AuthTokenInfo('token_test');
  impersonating: string;
  private _tokenExpired = false;
  private redirectUrl;

  constructor() {
    this.token.expires = Date.now() + (1000 * 60 * 60);
  }

  public authenticate(user: string, password: string): Observable<AuthStatus> {
    if (user === 'user' && password === 'password') {
      const authStatus = new AuthStatus();
      authStatus.okay = true;
      authStatus.authenticated = true;
      authStatus.token = this.token;
      authStatus.eperson = createSuccessfulRemoteDataObject$(EPersonMock);
      return observableOf(authStatus);
    } else {
      console.log('error');
      throw (new Error('Message Error test'));
    }
  }

  public authenticatedUser(token: AuthTokenInfo): Observable<string> {
    if (token.accessToken === 'token_test') {
      return observableOf(EPersonMock._links.self.href);
    } else {
      throw (new Error('Message Error test'));
    }
  }

  public retrieveAuthenticatedUserByHref(href: string): Observable<EPerson> {
    return observableOf(EPersonMock);
  }

  public retrieveAuthenticatedUserById(id: string): Observable<EPerson> {
    return observableOf(EPersonMock);
  }

  public buildAuthHeader(token?: AuthTokenInfo): string {
    return `Bearer ${token ? token.accessToken : ''}`;
  }

  public getToken(): AuthTokenInfo {
    return this.token;
  }

  public hasValidAuthenticationToken(): Observable<AuthTokenInfo> {
    return observableOf(this.token);
  }

  public logout(): Observable<boolean> {
    return observableOf(true);
  }

  public isTokenExpired(token?: AuthTokenInfo): boolean {
    return this._tokenExpired;
  }

  /**
   * This method is used to ease testing
   */
  public setTokenAsExpired() {
    this._tokenExpired = true;
  }

  /**
   * This method is used to ease testing
   */
  public setTokenAsNotExpired() {
    this._tokenExpired = false;
  }

  public isTokenExpiring(): Observable<boolean> {
    return observableOf(false);
  }

  public refreshAuthenticationToken(token: AuthTokenInfo): Observable<AuthTokenInfo> {
    return observableOf(this.token);
  }

  public redirectToPreviousUrl() {
    return;
  }

  public removeToken() {
    return;
  }

  setRedirectUrl(url: string) {
    this.redirectUrl = url;
  }

  getRedirectUrl() {
    return observableOf(this.redirectUrl);
  }

  public storeToken(token: AuthTokenInfo) {
    return;
  }

  isAuthenticated() {
    return observableOf(true);
  }

  checkAuthenticationCookie() {
    return;
  }

  retrieveAuthMethodsFromAuthStatus(status: AuthStatus) {
    return observableOf(authMethodsMock);
  }

  impersonate(id: string) {
    this.impersonating = id;
  }

  isImpersonating() {
    return hasValue(this.impersonating);
  }

  isImpersonatingUser(id: string) {
    return this.impersonating === id;
  }

  stopImpersonating() {
    this.impersonating = undefined;
  }

  stopImpersonatingAndRefresh() {
    this.stopImpersonating();
  }

  getImpersonateID() {
    return this.impersonating;
  }

  resetAuthenticationError() {
    return;
  }

  setRedirectUrlIfNotSet(url: string) {
    return;
  }

  redirectAfterLoginSuccess() {
    return;
  }

  clearRedirectUrl() {
    return;
  }
}
