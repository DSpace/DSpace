import { ChangeDetectionStrategy, Injector, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { Router } from '@angular/router';
import { By } from '@angular/platform-browser';

import { of as observableOf } from 'rxjs';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { TranslateLoaderMock } from '../../mocks/translate-loader.mock';
import { NotificationsService } from '../../notifications/notifications.service';
import { NotificationsServiceStub } from '../../testing/notifications-service.stub';
import { RouterStub } from '../../testing/router.stub';
import { Item } from '../../../core/shared/item.model';
import { WorkspaceItem } from '../../../core/submission/models/workspaceitem.model';
import { WorkspaceitemActionsComponent } from './workspaceitem-actions.component';
import { WorkspaceitemDataService } from '../../../core/submission/workspaceitem-data.service';
import {
  createFailedRemoteDataObject$,
  createSuccessfulRemoteDataObject,
  createSuccessfulRemoteDataObject$
} from '../../remote-data.utils';
import { RequestService } from '../../../core/data/request.service';
import { getMockRequestService } from '../../mocks/request.service.mock';
import { getMockSearchService } from '../../mocks/search-service.mock';
import { SearchService } from '../../../core/shared/search/search.service';

let component: WorkspaceitemActionsComponent;
let fixture: ComponentFixture<WorkspaceitemActionsComponent>;

let mockObject: WorkspaceItem;
let notificationsServiceStub: NotificationsServiceStub;

const mockDataService = jasmine.createSpyObj('WorkspaceitemDataService', {
  delete: jasmine.createSpy('delete')
});

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
mockObject = Object.assign(new WorkspaceItem(), { item: observableOf(rd), id: '1234', uuid: '1234' });

describe('WorkspaceitemActionsComponent', () => {
  beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      imports: [
        NgbModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      declarations: [WorkspaceitemActionsComponent],
      providers: [
        Injector,
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: Router, useValue: new RouterStub() },
        { provide: WorkspaceitemDataService, useValue: mockDataService },
        { provide: SearchService, useValue: searchService },
        { provide: RequestService, useValue: requestServce },
        NgbModal
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(WorkspaceitemActionsComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WorkspaceitemActionsComponent);
    component = fixture.componentInstance;
    component.object = mockObject;
    notificationsServiceStub = TestBed.inject(NotificationsService as any);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture = null;
    component = null;
  });

  it('should init object properly', () => {
    component.object = null;
    component.initObjects(mockObject);

    expect(component.object).toEqual(mockObject);
  });

  it('should display edit button', () => {
    const btn = fixture.debugElement.query(By.css('.btn-primary'));

    expect(btn).toBeDefined();
  });

  it('should display delete button', () => {
    const btn = fixture.debugElement.query(By.css('.btn-danger'));

    expect(btn).toBeDefined();
  });

  it('should display view button', () => {
    const btn = fixture.debugElement.query(By.css('button [data-test="view-btn"]'));

    expect(btn).toBeDefined();
  });

  describe('on discard confirmation', () => {
    beforeEach((done) => {
      mockDataService.delete.and.returnValue(observableOf(true));
      spyOn(component, 'reload');
      const btn = fixture.debugElement.query(By.css('.btn-danger'));
      btn.nativeElement.click();
      fixture.detectChanges();

      const confirmBtn: any = ((document as any).querySelector('.modal-footer .btn-danger'));
      confirmBtn.click();

      fixture.detectChanges();

      fixture.whenStable().then(() => {
        done();
      });
    });

    it('should call confirmDiscard', () => {
      expect(mockDataService.delete).toHaveBeenCalledWith(mockObject.id);
    });
  });

  it('should display a success notification on delete success', waitForAsync(() => {
    spyOn((component as any).modalService, 'open').and.returnValue({ result: Promise.resolve('ok') });
    mockDataService.delete.and.returnValue(createSuccessfulRemoteDataObject$({}));
    spyOn(component, 'reload');

    component.confirmDiscard('ok');
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      expect(notificationsServiceStub.success).toHaveBeenCalled();
    });
  }));

  it('should display an error notification on delete failure', waitForAsync(() => {
    spyOn((component as any).modalService, 'open').and.returnValue({ result: Promise.resolve('ok') });
    mockDataService.delete.and.returnValue(createFailedRemoteDataObject$('Error', 500));
    spyOn(component, 'reload');

    component.confirmDiscard('ok');
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      expect(notificationsServiceStub.error).toHaveBeenCalled();
    });
  }));

  it('should clear the object cache by href', waitForAsync(() => {
    component.reload();
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      expect(searchService.getEndpoint).toHaveBeenCalled();
      expect(requestServce.removeByHrefSubstring).toHaveBeenCalledWith('discover/search/objects');
    });
  }));
});
