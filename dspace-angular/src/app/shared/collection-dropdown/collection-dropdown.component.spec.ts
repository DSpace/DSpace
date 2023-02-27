import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectorRef, ElementRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';

import { getTestScheduler } from 'jasmine-marbles';
import { TestScheduler } from 'rxjs/testing';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { CollectionDropdownComponent } from './collection-dropdown.component';
import { buildPaginatedList } from '../../core/data/paginated-list.model';
import { createSuccessfulRemoteDataObject$ } from '../remote-data.utils';
import { PageInfo } from '../../core/shared/page-info.model';
import { Collection } from '../../core/shared/collection.model';
import { CollectionDataService } from '../../core/data/collection-data.service';
import { TranslateLoaderMock } from '../mocks/translate-loader.mock';
import { Community } from '../../core/shared/community.model';
import { MockElementRef } from '../testing/element-ref.mock';

const community: Community = Object.assign(new Community(), {
  id: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88',
  uuid: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88',
  name: 'Community 1'
});

const collections: Collection[] = [
  Object.assign(new Collection(), {
    id: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88',
    name: 'Collection 1',
    metadata: [
      {
        key: 'dc.title',
        language: 'en_US',
        value: 'Community 1-Collection 1'
      }],
    parentCommunity: createSuccessfulRemoteDataObject$(community)
  }),
  Object.assign(new Collection(), {
    id: '59ee713b-ee53-4220-8c3f-9860dc84fe33',
    name: 'Collection 2',
    metadata: [
      {
        key: 'dc.title',
        language: 'en_US',
        value: 'Community 1-Collection 2'
      }],
    parentCommunity: createSuccessfulRemoteDataObject$(community)
  }),
  Object.assign(new Collection(), {
    id: 'e9dbf393-7127-415f-8919-55be34a6e9ed',
    name: 'Collection 3',
    metadata: [
      {
        key: 'dc.title',
        language: 'en_US',
        value: 'Community 1-Collection 3'
      }],
    parentCommunity: createSuccessfulRemoteDataObject$(community)
  }),
  Object.assign(new Collection(), {
    id: '59da2ff0-9bf4-45bf-88be-e35abd33f304',
    name: 'Collection 4',
    metadata: [
      {
        key: 'dc.title',
        language: 'en_US',
        value: 'Community 1-Collection 4'
      }],
    parentCommunity: createSuccessfulRemoteDataObject$(community)
  }),
  Object.assign(new Collection(), {
    id: 'a5159760-f362-4659-9e81-e3253ad91ede',
    name: 'Collection 5',
    metadata: [
      {
        key: 'dc.title',
        language: 'en_US',
        value: 'Community 1-Collection 5'
      }],
    parentCommunity: createSuccessfulRemoteDataObject$(community)
  })
];

const listElementMock = {
  communities: [
    {
      id: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88',
      name: 'Community 1'
    }
  ],
  collection: {
    id: 'e9dbf393-7127-415f-8919-55be34a6e9ed',
    uuid: 'e9dbf393-7127-415f-8919-55be34a6e9ed',
    name: 'Collection 3'
  }
};

describe('CollectionDropdownComponent', () => {
  let component: CollectionDropdownComponent;
  let componentAsAny: any;
  let fixture: ComponentFixture<CollectionDropdownComponent>;
  let scheduler: TestScheduler;

  const collectionDataServiceMock: any = jasmine.createSpyObj('CollectionDataService', {
    getAuthorizedCollection: jasmine.createSpy('getAuthorizedCollection'),
    getAuthorizedCollectionByEntityType: jasmine.createSpy('getAuthorizedCollectionByEntityType')
  });

  const paginatedCollection = buildPaginatedList(new PageInfo(), collections);
  const paginatedCollectionRD$ = createSuccessfulRemoteDataObject$(paginatedCollection);

  const paginatedOneElementCollection = buildPaginatedList(new PageInfo(), [collections[0]]);
  const paginatedOneElementCollectionRD$ = createSuccessfulRemoteDataObject$(paginatedOneElementCollection);

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      declarations: [CollectionDropdownComponent],
      providers: [
        { provide: CollectionDataService, useValue: collectionDataServiceMock },
        { provide: ElementRef, useClass: MockElementRef },
        ChangeDetectorRef
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    scheduler = getTestScheduler();
    fixture = TestBed.createComponent(CollectionDropdownComponent);
    component = fixture.componentInstance;
    componentAsAny = component;
    componentAsAny.collectionDataService.getAuthorizedCollection.and.returnValue(paginatedCollectionRD$);
    componentAsAny.collectionDataService.getAuthorizedCollectionByEntityType.and.returnValue(paginatedCollectionRD$);
  });

  it('should init component with collection list', () => {
    spyOn(component.subs, 'push');
    spyOn(component, 'resetPagination');
    spyOn(component, 'populateCollectionList').and.callThrough();

    scheduler.schedule(() => fixture.detectChanges());
    scheduler.flush();
    const elements = fixture.debugElement.queryAll(By.css('.collection-item'));

    expect(elements.length).toEqual(5);
    expect(component.subs.push).toHaveBeenCalled();
    expect(component.resetPagination).toHaveBeenCalled();
    expect(component.populateCollectionList).toHaveBeenCalled();
    expect((component as any).collectionDataService.getAuthorizedCollection).toHaveBeenCalled();
  });

  it('should trigger onSelect method when select a new collection from list', () => {
    scheduler.schedule(() => fixture.detectChanges());
    scheduler.flush();

    spyOn(component, 'onSelect').and.callThrough();
    const collectionItem = fixture.debugElement.query(By.css('.collection-item:nth-child(2)'));
    collectionItem.triggerEventHandler('click', null);

    scheduler.schedule(() => fixture.detectChanges());
    scheduler.flush();

    expect(component.onSelect).toHaveBeenCalled();
  });

  it('should emit collectionChange event when selecting a new collection', () => {
    spyOn(component.selectionChange, 'emit').and.callThrough();
    component.ngOnInit();
    component.onSelect(listElementMock as any);
    fixture.detectChanges();

    expect(component.selectionChange.emit).toHaveBeenCalledWith(listElementMock as any);
  });

  it('should reset collections list after reset of searchField', () => {
    spyOn(component.subs, 'push').and.callThrough();
    spyOn(component.searchField, 'setValue').and.callThrough();
    spyOn(component, 'resetPagination').and.callThrough();
    spyOn(component, 'populateCollectionList').and.callThrough();
    scheduler.schedule(() => fixture.detectChanges());
    scheduler.flush();

    scheduler.schedule(() => component.reset());
    scheduler.flush();

    expect(component.searchField.setValue).toHaveBeenCalledWith('');
    expect(component.resetPagination).toHaveBeenCalled();
    expect(component.currentQuery).toEqual('');
    expect(component.populateCollectionList).toHaveBeenCalledWith(component.currentQuery, component.currentPage);
    expect(component.searchListCollection.length).toEqual(5);
    expect(component.subs.push).toHaveBeenCalled();
  });

  it('should change loader status', () => {
    spyOn(component.isLoading, 'next').and.callThrough();
    component.hideShowLoader(true);

    expect(component.isLoading.next).toHaveBeenCalledWith(true);
  });

  it('reset pagination fields', () => {
    component.resetPagination();

    expect(component.currentPage).toEqual(1);
    expect(component.currentQuery).toEqual('');
    expect(component.hasNextPage).toEqual(true);
    expect(component.searchListCollection).toEqual([]);
  });

  it('should invoke the method getAuthorizedCollectionByEntityType of CollectionDataService when entityType is set',() => {
    component.entityType = 'rel';
    scheduler.schedule(() => fixture.detectChanges());
    scheduler.flush();
    expect((component as any).collectionDataService.getAuthorizedCollectionByEntityType).toHaveBeenCalled();
  });

  it('should emit hasChoice true when totalElements is greater then one', () => {
    spyOn(component.searchComplete, 'emit').and.callThrough();
    component.ngOnInit();
    fixture.detectChanges();

    expect(component.searchComplete.emit).toHaveBeenCalledWith();
  });

  it('should emit theOnlySelectable when totalElements is equal to one', () => {

    componentAsAny.collectionDataService.getAuthorizedCollection.and.returnValue(paginatedOneElementCollectionRD$);
    componentAsAny.collectionDataService.getAuthorizedCollectionByEntityType.and.returnValue(paginatedOneElementCollectionRD$);

    spyOn(component.theOnlySelectable, 'emit').and.callThrough();
    component.ngOnInit();
    fixture.detectChanges();

    const expectedTheOnlySelectable = {
      communities: [ { id: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88', name: 'Community 1', uuid: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88' } ],
      collection: { id: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88', uuid: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88', name: 'Collection 1' }
    };

    expect(component.theOnlySelectable.emit).toHaveBeenCalledWith(expectedTheOnlySelectable);
  });
});
