import { ItemPageResolver } from './item-page.resolver';
import { createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';
import { DSpaceObject } from '../core/shared/dspace-object.model';
import { MetadataValueFilter } from '../core/shared/metadata.models';
import { first } from 'rxjs/operators';
import { Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

describe('ItemPageResolver', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule.withRoutes([{
        path: 'entities/:entity-type/:id',
        component: {} as any
      }])]
    });
  });

  describe('resolve', () => {
    let resolver: ItemPageResolver;
    let itemService: any;
    let store: any;
    let router: any;

    const uuid = '1234-65487-12354-1235';
    let item: DSpaceObject;

    function runTestsWithEntityType(entityType: string) {
      beforeEach(() => {
        router = TestBed.inject(Router);
        item = Object.assign(new DSpaceObject(), {
          uuid: uuid,
          firstMetadataValue(_keyOrKeys: string | string[], _valueFilter?: MetadataValueFilter): string {
            return entityType;
          }
        });
        itemService = {
          findById: (_id: string) => createSuccessfulRemoteDataObject$(item)
        };
        store = jasmine.createSpyObj('store', {
          dispatch: {},
        });
        resolver = new ItemPageResolver(itemService, store, router);
      });

      it('should redirect to the correct route for the entity type', (done) => {
        spyOn(item, 'firstMetadataValue').and.returnValue(entityType);
        spyOn(router, 'navigateByUrl').and.callThrough();

        resolver.resolve({ params: { id: uuid } } as any, { url: router.parseUrl(`/items/${uuid}`).toString() } as any)
          .pipe(first())
          .subscribe(
            () => {
              expect(router.navigateByUrl).toHaveBeenCalledWith(router.parseUrl(`/entities/${entityType}/${uuid}`).toString());
              done();
            }
          );
      });

      it('should not redirect if we’re already on the correct route', (done) => {
        spyOn(item, 'firstMetadataValue').and.returnValue(entityType);
        spyOn(router, 'navigateByUrl').and.callThrough();

        resolver.resolve({ params: { id: uuid } } as any, { url: router.parseUrl(`/entities/${entityType}/${uuid}`).toString() } as any)
          .pipe(first())
          .subscribe(
            () => {
              expect(router.navigateByUrl).not.toHaveBeenCalled();
              done();
            }
          );
      });
    }

    describe('when normal entity type is provided', () => {
      runTestsWithEntityType('publication');
    });

    describe('when entity type contains a special character', () => {
      runTestsWithEntityType('alligator,loki');
      runTestsWithEntityType('🐊');
      runTestsWithEntityType(' ');
    });

  });
});
