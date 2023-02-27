import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';
import { RemoteDataBuildService } from '../../../core/cache/builders/remote-data-build.service';
import { CollectionDataService } from '../../../core/data/collection-data.service';
import { Collection } from '../../../core/shared/collection.model';
import { Item } from '../../../core/shared/item.model';
import { getMockRemoteDataBuildService } from '../../../shared/mocks/remote-data-build.service.mock';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { CollectionsComponent } from './collections.component';
import { buildPaginatedList, PaginatedList } from '../../../core/data/paginated-list.model';
import { PageInfo } from '../../../core/shared/page-info.model';
import { FindListOptions } from '../../../core/data/find-list-options.model';

const createMockCollection = (id: string) => Object.assign(new Collection(), {
  id: id,
  name: `collection-${id}`,
});

const mockItem: Item = new Item();

describe('CollectionsComponent', () => {
  let collectionDataService;

  let mockCollection1: Collection;
  let mockCollection2: Collection;
  let mockCollection3: Collection;
  let mockCollection4: Collection;

  let component: CollectionsComponent;
  let fixture: ComponentFixture<CollectionsComponent>;

  beforeEach(waitForAsync(() => {
    collectionDataService = jasmine.createSpyObj([
      'findOwningCollectionFor',
      'findMappedCollectionsFor',
    ]);

    mockCollection1 = createMockCollection('c1');
    mockCollection2 = createMockCollection('c2');
    mockCollection3 = createMockCollection('c3');
    mockCollection4 = createMockCollection('c4');

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [ CollectionsComponent ],
      providers: [
        { provide: RemoteDataBuildService, useValue: getMockRemoteDataBuildService()},
        { provide: CollectionDataService, useValue: collectionDataService },
      ],

      schemas: [ NO_ERRORS_SCHEMA ]
    }).overrideComponent(CollectionsComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(CollectionsComponent);
    component = fixture.componentInstance;
    component.item = mockItem;
    component.label = 'test.test';
    component.separator = '<br/>';
    component.pageSize = 2;
  }));

  describe('when the item has only an owning collection', () => {
    let mockPage1: PaginatedList<Collection>;

    beforeEach(() => {
      mockPage1 = buildPaginatedList(Object.assign(new PageInfo(), {
        currentPage: 1,
        elementsPerPage: 2,
        totalPages: 0,
        totalElements: 0,
      }), []);

      collectionDataService.findOwningCollectionFor.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection1));
      collectionDataService.findMappedCollectionsFor.and.returnValue(createSuccessfulRemoteDataObject$(mockPage1));
      fixture.detectChanges();
    });

    it('should display the owning collection', () => {
      const collectionFields = fixture.debugElement.queryAll(By.css('ds-metadata-field-wrapper div.collections a'));
      const loadMoreBtn = fixture.debugElement.query(By.css('ds-metadata-field-wrapper .load-more-btn'));

      expect(collectionDataService.findOwningCollectionFor).toHaveBeenCalledOnceWith(mockItem);
      expect(collectionDataService.findMappedCollectionsFor).toHaveBeenCalledOnceWith(mockItem, Object.assign(new FindListOptions(), {
        elementsPerPage: 2,
        currentPage: 1,
      }));

      expect(collectionFields.length).toBe(1);
      expect(collectionFields[0].nativeElement.textContent).toEqual('collection-c1');

      expect(component.lastPage$.getValue()).toBe(1);
      expect(component.hasMore$.getValue()).toBe(false);
      expect(component.isLoading$.getValue()).toBe(false);

      expect(loadMoreBtn).toBeNull();
    });
  });

  describe('when the item has an owning collection and one mapped collection', () => {
    let mockPage1: PaginatedList<Collection>;

    beforeEach(() => {
      mockPage1 = buildPaginatedList(Object.assign(new PageInfo(), {
        currentPage: 1,
        elementsPerPage: 2,
        totalPages: 1,
        totalElements: 1,
      }), [mockCollection2]);

      collectionDataService.findOwningCollectionFor.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection1));
      collectionDataService.findMappedCollectionsFor.and.returnValue(createSuccessfulRemoteDataObject$(mockPage1));
      fixture.detectChanges();
    });

    it('should display the owning collection and the mapped collection', () => {
      const collectionFields = fixture.debugElement.queryAll(By.css('ds-metadata-field-wrapper div.collections a'));
      const loadMoreBtn = fixture.debugElement.query(By.css('ds-metadata-field-wrapper .load-more-btn'));

      expect(collectionDataService.findOwningCollectionFor).toHaveBeenCalledOnceWith(mockItem);
      expect(collectionDataService.findMappedCollectionsFor).toHaveBeenCalledOnceWith(mockItem, Object.assign(new FindListOptions(), {
        elementsPerPage: 2,
        currentPage: 1,
      }));

      expect(collectionFields.length).toBe(2);
      expect(collectionFields[0].nativeElement.textContent).toEqual('collection-c1');
      expect(collectionFields[1].nativeElement.textContent).toEqual('collection-c2');

      expect(component.lastPage$.getValue()).toBe(1);
      expect(component.hasMore$.getValue()).toBe(false);
      expect(component.isLoading$.getValue()).toBe(false);

      expect(loadMoreBtn).toBeNull();
    });
  });

  describe('when the item has an owning collection and multiple mapped collections', () => {
    let mockPage1: PaginatedList<Collection>;
    let mockPage2: PaginatedList<Collection>;

    beforeEach(() => {
      mockPage1 = buildPaginatedList(Object.assign(new PageInfo(), {
        currentPage: 1,
        elementsPerPage: 2,
        totalPages: 2,
        totalElements: 3,
      }), [mockCollection2, mockCollection3]);

      mockPage2 = buildPaginatedList(Object.assign(new PageInfo(), {
        currentPage: 2,
        elementsPerPage: 2,
        totalPages: 2,
        totalElements: 1,
      }), [mockCollection4]);

      collectionDataService.findOwningCollectionFor.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection1));
      collectionDataService.findMappedCollectionsFor.and.returnValues(
        createSuccessfulRemoteDataObject$(mockPage1),
        createSuccessfulRemoteDataObject$(mockPage2),
      );
      fixture.detectChanges();
    });

    it('should display the owning collection, two mapped collections and a load more button', () => {
      const collectionFields = fixture.debugElement.queryAll(By.css('ds-metadata-field-wrapper div.collections a'));
      const loadMoreBtn = fixture.debugElement.query(By.css('ds-metadata-field-wrapper .load-more-btn'));

      expect(collectionDataService.findOwningCollectionFor).toHaveBeenCalledOnceWith(mockItem);
      expect(collectionDataService.findMappedCollectionsFor).toHaveBeenCalledOnceWith(mockItem, Object.assign(new FindListOptions(), {
        elementsPerPage: 2,
        currentPage: 1,
      }));

      expect(collectionFields.length).toBe(3);
      expect(collectionFields[0].nativeElement.textContent).toEqual('collection-c1');
      expect(collectionFields[1].nativeElement.textContent).toEqual('collection-c2');
      expect(collectionFields[2].nativeElement.textContent).toEqual('collection-c3');

      expect(component.lastPage$.getValue()).toBe(1);
      expect(component.hasMore$.getValue()).toBe(true);
      expect(component.isLoading$.getValue()).toBe(false);

      expect(loadMoreBtn).toBeTruthy();
    });

    describe('when the load more button is clicked', () => {
      beforeEach(() => {
        const loadMoreBtn = fixture.debugElement.query(By.css('ds-metadata-field-wrapper .load-more-btn'));
        loadMoreBtn.nativeElement.click();
        fixture.detectChanges();
      });

      it('should display the owning collection and three mapped collections', () => {
        const collectionFields = fixture.debugElement.queryAll(By.css('ds-metadata-field-wrapper div.collections a'));
        const loadMoreBtn = fixture.debugElement.query(By.css('ds-metadata-field-wrapper .load-more-btn'));

        expect(collectionDataService.findOwningCollectionFor).toHaveBeenCalledOnceWith(mockItem);
        expect(collectionDataService.findMappedCollectionsFor).toHaveBeenCalledTimes(2);
        expect(collectionDataService.findMappedCollectionsFor).toHaveBeenCalledWith(mockItem, Object.assign(new FindListOptions(), {
          elementsPerPage: 2,
          currentPage: 1,
        }));
        expect(collectionDataService.findMappedCollectionsFor).toHaveBeenCalledWith(mockItem, Object.assign(new FindListOptions(), {
          elementsPerPage: 2,
          currentPage: 2,
        }));

        expect(collectionFields.length).toBe(4);
        expect(collectionFields[0].nativeElement.textContent).toEqual('collection-c1');
        expect(collectionFields[1].nativeElement.textContent).toEqual('collection-c2');
        expect(collectionFields[2].nativeElement.textContent).toEqual('collection-c3');
        expect(collectionFields[3].nativeElement.textContent).toEqual('collection-c4');

        expect(component.lastPage$.getValue()).toBe(2);
        expect(component.hasMore$.getValue()).toBe(false);
        expect(component.isLoading$.getValue()).toBe(false);

        expect(loadMoreBtn).toBeNull();
      });
    });
  });

  describe('when the request for the owning collection fails', () => {
    let mockPage1: PaginatedList<Collection>;

    beforeEach(() => {
      mockPage1 = buildPaginatedList(Object.assign(new PageInfo(), {
        currentPage: 1,
        elementsPerPage: 2,
        totalPages: 1,
        totalElements: 1,
      }), [mockCollection2]);

      collectionDataService.findOwningCollectionFor.and.returnValue(createFailedRemoteDataObject$());
      collectionDataService.findMappedCollectionsFor.and.returnValue(createSuccessfulRemoteDataObject$(mockPage1));
      fixture.detectChanges();
    });

    it('should display the mapped collection only', () => {
      const collectionFields = fixture.debugElement.queryAll(By.css('ds-metadata-field-wrapper div.collections a'));
      const loadMoreBtn = fixture.debugElement.query(By.css('ds-metadata-field-wrapper .load-more-btn'));

      expect(collectionDataService.findOwningCollectionFor).toHaveBeenCalledOnceWith(mockItem);
      expect(collectionDataService.findMappedCollectionsFor).toHaveBeenCalledOnceWith(mockItem, Object.assign(new FindListOptions(), {
        elementsPerPage: 2,
        currentPage: 1,
      }));

      expect(collectionFields.length).toBe(1);
      expect(collectionFields[0].nativeElement.textContent).toEqual('collection-c2');

      expect(component.lastPage$.getValue()).toBe(1);
      expect(component.hasMore$.getValue()).toBe(false);
      expect(component.isLoading$.getValue()).toBe(false);

      expect(loadMoreBtn).toBeNull();
    });
  });

  describe('when the request for the mapped collections fails', () => {
    beforeEach(() => {
      collectionDataService.findOwningCollectionFor.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection1));
      collectionDataService.findMappedCollectionsFor.and.returnValue(createFailedRemoteDataObject$());
      fixture.detectChanges();
    });

    it('should display the owning collection only', () => {
      const collectionFields = fixture.debugElement.queryAll(By.css('ds-metadata-field-wrapper div.collections a'));
      const loadMoreBtn = fixture.debugElement.query(By.css('ds-metadata-field-wrapper .load-more-btn'));

      expect(collectionDataService.findOwningCollectionFor).toHaveBeenCalledOnceWith(mockItem);
      expect(collectionDataService.findMappedCollectionsFor).toHaveBeenCalledOnceWith(mockItem, Object.assign(new FindListOptions(), {
        elementsPerPage: 2,
        currentPage: 1,
      }));

      expect(collectionFields.length).toBe(1);
      expect(collectionFields[0].nativeElement.textContent).toEqual('collection-c1');

      expect(component.lastPage$.getValue()).toBe(0);
      expect(component.hasMore$.getValue()).toBe(true);
      expect(component.isLoading$.getValue()).toBe(false);

      expect(loadMoreBtn).toBeTruthy();
    });
  });

  describe('when both requests fail', () => {
    beforeEach(() => {
      collectionDataService.findOwningCollectionFor.and.returnValue(createFailedRemoteDataObject$());
      collectionDataService.findMappedCollectionsFor.and.returnValue(createFailedRemoteDataObject$());
      fixture.detectChanges();
    });

    it('should display no collections', () => {
      const collectionFields = fixture.debugElement.queryAll(By.css('ds-metadata-field-wrapper div.collections a'));
      const loadMoreBtn = fixture.debugElement.query(By.css('ds-metadata-field-wrapper .load-more-btn'));

      expect(collectionDataService.findOwningCollectionFor).toHaveBeenCalledOnceWith(mockItem);
      expect(collectionDataService.findMappedCollectionsFor).toHaveBeenCalledOnceWith(mockItem, Object.assign(new FindListOptions(), {
        elementsPerPage: 2,
        currentPage: 1,
      }));

      expect(collectionFields.length).toBe(0);

      expect(component.lastPage$.getValue()).toBe(0);
      expect(component.hasMore$.getValue()).toBe(true);
      expect(component.isLoading$.getValue()).toBe(false);

      expect(loadMoreBtn).toBeTruthy();
    });
  });

});
