import { ChangeDetectionStrategy, Injector, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of as observableOf } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { PoolTaskDataService } from '../../core/tasks/pool-task-data.service';
import { ClaimedTaskDataService } from '../../core/tasks/claimed-task-data.service';
import { PoolTaskActionsComponent } from './pool-task/pool-task-actions.component';
import { PoolTask } from '../../core/tasks/models/pool-task-object.model';
import { NotificationsServiceStub } from '../testing/notifications-service.stub';
import { RouterStub } from '../testing/router.stub';
import { getMockSearchService } from '../mocks/search-service.mock';
import { getMockRequestService } from '../mocks/request.service.mock';
import { Item } from '../../core/shared/item.model';
import { createFailedRemoteDataObject, createSuccessfulRemoteDataObject } from '../remote-data.utils';
import { WorkflowItem } from '../../core/submission/models/workflowitem.model';
import { TranslateLoaderMock } from '../mocks/translate-loader.mock';
import { NotificationsService } from '../notifications/notifications.service';
import { SearchService } from '../../core/shared/search/search.service';
import { RequestService } from '../../core/data/request.service';
import { ProcessTaskResponse } from '../../core/tasks/models/process-task-response';

let mockDataService: PoolTaskDataService;
let mockClaimedTaskDataService: ClaimedTaskDataService;

let component: PoolTaskActionsComponent;
let fixture: ComponentFixture<PoolTaskActionsComponent>;

let mockObject: PoolTask;
let notificationsServiceStub: NotificationsServiceStub;
let router: RouterStub;

const searchService = getMockSearchService();

const requestService = getMockRequestService();

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
const rdItem = createSuccessfulRemoteDataObject(item);
const workflowitem = Object.assign(new WorkflowItem(), { item: observableOf(rdItem) });
const rdWorkflowitem = createSuccessfulRemoteDataObject(workflowitem);
mockObject = Object.assign(new PoolTask(), { workflowitem: observableOf(rdWorkflowitem), id: '1234' });

describe('MyDSpaceReloadableActionsComponent', () => {
  beforeEach(fakeAsync(() => {
    mockDataService = new PoolTaskDataService(null, null, null, null);
    mockClaimedTaskDataService = new ClaimedTaskDataService(null, null, null, null);
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock,
          },
        }),
      ],
      declarations: [PoolTaskActionsComponent],
      providers: [
        { provide: Injector, useValue: {} },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: Router, useValue: new RouterStub() },
        { provide: PoolTaskDataService, useValue: mockDataService },
        { provide: ClaimedTaskDataService, useValue: mockClaimedTaskDataService },
        { provide: SearchService, useValue: searchService },
        { provide: RequestService, useValue: requestService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(PoolTaskActionsComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PoolTaskActionsComponent);
    component = fixture.componentInstance;
    component.item = item;
    component.object = mockObject;
    component.workflowitem = workflowitem;
    notificationsServiceStub = TestBed.get(NotificationsService);
    router = TestBed.get(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture = null;
    component = null;
  });

  describe('on reload action init', () => {

    beforeEach(() => {
      spyOn(component, 'initReloadAnchor').and.returnValue(null);
      spyOn(component, 'initObjects');
    });

    it('should call initReloadAnchor and initObjects on init', fakeAsync(() => {
      component.ngOnInit();

      fixture.detectChanges();

      fixture.whenStable().then(() => {
        expect(component.initReloadAnchor).toHaveBeenCalled();
        expect(component.initObjects).toHaveBeenCalled();
      });

    }));

  });

  describe('on action execution fail', () => {

    let remoteClaimTaskErrorResponse;

    beforeEach(() => {

      mockDataService = new PoolTaskDataService(null, null, null, null);

      const poolTaskHref = 'poolTaskHref';
      remoteClaimTaskErrorResponse = new ProcessTaskResponse(false, null, null);
      const remoteReloadedObjectResponse: any = createSuccessfulRemoteDataObject(new PoolTask());

      spyOn(mockDataService, 'getPoolTaskEndpointById').and.returnValue(observableOf(poolTaskHref));
      spyOn(mockClaimedTaskDataService, 'findByItem').and.returnValue(observableOf(remoteReloadedObjectResponse));
      spyOn(mockClaimedTaskDataService, 'claimTask').and.returnValue(observableOf(remoteClaimTaskErrorResponse));
      spyOn(component, 'reloadObjectExecution').and.callThrough();
      spyOn(component.processCompleted, 'emit').and.callThrough();

      (component as any).objectDataService = mockDataService;
    });

    it('should show error notification', (done) => {

      component.startActionExecution().subscribe( (result) => {
        expect(notificationsServiceStub.error).toHaveBeenCalled();
        done();
      });
    });

    it('should not call reloadObject', (done) => {

      component.startActionExecution().subscribe( (result) => {
        expect(component.reloadObjectExecution).not.toHaveBeenCalled();
        done();
      });

    });

    it('should not emit processCompleted', (done) => {

      component.startActionExecution().subscribe( (result) => {
        expect(component.processCompleted.emit).not.toHaveBeenCalled();
        done();
      });

    });

  });

  describe('on action execution success', () => {

    beforeEach(() => {

      mockDataService = new PoolTaskDataService(null, null, null, null);

      const poolTaskHref = 'poolTaskHref';
      const remoteClaimTaskResponse: any = new ProcessTaskResponse(true, null, null);
      const remoteReloadedObjectResponse: any = createSuccessfulRemoteDataObject(new PoolTask());

      spyOn(mockDataService, 'getPoolTaskEndpointById').and.returnValue(observableOf(poolTaskHref));
      spyOn(mockClaimedTaskDataService, 'findByItem').and.returnValue(observableOf(remoteReloadedObjectResponse));
      spyOn(mockClaimedTaskDataService, 'claimTask').and.returnValue(observableOf(remoteClaimTaskResponse));
      spyOn(component, 'reloadObjectExecution').and.callThrough();
      spyOn(component, 'convertReloadedObject').and.callThrough();
      spyOn(component.processCompleted, 'emit').and.callThrough();

      (component as any).objectDataService = mockDataService;
    });

    it('should reloadObject in case of action execution success', (done) => {

      component.startActionExecution().subscribe( (result) => {
        expect(component.reloadObjectExecution).toHaveBeenCalled();
        done();
      });
    });

    it('should convert the reloaded object', (done) => {

      component.startActionExecution().subscribe( (result) => {
        expect(component.convertReloadedObject).toHaveBeenCalled();
        done();
      });
    });

    it('should emit the reloaded object in case of success', (done) => {

      component.startActionExecution().subscribe( (result) => {
        expect(component.processCompleted.emit).toHaveBeenCalledWith({result: true, reloadedObject: result as any});
        done();
      });
    });

  });

  describe('on action execution success but without a reloadedObject', () => {

    beforeEach(() => {

      mockDataService = new PoolTaskDataService(null, null, null, null);

      const poolTaskHref = 'poolTaskHref';
      const remoteClaimTaskResponse: any = new ProcessTaskResponse(true, null, null);
      const remoteReloadedObjectResponse: any = createFailedRemoteDataObject();

      spyOn(mockDataService, 'getPoolTaskEndpointById').and.returnValue(observableOf(poolTaskHref));
      spyOn(mockClaimedTaskDataService, 'findByItem').and.returnValue(observableOf(remoteReloadedObjectResponse));
      spyOn(mockClaimedTaskDataService, 'claimTask').and.returnValue(observableOf(remoteClaimTaskResponse));

      spyOn(component, 'convertReloadedObject').and.returnValue(null);
      spyOn(component, 'reload').and.returnValue(null);

      (component as any).objectDataService = mockDataService;
    });

    it('should call reload method', (done) => {

      component.startActionExecution().subscribe( (result) => {
        expect(component.reload).toHaveBeenCalled();
        done();
      });
    });

  });

});
