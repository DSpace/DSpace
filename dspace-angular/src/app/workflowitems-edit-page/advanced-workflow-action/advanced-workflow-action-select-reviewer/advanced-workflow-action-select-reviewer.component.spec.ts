import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import {
  AdvancedWorkflowActionSelectReviewerComponent,
  ADVANCED_WORKFLOW_TASK_OPTION_SELECT_REVIEWER,
} from './advanced-workflow-action-select-reviewer.component';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkflowItemDataService } from '../../../core/submission/workflowitem-data.service';
import { WorkflowItemDataServiceStub } from '../../../shared/testing/workflow-item-data-service.stub';
import { WorkflowActionDataServiceStub } from '../../../shared/testing/workflow-action-data-service.stub';
import { WorkflowActionDataService } from '../../../core/data/workflow-action-data.service';
import { RouteService } from '../../../core/services/route.service';
import { routeServiceStub } from '../../../shared/testing/route-service.stub';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { TranslateModule } from '@ngx-translate/core';
import { ClaimedTaskDataService } from '../../../core/tasks/claimed-task-data.service';
import { ClaimedTaskDataServiceStub } from '../../../shared/testing/claimed-task-data-service.stub';
import { of as observableOf } from 'rxjs';
import { WorkflowItem } from '../../../core/submission/models/workflowitem.model';
import { createSuccessfulRemoteDataObject$, createSuccessfulRemoteDataObject } from '../../../shared/remote-data.utils';
import { Item } from '../../../core/shared/item.model';
import { EPersonMock, EPersonMock2 } from '../../../shared/testing/eperson.mock';
import { ProcessTaskResponse } from '../../../core/tasks/models/process-task-response';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RequestService } from '../../../core/data/request.service';
import { RequestServiceStub } from '../../../shared/testing/request-service.stub';
import { RouterStub } from '../../../shared/testing/router.stub';
import { LocationStub } from '../../../shared/testing/location.stub';

const claimedTaskId = '2';
const workflowId = '1';

describe('AdvancedWorkflowActionSelectReviewerComponent', () => {
  const workflowItem: WorkflowItem = new WorkflowItem();
  workflowItem.item = createSuccessfulRemoteDataObject$(new Item());
  let component: AdvancedWorkflowActionSelectReviewerComponent;
  let fixture: ComponentFixture<AdvancedWorkflowActionSelectReviewerComponent>;

  let claimedTaskDataService: ClaimedTaskDataServiceStub;
  let location: LocationStub;
  let notificationService: NotificationsServiceStub;
  let router: RouterStub;
  let workflowActionDataService: WorkflowItemDataServiceStub;
  let workflowItemDataService: WorkflowItemDataServiceStub;

  beforeEach(async () => {
    claimedTaskDataService = new ClaimedTaskDataServiceStub();
    location = new LocationStub();
    notificationService = new NotificationsServiceStub();
    router = new RouterStub();
    workflowActionDataService = new WorkflowActionDataServiceStub();
    workflowItemDataService = new WorkflowItemDataServiceStub();

    await TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
      ],
      declarations: [
        AdvancedWorkflowActionSelectReviewerComponent,
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: observableOf({
              id: workflowId,
              wfi: createSuccessfulRemoteDataObject(workflowItem),
            }),
            snapshot: {
              queryParams: {
                claimedTask: claimedTaskId,
                workflow: 'testaction',
                previousSearchQuery: 'Thor%20Love%20and%20Thunder',
              },
            },
          },
        },
        { provide: ClaimedTaskDataService, useValue: claimedTaskDataService },
        { provide: Location, useValue: location },
        { provide: NotificationsService, useValue: notificationService },
        { provide: Router, useValue: router },
        { provide: RouteService, useValue: routeServiceStub },
        { provide: WorkflowActionDataService, useValue: workflowActionDataService },
        { provide: WorkflowItemDataService, useValue: workflowItemDataService },
        { provide: RequestService, useClass: RequestServiceStub },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AdvancedWorkflowActionSelectReviewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.debugElement.nativeElement.remove();
  });

  describe('previousPage', () => {
    it('should navigate back to the Workflow tasks page with the previous query', () => {
      spyOn(location, 'getState').and.returnValue({
        previousQueryParams: {
          configuration: 'workflow',
          query: 'Thor Love and Thunder',
        },
      });

      component.ngOnInit();
      component.previousPage();

      expect(router.navigate).toHaveBeenCalledWith(['/mydspace'], {
        queryParams: {
          configuration: 'workflow',
          query: 'Thor Love and Thunder',
        },
      });
    });
  });

  describe('performAction', () => {
    beforeEach(() => {
      spyOn(component, 'previousPage');
    });

    it('should call the claimedTaskDataService with the list of selected ePersons', () => {
      spyOn(claimedTaskDataService, 'submitTask').and.returnValue(observableOf(new ProcessTaskResponse(true)));
      component.selectedReviewers = [EPersonMock, EPersonMock2];

      component.performAction();

      expect(claimedTaskDataService.submitTask).toHaveBeenCalledWith(claimedTaskId, {
        [ADVANCED_WORKFLOW_TASK_OPTION_SELECT_REVIEWER]: true,
        eperson: [EPersonMock.id, EPersonMock2.id],
      });
      expect(notificationService.success).toHaveBeenCalled();
      expect(component.previousPage).toHaveBeenCalled();
    });

    it('should not call the claimedTaskDataService with the list of selected ePersons when it\'s empty', () => {
      spyOn(claimedTaskDataService, 'submitTask').and.returnValue(observableOf(new ProcessTaskResponse(true)));
      component.selectedReviewers = [];

      component.performAction();

      expect(claimedTaskDataService.submitTask).not.toHaveBeenCalled();
    });

    it('should not call the return to mydspace page when the request failed', () => {
      spyOn(claimedTaskDataService, 'submitTask').and.returnValue(observableOf(new ProcessTaskResponse(false)));
      component.selectedReviewers = [EPersonMock, EPersonMock2];

      component.performAction();

      expect(claimedTaskDataService.submitTask).toHaveBeenCalledWith(claimedTaskId, {
        [ADVANCED_WORKFLOW_TASK_OPTION_SELECT_REVIEWER]: true,
        eperson: [EPersonMock.id, EPersonMock2.id],
      });
      expect(notificationService.error).toHaveBeenCalled();
      expect(component.previousPage).not.toHaveBeenCalled();
    });
  });
});
