import { first } from 'rxjs/operators';
import { CommunityPageResolver } from './community-page.resolver';
import { createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';

describe('CommunityPageResolver', () => {
  describe('resolve', () => {
    let resolver: CommunityPageResolver;
    let communityService: any;
    let store: any;
    const uuid = '1234-65487-12354-1235';

    beforeEach(() => {
      communityService = {
        findById: (id: string) => createSuccessfulRemoteDataObject$({ id })
      };
      store = jasmine.createSpyObj('store', {
        dispatch: {},
      });
      resolver = new CommunityPageResolver(communityService, store);
    });

    it('should resolve a community with the correct id', (done) => {
      resolver.resolve({ params: { id: uuid } } as any, { url: 'current-url' } as any)
        .pipe(first())
        .subscribe(
          (resolved) => {
            expect(resolved.payload.id).toEqual(uuid);
            done();
          }
        );
    });
  });
});
