import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import {
  AdvancedWorkflowActionRatingComponent,
  ADVANCED_WORKFLOW_TASK_OPTION_RATING
} from './advanced-workflow-action-rating.component';
import { ActivatedRoute, Router } from '@angular/router';
import { of as observableOf } from 'rxjs';
import { ClaimedTaskDataService } from '../../../core/tasks/claimed-task-data.service';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { RouteService } from '../../../core/services/route.service';
import { routeServiceStub } from '../../../shared/testing/route-service.stub';
import { WorkflowActionDataService } from '../../../core/data/workflow-action-data.service';
import { WorkflowItemDataService } from '../../../core/submission/workflowitem-data.service';
import { ClaimedTaskDataServiceStub } from '../../../shared/testing/claimed-task-data-service.stub';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { WorkflowActionDataServiceStub } from '../../../shared/testing/workflow-action-data-service.stub';
import { WorkflowItemDataServiceStub } from '../../../shared/testing/workflow-item-data-service.stub';
import { RouterStub } from '../../../shared/testing/router.stub';
import { TranslateModule } from '@ngx-translate/core';
import { VarDirective } from '../../../shared/utils/var.directive';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { WorkflowItem } from '../../../core/submission/models/workflowitem.model';
import { createSuccessfulRemoteDataObject$, createSuccessfulRemoteDataObject } from '../../../shared/remote-data.utils';
import { Item } from '../../../core/shared/item.model';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ProcessTaskResponse } from '../../../core/tasks/models/process-task-response';
import { RatingAdvancedWorkflowInfo } from '../../../core/tasks/models/rating-advanced-workflow-info.model';
import { RequestService } from '../../../core/data/request.service';
import { RequestServiceStub } from '../../../shared/testing/request-service.stub';
import { LocationStub } from '../../../shared/testing/location.stub';

const claimedTaskId = '2';
const workflowId = '1';

describe('AdvancedWorkflowActionRatingComponent', () => {
  const workflowItem: WorkflowItem = new WorkflowItem();
  workflowItem.item = createSuccessfulRemoteDataObject$(new Item());
  let component: AdvancedWorkflowActionRatingComponent;
  let fixture: ComponentFixture<AdvancedWorkflowActionRatingComponent>;

  let claimedTaskDataService: ClaimedTaskDataServiceStub;
  let notificationService: NotificationsServiceStub;
  let workflowActionDataService: WorkflowItemDataServiceStub;
  let workflowItemDataService: WorkflowItemDataServiceStub;

  beforeEach(async () => {
    claimedTaskDataService = new ClaimedTaskDataServiceStub();
    notificationService = new NotificationsServiceStub();
    workflowActionDataService = new WorkflowActionDataServiceStub();
    workflowItemDataService = new WorkflowItemDataServiceStub();

    await TestBed.configureTestingModule({
      imports: [
        FormsModule,
        NgbModule,
        ReactiveFormsModule,
        TranslateModule.forRoot(),
      ],
      declarations: [
        AdvancedWorkflowActionRatingComponent,
        VarDirective,
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
              },
            },
          },
        },
        { provide: ClaimedTaskDataService, useValue: claimedTaskDataService },
        { provide: Location, useValue: new LocationStub() },
        { provide: NotificationsService, useValue: notificationService },
        { provide: RouteService, useValue: routeServiceStub },
        { provide: Router, useValue: new RouterStub() },
        { provide: WorkflowActionDataService, useValue: workflowActionDataService },
        { provide: WorkflowItemDataService, useValue: workflowItemDataService },
        { provide: RequestService, useClass: RequestServiceStub },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AdvancedWorkflowActionRatingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.debugElement.nativeElement.remove();
  });

  describe('performAction', () => {
    let ratingAdvancedWorkflowInfo: RatingAdvancedWorkflowInfo;
    beforeEach(() => {
      ratingAdvancedWorkflowInfo = new RatingAdvancedWorkflowInfo();
      ratingAdvancedWorkflowInfo.maxValue = 5;
      spyOn(component, 'getAdvancedInfo').and.returnValue(ratingAdvancedWorkflowInfo);
      spyOn(component, 'previousPage');
      // The form validators are set in the HTML code so the getAdvancedInfo needs to return a value
      fixture.detectChanges();
    });

    describe('with required review', () => {
      beforeEach(() => {
        ratingAdvancedWorkflowInfo.descriptionRequired = true;
        fixture.detectChanges();
      });

      it('should call the claimedTaskDataService with the rating and the required description when it has been rated and return to the mydspace page', () => {
        spyOn(claimedTaskDataService, 'submitTask').and.returnValue(observableOf(new ProcessTaskResponse(true)));
        component.ratingForm.setValue({
          review: 'Good job!',
          rating: 4,
        });

        component.performAction();

        expect(claimedTaskDataService.submitTask).toHaveBeenCalledWith(claimedTaskId, {
          [ADVANCED_WORKFLOW_TASK_OPTION_RATING]: true,
          review: 'Good job!',
          score: 4,
        });
        expect(notificationService.success).toHaveBeenCalled();
        expect(component.previousPage).toHaveBeenCalled();
      });

      it('should not call the claimedTaskDataService when the required description is empty', () => {
        spyOn(claimedTaskDataService, 'submitTask').and.returnValue(observableOf(new ProcessTaskResponse(true)));
        component.ratingForm.setValue({
          review: '',
          rating: 4,
        });

        component.performAction();

        expect(claimedTaskDataService.submitTask).not.toHaveBeenCalled();
        expect(notificationService.success).not.toHaveBeenCalled();
        expect(component.previousPage).not.toHaveBeenCalled();
      });
    });

    describe('with an optional review', () => {
      beforeEach(() => {
        ratingAdvancedWorkflowInfo.descriptionRequired = false;
        fixture.detectChanges();
      });

      it('should call the claimedTaskDataService with the optional review when provided and return to the mydspace page', () => {
        spyOn(claimedTaskDataService, 'submitTask').and.returnValue(observableOf(new ProcessTaskResponse(true)));
        component.ratingForm.setValue({
          review: 'Good job!',
          rating: 4,
        });

        component.performAction();

        expect(claimedTaskDataService.submitTask).toHaveBeenCalledWith(claimedTaskId, {
          [ADVANCED_WORKFLOW_TASK_OPTION_RATING]: true,
          review: 'Good job!',
          score: 4,
        });
        expect(notificationService.success).toHaveBeenCalled();
        expect(component.previousPage).toHaveBeenCalled();
      });

      it('should call the claimedTaskDataService when the optional description is empty and return to the mydspace page', () => {
        spyOn(claimedTaskDataService, 'submitTask').and.returnValue(observableOf(new ProcessTaskResponse(true)));
        component.ratingForm.setValue({
          review: '',
          rating: 4,
        });

        component.performAction();

        expect(claimedTaskDataService.submitTask).toHaveBeenCalledWith(claimedTaskId, {
          [ADVANCED_WORKFLOW_TASK_OPTION_RATING]: true,
          score: 4,
        });
        expect(notificationService.success).toHaveBeenCalled();
        expect(component.previousPage).toHaveBeenCalled();
      });
    });
  });
});
