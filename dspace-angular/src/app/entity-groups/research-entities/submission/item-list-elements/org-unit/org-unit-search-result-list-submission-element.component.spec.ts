import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of as observableOf } from 'rxjs';
import { RemoteDataBuildService } from '../../../../../core/cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../../../../../core/cache/object-cache.service';
import { BitstreamDataService } from '../../../../../core/data/bitstream-data.service';
import { CommunityDataService } from '../../../../../core/data/community-data.service';
import { DefaultChangeAnalyzer } from '../../../../../core/data/default-change-analyzer.service';
import { DSOChangeAnalyzer } from '../../../../../core/data/dso-change-analyzer.service';
import { ItemDataService } from '../../../../../core/data/item-data.service';
import { buildPaginatedList } from '../../../../../core/data/paginated-list.model';
import { RelationshipDataService } from '../../../../../core/data/relationship-data.service';
import { RemoteData } from '../../../../../core/data/remote-data';
import { Bitstream } from '../../../../../core/shared/bitstream.model';
import { HALEndpointService } from '../../../../../core/shared/hal-endpoint.service';
import { Item } from '../../../../../core/shared/item.model';
import { UUIDService } from '../../../../../core/shared/uuid.service';
import { NotificationsService } from '../../../../../shared/notifications/notifications.service';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { SelectableListService } from '../../../../../shared/object-list/selectable-list/selectable-list.service';
import { createSuccessfulRemoteDataObject$ } from '../../../../../shared/remote-data.utils';
import { TruncatableService } from '../../../../../shared/truncatable/truncatable.service';
import { TruncatePipe } from '../../../../../shared/utils/truncate.pipe';
import { OrgUnitSearchResultListSubmissionElementComponent } from './org-unit-search-result-list-submission-element.component';
import { DSONameService } from '../../../../../core/breadcrumbs/dso-name.service';
import { DSONameServiceMock } from '../../../../../shared/mocks/dso-name.service.mock';
import { APP_CONFIG } from '../../../../../../config/app-config.interface';
import { environment } from '../../../../../../environments/environment';

let personListElementComponent: OrgUnitSearchResultListSubmissionElementComponent;
let fixture: ComponentFixture<OrgUnitSearchResultListSubmissionElementComponent>;

let mockItemWithMetadata: ItemSearchResult;
let mockItemWithoutMetadata: ItemSearchResult;

let nameVariant;
let mockRelationshipService;

function init() {
  mockItemWithMetadata = Object.assign(
    new ItemSearchResult(),
    {
      indexableObject: Object.assign(new Item(), {
        bundles: createSuccessfulRemoteDataObject$(buildPaginatedList(undefined, [])),
        metadata: {
          'dc.title': [
            {
              language: 'en_US',
              value: 'This is just another title'
            }
          ],
          'organization.address.addressLocality': [
            {
              language: 'en_US',
              value: 'Europe'
            }
          ],
          'organization.address.addressCountry': [
            {
              language: 'en_US',
              value: 'Belgium'
            }
          ]
        }
      })
    });
  mockItemWithoutMetadata = Object.assign(
    new ItemSearchResult(),
    {
      indexableObject: Object.assign(new Item(), {
        bundles: createSuccessfulRemoteDataObject$(buildPaginatedList(undefined, [])),
        metadata: {
          'dc.title': [
            {
              language: 'en_US',
              value: 'This is just another title'
            }
          ]
        }
      })
    });

  nameVariant = 'Doe J.';
  mockRelationshipService = {
    getNameVariant: () => observableOf(nameVariant)
  };
}

describe('OrgUnitSearchResultListSubmissionElementComponent', () => {
  beforeEach(waitForAsync(() => {
    init();
    const mockBitstreamDataService = {
      getThumbnailFor(item: Item): Observable<RemoteData<Bitstream>> {
        return createSuccessfulRemoteDataObject$(new Bitstream());
      }
    };
    TestBed.configureTestingModule({
      declarations: [OrgUnitSearchResultListSubmissionElementComponent, TruncatePipe],
      providers: [
        { provide: TruncatableService, useValue: {} },
        { provide: RelationshipDataService, useValue: mockRelationshipService },
        { provide: NotificationsService, useValue: {} },
        { provide: TranslateService, useValue: {} },
        { provide: NgbModal, useValue: {} },
        { provide: ItemDataService, useValue: {} },
        { provide: SelectableListService, useValue: {} },
        { provide: Store, useValue: {} },
        { provide: ObjectCacheService, useValue: {} },
        { provide: UUIDService, useValue: {} },
        { provide: RemoteDataBuildService, useValue: {} },
        { provide: CommunityDataService, useValue: {} },
        { provide: HALEndpointService, useValue: {} },
        { provide: HttpClient, useValue: {} },
        { provide: DSOChangeAnalyzer, useValue: {} },
        { provide: DefaultChangeAnalyzer, useValue: {} },
        { provide: BitstreamDataService, useValue: mockBitstreamDataService },
        { provide: DSONameService, useClass: DSONameServiceMock },
        { provide: APP_CONFIG, useValue: environment }
      ],

      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(OrgUnitSearchResultListSubmissionElementComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(OrgUnitSearchResultListSubmissionElementComponent);
    personListElementComponent = fixture.componentInstance;

  }));

  describe('When the item has a address locality span', () => {
    beforeEach(() => {
      personListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });

    it('should show the address locality span', () => {
      const jobTitleField = fixture.debugElement.query(By.css('span.item-list-address-locality'));
      expect(jobTitleField).not.toBeNull();
    });
  });

  describe('When the item has no address locality', () => {
    beforeEach(() => {
      personListElementComponent.object = mockItemWithoutMetadata;
      fixture.detectChanges();
    });

    it('should not show the address locality span', () => {
      const jobTitleField = fixture.debugElement.query(By.css('span.item-list-address-locality'));
      expect(jobTitleField).toBeNull();
    });
  });

  describe('When the item has a address country span', () => {
    beforeEach(() => {
      personListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });

    it('should show the address country span', () => {
      const jobTitleField = fixture.debugElement.query(By.css('span.item-list-address-country'));
      expect(jobTitleField).not.toBeNull();
    });
  });

  describe('When the item has no address country', () => {
    beforeEach(() => {
      personListElementComponent.object = mockItemWithoutMetadata;
      fixture.detectChanges();
    });

    it('should not show the address country span', () => {
      const jobTitleField = fixture.debugElement.query(By.css('span.item-list-address-country'));
      expect(jobTitleField).toBeNull();
    });
  });

});

