import { first } from 'rxjs/operators';
import { WorkflowItemDataService } from '../core/submission/workflowitem-data.service';
import { createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';
import { ItemFromWorkflowResolver } from './item-from-workflow.resolver';

describe('ItemFromWorkflowResolver', () => {
  describe('resolve', () => {
    let resolver: ItemFromWorkflowResolver;
    let wfiService: WorkflowItemDataService;
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
      resolver = new ItemFromWorkflowResolver(wfiService, null);
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
