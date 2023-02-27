import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { LinkService } from '../../../../core/cache/builders/link.service';
import { ObjectUpdatesService } from '../../../../core/data/object-updates/object-updates.service';
import { RelationshipDataService } from '../../../../core/data/relationship-data.service';
import { ItemType } from '../../../../core/shared/item-relationships/item-type.model';
import { RelationshipType } from '../../../../core/shared/item-relationships/relationship-type.model';
import { Relationship } from '../../../../core/shared/item-relationships/relationship.model';
import { Item } from '../../../../core/shared/item.model';
import { SelectableListService } from '../../../../shared/object-list/selectable-list/selectable-list.service';
import { SharedModule } from '../../../../shared/shared.module';
import { EditRelationshipListComponent } from './edit-relationship-list.component';
import { createSuccessfulRemoteDataObject$ } from '../../../../shared/remote-data.utils';
import { createPaginatedList } from '../../../../shared/testing/utils.test';
import { PaginationService } from '../../../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../../../shared/testing/pagination-service.stub';
import { HostWindowService } from '../../../../shared/host-window.service';
import { HostWindowServiceStub } from '../../../../shared/testing/host-window-service.stub';
import { PaginationComponent } from '../../../../shared/pagination/pagination.component';
import { PaginationComponentOptions } from '../../../../shared/pagination/pagination-component-options.model';
import { RelationshipTypeDataService } from '../../../../core/data/relationship-type-data.service';
import { FieldChangeType } from '../../../../core/data/object-updates/field-change-type.model';
import { GroupDataService } from '../../../../core/eperson/group-data.service';
import { ConfigurationDataService } from '../../../../core/data/configuration-data.service';
import { LinkHeadService } from '../../../../core/services/link-head.service';
import { SearchConfigurationService } from '../../../../core/shared/search/search-configuration.service';
import { SearchConfigurationServiceStub } from '../../../../shared/testing/search-configuration-service.stub';
import { ConfigurationProperty } from '../../../../core/shared/configuration-property.model';
import { Router } from '@angular/router';
import { RouterMock } from '../../../../shared/mocks/router.mock';
import { APP_CONFIG } from '../../../../../config/app-config.interface';

let comp: EditRelationshipListComponent;
let fixture: ComponentFixture<EditRelationshipListComponent>;
let de: DebugElement;

let linkService;
let objectUpdatesService;
let relationshipService;
let selectableListService;
let paginationService;
let hostWindowService;
const relationshipTypeService = {};

const url = 'http://test-url.com/test-url';

let item;
let entityType;
let relatedEntityType;
let author1;
let author2;
let fieldUpdate1;
let fieldUpdate2;
let relationships;
let relationshipType;
let paginationOptions;

describe('EditRelationshipListComponent', () => {

  const resetComponent = () => {
    fixture = TestBed.createComponent(EditRelationshipListComponent);
    comp = fixture.componentInstance;
    de = fixture.debugElement;
    comp.item = item;
    comp.itemType = entityType;
    comp.url = url;
    comp.relationshipType = relationshipType;
    comp.hasChanges = observableOf(false);
    fixture.detectChanges();
  };

  beforeEach(waitForAsync(() => {

    entityType = Object.assign(new ItemType(), {
      id: 'Publication',
      uuid: 'Publication',
      label: 'Publication',
    });

    relatedEntityType = Object.assign(new ItemType(), {
      id: 'Author',
      uuid: 'Author',
      label: 'Author',
    });

    relationshipType = Object.assign(new RelationshipType(), {
      id: '1',
      uuid: '1',
      leftType: createSuccessfulRemoteDataObject$(entityType),
      rightType: createSuccessfulRemoteDataObject$(relatedEntityType),
      leftwardType: 'isAuthorOfPublication',
      rightwardType: 'isPublicationOfAuthor',
    });

    paginationOptions = Object.assign(new PaginationComponentOptions(), {
      id: `er${relationshipType.id}`,
      pageSize: 5,
      currentPage: 1,
    });

    author1 = Object.assign(new Item(), {
      id: 'author1',
      uuid: 'author1'
    });
    author2 = Object.assign(new Item(), {
      id: 'author2',
      uuid: 'author2'
    });

    relationships = [
      Object.assign(new Relationship(), {
        self: url + '/2',
        id: '2',
        uuid: '2',
        relationshipType: createSuccessfulRemoteDataObject$(relationshipType),
        leftItem: createSuccessfulRemoteDataObject$(item),
        rightItem: createSuccessfulRemoteDataObject$(author1),
      }),
      Object.assign(new Relationship(), {
        self: url + '/3',
        id: '3',
        uuid: '3',
        relationshipType: createSuccessfulRemoteDataObject$(relationshipType),
        leftItem: createSuccessfulRemoteDataObject$(item),
        rightItem: createSuccessfulRemoteDataObject$(author2),
      })
    ];

    item = Object.assign(new Item(), {
      _links: {
        self: { href: 'fake-item-url/publication' }
      },
      id: 'publication',
      uuid: 'publication',
      relationships: createSuccessfulRemoteDataObject$(createPaginatedList(relationships))
    });

    fieldUpdate1 = {
      field: {
        uuid: relationships[0].uuid,
        relationship: relationships[0],
        type: relationshipType,
      },
      changeType: undefined
    };
    fieldUpdate2 = {
      field: {
        uuid: relationships[1].uuid,
        relationship: relationships[1],
        type: relationshipType,
      },
      changeType: FieldChangeType.REMOVE
    };

    objectUpdatesService = jasmine.createSpyObj('objectUpdatesService',
      {
        getFieldUpdates: observableOf({
          [relationships[0].uuid]: fieldUpdate1,
          [relationships[1].uuid]: fieldUpdate2
        })
      }
    );

    relationshipService = jasmine.createSpyObj('relationshipService',
      {
        getRelatedItemsByLabel: createSuccessfulRemoteDataObject$(createPaginatedList([author1, author2])),
        getItemRelationshipsByLabel: createSuccessfulRemoteDataObject$(createPaginatedList(relationships)),
        isLeftItem: observableOf(true),
      }
    );

    selectableListService = {};

    linkService = {
      resolveLink: () => null,
      resolveLinks: () => null,
    };

    paginationService = new PaginationServiceStub(paginationOptions);

    hostWindowService = new HostWindowServiceStub(1200);

    const linkHeadService = jasmine.createSpyObj('linkHeadService', {
      addTag: ''
    });

    const groupDataService = jasmine.createSpyObj('groupsDataService', {
      findListByHref: createSuccessfulRemoteDataObject$(createPaginatedList([])),
      getGroupRegistryRouterLink: '',
      getUUIDFromString: '',
    });

    const configurationDataService = jasmine.createSpyObj('configurationDataService', {
      findByPropertyName: createSuccessfulRemoteDataObject$(Object.assign(new ConfigurationProperty(), {
        name: 'test',
        values: [
          'org.dspace.ctask.general.ProfileFormats = test'
        ]
      }))
    });

    const environmentUseThumbs = {
      browseBy: {
        showThumbnails: true
      }
    };

    TestBed.configureTestingModule({
      imports: [SharedModule, TranslateModule.forRoot()],
      declarations: [EditRelationshipListComponent],
      providers: [
        { provide: ObjectUpdatesService, useValue: objectUpdatesService },
        { provide: RelationshipDataService, useValue: relationshipService },
        { provide: SelectableListService, useValue: selectableListService },
        { provide: LinkService, useValue: linkService },
        { provide: PaginationService, useValue: paginationService },
        { provide: HostWindowService, useValue: hostWindowService },
        { provide: RelationshipTypeDataService, useValue: relationshipTypeService },
        { provide: GroupDataService, useValue: groupDataService },
        { provide: Router, useValue: new RouterMock() },
        { provide: LinkHeadService, useValue: linkHeadService },
        { provide: ConfigurationDataService, useValue: configurationDataService },
        { provide: SearchConfigurationService, useValue: new SearchConfigurationServiceStub() },
        { provide: APP_CONFIG, useValue: environmentUseThumbs }
      ], schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();

    resetComponent();
  }));

  describe('changeType is REMOVE', () => {
    beforeEach(() => {
      fieldUpdate1.changeType = FieldChangeType.REMOVE;
      fixture.detectChanges();
    });
    it('the div should have class alert-danger', () => {
      const element = de.queryAll(By.css('.relationship-row'))[1].nativeElement;
      expect(element.classList).toContain('alert-danger');
    });
  });

  describe('pagination component', () => {
    let paginationComp: PaginationComponent;

    beforeEach(() => {
      paginationComp = de.query(By.css('ds-pagination')).componentInstance;
    });

    it('should receive the correct pagination config', () => {
      expect(paginationComp.paginationOptions).toEqual(paginationOptions);
    });

    it('should receive correct collection size', () => {
      expect(paginationComp.collectionSize).toEqual(relationships.length);
    });

  });

  describe('relationshipService.getItemRelationshipsByLabel', () => {
    it('should receive the correct pagination info', () => {
      expect(relationshipService.getItemRelationshipsByLabel).toHaveBeenCalledTimes(1);

      const callArgs = relationshipService.getItemRelationshipsByLabel.calls.mostRecent().args;
      const findListOptions = callArgs[2];
      const linksToFollow = callArgs[5];
      expect(findListOptions.elementsPerPage).toEqual(paginationOptions.pageSize);
      expect(findListOptions.currentPage).toEqual(paginationOptions.currentPage);
      expect(linksToFollow.linksToFollow[0].name).toEqual('thumbnail');

    });

    describe('when the publication is on the left side of the relationship', () => {
      beforeEach(() => {
        relationshipType = Object.assign(new RelationshipType(), {
          id: '1',
          uuid: '1',
          leftType: createSuccessfulRemoteDataObject$(entityType), // publication
          rightType: createSuccessfulRemoteDataObject$(relatedEntityType), // author
          leftwardType: 'isAuthorOfPublication',
          rightwardType: 'isPublicationOfAuthor',
        });
        relationshipService.getItemRelationshipsByLabel.calls.reset();
        resetComponent();
      });

      it('should fetch isAuthorOfPublication', () => {
        expect(relationshipService.getItemRelationshipsByLabel).toHaveBeenCalledTimes(1);

        const callArgs = relationshipService.getItemRelationshipsByLabel.calls.mostRecent().args;
        const label = callArgs[1];

        expect(label).toEqual('isAuthorOfPublication');
      });
    });

    describe('when the publication is on the right side of the relationship', () => {
      beforeEach(() => {
        relationshipType = Object.assign(new RelationshipType(), {
          id: '1',
          uuid: '1',
          leftType: createSuccessfulRemoteDataObject$(relatedEntityType), // author
          rightType: createSuccessfulRemoteDataObject$(entityType), // publication
          leftwardType: 'isPublicationOfAuthor',
          rightwardType: 'isAuthorOfPublication',
        });
        relationshipService.getItemRelationshipsByLabel.calls.reset();
        resetComponent();
      });

      it('should fetch isAuthorOfPublication', () => {
        expect(relationshipService.getItemRelationshipsByLabel).toHaveBeenCalledTimes(1);

        const callArgs = relationshipService.getItemRelationshipsByLabel.calls.mostRecent().args;
        const label = callArgs[1];

        expect(label).toEqual('isAuthorOfPublication');
      });
    });



    describe('changes managment for add buttons', () => {

      it('should show enabled add buttons', () => {
        const element = de.query(By.css('.btn-success'));
        expect(element.nativeElement?.disabled).toBeFalse();
      });

      it('after hash changes changed', () => {
        comp.hasChanges = observableOf(true);
        fixture.detectChanges();
        const element = de.query(By.css('.btn-success'));
        expect(element.nativeElement?.disabled).toBeTrue();
      });
    });

  });

});
