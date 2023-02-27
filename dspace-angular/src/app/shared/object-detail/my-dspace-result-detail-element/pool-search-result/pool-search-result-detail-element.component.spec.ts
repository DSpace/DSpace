import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, flush, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { of as observableOf } from 'rxjs';

import { Item } from '../../../../core/shared/item.model';
import { PoolTask } from '../../../../core/tasks/models/pool-task-object.model';
import {
  MyDspaceItemStatusType
} from '../../../object-collection/shared/mydspace-item-status/my-dspace-item-status-type';
import { WorkflowItem } from '../../../../core/submission/models/workflowitem.model';
import { createSuccessfulRemoteDataObject } from '../../../remote-data.utils';
import { PoolSearchResultDetailElementComponent } from './pool-search-result-detail-element.component';
import { PoolTaskSearchResult } from '../../../object-collection/shared/pool-task-search-result.model';
import { VarDirective } from '../../../utils/var.directive';
import { LinkService } from '../../../../core/cache/builders/link.service';
import { getMockLinkService } from '../../../mocks/link-service.mock';
import { By } from '@angular/platform-browser';
import { ObjectCacheService } from '../../../../core/cache/object-cache.service';

let component: PoolSearchResultDetailElementComponent;
let fixture: ComponentFixture<PoolSearchResultDetailElementComponent>;

const compIndex = 1;

const mockResultObject: PoolTaskSearchResult = new PoolTaskSearchResult();
mockResultObject.hitHighlights = {};

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
mockResultObject.indexableObject = Object.assign(new PoolTask(), { workflowitem: observableOf(rdWorkflowitem) });
const linkService = getMockLinkService();
const objectCacheServiceMock = jasmine.createSpyObj('ObjectCacheService', {
  remove: jasmine.createSpy('remove')
});

describe('PoolSearchResultDetailElementComponent', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule],
      declarations: [PoolSearchResultDetailElementComponent, VarDirective],
      providers: [
        { provide: 'objectElementProvider', useValue: (mockResultObject) },
        { provide: 'indexElementProvider', useValue: (compIndex) },
        { provide: LinkService, useValue: linkService },
        { provide: ObjectCacheService, useValue: objectCacheServiceMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(PoolSearchResultDetailElementComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(PoolSearchResultDetailElementComponent);
    component = fixture.componentInstance;
  }));

  beforeEach(() => {
    component.dso = mockResultObject.indexableObject;
    fixture.detectChanges();
  });

  it('should init workflowitem properly', fakeAsync(() => {
    flush();
    expect(linkService.resolveLinks).toHaveBeenCalledWith(
      component.dso,
      jasmine.objectContaining({ name: 'workflowitem' }),
      jasmine.objectContaining({ name: 'action' })
    );
    expect(component.workflowitem$.value).toEqual(workflowitem);
    expect(component.item$.value).toEqual(item);
  }));

  it('should have properly status', () => {
    expect(component.status).toEqual(MyDspaceItemStatusType.WAITING_CONTROLLER);
  });

  it('should forward pool-task-actions processCompleted event to the reloadedObject event emitter', fakeAsync(() => {
    spyOn(component.reloadedObject, 'emit').and.callThrough();
    const actionPayload: any = { reloadedObject: {}};
    const actionsComponents = fixture.debugElement.query(By.css('ds-pool-task-actions'));
    actionsComponents.triggerEventHandler('processCompleted', actionPayload);
    tick();

    expect(component.reloadedObject.emit).toHaveBeenCalledWith(actionPayload.reloadedObject);

  }));
});
