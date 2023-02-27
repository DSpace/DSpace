import { CacheableObject } from '../../core/cache/cacheable-object.model';
import { BaseDataServiceStub } from './base-data-service.stub';
import { FollowLinkConfig } from '../utils/follow-link-config.model';
import { Observable, EMPTY } from 'rxjs';
import { RemoteData } from '../../core/data/remote-data';

/**
 * Stub class for {@link IdentifiableDataService}
 */
export class IdentifiableDataServiceStub<T extends CacheableObject> extends BaseDataServiceStub<T> {

  findById(_id: string, _useCachedVersionIfAvailable = true, _reRequestOnStale = true, ..._linksToFollow: FollowLinkConfig<T>[]): Observable<RemoteData<T>> {
    return EMPTY;
  }

}
