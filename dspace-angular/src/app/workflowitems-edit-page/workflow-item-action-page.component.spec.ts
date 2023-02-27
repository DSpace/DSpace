import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { WorkflowItemActionPageComponent } from './workflow-item-action-page.component';
import { NotificationsService } from '../shared/notifications/notifications.service';
import { RouteService } from '../core/services/route.service';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { WorkflowItemDataService } from '../core/submission/workflowitem-data.service';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkflowItem } from '../core/submission/models/workflowitem.model';
import { Observable, of as observableOf } from 'rxjs';
import { VarDirective } from '../shared/utils/var.directive';
import { By } from '@angular/platform-browser';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';
import { TranslateLoaderMock } from '../shared/mocks/translate-loader.mock';
import { ActivatedRouteStub } from '../shared/testing/active-router.stub';
import { RouterStub } from '../shared/testing/router.stub';
import { NotificationsServiceStub } from '../shared/testing/notifications-service.stub';
import { RequestService } from '../core/data/request.service';
import { RequestServiceStub } from '../shared/testing/request-service.stub';
import { Location } from '@angular/common';
import { LocationStub } from '../shared/testing/location.stub';

const type = 'testType';
describe('WorkflowItemActionPageComponent', () => {
  let component: WorkflowItemActionPageComponent;
  let fixture: ComponentFixture<WorkflowItemActionPageComponent>;
  let wfiService;
  let wfi;
  let itemRD$;
  let id;

  function init() {
    wfiService = jasmine.createSpyObj('workflowItemService', {
      sendBack: observableOf(true)
    });
    itemRD$ = createSuccessfulRemoteDataObject$(itemRD$);
    wfi = new WorkflowItem();
    wfi.item = itemRD$;
    id = 'de11b5e5-064a-4e98-a7ac-a1a6a65ddf80';
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useClass: TranslateLoaderMock
        }
      })],
      declarations: [TestComponent, VarDirective],
      providers: [
        { provide: ActivatedRoute, useValue: new ActivatedRouteStub({}, { wfi: createSuccessfulRemoteDataObject(wfi) }) },
        { provide: Router, useClass: RouterStub },
        { provide: RouteService, useValue: {} },
        { provide: Location, useValue: new LocationStub() },
        { provide: NotificationsService, useClass: NotificationsServiceStub },
        { provide: WorkflowItemDataService, useValue: wfiService },
        { provide: RequestService, useClass: RequestServiceStub },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should set the initial type correctly', () => {
    expect(component.type).toEqual(type);
  });

  describe('clicking the button with class btn-danger', () => {
    beforeEach(() => {
      spyOn(component, 'performAction');
    });

    it('should call performAction on clicking the btn-danger', () => {
      const button = fixture.debugElement.query(By.css('.btn-danger')).nativeElement;
      button.click();
      fixture.detectChanges();
      expect(component.performAction).toHaveBeenCalled();
    });
  });

  describe('clicking the button with class btn-default', () => {
    beforeEach(() => {
      spyOn(component, 'previousPage');
    });

    it('should call performAction on clicking the btn-default', () => {
      const button = fixture.debugElement.query(By.css('.btn-default')).nativeElement;
      button.click();
      fixture.detectChanges();
      expect(component.previousPage).toHaveBeenCalled();
    });
  });
});

@Component({
    selector: 'ds-workflow-item-test-action-page',
    templateUrl: 'workflow-item-action-page.component.html'
  }
)
class TestComponent extends WorkflowItemActionPageComponent {
  constructor(protected route: ActivatedRoute,
              protected workflowItemService: WorkflowItemDataService,
              protected router: Router,
              protected routeService: RouteService,
              protected notificationsService: NotificationsService,
              protected translationService: TranslateService,
              protected requestService: RequestService,
              protected location: Location,
  ) {
    super(route, workflowItemService, router, routeService, notificationsService, translationService, requestService, location);
  }

  getType(): string {
    return type;
  }

  sendRequest(id: string): Observable<boolean> {
    return observableOf(true);
  }
}
