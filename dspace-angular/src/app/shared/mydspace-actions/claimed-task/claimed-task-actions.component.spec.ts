import { ChangeDetectionStrategy, Injector, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { Router } from '@angular/router';

import { of as observableOf } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { TranslateLoaderMock } from '../../mocks/translate-loader.mock';
import { NotificationsService } from '../../notifications/notifications.service';
import { NotificationsServiceStub } from '../../testing/notifications-service.stub';
import { RouterStub } from '../../testing/router.stub';
import { Item } from '../../../core/shared/item.model';
import { ClaimedTaskDataService } from '../../../core/tasks/claimed-task-data.service';
import { ClaimedTaskActionsComponent } from './claimed-task-actions.component';
import { ClaimedTask } from '../../../core/tasks/models/claimed-task-object.model';
import { WorkflowItem } from '../../../core/submission/models/workflowitem.model';
import { getMockSearchService } from '../../mocks/search-service.mock';
import { getMockRequestService } from '../../mocks/request.service.mock';
import { RequestService } from '../../../core/data/request.service';
import { SearchService } from '../../../core/shared/search/search.service';
import { WorkflowActionDataService } from '../../../core/data/workflow-action-data.service';
import { WorkflowAction } from '../../../core/tasks/models/workflow-action-object.model';
import { VarDirective } from '../../utils/var.directive';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../../remote-data.utils';
import { By } from '@angular/platform-browser';

let component: ClaimedTaskActionsComponent;
let fixture: ComponentFixture<ClaimedTaskActionsComponent>;

let mockObject: ClaimedTask;
let notificationsServiceStub: NotificationsServiceStub;
let router: RouterStub;

let mockDataService;
let searchService;
let requestServce;
let workflowActionService: WorkflowActionDataService;

let item;
let rdItem;
let workflowitem;
let rdWorkflowitem;
let workflowAction;

function init() {
  mockDataService = jasmine.createSpyObj('ClaimedTaskDataService', {
    approveTask: jasmine.createSpy('approveTask'),
    rejectTask: jasmine.createSpy('rejectTask'),
    returnToPoolTask: jasmine.createSpy('returnToPoolTask'),
  });
  searchService = getMockSearchService();
  requestServce = getMockRequestService();

  item = Object.assign(new Item(), {
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
  rdItem = createSuccessfulRemoteDataObject(item);
  workflowitem = Object.assign(new WorkflowItem(), { item: observableOf(rdItem), id: '333' });
  rdWorkflowitem = createSuccessfulRemoteDataObject(workflowitem);
  mockObject = Object.assign(new ClaimedTask(), { workflowitem: observableOf(rdWorkflowitem), id: '1234' });
  workflowAction = Object.assign(new WorkflowAction(), { id: 'action-1', options: ['option-1', 'option-2'] });

  workflowActionService = jasmine.createSpyObj('workflowActionService', {
    findById: createSuccessfulRemoteDataObject$(workflowAction)
  });
}

describe('ClaimedTaskActionsComponent', () => {
  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      declarations: [ClaimedTaskActionsComponent, VarDirective],
      providers: [
        { provide: Injector, useValue: {} },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: Router, useValue: new RouterStub() },
        { provide: ClaimedTaskDataService, useValue: mockDataService },
        { provide: SearchService, useValue: searchService },
        { provide: RequestService, useValue: requestServce },
        { provide: WorkflowActionDataService, useValue: workflowActionService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ClaimedTaskActionsComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ClaimedTaskActionsComponent);
    component = fixture.componentInstance;
    component.item = item;
    component.object = mockObject;
    component.workflowitem = workflowitem;
    notificationsServiceStub = TestBed.inject(NotificationsService as any);
    router = TestBed.inject(Router as any);
    fixture.detectChanges();
  });

  it('should init objects properly', () => {
    component.object = null;
    component.initObjects(mockObject);

    expect(component.item).toEqual(item);

    expect(component.object).toEqual(mockObject);

    expect(component.workflowitem).toEqual(workflowitem);
  });

  it('should reload page on process completed', waitForAsync(() => {
    spyOn(router, 'navigateByUrl');
    router.url = 'test.url/test';

    component.handleActionResponse(true);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      expect(router.navigateByUrl).toHaveBeenCalledWith('test.url/test');
    });
  }));

  it('should display an error notification on process failure', waitForAsync(() => {
    component.handleActionResponse(false);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      expect(notificationsServiceStub.error).toHaveBeenCalled();
    });
  }));

  it('should display a view button', waitForAsync(() => {
    component.object = null;
    component.initObjects(mockObject);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const debugElement = fixture.debugElement.query(By.css('.workflow-view'));
      expect(debugElement).toBeTruthy();
      expect(debugElement.nativeElement.innerText.trim()).toBe('submission.workflow.generic.view');
    });

  }));

  it('getWorkflowItemViewRoute should return the combined uri to show a workspaceitem', waitForAsync(() => {
    const href = component.getWorkflowItemViewRoute(workflowitem);
    expect(href).toEqual('/workflowitems/333/view');
  }));

});
