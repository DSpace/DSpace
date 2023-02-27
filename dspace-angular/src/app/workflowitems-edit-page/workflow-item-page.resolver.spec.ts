import { first } from 'rxjs/operators';
import { WorkflowItemPageResolver } from './workflow-item-page.resolver';
import { WorkflowItemDataService } from '../core/submission/workflowitem-data.service';
import { createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';

describe('WorkflowItemPageResolver', () => {
  describe('resolve', () => {
    let resolver: WorkflowItemPageResolver;
    let wfiService: WorkflowItemDataService;
    const uuid = '1234-65487-12354-1235';

    beforeEach(() => {
      wfiService = {
        findById: (id: string) => createSuccessfulRemoteDataObject$({ id })
      } as any;
      resolver = new WorkflowItemPageResolver(wfiService);
    });

    it('should resolve a workflow item with the correct id', (done) => {
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
