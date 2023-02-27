import { ChangeDetectionStrategy, Injector, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { ClaimedTaskActionsRejectComponent } from './claimed-task-actions-reject.component';
import { TranslateLoaderMock } from '../../../mocks/translate-loader.mock';
import { ClaimedTask } from '../../../../core/tasks/models/claimed-task-object.model';
import { ProcessTaskResponse } from '../../../../core/tasks/models/process-task-response';
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
import { of } from 'rxjs';
import { ClaimedDeclinedTaskSearchResult } from '../../../object-collection/shared/claimed-declined-task-search-result.model';

let component: ClaimedTaskActionsRejectComponent;
let fixture: ComponentFixture<ClaimedTaskActionsRejectComponent>;
let formBuilder: FormBuilder;
let modalService: NgbModal;

const searchService = getMockSearchService();

const requestService = getMockRequestService();

const object = Object.assign(new ClaimedTask(), { id: 'claimed-task-1' });

const claimedTaskService = jasmine.createSpyObj('claimedTaskService', {
  submitTask: of(new ProcessTaskResponse(true))
});

let mockPoolTaskDataService: PoolTaskDataService;

describe('ClaimedTaskActionsRejectComponent', () => {
  beforeEach(waitForAsync(() => {
    mockPoolTaskDataService = new PoolTaskDataService(null, null, null, null);
    TestBed.configureTestingModule({
      imports: [
        NgbModule,
        ReactiveFormsModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock,
          },
        }),
      ],
      declarations: [ClaimedTaskActionsRejectComponent],
      providers: [
        { provide: ClaimedTaskDataService, useValue: claimedTaskService },
        Injector,
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: Router, useValue: new RouterStub() },
        { provide: SearchService, useValue: searchService },
        { provide: RequestService, useValue: requestService },
        { provide: PoolTaskDataService, useValue: mockPoolTaskDataService },
        FormBuilder,
        NgbModal,
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ClaimedTaskActionsRejectComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
    fixture = TestBed.createComponent(ClaimedTaskActionsRejectComponent);
    component = fixture.componentInstance;
    formBuilder = TestBed.inject(FormBuilder);
    modalService = TestBed.inject(NgbModal);
    component.object = object;
    component.modalRef = modalService.open('ok');
    spyOn(component, 'initReloadAnchor').and.returnValue(undefined);
    fixture.detectChanges();
  }));

  it('should init reject form properly', () => {
    expect(component.rejectForm).toBeDefined();
    expect(component.rejectForm instanceof FormGroup).toBeTruthy();
    expect(component.rejectForm.controls.reason).toBeDefined();
  });

  it('should display reject button', () => {
    const btn = fixture.debugElement.query(By.css('.btn-danger'));

    expect(btn).toBeDefined();
  });

  it('should display spin icon when reject is pending', () => {
    component.processing$.next(true);
    fixture.detectChanges();

    const span = fixture.debugElement.query(By.css('.btn-danger .fa-spin'));

    expect(span).toBeDefined();
  });

  it('should call openRejectModal on reject button click', () => {
    spyOn(component.rejectForm, 'reset');
    const btn = fixture.debugElement.query(By.css('.btn-danger'));
    btn.nativeElement.click();
    fixture.detectChanges();

    expect(component.rejectForm.reset).toHaveBeenCalled();
    expect(component.modalRef).toBeDefined();

    component.modalRef.close();
  });

  describe('on form submit', () => {
    let expectedBody;

    beforeEach(() => {
      spyOn(component.processCompleted, 'emit');
      spyOn(component, 'startActionExecution').and.returnValue(of(null));

      expectedBody = {
        [component.option]: 'true',
        reason: null
      };

      const btn = fixture.debugElement.query(By.css('.btn-danger'));
      btn.nativeElement.click();
      fixture.detectChanges();

      expect(component.modalRef).toBeDefined();

      const form = ((document as any).querySelector('form'));
      form.dispatchEvent(new Event('ngSubmit'));
      fixture.detectChanges();
    });

    it('should start the action execution', () => {
      expect(component.startActionExecution).toHaveBeenCalled();
    });

  });

  describe('actionExecution', () => {

    let expectedBody;

    beforeEach(() => {
      spyOn((component.rejectForm as any), 'get').and.returnValue({value: 'required'});
      expectedBody = {
        [component.option]: 'true',
        reason: 'required'
      };
    });

    it('should call claimedTaskService\'s submitTask with the proper reason', (done) => {
      component.actionExecution().subscribe(() => {
        expect(claimedTaskService.submitTask).toHaveBeenCalledWith(object.id, expectedBody);
        done();
      });
    });
  });

  describe('reloadObjectExecution', () => {

    it('should return the component object itself', (done) => {
      component.reloadObjectExecution().subscribe((val) => {
        expect(val).toEqual(component.object);
        done();
      });
    });
  });

  describe('convertReloadedObject', () => {

    it('should return a ClaimedDeclinedTaskSearchResult instance', () => {
      const reloadedObject = component.convertReloadedObject(component.object);
      expect(reloadedObject instanceof ClaimedDeclinedTaskSearchResult).toEqual(true);
    });
  });

});
