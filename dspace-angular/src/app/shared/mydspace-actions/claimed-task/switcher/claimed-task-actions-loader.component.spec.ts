import { ClaimedTaskActionsLoaderComponent } from './claimed-task-actions-loader.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectionStrategy, Injector, NO_ERRORS_SCHEMA } from '@angular/core';
import { ClaimedTaskActionsDirective } from './claimed-task-actions.directive';
import { ClaimedTask } from '../../../../core/tasks/models/claimed-task-object.model';
import { TranslateModule } from '@ngx-translate/core';
import { ClaimedTaskActionsEditMetadataComponent } from '../edit-metadata/claimed-task-actions-edit-metadata.component';
import { ClaimedTaskDataService } from '../../../../core/tasks/claimed-task-data.service';
import { NotificationsService } from '../../../notifications/notifications.service';
import { NotificationsServiceStub } from '../../../testing/notifications-service.stub';
import { Router } from '@angular/router';
import { RouterStub } from '../../../testing/router.stub';
import { SearchService } from '../../../../core/shared/search/search.service';
import { RequestService } from '../../../../core/data/request.service';
import { PoolTaskDataService } from '../../../../core/tasks/pool-task-data.service';
import { getMockSearchService } from '../../../mocks/search-service.mock';
import { getMockRequestService } from '../../../mocks/request.service.mock';
import { Item } from '../../../../core/shared/item.model';
import { WorkflowItem } from '../../../../core/submission/models/workflowitem.model';

const searchService = getMockSearchService();

const requestService = getMockRequestService();

describe('ClaimedTaskActionsLoaderComponent', () => {
  let comp: ClaimedTaskActionsLoaderComponent;
  let fixture: ComponentFixture<ClaimedTaskActionsLoaderComponent>;

  const option = 'test_option';
  const object = Object.assign(new ClaimedTask(), { id: 'claimed-task-1' });

  const item = Object.assign(new Item(), {
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

  const workflowitem = Object.assign(new WorkflowItem(), { id: '333' });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [ClaimedTaskActionsLoaderComponent, ClaimedTaskActionsEditMetadataComponent, ClaimedTaskActionsDirective],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: ClaimedTaskDataService, useValue: {} },
        { provide: Injector, useValue: {} },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: Router, useValue: new RouterStub() },
        { provide: SearchService, useValue: searchService },
        { provide: RequestService, useValue: requestService },
        { provide: PoolTaskDataService, useValue: {} }
      ]
    }).overrideComponent(ClaimedTaskActionsLoaderComponent, {
      set: {
        changeDetection: ChangeDetectionStrategy.Default,
        entryComponents: [ClaimedTaskActionsEditMetadataComponent]
      }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(ClaimedTaskActionsLoaderComponent);
    comp = fixture.componentInstance;
    comp.item = item;
    comp.object = object;
    comp.option = option;
    comp.workflowitem = workflowitem;
    spyOn(comp, 'getComponentByWorkflowTaskOption').and.returnValue(ClaimedTaskActionsEditMetadataComponent);

    fixture.detectChanges();
  }));

  describe('When the component is rendered', () => {
    it('should call the getComponentByWorkflowTaskOption function with the right option', () => {
      expect(comp.getComponentByWorkflowTaskOption).toHaveBeenCalledWith(option);
    });
  });
});
