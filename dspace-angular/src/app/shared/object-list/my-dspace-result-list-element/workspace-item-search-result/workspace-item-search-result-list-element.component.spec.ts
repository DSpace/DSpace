import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { waitForAsync, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { of as observableOf } from 'rxjs';
import { take } from 'rxjs/operators';
import { LinkService } from '../../../../core/cache/builders/link.service';
import { ItemDataService } from '../../../../core/data/item-data.service';

import { Item } from '../../../../core/shared/item.model';
import { WorkspaceItem } from '../../../../core/submission/models/workspaceitem.model';
import { getMockLinkService } from '../../../mocks/link-service.mock';
import { MyDspaceItemStatusType } from '../../../object-collection/shared/mydspace-item-status/my-dspace-item-status-type';
import { WorkflowItemSearchResult } from '../../../object-collection/shared/workflow-item-search-result.model';
import { createSuccessfulRemoteDataObject } from '../../../remote-data.utils';
import { TruncatableService } from '../../../truncatable/truncatable.service';
import { WorkspaceItemSearchResultListElementComponent } from './workspace-item-search-result-list-element.component';
import { By } from '@angular/platform-browser';
import { DSONameService } from '../../../../core/breadcrumbs/dso-name.service';
import { DSONameServiceMock } from '../../../mocks/dso-name.service.mock';
import { APP_CONFIG } from '../../../../../config/app-config.interface';

let component: WorkspaceItemSearchResultListElementComponent;
let fixture: ComponentFixture<WorkspaceItemSearchResultListElementComponent>;

const mockResultObject: WorkflowItemSearchResult = new WorkflowItemSearchResult();
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

const environmentUseThumbs = {
  browseBy: {
    showThumbnails: true
  }
};

const rd = createSuccessfulRemoteDataObject(item);
mockResultObject.indexableObject = Object.assign(new WorkspaceItem(), { item: observableOf(rd) });
let linkService;

describe('WorkspaceItemSearchResultListElementComponent', () => {
  beforeEach(waitForAsync(() => {
    linkService = getMockLinkService();
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule],
      declarations: [WorkspaceItemSearchResultListElementComponent],
      providers: [
        { provide: TruncatableService, useValue: {} },
        { provide: ItemDataService, useValue: {} },
        { provide: LinkService, useValue: linkService },
        { provide: DSONameService, useClass: DSONameServiceMock },
        { provide: APP_CONFIG, useValue: environmentUseThumbs }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(WorkspaceItemSearchResultListElementComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(WorkspaceItemSearchResultListElementComponent);
    component = fixture.componentInstance;
  }));

  beforeEach(() => {
    component.object = mockResultObject;
    fixture.detectChanges();
  });

  it('should init derivedSearchResult$ properly', (done) => {
    component.derivedSearchResult$.pipe(take(1)).subscribe((i) => {
      expect(linkService.resolveLink).toHaveBeenCalled();
      expect(i.indexableObject).toBe(item);
      expect(i.hitHighlights).toBe(mockResultObject.hitHighlights);
      done();
    });
  });

  it('should have properly status', () => {
    expect(component.status).toEqual(MyDspaceItemStatusType.WORKSPACE);
  });

  it('should forward workspaceitem-actions processCompleted event to the reloadedObject event emitter', fakeAsync(() => {

    spyOn(component.reloadedObject, 'emit').and.callThrough();
    const actionPayload: any = { reloadedObject: {}};

    const actionsComponent = fixture.debugElement.query(By.css('ds-workspaceitem-actions'));
    actionsComponent.triggerEventHandler('processCompleted', actionPayload);
    tick();

    expect(component.reloadedObject.emit).toHaveBeenCalledWith(actionPayload.reloadedObject);

  }));


  it('should add an offset to the actions element', () => {
    const thumbnail = fixture.debugElement.query(By.css('.offset-3'));
    expect(thumbnail).toBeTruthy();
  });
});
