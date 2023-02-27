import { first } from 'rxjs/operators';
import { BrowseByGuard } from './browse-by-guard';
import { of as observableOf } from 'rxjs';
import { createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';
import { BrowseDefinition } from '../core/shared/browse-definition.model';
import { BrowseByDataType } from './browse-by-switcher/browse-by-decorator';

describe('BrowseByGuard', () => {
  describe('canActivate', () => {
    let guard: BrowseByGuard;
    let dsoService: any;
    let translateService: any;
    let browseDefinitionService: any;

    const name = 'An interesting DSO';
    const title = 'Author';
    const field = 'Author';
    const id = 'author';
    const scope = '1234-65487-12354-1235';
    const value = 'Filter';
    const browseDefinition = Object.assign(new BrowseDefinition(), { type: BrowseByDataType.Metadata, metadataKeys: ['dc.contributor'] });

    beforeEach(() => {
      dsoService = {
        findById: (dsoId: string) => observableOf({ payload: { name: name }, hasSucceeded: true })
      };

      translateService = {
        instant: () => field
      };

      browseDefinitionService = {
        findById: () => createSuccessfulRemoteDataObject$(browseDefinition)
      };

      guard = new BrowseByGuard(dsoService, translateService, browseDefinitionService);
    });

    it('should return true, and sets up the data correctly, with a scope and value', () => {
      const scopedRoute = {
        data: {
          title: field,
          browseDefinition,
        },
        params: {
          id,
        },
        queryParams: {
          scope,
          value
        }
      };
      guard.canActivate(scopedRoute as any, undefined)
        .pipe(first())
        .subscribe(
          (canActivate) => {
            const result = {
              title,
              id,
              browseDefinition,
              collection: name,
              field,
              value: '"' + value + '"'
            };
            expect(scopedRoute.data).toEqual(result);
            expect(canActivate).toEqual(true);
          }
        );
    });

    it('should return true, and sets up the data correctly, with a scope and without value', () => {
      const scopedNoValueRoute = {
        data: {
          title: field,
          browseDefinition,
        },
        params: {
          id,
        },
        queryParams: {
          scope
        }
      };

      guard.canActivate(scopedNoValueRoute as any, undefined)
        .pipe(first())
        .subscribe(
          (canActivate) => {
            const result = {
              title,
              id,
              browseDefinition,
              collection: name,
              field,
              value: ''
            };
            expect(scopedNoValueRoute.data).toEqual(result);
            expect(canActivate).toEqual(true);
          }
        );
    });

    it('should return true, and sets up the data correctly, without a scope and with a value', () => {
      const route = {
        data: {
          title: field,
          browseDefinition,
        },
        params: {
          id,
        },
        queryParams: {
          value
        }
      };
      guard.canActivate(route as any, undefined)
        .pipe(first())
        .subscribe(
          (canActivate) => {
            const result = {
              title,
              id,
              browseDefinition,
              collection: '',
              field,
              value: '"' + value + '"'
            };
            expect(route.data).toEqual(result);
            expect(canActivate).toEqual(true);
          }
        );
    });
  });
});
