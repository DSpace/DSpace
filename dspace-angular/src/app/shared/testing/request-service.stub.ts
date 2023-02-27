import { Observable, of as observableOf } from 'rxjs';

/**
 * Stub service for {@link RequestService}.
 */
export class RequestServiceStub {

  removeByHrefSubstring(_href: string): Observable<boolean> {
    return observableOf(true);
  }

}
