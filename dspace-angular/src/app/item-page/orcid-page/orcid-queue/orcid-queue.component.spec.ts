import { OrcidQueueComponent } from './orcid-queue.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateLoaderMock } from '../../../shared/mocks/translate-loader.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { OrcidQueueDataService } from '../../../core/orcid/orcid-queue-data.service';
import { PaginationService } from '../../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../../shared/testing/pagination-service.stub';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { OrcidHistoryDataService } from '../../../core/orcid/orcid-history-data.service';
import { OrcidQueue } from '../../../core/orcid/model/orcid-queue.model';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { PaginatedList } from '../../../core/data/paginated-list.model';
import { By } from '@angular/platform-browser';
import { Item } from '../../../core/shared/item.model';
import { OrcidAuthService } from '../../../core/orcid/orcid-auth.service';

describe('OrcidQueueComponent test suite', () => {
  let component: OrcidQueueComponent;
  let fixture: ComponentFixture<OrcidQueueComponent>;
  let debugElement: DebugElement;
  let orcidQueueService: OrcidQueueDataService;
  let orcidAuthService: jasmine.SpyObj<OrcidAuthService>;

  const testProfileItemId = 'test-owner-id';

  const mockItemLinkedToOrcid: Item = Object.assign(new Item(), {
    bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    metadata: {
      'dc.title': [{
        value: 'test person'
      }],
      'dspace.entity.type': [{
        'value': 'Person'
      }],
      'dspace.object.owner': [{
        'value': 'test person',
        'language': null,
        'authority': 'deced3e7-68e2-495d-bf98-7c44fc33b8ff',
        'confidence': 600,
        'place': 0
      }],
      'dspace.orcid.authenticated': [{
        'value': '2022-06-10T15:15:12.952872',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }],
      'dspace.orcid.scope': [{
        'value': '/authenticate',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }, {
        'value': '/read-limited',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 1
      }, {
        'value': '/activities/update',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 2
      }, {
        'value': '/person/update',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 3
      }],
      'person.identifier.orcid': [{
        'value': 'orcid-id',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }]
    }
  });

  function orcidQueueElement(id: number) {
    return Object.assign(new OrcidQueue(), {
      'id': id,
      'profileItemId': testProfileItemId,
      'entityId': `test-entity-${id}`,
      'description': `test description ${id}`,
      'recordType': 'Publication',
      'operation': 'INSERT',
      'type': 'orcidqueue',
    });
  }

  const orcidQueueElements = [orcidQueueElement(1), orcidQueueElement(2)];

  const orcidQueueServiceSpy = jasmine.createSpyObj('orcidQueueService', ['searchByProfileItemId', 'clearFindByProfileItemRequests']);
  orcidQueueServiceSpy.searchByProfileItemId.and.returnValue(createSuccessfulRemoteDataObject$<PaginatedList<OrcidQueue>>(createPaginatedList<OrcidQueue>(orcidQueueElements)));

  beforeEach(waitForAsync(() => {
    orcidAuthService = jasmine.createSpyObj('OrcidAuthService', {
      getOrcidAuthorizeUrl: jasmine.createSpy('getOrcidAuthorizeUrl')
    });

    void TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
        RouterTestingModule.withRoutes([])
      ],
      declarations: [OrcidQueueComponent],
      providers: [
        { provide: OrcidAuthService, useValue: orcidAuthService },
        { provide: OrcidQueueDataService, useValue: orcidQueueServiceSpy },
        { provide: OrcidHistoryDataService, useValue: {} },
        { provide: PaginationService, useValue: new PaginationServiceStub() },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    orcidQueueService = TestBed.inject(OrcidQueueDataService);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OrcidQueueComponent);
    component = fixture.componentInstance;
    component.item = mockItemLinkedToOrcid;
    debugElement = fixture.debugElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show the ORCID queue elements', () => {
    const table = debugElement.queryAll(By.css('[data-test="orcidQueueElementRow"]'));
    expect(table.length).toBe(2);
  });

});
