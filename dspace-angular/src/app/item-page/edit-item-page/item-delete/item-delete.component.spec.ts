import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ItemType } from '../../../core/shared/item-relationships/item-type.model';
import { Relationship } from '../../../core/shared/item-relationships/relationship.model';
import { Item } from '../../../core/shared/item.model';
import { RouterStub } from '../../../shared/testing/router.stub';
import { of as observableOf, EMPTY } from 'rxjs';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ActivatedRoute, Router } from '@angular/router';
import { ItemDataService } from '../../../core/data/item-data.service';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ItemDeleteComponent } from './item-delete.component';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { VarDirective } from '../../../shared/utils/var.directive';
import { ObjectUpdatesService } from '../../../core/data/object-updates/object-updates.service';
import { RelationshipDataService } from '../../../core/data/relationship-data.service';
import { RelationshipType } from '../../../core/shared/item-relationships/relationship-type.model';
import { EntityTypeDataService } from '../../../core/data/entity-type-data.service';
import { getItemEditRoute } from '../../item-page-routing-paths';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { RelationshipTypeDataService } from '../../../core/data/relationship-type-data.service';
import { LinkService } from '../../../core/cache/builders/link.service';

let comp: ItemDeleteComponent;
let fixture: ComponentFixture<ItemDeleteComponent>;

let mockItem;
let itemType;
let type1;
let type2;
let types;
let relationships;
let itemPageUrl;
let routerStub;
let mockItemDataService: ItemDataService;
let routeStub;
let objectUpdatesServiceStub;
let relationshipService;
let linkService;
let entityTypeService;
let notificationsServiceStub;
let typesSelection;

describe('ItemDeleteComponent', () => {
  beforeEach(waitForAsync(() => {

    mockItem = Object.assign(new Item(), {
      id: 'fake-id',
      uuid: 'fake-uuid',
      handle: 'fake/handle',
      lastModified: '2018',
      isWithdrawn: true,
      metadata: {
        'dspace.entity.type': [
          { value: 'Person' }
        ]
      }
    });

    itemType = Object.assign(new ItemType(), {
      id: 'itemType',
      uuid: 'itemType',
    });

    type1 = Object.assign(new RelationshipType(), {
      id: '1',
      uuid: 'type-1',
    });

    type2 = Object.assign(new RelationshipType(), {
      id: '2',
      uuid: 'type-2',
    });

    types = [type1, type2];

    relationships = [
      Object.assign(new Relationship(), {
        id: '1',
        uuid: 'relationship-1',
        relationshipType: createSuccessfulRemoteDataObject$(type1),
        leftItem: createSuccessfulRemoteDataObject$(mockItem),
        rightItem: createSuccessfulRemoteDataObject$(new Item()),
      }),
      Object.assign(new Relationship(), {
        id: '2',
        uuid: 'relationship-2',
        relationshipType: createSuccessfulRemoteDataObject$(type2),
        leftItem: createSuccessfulRemoteDataObject$(mockItem),
        rightItem: createSuccessfulRemoteDataObject$(new Item()),
      }),
    ];

    itemPageUrl = `fake-url/${mockItem.id}`;
    routerStub = Object.assign(new RouterStub(), {
      url: `${itemPageUrl}/edit`
    });

    mockItemDataService = jasmine.createSpyObj('mockItemDataService', {
      delete: createSuccessfulRemoteDataObject$({})
    });

    routeStub = {
      data: observableOf({
        dso: createSuccessfulRemoteDataObject(mockItem)
      })
    };

    typesSelection = {
      type1: false,
      type2: true,
    };

    entityTypeService = jasmine.createSpyObj('entityTypeService',
      {
        getEntityTypeByLabel: createSuccessfulRemoteDataObject$(itemType),
        getEntityTypeRelationships: createSuccessfulRemoteDataObject$(createPaginatedList(types)),
      }
    );

    objectUpdatesServiceStub = {
      initialize: () => {
        // do nothing
      },
      isSelectedVirtualMetadata: (type) => observableOf(typesSelection[type]),
    };

    relationshipService = jasmine.createSpyObj('relationshipService',
      {
        getItemRelationshipsArray: observableOf(relationships),
      }
    );

    linkService = jasmine.createSpyObj('linkService',
      {
        resolveLinks: relationships[0],
      }
    );

    notificationsServiceStub = new NotificationsServiceStub();

    TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule, RouterTestingModule.withRoutes([]), TranslateModule.forRoot(), NgbModule],
      declarations: [ItemDeleteComponent, VarDirective],
      providers: [
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: Router, useValue: routerStub },
        { provide: ItemDataService, useValue: mockItemDataService },
        { provide: NotificationsService, useValue: notificationsServiceStub },
        { provide: ObjectUpdatesService, useValue: objectUpdatesServiceStub },
        { provide: RelationshipDataService, useValue: relationshipService },
        { provide: EntityTypeDataService, useValue: entityTypeService },
        { provide: RelationshipTypeDataService, useValue: {} },
        { provide: LinkService, useValue: linkService },
      ], schemas: [
        CUSTOM_ELEMENTS_SCHEMA
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemDeleteComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render a page with messages based on the \'delete\' messageKey', () => {
    const header = fixture.debugElement.query(By.css('h2')).nativeElement;
    expect(header.innerHTML).toContain('item.edit.delete.header');
    const description = fixture.debugElement.query(By.css('p')).nativeElement;
    expect(description.innerHTML).toContain('item.edit.delete.description');
    const confirmButton = fixture.debugElement.query(By.css('button.perform-action')).nativeElement;
    expect(confirmButton.innerHTML).toContain('item.edit.delete.confirm');
    const cancelButton = fixture.debugElement.query(By.css('button.cancel')).nativeElement;
    expect(cancelButton.innerHTML).toContain('item.edit.delete.cancel');
  });

  describe('performAction', () => {
    describe(`when there are entitytypes`, () => {
      it('should call delete function from the ItemDataService', () => {
        spyOn(comp, 'notify');
        comp.performAction();
        expect(mockItemDataService.delete)
          .toHaveBeenCalledWith(mockItem.id, types.filter((type) => typesSelection[type]).map((type) => type.id));
        expect(comp.notify).toHaveBeenCalled();
      });

      it('should call delete function from the ItemDataService with empty types', () => {

        spyOn(comp, 'notify');
        jasmine.getEnv().allowRespy(true);
        spyOn(entityTypeService, 'getEntityTypeRelationships').and.returnValue([]);
        comp.ngOnInit();

        comp.performAction();

        expect(mockItemDataService.delete).toHaveBeenCalledWith(mockItem.id, []);
        expect(comp.notify).toHaveBeenCalled();
      });
    });

    describe(`when there are no entity types`, () => {
      beforeEach(() => {
        (comp as any).entityTypeService = jasmine.createSpyObj('entityTypeService',
          {
            getEntityTypeByLabel: EMPTY,
          }
        );
      });

      it('should call delete function from the ItemDataService', () => {
        spyOn(comp, 'notify');
        comp.performAction();
        expect(mockItemDataService.delete)
          .toHaveBeenCalledWith(mockItem.id, types.filter((type) => typesSelection[type]).map((type) => type.id));
        expect(comp.notify).toHaveBeenCalled();
      });
    });
  });
  describe('notify', () => {
    it('should navigate to the homepage on successful deletion of the item', () => {
      comp.notify(true);
      expect(routerStub.navigate).toHaveBeenCalledWith(['']);
    });
  });
  describe('notify', () => {
    it('should navigate to the item edit page on failed deletion of the item', () => {
      comp.notify(false);
      expect(routerStub.navigate).toHaveBeenCalledWith([getItemEditRoute(mockItem)]);
    });
  });
});
