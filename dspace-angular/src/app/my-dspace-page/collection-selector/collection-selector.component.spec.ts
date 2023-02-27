/* eslint-disable max-classes-per-file */
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectorRef, ElementRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { createPaginatedList } from '../../shared/testing/utils.test';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { getTestScheduler, hot } from 'jasmine-marbles';
import { TestScheduler } from 'rxjs/testing';
import { Observable } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { CollectionSelectorComponent } from './collection-selector.component';
import { CollectionDropdownComponent } from '../../shared/collection-dropdown/collection-dropdown.component';
import { Collection } from '../../core/shared/collection.model';
import { RemoteData } from '../../core/data/remote-data';
import { Community } from '../../core/shared/community.model';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { TranslateLoaderMock } from '../../shared/mocks/translate-loader.mock';
import { CollectionDataService } from '../../core/data/collection-data.service';
import { MockElementRef } from '../../shared/testing/element-ref.mock';
import { FindListOptions } from '../../core/data/find-list-options.model';


describe('CollectionSelectorComponent', () => {
  let component: CollectionSelectorComponent;
  let fixture: ComponentFixture<CollectionSelectorComponent>;
  let scheduler: TestScheduler;
  const modal = jasmine.createSpyObj('modal', ['close', 'dismiss']);

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

  const collectionDataServiceMock = {
    getAuthorizedCollection(query: string, options: FindListOptions = {}, ...linksToFollow: FollowLinkConfig<Collection>[]): Observable<RemoteData<PaginatedList<Collection>>> {
      return hot( 'a|', {
        a: createSuccessfulRemoteDataObject(createPaginatedList(collections))
      });
    }
  };

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
      declarations: [ CollectionSelectorComponent, CollectionDropdownComponent ],
      providers: [
        {provide: CollectionDataService, useValue: collectionDataServiceMock},
        {provide: ElementRef, useClass: MockElementRef},
        {provide: NgbActiveModal, useValue: modal},
        {provide: ActivatedRoute, useValue: {}},
        ChangeDetectorRef
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    scheduler = getTestScheduler();
    fixture = TestBed.overrideComponent(CollectionSelectorComponent, {
      set: {
        template: '<ds-collection-dropdown (selectionChange)="selectObject($event)"></ds-collection-dropdown>'
      }
    }).createComponent(CollectionSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call selectObject', () => {
    spyOn(component, 'selectObject');
    scheduler.schedule(() => fixture.detectChanges());
    scheduler.flush();
    const collectionItem = fixture.debugElement.query(By.css('.collection-item:nth-child(2)'));
    collectionItem.triggerEventHandler('click', null);
    expect(component.selectObject).toHaveBeenCalled();
  });

  it('should close the dialog', () => {
    component.close();
    expect((component as any).activeModal.close).toHaveBeenCalled();
  });
});
