import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { of as observableOf } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { WorkflowItem } from '../../../../core/submission/models/workflowitem.model';
import { PoolTask } from '../../../../core/tasks/models/pool-task-object.model';
import { EPersonMock } from '../../../testing/eperson.mock';
import { MyDSpaceItemStatusComponent } from './my-dspace-item-status.component';
import { MyDspaceItemStatusType } from './my-dspace-item-status-type';
import { TranslateLoaderMock } from '../../../mocks/translate-loader.mock';
import { By } from '@angular/platform-browser';
import { createSuccessfulRemoteDataObject } from '../../../remote-data.utils';

let component: MyDSpaceItemStatusComponent;
let fixture: ComponentFixture<MyDSpaceItemStatusComponent>;

let mockResultObject: PoolTask;

const rdSumbitter = createSuccessfulRemoteDataObject(EPersonMock);
const workflowitem = Object.assign(new WorkflowItem(), { submitter: observableOf(rdSumbitter) });
const rdWorkflowitem = createSuccessfulRemoteDataObject(workflowitem);
mockResultObject = Object.assign(new PoolTask(), { workflowitem: observableOf(rdWorkflowitem) });

describe('MyDSpaceItemStatusComponent', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      declarations: [MyDSpaceItemStatusComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(MyDSpaceItemStatusComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MyDSpaceItemStatusComponent);
    component = fixture.componentInstance;
  });

  it('should display badge', () => {
    const badge = fixture.debugElement.query(By.css('span'));
    expect(badge).toBeDefined();
  });

  it('should init badge content and class', () => {
    component.status = MyDspaceItemStatusType.VALIDATION;
    fixture.detectChanges();
    expect(component.badgeContent).toBe(MyDspaceItemStatusType.VALIDATION);
    expect(component.badgeClass).toBe('text-light badge badge-validation');
  });

  it('should init badge content and class', () => {
    component.status = MyDspaceItemStatusType.WAITING_CONTROLLER;
    fixture.detectChanges();
    expect(component.badgeContent).toBe(MyDspaceItemStatusType.WAITING_CONTROLLER);
    expect(component.badgeClass).toBe('text-light badge badge-waiting-controller');
  });

  it('should init badge content and class', () => {
    component.status = MyDspaceItemStatusType.WORKSPACE;
    fixture.detectChanges();
    expect(component.badgeContent).toBe(MyDspaceItemStatusType.WORKSPACE);
    expect(component.badgeClass).toBe('text-light badge badge-workspace');
  });

  it('should init badge content and class', () => {
    component.status = MyDspaceItemStatusType.ARCHIVED;
    fixture.detectChanges();
    expect(component.badgeContent).toBe(MyDspaceItemStatusType.ARCHIVED);
    expect(component.badgeClass).toBe('text-light badge badge-archived');
  });

  it('should init badge content and class', () => {
    component.status = MyDspaceItemStatusType.WORKFLOW;
    fixture.detectChanges();
    expect(component.badgeContent).toBe(MyDspaceItemStatusType.WORKFLOW);
    expect(component.badgeClass).toBe('text-light badge badge-workflow');
  });
});
