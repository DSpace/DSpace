import { Observable, of as observableOf } from 'rxjs';
import { CacheableObject } from '../../core/cache/cacheable-object.model';

/**
 * Stub class for {@link BaseDataService}
 */
export abstract class BaseDataServiceStub<T extends CacheableObject> {

  invalidateByHref(_href: string): Observable<boolean> {
    return observableOf(true);
  }

}
