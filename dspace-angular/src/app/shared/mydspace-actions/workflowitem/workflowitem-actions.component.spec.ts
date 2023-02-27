import { ChangeDetectionStrategy, Injector, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { Router } from '@angular/router';

import { of as observableOf } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { TranslateLoaderMock } from '../../mocks/translate-loader.mock';
import { RouterStub } from '../../testing/router.stub';
import { Item } from '../../../core/shared/item.model';
import { WorkflowItem } from '../../../core/submission/models/workflowitem.model';
import { WorkflowitemActionsComponent } from './workflowitem-actions.component';
import { WorkflowItemDataService } from '../../../core/submission/workflowitem-data.service';
import { NotificationsService } from '../../notifications/notifications.service';
import { NotificationsServiceStub } from '../../testing/notifications-service.stub';
import { createSuccessfulRemoteDataObject } from '../../remote-data.utils';
import { getMockRequestService } from '../../mocks/request.service.mock';
import { RequestService } from '../../../core/data/request.service';
import { getMockSearchService } from '../../mocks/search-service.mock';
import { SearchService } from '../../../core/shared/search/search.service';
import { By } from '@angular/platform-browser';

let component: WorkflowitemActionsComponent;
let fixture: ComponentFixture<WorkflowitemActionsComponent>;

let mockObject: WorkflowItem;

const mockDataService = {};

const searchService = getMockSearchService();

const requestServce = getMockRequestService();

const item = Object.assign(new Item(), {
  bundles: observableOf({}),
  metadata: {
    'dc.title': [
      {
        language: 'en_US',
        value: 'This is just another title'
      }
    ],
    'dc.type': [
      {
        language: null,
        value: 'Article'
      }
    ],
    'dc.contributor.author': [
      {
        language: 'en_US',
        value: 'Smith, Donald'
      }
    ],
    'dc.date.issued': [
      {
        language: null,
        value: '2015-06-26'
      }
    ]
  }
});
const rd = createSuccessfulRemoteDataObject(item);
mockObject = Object.assign(new WorkflowItem(), { item: observableOf(rd), id: '1234', uuid: '1234' });

describe('WorkflowitemActionsComponent', () => {
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
      declarations: [WorkflowitemActionsComponent],
      providers: [
        { provide: Injector, useValue: {} },
        { provide: Router, useValue: new RouterStub() },
        { provide: WorkflowItemDataService, useValue: mockDataService },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: SearchService, useValue: searchService },
        { provide: RequestService, useValue: requestServce }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(WorkflowitemActionsComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WorkflowitemActionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture = null;
    component = null;
  });

  it('should init object properly', () => {
    component.initObjects(mockObject);

    expect(component.object).toEqual(mockObject);
  });

  it('should display view button', () => {
    const btn = fixture.debugElement.query(By.css('button [data-test="view-btn"]'));

    expect(btn).toBeDefined();
  });

});
