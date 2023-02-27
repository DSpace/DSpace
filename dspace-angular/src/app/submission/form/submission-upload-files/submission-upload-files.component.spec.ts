import { ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';

import { of as observableOf } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';

import { SubmissionServiceStub } from '../../../shared/testing/submission-service.stub';
import {
  mockSectionsData,
  mockSubmissionCollectionId,
  mockSubmissionId,
  mockSubmissionObject,
  mockUploadResponse1ParsedErrors,
  mockUploadResponse2Errors,
  mockUploadResponse2ParsedErrors
} from '../../../shared/mocks/submission.mock';
import { SubmissionService } from '../../submission.service';

import { SectionsServiceStub } from '../../../shared/testing/sections-service.stub';
import { SectionsService } from '../../sections/sections.service';
import { SubmissionUploadFilesComponent } from './submission-upload-files.component';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { getMockTranslateService } from '../../../shared/mocks/translate.service.mock';
import { cold, hot } from 'jasmine-marbles';
import { SubmissionJsonPatchOperationsServiceStub } from '../../../shared/testing/submission-json-patch-operations-service.stub';
import { SubmissionJsonPatchOperationsService } from '../../../core/submission/submission-json-patch-operations.service';
import { SharedModule } from '../../../shared/shared.module';
import { createTestComponent } from '../../../shared/testing/utils.test';
import { UploaderOptions } from '../../../shared/upload/uploader/uploader-options.model';

describe('SubmissionUploadFilesComponent Component', () => {

  let comp: SubmissionUploadFilesComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<SubmissionUploadFilesComponent>;
  let submissionServiceStub: SubmissionServiceStub;
  let sectionsServiceStub: SectionsServiceStub;
  let notificationsServiceStub: NotificationsServiceStub;
  let translateService: any;

  const submissionJsonPatchOperationsServiceStub = new SubmissionJsonPatchOperationsServiceStub();
  const submissionId = mockSubmissionId;
  const collectionId = mockSubmissionCollectionId;
  const uploadRestResponse: any = mockSubmissionObject;

  const store: any = jasmine.createSpyObj('store', {
    dispatch: jasmine.createSpy('dispatch'),
    select: jasmine.createSpy('select')
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        SharedModule,
        TranslateModule.forRoot()
      ],
      declarations: [
        SubmissionUploadFilesComponent,
        TestComponent
      ],
      providers: [
        { provide: NotificationsService, useClass: NotificationsServiceStub },
        { provide: SubmissionService, useClass: SubmissionServiceStub },
        { provide: SectionsService, useClass: SectionsServiceStub },
        { provide: TranslateService, useValue: getMockTranslateService() },
        { provide: SubmissionJsonPatchOperationsService, useValue: submissionJsonPatchOperationsServiceStub },
        { provide: Store, useValue: store },
        ChangeDetectorRef,
        SubmissionUploadFilesComponent
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  }));

  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      const html = `
        <ds-submission-upload-files [submissionId]="submissionId"
                                    [collectionId]="collectionId"
                                    [uploadFilesOptions]="uploadFilesOptions"></ds-submission-upload-files>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create SubmissionUploadFilesComponent', inject([SubmissionUploadFilesComponent], (app: SubmissionUploadFilesComponent) => {

      expect(app).toBeDefined();

    }));
  });

  describe('', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(SubmissionUploadFilesComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      submissionServiceStub = TestBed.inject(SubmissionService as any);
      sectionsServiceStub = TestBed.inject(SectionsService as any);
      sectionsServiceStub.isSectionTypeAvailable.and.returnValue(observableOf(true));
      notificationsServiceStub = TestBed.inject(NotificationsService as any);
      translateService = TestBed.inject(TranslateService);
      comp.submissionId = submissionId;
      comp.collectionId = collectionId;
      comp.uploadFilesOptions = Object.assign(new UploaderOptions(),{
        url: '',
        authToken: null,
        disableMultipart: false,
        itemAlias: null
      });

    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      fixture = null;
      submissionServiceStub = null;
      sectionsServiceStub = null;
      notificationsServiceStub = null;
      translateService = null;
    });

    it('should init uploadEnabled properly', () => {
      sectionsServiceStub.isSectionTypeAvailable.and.returnValue(hot('-a-b', {
        a: false,
        b: true
      }));

      const expected = cold('-c-d', {
        c: false,
        d: true
      });

      comp.ngOnChanges();
      fixture.detectChanges();

      expect(compAsAny.uploadEnabled).toBeObservable(expected);
    });

    describe('on upload complete', () => {
      beforeEach(() => {
        sectionsServiceStub.isSectionType.and.callFake((_, sectionId, __) => observableOf(sectionId === 'upload'));
        compAsAny.uploadEnabled = observableOf(true);
      });

      it('should show a success notification and call updateSectionData if successful', () => {
        const expectedErrors: any = mockUploadResponse1ParsedErrors;
        fixture.detectChanges();

        comp.onCompleteItem(Object.assign({}, uploadRestResponse, { sections: mockSectionsData }));

        Object.keys(mockSectionsData).forEach((sectionId) => {
          expect(sectionsServiceStub.updateSectionData).toHaveBeenCalledWith(
            submissionId,
            sectionId,
            mockSectionsData[sectionId],
          expectedErrors[sectionId],
            expectedErrors[sectionId]
          );
        });

        expect(notificationsServiceStub.success).toHaveBeenCalled();

      });

      it('should show an error notification and call updateSectionData if unsuccessful', () => {
        const responseErrors = mockUploadResponse2Errors;
        const expectedErrors: any = mockUploadResponse2ParsedErrors;
        fixture.detectChanges();

        comp.onCompleteItem(Object.assign({}, uploadRestResponse, {
          sections: mockSectionsData,
          errors: responseErrors.errors
        }));

        Object.keys(mockSectionsData).forEach((sectionId) => {
          expect(sectionsServiceStub.updateSectionData).toHaveBeenCalledWith(
            submissionId,
            sectionId,
            mockSectionsData[sectionId],
          expectedErrors[sectionId],
            expectedErrors[sectionId]
          );
        });

        expect(notificationsServiceStub.success).not.toHaveBeenCalled();

      });
    });
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

  submissionId = mockSubmissionId;
  collectionId = mockSubmissionCollectionId;
  uploadFilesOptions = Object.assign(new UploaderOptions(), {
    url: '',
    authToken: null,
    disableMultipart: false,
    itemAlias: null
  });

}
