import { ObjectCacheService } from '../../core/cache/object-cache.service';

export function getMockObjectCacheService(): ObjectCacheService {
  return jasmine.createSpyObj('objectCacheService', [
    'add',
    'remove',
    'getByUUID',
    'getByHref',
    'getObjectByHref',
    'getRequestHrefBySelfLink',
    'getRequestHrefByUUID',
    'getList',
    'hasByUUID',
    'hasByHref',
    'getRequestUUIDBySelfLink',
    'addDependency',
    'removeDependents',
  ]);

}
