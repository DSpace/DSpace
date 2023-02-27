import { DSOBreadcrumbResolver } from './dso-breadcrumb.resolver';
import { Collection } from '../shared/collection.model';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { getTestScheduler } from 'jasmine-marbles';
import { CollectionBreadcrumbResolver } from './collection-breadcrumb.resolver';

describe('DSOBreadcrumbResolver', () => {
  describe('resolve', () => {
    let resolver: DSOBreadcrumbResolver<Collection>;
    let collectionService: any;
    let dsoBreadcrumbService: any;
    let testCollection: Collection;
    let uuid;
    let breadcrumbUrl;
    let currentUrl;

    beforeEach(() => {
      uuid = '1234-65487-12354-1235';
      breadcrumbUrl = '/collections/' + uuid;
      currentUrl = breadcrumbUrl + '/edit';
      testCollection = Object.assign(new Collection(), { uuid });
      dsoBreadcrumbService = {};
      collectionService = {
        findById: (id: string) => createSuccessfulRemoteDataObject$(testCollection)
      };
      resolver = new CollectionBreadcrumbResolver(dsoBreadcrumbService, collectionService);
    });

    it('should resolve a breadcrumb config for the correct DSO', () => {
      const resolvedConfig = resolver.resolve({ params: { id: uuid } } as any, { url: currentUrl } as any);
      const expectedConfig = { provider: dsoBreadcrumbService, key: testCollection, url: breadcrumbUrl };
      getTestScheduler().expectObservable(resolvedConfig).toBe('(a|)', { a: expectedConfig });
    });
  });
});
