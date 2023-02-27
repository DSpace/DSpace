import { ChangeDetectorRef, Component, NO_ERRORS_SCHEMA, SimpleChange } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';

import { of as observableOf } from 'rxjs';
import { cold, getTestScheduler } from 'jasmine-marbles';

import { SubmissionServiceStub } from '../../shared/testing/submission-service.stub';
import {
  mockSectionsData,
  mockSectionsList,
  mockSubmissionCollectionId,
  mockSubmissionDefinition,
  mockSubmissionId,
  mockSubmissionObjectNew,
  mockSubmissionSelfUrl,
  mockSubmissionState
} from '../../shared/mocks/submission.mock';
import { SubmissionService } from '../submission.service';
import { SubmissionFormComponent } from './submission-form.component';
import { HALEndpointService } from '../../core/shared/hal-endpoint.service';
import { AuthServiceStub } from '../../shared/testing/auth-service.stub';
import { AuthService } from '../../core/auth/auth.service';
import { HALEndpointServiceStub } from '../../shared/testing/hal-endpoint-service.stub';
import { createTestComponent } from '../../shared/testing/utils.test';
import { Item } from '../../core/shared/item.model';
import { TestScheduler } from 'rxjs/testing';
import { SectionsService } from '../sections/sections.service';

describe('SubmissionFormComponent Component', () => {

  let comp: SubmissionFormComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<SubmissionFormComponent>;
  let authServiceStub: AuthServiceStub;
  let scheduler: TestScheduler;

  const submissionServiceStub: SubmissionServiceStub = new SubmissionServiceStub();
  const submissionId = mockSubmissionId;
  const collectionId = mockSubmissionCollectionId;
  const submissionObjectNew: any = mockSubmissionObjectNew;
  const submissionDefinition: any = mockSubmissionDefinition;
  const submissionState: any = Object.assign({}, mockSubmissionState);
  const selfUrl: any = mockSubmissionSelfUrl;
  const sectionsList: any = mockSectionsList;
  const sectionsData: any = mockSectionsData;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [],
      declarations: [
        SubmissionFormComponent,
        TestComponent
      ],
      providers: [
        { provide: AuthService, useClass: AuthServiceStub },
        { provide: HALEndpointService, useValue: new HALEndpointServiceStub('workspaceitems') },
        { provide: SubmissionService, useValue: submissionServiceStub },
        { provide: SectionsService, useValue: { isSectionTypeAvailable: () => observableOf(true) } },
        ChangeDetectorRef,
        SubmissionFormComponent
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      submissionServiceStub.getSubmissionObject.and.returnValue(observableOf(submissionState));
      const html = `
        <ds-submission-form [collectionId]="collectionId"
                                   [selfUrl]="selfUrl"
                                   [submissionDefinition]="submissionDefinition"
                                   [submissionId]="submissionId" [item]="item"></ds-submission-form>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create SubmissionFormComponent', inject([SubmissionFormComponent], (app: SubmissionFormComponent) => {
      expect(app).toBeDefined();
    }));
  });

  describe('', () => {
    beforeEach(() => {
      scheduler = getTestScheduler();
      fixture = TestBed.createComponent(SubmissionFormComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      authServiceStub = TestBed.inject(AuthService as any);
      submissionServiceStub.startAutoSave.calls.reset();
      submissionServiceStub.resetSubmissionObject.calls.reset();
      submissionServiceStub.dispatchInit.calls.reset();
    });

    afterEach(() => {
      fixture.destroy();
      comp = null;
      compAsAny = null;
    });

    it('should not has effect when collectionId and submissionId are undefined', (done) => {

      scheduler.schedule(() => fixture.detectChanges());
      scheduler.flush();

      expect(compAsAny.isActive).toBeTruthy();
      expect(compAsAny.submissionSections).toBeUndefined();
      expect(compAsAny.subs).toEqual([]);
      expect(submissionServiceStub.startAutoSave).not.toHaveBeenCalled();
      expect(comp.loading).toBeObservable(cold('(a|)', { a: true }));
      done();
    });

    it('should init properly when collectionId and submissionId are defined', (done) => {
      comp.collectionId = collectionId;
      comp.submissionId = submissionId;
      comp.submissionDefinition = submissionDefinition;
      comp.selfUrl = selfUrl;
      comp.sections = sectionsData;
      comp.submissionErrors = null;
      comp.item = new Item();

      submissionServiceStub.getSubmissionObject.and.returnValue(observableOf(submissionState));
      submissionServiceStub.getSubmissionSections.and.returnValue(observableOf(sectionsList));
      spyOn(authServiceStub, 'buildAuthHeader').and.returnValue('token');

      scheduler.schedule(() => {
        comp.ngOnChanges({
          collectionId: new SimpleChange(null, collectionId, true),
          submissionId: new SimpleChange(null, submissionId, true)
        });
        fixture.detectChanges();
      });
      scheduler.flush();

      expect(comp.submissionSections).toBeObservable(cold('(a|)', { a: sectionsList }));

      expect(submissionServiceStub.dispatchInit).toHaveBeenCalledWith(
        collectionId,
        submissionId,
        selfUrl,
        submissionDefinition,
        sectionsData,
        comp.item,
        null);
      expect(submissionServiceStub.startAutoSave).toHaveBeenCalled();
      done();
    });

    it('should update properly on collection change', (done) => {
      comp.collectionId = collectionId;
      comp.submissionId = submissionId;
      comp.submissionDefinition = submissionDefinition;
      comp.selfUrl = selfUrl;
      comp.sections = sectionsData;
      comp.item = new Item();

      scheduler.schedule(() => {
        comp.onCollectionChange(submissionObjectNew);
        fixture.detectChanges();
      });
      scheduler.flush();

      expect(comp.collectionId).toEqual(submissionObjectNew.collection.id);
      expect(comp.submissionDefinition).toEqual(submissionObjectNew.submissionDefinition);
      expect(comp.definitionId).toEqual(submissionObjectNew.submissionDefinition.name);
      expect(comp.sections).toEqual(submissionObjectNew.sections);

      expect(submissionServiceStub.resetSubmissionObject).toHaveBeenCalledWith(
        submissionObjectNew.collection.id,
        submissionId,
        selfUrl,
        submissionObjectNew.submissionDefinition,
        submissionObjectNew.sections,
        comp.item,
      );
      done();
    });

    it('should update only collection id on collection change when submission definition is not changed', (done) => {
      comp.collectionId = collectionId;
      comp.submissionId = submissionId;
      comp.definitionId = 'traditional';
      comp.submissionDefinition = submissionDefinition;
      comp.selfUrl = selfUrl;
      comp.sections = sectionsData;
      comp.item = new Item();

      scheduler.schedule(() => {
        comp.onCollectionChange({
          collection: {
            id: '45f2f3f1-ba1f-4f36-908a-3f1ea9a557eb'
          },
          submissionDefinition: {
            name: 'traditional'
          }
        } as any);
        fixture.detectChanges();
      });
      scheduler.flush();

      expect(comp.collectionId).toEqual('45f2f3f1-ba1f-4f36-908a-3f1ea9a557eb');
      expect(submissionServiceStub.resetSubmissionObject).not.toHaveBeenCalled();
      done();
    });

  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

  collectionId = mockSubmissionCollectionId;
  selfUrl = mockSubmissionSelfUrl;
  submissionDefinition = mockSubmissionDefinition;
  submissionId = mockSubmissionId;

}
