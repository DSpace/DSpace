import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdvancedClaimedTaskActionRatingComponent } from './advanced-claimed-task-action-rating.component';
import { By } from '@angular/platform-browser';
import { ClaimedTask } from '../../../../core/tasks/models/claimed-task-object.model';
import { ClaimedTaskDataServiceStub } from '../../../testing/claimed-task-data-service.stub';
import { NotificationsServiceStub } from '../../../testing/notifications-service.stub';
import { RouterStub } from '../../../testing/router.stub';
import { SearchServiceStub } from '../../../testing/search-service.stub';
import { ClaimedTaskDataService } from '../../../../core/tasks/claimed-task-data.service';
import { NotificationsService } from '../../../notifications/notifications.service';
import { RequestService } from '../../../../core/data/request.service';
import { Router, ActivatedRoute } from '@angular/router';
import { SearchService } from '../../../../core/shared/search/search.service';
import { Location } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import {
  ADVANCED_WORKFLOW_ACTION_RATING
} from '../../../../workflowitems-edit-page/advanced-workflow-action/advanced-workflow-action-rating/advanced-workflow-action-rating.component';
import { of as observableOf } from 'rxjs';
import { WorkflowItem } from '../../../../core/submission/models/workflowitem.model';
import { ActivatedRouteStub } from '../../../testing/active-router.stub';

const taskId = 'claimed-task-1';
const workflowId = 'workflow-1';

describe('AdvancedClaimedTaskActionRatingComponent', () => {
  const object = Object.assign(new ClaimedTask(), {
    id: taskId,
    workflowitem: observableOf(Object.assign(new WorkflowItem(), {
      id: workflowId,
    })),
  });
  let component: AdvancedClaimedTaskActionRatingComponent;
  let fixture: ComponentFixture<AdvancedClaimedTaskActionRatingComponent>;

  let claimedTaskDataService: ClaimedTaskDataServiceStub;
  let notificationService: NotificationsServiceStub;
  let route: ActivatedRouteStub;
  let router: RouterStub;
  let searchService: SearchServiceStub;

  beforeEach(async () => {
    claimedTaskDataService = new ClaimedTaskDataServiceStub();
    notificationService = new NotificationsServiceStub();
    route = new ActivatedRouteStub();
    router = new RouterStub();
    searchService = new SearchServiceStub();

    await TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
      ],
      declarations: [
        AdvancedClaimedTaskActionRatingComponent,
      ],
      providers: [
        { provide: ActivatedRoute, useValue: route },
        { provide: ClaimedTaskDataService, useValue: claimedTaskDataService },
        { provide: NotificationsService, useValue: notificationService },
        { provide: RequestService, useValue: {} },
        { provide: Router, useValue: router },
        { provide: SearchService, useValue: searchService },
        Location,
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AdvancedClaimedTaskActionRatingComponent);
    component = fixture.componentInstance;
    component.object = object;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.debugElement.nativeElement.remove();
  });

  it('should display select reviewer button', () => {
    const btn = fixture.debugElement.query(By.css('.ratingReviewerAction'));

    expect(btn).not.toBeNull();
  });

  it('should navigate to the advanced workflow page when clicked', () => {
    component.workflowTaskPageRoute = `/workflowitems/${workflowId}/advanced`;
    fixture.debugElement.query(By.css('.ratingReviewerAction')).nativeElement.click();

    expect(router.navigate).toHaveBeenCalledWith([`/workflowitems/${workflowId}/advanced`], {
      queryParams: {
        workflow: ADVANCED_WORKFLOW_ACTION_RATING,
        claimedTask: taskId,
      },
    });
  });
});
