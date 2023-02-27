/* eslint-disable no-empty, @typescript-eslint/no-empty-function */
import { Observable, of as observableOf } from 'rxjs';

export class AuthServiceMock {
  public checksAuthenticationToken() {
    return;
  }
  public buildAuthHeader() {
    return 'auth-header';
  }

  public getShortlivedToken(): Observable<string> {
    return observableOf('token');
  }

  public isAuthenticated(): Observable<boolean> {
    return observableOf(true);
  }

  public setRedirectUrl(url: string) {
  }

  public trackTokenExpiration(): void {
  }

  public isUserIdle(): Observable<boolean> {
    return observableOf(false);
  }
}
