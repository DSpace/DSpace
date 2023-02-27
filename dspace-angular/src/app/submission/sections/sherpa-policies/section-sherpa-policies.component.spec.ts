import { SharedModule } from '../../../shared/shared.module';
import { NgbCollapseModule } from '@ng-bootstrap/ng-bootstrap';
import { SubmissionServiceStub } from '../../../shared/testing/submission-service.stub';
import { SherpaDataResponse } from '../../../shared/mocks/section-sherpa-policies.service.mock';
import { ComponentFixture, inject, TestBed } from '@angular/core/testing';

import { SectionsService } from '../sections.service';
import { SectionsServiceStub } from '../../../shared/testing/sections-service.stub';
import { JsonPatchOperationsBuilder } from '../../../core/json-patch/builder/json-patch-operations-builder';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { BrowserModule, By } from '@angular/platform-browser';

import { Store } from '@ngrx/store';
import { AppState } from '../../../app.reducer';
import { SubmissionSectionSherpaPoliciesComponent } from './section-sherpa-policies.component';
import { SubmissionService } from '../../submission.service';
import { DebugElement } from '@angular/core';
import { TranslateLoaderMock } from '../../../shared/mocks/translate-loader.mock';
import { of as observableOf } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('SubmissionSectionSherpaPoliciesComponent', () => {
  let component: SubmissionSectionSherpaPoliciesComponent;
  let fixture: ComponentFixture<SubmissionSectionSherpaPoliciesComponent>;
  let de: DebugElement;

  const sectionsServiceStub = new SectionsServiceStub();

  const operationsBuilder = jasmine.createSpyObj('operationsBuilder', {
    add: undefined,
    remove: undefined,
    replace: undefined,
  });

  const storeStub = jasmine.createSpyObj('store', ['dispatch']);

  const sectionData = {
    header: 'submit.progressbar.sherpaPolicies',
    config: 'http://localhost:8080/server/api/config/submissionaccessoptions/SherpaPoliciesDefaultConfiguration',
    mandatory: true,
    sectionType: 'sherpaPolicies',
    collapsed: false,
    enabled: true,
    data: SherpaDataResponse,
    errorsToShow: [],
    serverValidationErrors: [],
    isLoading: false,
    isValid: true
  };

  describe('SubmissionSectionSherpaPoliciesComponent', () => {

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [
          BrowserModule,
          NoopAnimationsModule,
          TranslateModule.forRoot({
            loader: {
              provide: TranslateLoader,
              useClass: TranslateLoaderMock
            }
          }),
          NgbCollapseModule,
          SharedModule
        ],
        declarations: [SubmissionSectionSherpaPoliciesComponent],
        providers: [
          { provide: SectionsService, useValue: sectionsServiceStub },
          { provide: JsonPatchOperationsBuilder, useValue: operationsBuilder },
          { provide: SubmissionService, useValue: SubmissionServiceStub },
          { provide: Store, useValue: storeStub },
          { provide: 'sectionDataProvider', useValue: sectionData },
          { provide: 'submissionIdProvider', useValue: '1508' },
        ]
      })
        .compileComponents();
    });

    beforeEach(inject([Store], (store: Store<AppState>) => {
      fixture = TestBed.createComponent(SubmissionSectionSherpaPoliciesComponent);
      component = fixture.componentInstance;
      de = fixture.debugElement;
      sectionsServiceStub.getSectionData.and.returnValue(observableOf(SherpaDataResponse));
      fixture.detectChanges();
    }));


    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should show refresh button', () => {
      expect(de.query(By.css('[data-test="refresh-btn"]'))).toBeTruthy();
    });

    it('should show publisher information', () => {
      expect(de.query(By.css('ds-publication-information'))).toBeTruthy();
    });

    it('should show publisher policy', () => {
      expect(de.query(By.css('ds-publisher-policy'))).toBeTruthy();
    });

    it('should show metadata information', () => {
      expect(de.query(By.css('ds-metadata-information'))).toBeTruthy();
    });

    it('when refresh button click operationsBuilder.remove should have been called', () => {
      de.query(By.css('[data-test="refresh-btn"]')).nativeElement.click();
      expect(operationsBuilder.remove).toHaveBeenCalled();
    });


  });

});
