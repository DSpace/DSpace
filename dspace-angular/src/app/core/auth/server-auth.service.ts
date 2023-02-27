import { Injectable } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';

import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { hasValue, isNotEmpty } from '../../shared/empty.util';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import { AuthService } from './auth.service';
import { AuthStatus } from './models/auth-status.model';
import { AuthTokenInfo } from './models/auth-token-info.model';
import { RemoteData } from '../data/remote-data';

/**
 * The auth service.
 */
@Injectable()
export class ServerAuthService extends AuthService {

  /**
   * Returns the authenticated user
   * @returns {User}
   */
  public authenticatedUser(token: AuthTokenInfo): Observable<string> {
    // Determine if the user has an existing auth session on the server
    const options: HttpOptions = Object.create({});
    let headers = new HttpHeaders();

    headers = headers.append('Accept', 'application/json');
    headers = headers.append('Authorization', `Bearer ${token.accessToken}`);

    options.headers = headers;
    return this.authRequestService.getRequest('status', options).pipe(
      map((rd: RemoteData<AuthStatus>) => {
        const status = rd.payload;
        if (hasValue(status) && status.authenticated) {
          return status._links.eperson.href;
        } else {
          throw (new Error('Not authenticated'));
        }
      }));
  }

  /**
   * Checks if token is present into the request cookie
   */
  public checkAuthenticationCookie(): Observable<AuthStatus> {
    // Determine if the user has an existing auth session on the server
    const options: HttpOptions = Object.create({});
    let headers = new HttpHeaders();
    headers = headers.append('Accept', 'application/json');
    if (isNotEmpty(this.req.protocol) && isNotEmpty(this.req.header('host'))) {
      const referer = this.req.protocol + '://' + this.req.header('host') + this.req.path;
      // use to allow the rest server to identify the real origin on SSR
      headers = headers.append('X-Requested-With', referer);
    }
    options.headers = headers;
    options.withCredentials = true;
    return this.authRequestService.getRequest('status', options).pipe(
      map((rd: RemoteData<AuthStatus>) => Object.assign(new AuthStatus(), rd.payload))
    );
  }
}
