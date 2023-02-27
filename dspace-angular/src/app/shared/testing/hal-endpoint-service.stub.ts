import { of as observableOf } from 'rxjs';
import { hasValue } from '../empty.util';

export class HALEndpointServiceStub {

  constructor(private url: string) {}
  getEndpoint(path: string, startHref?: string) {
    if (hasValue(startHref)) {
      return observableOf(startHref + '/' + path);
    }
    return observableOf(this.url + '/' + path);
  }
}
