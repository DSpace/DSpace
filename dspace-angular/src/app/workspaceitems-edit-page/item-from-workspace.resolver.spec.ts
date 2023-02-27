import { first } from 'rxjs/operators';
import { WorkspaceitemDataService } from '../core/submission/workspaceitem-data.service';
import { createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';
import { ItemFromWorkspaceResolver } from './item-from-workspace.resolver';

describe('ItemFromWorkspaceResolver', () => {
  describe('resolve', () => {
    let resolver: ItemFromWorkspaceResolver;
    let wfiService: WorkspaceitemDataService;
    const uuid = '1234-65487-12354-1235';
    const itemUuid = '8888-8888-8888-8888';
    const wfi = {
      id: uuid,
      item: createSuccessfulRemoteDataObject$({ id: itemUuid })
    };


    beforeEach(() => {
      wfiService = {
        findById: (id: string) => createSuccessfulRemoteDataObject$(wfi)
      } as any;
      resolver = new ItemFromWorkspaceResolver(wfiService, null);
    });

    it('should resolve a an item from from the workflow item with the correct id', (done) => {
      resolver.resolve({ params: { id: uuid } } as any, undefined)
        .pipe(first())
        .subscribe(
          (resolved) => {
            expect(resolved.payload.id).toEqual(itemUuid);
            done();
          }
        );
    });
  });
});
