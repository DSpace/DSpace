import { Observable, of as observableOf } from 'rxjs';
import { HttpOptions } from '../../core/dspace-rest/dspace-rest.service';
import { AuthStatus } from '../../core/auth/models/auth-status.model';
import { AuthTokenInfo } from '../../core/auth/models/auth-token-info.model';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { isNotEmpty } from '../empty.util';
import { EPersonMock } from './eperson.mock';
import { createSuccessfulRemoteDataObject$ } from '../remote-data.utils';

export class AuthRequestServiceStub {
  protected mockUser: EPerson = EPersonMock;
  protected mockTokenInfo = new AuthTokenInfo('test_token');
  protected mockShortLivedToken = 'test-shortlived-token';

  public postToEndpoint(method: string, body: any, options?: HttpOptions): Observable<any> {
    const authStatusStub: AuthStatus = new AuthStatus();
    if (isNotEmpty(body)) {
      const parsedBody = this.parseQueryString(body);
      authStatusStub.okay = true;
      if (parsedBody.user === 'user' && parsedBody.password === 'password') {
        authStatusStub.authenticated = true;
        authStatusStub.token = this.mockTokenInfo;
      } else {
        authStatusStub.authenticated = false;
      }
    } else if (isNotEmpty(options)) {
      const token = (options.headers as any).lazyUpdate[1].value;
      if (this.validateToken(token)) {
        authStatusStub.authenticated = true;
        authStatusStub.token = this.mockTokenInfo;
        authStatusStub._links = {
          self: {
            href: 'dspace.org/api/status',
          },
          eperson: {
            href: this.mockUser._links.self.href
          },
          specialGroups: {
            href: this.mockUser._links.self.href
          }
        };
      } else {
        authStatusStub.authenticated = false;
      }
    } else {
      authStatusStub.authenticated = false;
    }
    return createSuccessfulRemoteDataObject$(authStatusStub);
  }

  public getRequest(method: string, options?: HttpOptions): Observable<any> {
    const authStatusStub: AuthStatus = new AuthStatus();
    switch (method) {
      case 'logout':
        authStatusStub.authenticated = false;
        break;
      case 'status':
        const token = ((options.headers as any).lazyUpdate[1]) ? (options.headers as any).lazyUpdate[1].value : null;
        if (this.validateToken(token)) {
          authStatusStub.authenticated = true;
          authStatusStub.token = this.mockTokenInfo;
          authStatusStub._links = {
            self: {
              href: 'dspace.org/api/status',
            },
            eperson: {
              href: this.mockUser._links.self.href
            },
            specialGroups: {
              href: this.mockUser._links.self.href
            }
          };
        } else {
          authStatusStub.authenticated = false;
        }
        break;
    }
    return createSuccessfulRemoteDataObject$(authStatusStub);
  }

  private validateToken(token): boolean {
    return (token === 'Bearer test_token');
  }
  private parseQueryString(query): any {
    const obj = Object.create({});
    const vars = query.split('&');
    for (const param of vars) {
      const pair = param.split('=');
      obj[pair[0]] = pair[1];
    }
    return obj;
  }

  public getShortlivedToken() {
    return observableOf(this.mockShortLivedToken);
  }
}
