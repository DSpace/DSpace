import { AbstractPaginatedDragAndDropListComponent } from './abstract-paginated-drag-and-drop-list.component';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { ObjectUpdatesService } from '../../core/data/object-updates/object-updates.service';
import { Component, ElementRef } from '@angular/core';
import { BehaviorSubject, Observable, of as observableOf } from 'rxjs';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { RemoteData } from '../../core/data/remote-data';
import { take } from 'rxjs/operators';
import { PaginationComponent } from '../pagination/pagination.component';
import { createSuccessfulRemoteDataObject } from '../remote-data.utils';
import { createPaginatedList } from '../testing/utils.test';
import { ObjectValuesPipe } from '../utils/object-values-pipe';
import { PaginationService } from '../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../testing/pagination-service.stub';
import { FieldUpdates } from '../../core/data/object-updates/field-updates.model';

@Component({
  selector: 'ds-mock-paginated-drag-drop-abstract',
  template: ''
})
class MockAbstractPaginatedDragAndDropListComponent extends AbstractPaginatedDragAndDropListComponent<DSpaceObject> {

  constructor(protected objectUpdatesService: ObjectUpdatesService,
              protected elRef: ElementRef,
              protected objectValuesPipe: ObjectValuesPipe,
              protected mockUrl: string,
              protected paginationService: PaginationService,
  protected mockObjectsRD$: Observable<RemoteData<PaginatedList<DSpaceObject>>>) {
    super(objectUpdatesService, elRef, objectValuesPipe, paginationService);
  }

  initializeObjectsRD(): void {
    this.objectsRD$ = this.mockObjectsRD$;
  }

  initializeURL(): void {
    this.url = this.mockUrl;
  }
}

describe('AbstractPaginatedDragAndDropListComponent', () => {
  let component: MockAbstractPaginatedDragAndDropListComponent;
  let objectUpdatesService: ObjectUpdatesService;
  let elRef: ElementRef;
  let objectValuesPipe: ObjectValuesPipe;

  const url = 'mock-abstract-paginated-drag-and-drop-list-component';


  const object1 = Object.assign(new DSpaceObject(), { uuid: 'object-1' });
  const object2 = Object.assign(new DSpaceObject(), { uuid: 'object-2' });
  const objectsRD = createSuccessfulRemoteDataObject(createPaginatedList([object1, object2]));
  let objectsRD$: BehaviorSubject<RemoteData<PaginatedList<DSpaceObject>>>;
  let paginationService;

  const updates = {
    [object1.uuid]: { field: object1, changeType: undefined },
    [object2.uuid]: { field: object2, changeType: undefined }
  } as FieldUpdates;

  let paginationComponent: PaginationComponent;

  beforeEach(() => {
    objectUpdatesService = jasmine.createSpyObj('objectUpdatesService', {
      initialize: {},
      getFieldUpdatesExclusive: observableOf(updates)
    });
    elRef = {
      nativeElement: jasmine.createSpyObj('nativeElement', {
        querySelector: {}
      })
    };
    objectValuesPipe = new ObjectValuesPipe();
    paginationComponent = jasmine.createSpyObj('paginationComponent', {
      doPageChange: {}
    });
    paginationService = new PaginationServiceStub();
    objectsRD$ = new BehaviorSubject(objectsRD);
    component = new MockAbstractPaginatedDragAndDropListComponent(objectUpdatesService, elRef, objectValuesPipe, url, paginationService, objectsRD$);
    component.paginationComponent = paginationComponent;
    component.ngOnInit();
  });

  it('should call initialize to initialize the objects in the store', () => {
    expect(objectUpdatesService.initialize).toHaveBeenCalled();
  });

  it('should initialize the updates correctly', (done) => {
    component.updates$.pipe(take(1)).subscribe((fieldUpdates) => {
      expect(fieldUpdates).toEqual(updates);
      done();
    });
  });

  describe('drop', () => {
    const event = {
      previousIndex: 0,
      currentIndex: 1,
      item: { element: { nativeElement: { id: object1.uuid } } }
    } as any;

    describe('when the user is hovering over a new page', () => {
      const hoverPage = 3;
      const hoverElement = { textContent: '' + hoverPage };

      beforeEach(() => {
        elRef.nativeElement.querySelector.and.returnValue(hoverElement);
        spyOn(component.dropObject, 'emit');
        component.drop(event);
      });

      it('should send out a dropObject event with the expected processed paginated indexes', () => {
        expect(component.dropObject.emit).toHaveBeenCalledWith(Object.assign({
          fromIndex: ((component.currentPage$.value.currentPage - 1) * component.pageSize) + event.previousIndex,
          toIndex: ((hoverPage - 1) * component.pageSize),
          finish: jasmine.anything()
        }));
      });
    });

    describe('when the user is not hovering over a new page', () => {
      beforeEach(() => {
        spyOn(component.dropObject, 'emit');
        component.drop(event);
      });

      it('should send out a dropObject event with the expected properties', () => {
        expect(component.dropObject.emit).toHaveBeenCalledWith(Object.assign({
          fromIndex: event.previousIndex,
          toIndex: event.currentIndex,
          finish: jasmine.anything()
        }));
      });
    });
  });
});
