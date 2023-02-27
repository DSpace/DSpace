import { Observable, of as observableOf, throwError as observableThrowError } from 'rxjs';

import { catchError, map } from 'rxjs/operators';
import { Injectable, Injector } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpHeaders,
  HttpInterceptor,
  HttpRequest,
  HttpResponse,
  HttpResponseBase
} from '@angular/common/http';

import { AppState } from '../../app.reducer';
import { AuthService } from './auth.service';
import { AuthStatus } from './models/auth-status.model';
import { AuthTokenInfo } from './models/auth-token-info.model';
import { hasValue, isNotEmpty, isNotNull } from '../../shared/empty.util';
import { RedirectWhenTokenExpiredAction } from './auth.actions';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { AuthMethod } from './models/auth.method';
import { AuthMethodType } from './models/auth.method-type';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  // Interceptor is called twice per request,
  // so to prevent RefreshTokenAction is dispatched twice
  // we're creating a refresh token request list
  protected refreshTokenRequestUrls = [];

  constructor(private inj: Injector, private router: Router, private store: Store<AppState>) {
  }

  /**
   * Check if response status code is 401
   *
   * @param response
   */
  private isUnauthorized(response: HttpResponseBase): boolean {
    // invalid_token The access token provided is expired, revoked, malformed, or invalid for other reasons
    return response.status === 401;
  }

  /**
   * Check if response status code is 200 or 204
   *
   * @param response
   */
  private isSuccess(response: HttpResponseBase): boolean {
    return (response.status === 200 || response.status === 204);
  }

  /**
   * Check if http request is to authn endpoint
   *
   * @param http
   */
  private isAuthRequest(http: HttpRequest<any> | HttpResponseBase): boolean {
    return http && http.url
      && (http.url.endsWith('/authn/login')
        || http.url.endsWith('/authn/logout')
        || http.url.endsWith('/authn/status'));
  }

  /**
   * Check if response is from a login request
   *
   * @param http
   */
  private isLoginResponse(http: HttpRequest<any> | HttpResponseBase): boolean {
    return http.url && http.url.endsWith('/authn/login');
  }

  /**
   * Check if response is from a logout request
   *
   * @param http
   */
  private isLogoutResponse(http: HttpRequest<any> | HttpResponseBase): boolean {
    return http.url && http.url.endsWith('/authn/logout');
  }

  /**
   * Check if response is from a status request
   *
   * @param http
   */
  private isStatusResponse(http: HttpRequest<any> | HttpResponseBase): boolean {
    return http.url && http.url.endsWith('/authn/status');
  }

  /**
   * Extract location url from the WWW-Authenticate header
   *
   * @param header
   */
  private parseLocation(header: string): string {
    let location = header.trim();
    location = location.replace('location="', '');
    location = location.replace('"', ''); /* lgtm [js/incomplete-sanitization] */
    let re = /%3A%2F%2F/g;
    location = location.replace(re, '://');
    re = /%3A/g;
    location = location.replace(re, ':');
    return location.trim();
  }

  /**
   * Sort authentication methods list
   *
   * @param authMethodModels
   */
  private sortAuthMethods(authMethodModels: AuthMethod[]): AuthMethod[] {
    const sortedAuthMethodModels: AuthMethod[] = [];
    authMethodModels.forEach((method) => {
      if (method.authMethodType === AuthMethodType.Password) {
        sortedAuthMethodModels.push(method);
      }
    });

    authMethodModels.forEach((method) => {
      if (method.authMethodType !== AuthMethodType.Password) {
        sortedAuthMethodModels.push(method);
      }
    });

    return sortedAuthMethodModels;
  }

  /**
   * Extract authentication methods list from the WWW-Authenticate headers
   *
   * @param headers
   */
  private parseAuthMethodsFromHeaders(headers: HttpHeaders): AuthMethod[] {
    let authMethodModels: AuthMethod[] = [];
    if (isNotEmpty(headers.get('www-authenticate'))) {
      // get the realms from the header -  a realm is a single auth method
      const completeWWWauthenticateHeader = headers.get('www-authenticate');
      const regex = /(\w+ (\w+=((".*?")|[^,]*)(, )?)*)/g;
      const realms = completeWWWauthenticateHeader.match(regex);

      // eslint-disable-next-line guard-for-in
      for (const j in realms) {

        const splittedRealm = realms[j].split(', ');
        const methodName = splittedRealm[0].split(' ')[0].trim();

        let authMethodModel: AuthMethod;
        if (splittedRealm.length === 1) {
          authMethodModel = new AuthMethod(methodName);
          authMethodModels.push(authMethodModel);
        } else if (splittedRealm.length > 1) {
          let location = splittedRealm[1];
          location = this.parseLocation(location);
          authMethodModel = new AuthMethod(methodName, location);
          authMethodModels.push(authMethodModel);
        }
      }

      // make sure the email + password login component gets rendered first
      authMethodModels = this.sortAuthMethods(authMethodModels);
    } else {
      authMethodModels.push(new AuthMethod(AuthMethodType.Password));
    }

    return authMethodModels;
  }

  /**
   * Generate an AuthStatus object
   *
   * @param authenticated
   * @param accessToken
   * @param error
   * @param httpHeaders
   */
  private makeAuthStatusObject(authenticated: boolean, accessToken?: string, error?: string, httpHeaders?: HttpHeaders): AuthStatus {
    const authStatus = new AuthStatus();
    // let authMethods: AuthMethodModel[];
    if (httpHeaders) {
      authStatus.authMethods = this.parseAuthMethodsFromHeaders(httpHeaders);
    }

    authStatus.id = null;

    authStatus.okay = true;
    // authStatus.authMethods = authMethods;

    if (authenticated) {
      authStatus.authenticated = true;
      authStatus.token = new AuthTokenInfo(accessToken);
    } else {
      authStatus.authenticated = false;
      if (isNotEmpty(error)) {
        if (typeof error === 'string') {
          try {
            authStatus.error = JSON.parse(error);
          } catch (e) {
            console.error('Unknown auth error "', error, '" caused ', e);
            authStatus.error = {
              error: 'Unknown',
              message: 'Unknown auth error',
              status: 500,
              timestamp: Date.now(),
              path: ''
              };
          }
        } else {
          authStatus.error = error;
        }
      }
    }
    return authStatus;
  }

  /**
   * Intercept method
   * @param req
   * @param next
   */
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    const authService = this.inj.get(AuthService);

    const token: AuthTokenInfo = authService.getToken();
    let newReq: HttpRequest<any>;
    let authorization: string;

    if (authService.isTokenExpired()) {
      return observableOf(null);
    } else if ((!this.isAuthRequest(req) || this.isLogoutResponse(req)) && isNotEmpty(token)) {
      // Get the auth header from the service.
      authorization = authService.buildAuthHeader(token);
      let newHeaders = req.headers.set('authorization', authorization);

      // When present, add the ID of the EPerson we're impersonating to the headers
      const impersonatingID = authService.getImpersonateID();
      if (hasValue(impersonatingID)) {
        newHeaders = newHeaders.set('X-On-Behalf-Of', impersonatingID);
      }

      // Clone the request to add the new header.
      newReq = req.clone({ headers: newHeaders });
    } else {
      newReq = req.clone();
    }

    // Pass on the new request instead of the original request.
    return next.handle(newReq).pipe(
      map((response) => {
        // Intercept a Login/Logout response
        if (response instanceof HttpResponse && this.isSuccess(response) && this.isAuthRequest(response)) {
          // It's a success Login/Logout response
          let authRes: HttpResponse<any>;
          if (this.isLoginResponse(response)) {
            // login successfully
            const newToken = response.headers.get('authorization');
            authRes = response.clone({
              body: this.makeAuthStatusObject(true, newToken)
            });

            // clean eventually refresh Requests list
            this.refreshTokenRequestUrls = [];
          } else if (this.isStatusResponse(response)) {
            authRes = response.clone({
              body: Object.assign(response.body, {
                authMethods: this.parseAuthMethodsFromHeaders(response.headers)
              })
            });
          } else {
            // logout successfully
            authRes = response.clone({
              body: this.makeAuthStatusObject(false)
            });
          }
          return authRes;
        } else {
          return response;
        }
      }),
      catchError((error, caught) => {
        // Intercept an error response
        if (error instanceof HttpErrorResponse) {

          // Checks if is a response from a request to an authentication endpoint
          if (this.isAuthRequest(error)) {
            // clean eventually refresh Requests list
            this.refreshTokenRequestUrls = [];

            // Create a new HttpResponse and return it, so it can be handle properly by AuthService.
            const authResponse = new HttpResponse({
              body: this.makeAuthStatusObject(false, null, error.error, error.headers),
              headers: error.headers,
              status: error.status,
              statusText: error.statusText,
              url: error.url
            });
            return observableOf(authResponse);
          } else if (this.isUnauthorized(error) && isNotNull(token) && authService.isTokenExpired()) {
            // The access token provided is expired, revoked, malformed, or invalid for other reasons
            // Redirect to the login route
            this.store.dispatch(new RedirectWhenTokenExpiredAction('auth.messages.expired'));
          }
        }
        // Return error response as is.
        return observableThrowError(error);
      })) as any;
  }
}
