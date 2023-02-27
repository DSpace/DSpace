import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectorRef, Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { BrowserModule, By } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { getTestScheduler } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { delay } from 'rxjs/operators';
import { TranslateModule } from '@ngx-translate/core';

import { createSuccessfulRemoteDataObject } from '../../remote-data.utils';
import { createTestComponent } from '../../testing/utils.test';
import { EPersonDataService } from '../../../core/eperson/eperson-data.service';
import { GroupDataService } from '../../../core/eperson/group-data.service';
import { RequestService } from '../../../core/data/request.service';
import { getMockRequestService } from '../../mocks/request.service.mock';
import { PolicyType } from '../../../core/resource-policy/models/policy-type.model';
import { ActionType } from '../../../core/resource-policy/models/action-type.model';
import { GroupMock } from '../../testing/group-mock';
import { ResourcePolicyEvent, ResourcePolicyFormComponent } from './resource-policy-form.component';
import { FormService } from '../../form/form.service';
import { getMockFormService } from '../../mocks/form-service.mock';
import { FormBuilderService } from '../../form/builder/form-builder.service';
import { EpersonGroupListComponent } from '../../eperson-group-list/eperson-group-list.component';
import { FormComponent } from '../../form/form.component';
import { dateToISOFormat, stringToNgbDateStruct } from '../../date.util';
import { ResourcePolicy } from '../../../core/resource-policy/models/resource-policy.model';
import { RESOURCE_POLICY } from '../../../core/resource-policy/models/resource-policy.resource-type';
import { EPersonMock } from '../../testing/eperson.mock';
import { isNotEmptyOperator } from '../../empty.util';
import { ActivatedRoute, Router } from '@angular/router';
import { RemoteData } from '../../../core/data/remote-data';
import { RouterMock } from '../../mocks/router.mock';
import { Store } from '@ngrx/store';
import { PaginationServiceStub } from '../../testing/pagination-service.stub';
import { PaginationService } from '../../../core/pagination/pagination.service';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { StoreMock } from '../../testing/store.mock';

export const mockResourcePolicyFormData = {
  name: [
    {
      value: 'name',
      language: null,
      authority: null,
      display: 'name',
      confidence: -1,
      place: 0,
      otherInformation: null
    }
  ],
  description: [
    {
      value: 'description',
      language: null,
      authority: null,
      display: 'description',
      confidence: -1,
      place: 0,
      otherInformation: null
    }
  ],
  policyType: [
    {
      value: 'TYPE_WORKFLOW',
      language: null,
      authority: null,
      display: 'TYPE_WORKFLOW',
      confidence: -1,
      place: 0,
      otherInformation: null
    }
  ],
  action: [
    {
      value: 'WRITE',
      language: null,
      authority: null,
      display: 'WRITE',
      confidence: -1,
      place: 0,
      otherInformation: null
    }
  ],
  date: {
    start: [
      {
        value: { year: '2019', month: '04', day: '14' },
        language: null,
        authority: null,
        display: '2019-04-14',
        confidence: -1,
        place: 0,
        otherInformation: null
      }
    ],
    end: [
      {
        value: { year: '2020', month: '04', day: '14' },
        language: null,
        authority: null,
        display: '2020-04-14',
        confidence: -1,
        place: 0,
        otherInformation: null
      }
    ],
  }
};

export const submittedResourcePolicy = Object.assign(new ResourcePolicy(), {
  name: 'name',
  description: 'description',
  policyType: PolicyType.TYPE_WORKFLOW,
  action: ActionType.WRITE,
  startDate: dateToISOFormat('2019-04-14T00:00:00Z'),
  endDate: dateToISOFormat('2020-04-14T00:00:00Z'),
  type: RESOURCE_POLICY
});

describe('ResourcePolicyFormComponent test suite', () => {
  let comp: ResourcePolicyFormComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<ResourcePolicyFormComponent>;
  let de;
  let scheduler: TestScheduler;

  const formService: any = getMockFormService();

  const resourcePolicy: any = {
    id: '1',
    name: null,
    description: null,
    policyType: PolicyType.TYPE_SUBMISSION,
    action: ActionType.READ,
    startDate: '2019-04-14',
    endDate: '2020-04-14',
    type: 'resourcepolicy',
    uuid: 'resource-policy-1',
    _links: {
      eperson: {
        href: 'https://rest.api/rest/api/eperson'
      },
      group: {
        href: 'https://rest.api/rest/api/group'
      },
      self: {
        href: 'https://rest.api/rest/api/resourcepolicies/1'
      },
    },
    eperson: observableOf(createSuccessfulRemoteDataObject({})),
    group: observableOf(createSuccessfulRemoteDataObject(GroupMock))
  };

  const epersonService = jasmine.createSpyObj('epersonService', {
    findByHref: jasmine.createSpy('findByHref'),
    findAll: jasmine.createSpy('findAll')
  });

  const groupService = jasmine.createSpyObj('groupService', {
    findByHref: jasmine.createSpy('findByHref'),
    findAll: jasmine.createSpy('findAll')
  });

  const mockPolicyRD: RemoteData<ResourcePolicy> = createSuccessfulRemoteDataObject(resourcePolicy);
  const activatedRouteStub = {
    parent: {
      data: observableOf({
        dso: mockPolicyRD
      })
    }
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserModule,
        CommonModule,
        FormsModule,
        NgbModule,
        NoopAnimationsModule,
        ReactiveFormsModule,
        TranslateModule.forRoot()
      ],
      declarations: [
        FormComponent,
        EpersonGroupListComponent,
        ResourcePolicyFormComponent,
        TestComponent
      ],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: Router, useValue: new RouterMock() },
        { provide: Store, useValue: StoreMock },
        { provide: EPersonDataService, useValue: epersonService },
        { provide: FormService, useValue: formService },
        { provide: GroupDataService, useValue: groupService },
        { provide: PaginationService, useValue: new PaginationServiceStub() },
        { provide: RequestService, useValue: getMockRequestService() },
        FormBuilderService,
        ChangeDetectorRef,
        ResourcePolicyFormComponent
      ],
      schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();
  }));

  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      formService.isValid.and.returnValue(observableOf(true));
      const html = `
        <ds-resource-policy-form [resourcePolicy]="resourcePolicy" [isProcessing]="isProcessing"></ds-resource-policy-form>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create ResourcePolicyFormComponent', inject([ResourcePolicyFormComponent], (app: ResourcePolicyFormComponent) => {

      expect(app).toBeDefined();

    }));
  });

  describe('when resource policy is not provided', () => {

    beforeEach(() => {
      // initTestScheduler();
      fixture = TestBed.createComponent(ResourcePolicyFormComponent);
      comp = fixture.componentInstance;
      compAsAny = fixture.componentInstance;
      compAsAny.resourcePolicy = resourcePolicy;
      comp.isProcessing = observableOf(false);
    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      de = null;
      fixture.destroy();
    });

    it('should init form model properly', () => {
      epersonService.findByHref.and.returnValue(observableOf(undefined));
      groupService.findByHref.and.returnValue(observableOf(undefined));
      spyOn(compAsAny, 'isFormValid').and.returnValue(observableOf(false));
      spyOn(compAsAny, 'initModelsValue').and.callThrough();
      spyOn(compAsAny, 'buildResourcePolicyForm').and.callThrough();
      fixture.detectChanges();

      expect(compAsAny.buildResourcePolicyForm).toHaveBeenCalled();
      expect(compAsAny.initModelsValue).toHaveBeenCalled();
      expect(compAsAny.formModel.length).toBe(5);
      expect(compAsAny.subs.length).toBe(1);

    });

    it('should can set grant', () => {
      expect(comp.isBeingEdited()).toBeTruthy();
    });

    it('should not have a target name', () => {
      expect(comp.getResourcePolicyTargetName()).toBe('');
    });

    it('should emit reset event', () => {
      spyOn(compAsAny.reset, 'emit');
      comp.onReset();
      expect(compAsAny.reset.emit).toHaveBeenCalled();
    });

    it('should update resource policy grant object properly', () => {
      comp.updateObjectSelected(EPersonMock, true);

      expect(comp.resourcePolicyGrant).toEqual(EPersonMock);
      expect(comp.resourcePolicyGrantType).toBe('eperson');

      comp.updateObjectSelected(GroupMock, false);

      expect(comp.resourcePolicyGrant).toEqual(GroupMock);
      expect(comp.resourcePolicyGrantType).toBe('group');
    });

  });

  describe('when resource policy is provided', () => {

    beforeEach(() => {
      // initTestScheduler();
      fixture = TestBed.createComponent(ResourcePolicyFormComponent);
      comp = fixture.componentInstance;
      compAsAny = fixture.componentInstance;
      comp.resourcePolicy = resourcePolicy;
      compAsAny.resourcePolicy = resourcePolicy;
      comp.isProcessing = observableOf(false);
      compAsAny.ePersonService.findByHref.and.returnValue(
        observableOf(createSuccessfulRemoteDataObject({})).pipe(delay(100))
      );
      compAsAny.groupService.findByHref.and.returnValue(observableOf(createSuccessfulRemoteDataObject(GroupMock)));
    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      de = null;
      fixture.destroy();
    });

    it('should init form model properly', () => {
      spyOn(compAsAny, 'isFormValid').and.returnValue(observableOf(false));
      spyOn(compAsAny, 'initModelsValue').and.callThrough();
      spyOn(compAsAny, 'buildResourcePolicyForm').and.callThrough();
      fixture.detectChanges();

      expect(compAsAny.buildResourcePolicyForm).toHaveBeenCalled();
      expect(compAsAny.initModelsValue).toHaveBeenCalled();
      expect(compAsAny.formModel.length).toBe(5);
      expect(compAsAny.subs.length).toBe(1);
      expect(compAsAny.formModel[2].value).toBe('TYPE_SUBMISSION');
      expect(compAsAny.formModel[3].value).toBe('READ');
      expect(compAsAny.formModel[4].get(0).value).toEqual(stringToNgbDateStruct('2019-04-14'));
      expect(compAsAny.formModel[4].get(1).value).toEqual(stringToNgbDateStruct('2020-04-14'));

    });

    it('should init resourcePolicyGrant properly', (done) => {
      compAsAny.isActive = true;
      comp.ngOnInit();
      comp.resourcePolicyTargetName$.pipe(
        isNotEmptyOperator()
      ).subscribe(() => {
        expect(compAsAny.resourcePolicyGrant).toEqual(GroupMock);
        done();
      });
    });

    it('should be being edited', () => {
      expect(comp.isBeingEdited()).toBeTrue();
    });

    it('should have a target name', () => {
      compAsAny.resourcePolicyGrant = GroupMock;

      expect(comp.getResourcePolicyTargetName()).toBe('testgroupname');
    });

  });

  describe('when form is valid', () => {
    beforeEach(() => {

      fixture = TestBed.createComponent(ResourcePolicyFormComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      comp.resourcePolicy = resourcePolicy;
      comp.isProcessing = observableOf(false);
      compAsAny.ePersonService.findByHref.and.returnValue(
        observableOf(createSuccessfulRemoteDataObject({})).pipe(delay(100))
      );
      compAsAny.groupService.findByHref.and.returnValue(observableOf(createSuccessfulRemoteDataObject(GroupMock)));
      compAsAny.formService.isValid.and.returnValue(observableOf(true));
      compAsAny.isActive = true;
      comp.resourcePolicyGrant = GroupMock;
      comp.resourcePolicyGrantType = 'group';
      fixture.detectChanges();
    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      de = null;
      fixture.destroy();
    });

    it('should not have submit button disabled when submission is valid', () => {

      const depositBtn: any = fixture.debugElement.query(By.css('.btn-primary'));

      expect(depositBtn.nativeElement.disabled).toBeFalsy();
    });

    it('should emit submit event', () => {
      spyOn(compAsAny.submit, 'emit');
      spyOn(compAsAny, 'createResourcePolicyByFormData').and.callThrough();
      compAsAny.formService.getFormData.and.returnValue(observableOf(mockResourcePolicyFormData));
      const eventPayload: ResourcePolicyEvent = Object.create({});
      eventPayload.object = submittedResourcePolicy;
      eventPayload.target = {
        type: 'group',
        uuid: GroupMock.id
      };
      eventPayload.updateTarget = false;

      scheduler = getTestScheduler();
      scheduler.schedule(() => comp.onSubmit());

      scheduler.flush();

      expect(compAsAny.submit.emit).toHaveBeenCalledWith(eventPayload);
      expect(compAsAny.createResourcePolicyByFormData).toHaveBeenCalled();
    });

  });

  describe('when form is not valid', () => {
    beforeEach(() => {

      fixture = TestBed.createComponent(ResourcePolicyFormComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      comp.resourcePolicy = resourcePolicy;
      comp.isProcessing = observableOf(false);
      compAsAny.ePersonService.findByHref.and.returnValue(
        observableOf(createSuccessfulRemoteDataObject({})).pipe(delay(100))
      );
      compAsAny.groupService.findByHref.and.returnValue(observableOf(createSuccessfulRemoteDataObject(GroupMock)));
      compAsAny.formService.isValid.and.returnValue(observableOf(false));
      compAsAny.isActive = true;
      fixture.detectChanges();
    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      de = null;
      fixture.destroy();
    });

    it('should have submit button disabled when submission is valid', () => {

      const depositBtn: any = fixture.debugElement.query(By.css('.btn-primary'));

      expect(depositBtn.nativeElement.disabled).toBeTruthy();
    });

  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

  resourcePolicy = null;
  isProcessing = observableOf(false);
}
