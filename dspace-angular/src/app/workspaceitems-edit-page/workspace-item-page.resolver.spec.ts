import { first } from 'rxjs/operators';
import { WorkspaceItemPageResolver } from './workspace-item-page.resolver';
import { WorkspaceitemDataService } from '../core/submission/workspaceitem-data.service';
import { createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';

describe('WorkflowItemPageResolver', () => {
  describe('resolve', () => {
    let resolver: WorkspaceItemPageResolver;
    let wsiService: WorkspaceitemDataService;
    const uuid = '1234-65487-12354-1235';

    beforeEach(() => {
      wsiService = {
        findById: (id: string) => createSuccessfulRemoteDataObject$({ id })
      } as any;
      resolver = new WorkspaceItemPageResolver(wsiService);
    });

    it('should resolve a workspace item with the correct id', (done) => {
      resolver.resolve({ params: { id: uuid } } as any, undefined)
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
