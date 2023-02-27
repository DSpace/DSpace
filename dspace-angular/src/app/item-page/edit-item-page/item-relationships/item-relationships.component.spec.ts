import { ChangeDetectorRef, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { getTestScheduler } from 'jasmine-marbles';
import { combineLatest as observableCombineLatest, of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { ObjectCacheService } from '../../../core/cache/object-cache.service';
import { RestResponse } from '../../../core/cache/response.models';
import { EntityTypeDataService } from '../../../core/data/entity-type-data.service';
import { ItemDataService } from '../../../core/data/item-data.service';
import { ObjectUpdatesService } from '../../../core/data/object-updates/object-updates.service';
import { RelationshipDataService } from '../../../core/data/relationship-data.service';
import { RequestService } from '../../../core/data/request.service';
import { ItemType } from '../../../core/shared/item-relationships/item-type.model';
import { RelationshipType } from '../../../core/shared/item-relationships/relationship-type.model';
import { Relationship } from '../../../core/shared/item-relationships/relationship.model';
import { Item } from '../../../core/shared/item.model';
import { NotificationType } from '../../../shared/notifications/models/notification-type';
import { INotification, Notification } from '../../../shared/notifications/models/notification.model';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { SharedModule } from '../../../shared/shared.module';
import { RouterStub } from '../../../shared/testing/router.stub';
import { ItemRelationshipsComponent } from './item-relationships.component';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { FieldChangeType } from '../../../core/data/object-updates/field-change-type.model';
import { RelationshipTypeDataService } from '../../../core/data/relationship-type-data.service';
import { relationshipTypes } from '../../../shared/testing/relationship-types.mock';
import { ThemeService } from '../../../shared/theme-support/theme.service';
import { getMockThemeService } from '../../../shared/mocks/theme-service.mock';

let comp: any;
let fixture: ComponentFixture<ItemRelationshipsComponent>;
let de: DebugElement;
let el: HTMLElement;
let objectUpdatesService;
let relationshipService;
let requestService;
let entityTypeService;
let objectCache;
const infoNotification: INotification = new Notification('id', NotificationType.Info, 'info');
const warningNotification: INotification = new Notification('id', NotificationType.Warning, 'warning');
const successNotification: INotification = new Notification('id', NotificationType.Success, 'success');
const notificationsService = jasmine.createSpyObj('notificationsService',
  {
    info: infoNotification,
    warning: warningNotification,
    success: successNotification
  }
);
const router = new RouterStub();
let relationshipTypeService;
let routeStub;
let itemService;

const url = 'http://test-url.com/test-url';
router.url = url;

let scheduler: TestScheduler;
let item;
let author1;
let author2;
let fieldUpdate1;
let fieldUpdate2;
let entityType;
let relationships;
let relationshipType;

describe('ItemRelationshipsComponent', () => {
  beforeEach(waitForAsync(() => {
    const date = new Date();

    relationshipType = Object.assign(new RelationshipType(), {
      id: '1',
      uuid: '1',
      leftwardType: 'isAuthorOfPublication',
      rightwardType: 'isPublicationOfAuthor'
    });

    relationships = [
      Object.assign(new Relationship(), {
        _links: {
          self: { href: url + '/2' }
        },
        id: '2',
        uuid: '2',
        relationshipType: createSuccessfulRemoteDataObject$(relationshipType)
      }),
      Object.assign(new Relationship(), {
        _links: {
          self: { href: url + '/3' }
        },
        id: '3',
        uuid: '3',
        relationshipType: createSuccessfulRemoteDataObject$(relationshipType)
      })
    ];

    item = Object.assign(new Item(), {
      _links: {
        self: { href: 'fake-item-url/publication' }
      },
      id: 'publication',
      uuid: 'publication',
      relationships: createSuccessfulRemoteDataObject$(createPaginatedList(relationships)),
      lastModified: date
    });

    entityType = Object.assign(new ItemType(), {
      id: 'entityType',
    });

    author1 = Object.assign(new Item(), {
      id: 'author1',
      uuid: 'author1'
    });
    author2 = Object.assign(new Item(), {
      id: 'author2',
      uuid: 'author2'
    });

    relationships[0].leftItem = createSuccessfulRemoteDataObject$(author1);
    relationships[0].rightItem = createSuccessfulRemoteDataObject$(item);
    relationships[1].leftItem = createSuccessfulRemoteDataObject$(author2);
    relationships[1].rightItem = createSuccessfulRemoteDataObject$(item);

    fieldUpdate1 = {
      field: relationships[0],
      changeType: undefined
    };
    fieldUpdate2 = {
      field: Object.assign(
        relationships[1],
        {keepLeftVirtualMetadata: true, keepRightVirtualMetadata: false}
      ),
      changeType: FieldChangeType.REMOVE
    };

    itemService = jasmine.createSpyObj('itemService', {
      findByHref: createSuccessfulRemoteDataObject$(item),
      findById: createSuccessfulRemoteDataObject$(item)
    });
    routeStub = {
      data: observableOf({}),
      parent: {
        data: observableOf({ dso: createSuccessfulRemoteDataObject(item) })
      }
    };

    objectUpdatesService = jasmine.createSpyObj('objectUpdatesService',
      {
        getFieldUpdates: observableOf({
          [relationships[0].uuid]: fieldUpdate1,
          [relationships[1].uuid]: fieldUpdate2
        }),
        getFieldUpdatesExclusive: observableOf({
          [relationships[0].uuid]: fieldUpdate1,
          [relationships[1].uuid]: fieldUpdate2
        }),
        saveAddFieldUpdate: {},
        discardFieldUpdates: {},
        reinstateFieldUpdates: observableOf(true),
        initialize: {},
        getUpdatedFields: observableOf([author1, author2]),
        getLastModified: observableOf(date),
        hasUpdates: observableOf(true),
        isReinstatable: observableOf(false), // should always return something --> its in ngOnInit
        isValidPage: observableOf(true)
      }
    );

    relationshipService = jasmine.createSpyObj('relationshipService',
      {
        getItemRelationshipLabels: observableOf(['isAuthorOfPublication']),
        getRelatedItems: observableOf([author1, author2]),
        getRelatedItemsByLabel: observableOf([author1, author2]),
        getItemRelationshipsArray: observableOf(relationships),
        deleteRelationship: observableOf(new RestResponse(true, 200, 'OK')),
        getItemResolvedRelatedItemsAndRelationships: observableCombineLatest(observableOf([author1, author2]), observableOf([item, item]), observableOf(relationships)),
        getRelationshipsByRelatedItemIds: observableOf(relationships),
        getRelationshipTypeLabelsByItem: observableOf([relationshipType.leftwardType])
      }
    );


    relationshipTypeService = jasmine.createSpyObj('searchByEntityType',
      {
        searchByEntityType: observableOf(relationshipTypes)
      }
    );

    requestService = jasmine.createSpyObj('requestService',
      {
        removeByHrefSubstring: {},
        hasByHref$: observableOf(false)
      }
    );

    objectCache = jasmine.createSpyObj('objectCache', {
      remove: undefined
    });

    entityTypeService = jasmine.createSpyObj('entityTypeService',
      {
        getEntityTypeByLabel: createSuccessfulRemoteDataObject$(entityType),
        getEntityTypeRelationships: createSuccessfulRemoteDataObject$(createPaginatedList([relationshipType])),
      }
    );

    scheduler = getTestScheduler();
    TestBed.configureTestingModule({
      imports: [SharedModule, TranslateModule.forRoot()],
      declarations: [ItemRelationshipsComponent],
      providers: [
        { provide: ThemeService, useValue: getMockThemeService() },
        { provide: ItemDataService, useValue: itemService },
        { provide: ObjectUpdatesService, useValue: objectUpdatesService },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: NotificationsService, useValue: notificationsService },
        { provide: RelationshipDataService, useValue: relationshipService },
        { provide: EntityTypeDataService, useValue: entityTypeService },
        { provide: ObjectCacheService, useValue: objectCache },
        { provide: RequestService, useValue: requestService },
        { provide: RelationshipTypeDataService, useValue: relationshipTypeService },
        ChangeDetectorRef
      ], schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemRelationshipsComponent);
    comp = fixture.componentInstance;
    de = fixture.debugElement;
    el = de.nativeElement;
    comp.url = url;
    fixture.detectChanges();
  });

  describe('discard', () => {
    beforeEach(() => {
      comp.discard();
    });

    it('it should call discardFieldUpdates on the objectUpdatesService with the correct url and notification', () => {
      expect(objectUpdatesService.discardFieldUpdates).toHaveBeenCalledWith(url, infoNotification);
    });
  });

  describe('reinstate', () => {
    beforeEach(() => {
      comp.reinstate();
    });

    it('it should call reinstateFieldUpdates on the objectUpdatesService with the correct url', () => {
      expect(objectUpdatesService.reinstateFieldUpdates).toHaveBeenCalledWith(url);
    });
  });

  describe('submit', () => {
    beforeEach(() => {
      comp.submit();
    });

    it('it should delete the correct relationship', () => {
      expect(relationshipService.deleteRelationship).toHaveBeenCalledWith(relationships[1].uuid, 'left');
    });
  });



  describe('discard', () => {
    beforeEach(() => {
      comp.item.firstMetadataValue = (info) => {
        return 'Publication';
      };
      fixture.detectChanges();
      comp.initializeUpdates();
      fixture.detectChanges();
    });

    it('it should call relationshipTypeService.searchByEntityType', () => {
      expect(relationshipTypeService.searchByEntityType).toHaveBeenCalled();
    });
  });

});
