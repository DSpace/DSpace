import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { SubmissionSectionCcLicensesComponent } from './submission-section-cc-licenses.component';
import { SUBMISSION_CC_LICENSE } from '../../../core/submission/models/submission-cc-licence.resource-type';
import { of as observableOf } from 'rxjs';
import { SubmissionCcLicenseDataService } from '../../../core/submission/submission-cc-license-data.service';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { SharedModule } from '../../../shared/shared.module';
import { SectionsService } from '../sections.service';
import { SectionDataObject } from '../models/section-data.model';
import { SectionsType } from '../sections-type';
import { TranslateModule } from '@ngx-translate/core';
import { SubmissionCcLicence } from '../../../core/submission/models/submission-cc-license.model';
import { cold } from 'jasmine-marbles';
import { JsonPatchOperationsBuilder } from '../../../core/json-patch/builder/json-patch-operations-builder';
import { SubmissionCcLicenseUrlDataService } from '../../../core/submission/submission-cc-license-url-data.service';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import {ConfigurationDataService} from '../../../core/data/configuration-data.service';
import {ConfigurationProperty} from '../../../core/shared/configuration-property.model';

describe('SubmissionSectionCcLicensesComponent', () => {

  let component: SubmissionSectionCcLicensesComponent;
  let fixture: ComponentFixture<SubmissionSectionCcLicensesComponent>;
  let de: DebugElement;

  const sectionObject: SectionDataObject = {
    config: 'test config',
    mandatory: true,
    data: {},
    errorsToShow: [],
    serverValidationErrors: [],
    header: 'test header',
    id: 'test section id',
    sectionType: SectionsType.SubmissionForm
  };

  const submissionCcLicenses: SubmissionCcLicence[] = [
    {
      id: 'test license id 1',
      type: SUBMISSION_CC_LICENSE,
      name: 'test license name 1',
      fields: [
        {
          id: 'test-field-id-1a',
          label: 'test field label 1a',
          description: 'test field description 1a',
          enums: [
            {
              id: 'test enum id 1a I',
              label: 'test enum label 1a I',
              description: 'test enum description 1a I',
            },
            {
              id: 'test enum id 1a II',
              label: 'test enum label 1a II',
              description: 'test enum description 1a II',
            },
          ],
        },
        {
          id: 'test-field-id-1b',
          label: 'test field label 1b',
          description: 'test field description 1b',
          enums: [
            {
              id: 'test enum id 1b I',
              label: 'test enum label 1b I',
              description: 'test enum description 1b I',
            },
            {
              id: 'test enum id 1b II',
              label: 'test enum label 1b II',
              description: 'test enum description 1b II',
            },
          ],
        },
      ],
      _links: {
        self: {
          href: 'test link',
        },
      },
    },
    {
      id: 'test license id 2',
      type: SUBMISSION_CC_LICENSE,
      name: 'test license name 2',
      fields: [
        {
          id: 'test-field-id-2a',
          label: 'test field label 2a',
          description: 'test field description 2a',
          enums: [
            {
              id: 'test enum id 2a I',
              label: 'test enum label 2a I',
              description: 'test enum description 2a I'
            },
            {
              id: 'test enum id 2a II',
              label: 'test enum label 2a II',
              description: 'test enum description 2a II'
            },
          ],
        },
        {
          id: 'test-field-id-2b',
          label: 'test field label 2b',
          description: 'test field description 2b',
          enums: [
            {
              id: 'test enum id 2b I',
              label: 'test enum label 2b I',
              description: 'test enum description 2b I'
            },
            {
              id: 'test enum id 2b II',
              label: 'test enum label 2b II',
              description: 'test enum description 2b II'
            },
          ],
        },
      ],
      _links: {
        self: {
          href: 'test link',
        },
      },
    },
  ];

  const submissionCcLicensesDataService = jasmine.createSpyObj('submissionCcLicensesDataService', {
    findAll: createSuccessfulRemoteDataObject$(createPaginatedList(submissionCcLicenses)),
  });

  const submissionCcLicenseUrlDataService = jasmine.createSpyObj('submissionCcLicenseUrlDataService', {
    getCcLicenseLink: createSuccessfulRemoteDataObject$(
      {
        url: 'test cc license link',
      }
    ),
  });

  const sectionService = {
    getSectionState: () => {
      return observableOf({});
    },
    setSectionStatus: () => undefined,
    updateSectionData: (submissionId, sectionId, updatedData) => {
      component.sectionData.data = updatedData;
    }
  };

  const operationsBuilder = jasmine.createSpyObj('operationsBuilder', {
    add: undefined,
    remove: undefined,
  });

  const configurationDataService = jasmine.createSpyObj('configurationDataService', {
    findByPropertyName: createSuccessfulRemoteDataObject$({
      ... new ConfigurationProperty(),
      name: 'cc.license.jurisdiction',
      values: ['mock-jurisdiction-value'],
    }),
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        SharedModule,
        TranslateModule.forRoot(),
      ],
      declarations: [
        SubmissionSectionCcLicensesComponent,
      ],
      providers: [
        { provide: SubmissionCcLicenseDataService, useValue: submissionCcLicensesDataService },
        { provide: SubmissionCcLicenseUrlDataService, useValue: submissionCcLicenseUrlDataService },
        { provide: SectionsService, useValue: sectionService },
        { provide: JsonPatchOperationsBuilder, useValue: operationsBuilder },
        { provide: ConfigurationDataService, useValue: configurationDataService },
        { provide: 'collectionIdProvider', useValue: 'test collection id' },
        { provide: 'sectionDataProvider', useValue: Object.assign({}, sectionObject) },
        { provide: 'submissionIdProvider', useValue: 'test submission id' },
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SubmissionSectionCcLicensesComponent);
    component = fixture.componentInstance;
    de = fixture.debugElement;
    fixture.detectChanges();
  });

  it('should display a dropdown with the different cc licenses', () => {
    expect(
      de.query(By.css('.ccLicense-select ds-select .dropdown-menu button:nth-child(1)')).nativeElement.innerText
    ).toContain('test license name 1');
    expect(
      de.query(By.css('.ccLicense-select ds-select .dropdown-menu button:nth-child(2)')).nativeElement.innerText
    ).toContain('test license name 2');
  });

  describe('when a license is selected', () => {

    const ccLicence = submissionCcLicenses[1];

    beforeEach(() => {
      component.selectCcLicense(ccLicence);
      fixture.detectChanges();
    });

    it('should display the selected cc license', () => {
      expect(
        de.query(By.css('.ccLicense-select ds-select button.selection')).nativeElement.innerText
      ).toContain('test license name 2');
    });

    it('should display all field labels of the selected cc license only', () => {
      expect(de.query(By.css('div.test-field-id-1a'))).toBeNull();
      expect(de.query(By.css('div.test-field-id-1b'))).toBeNull();
      expect(de.query(By.css('div.test-field-id-2a'))).toBeTruthy();
      expect(de.query(By.css('div.test-field-id-2b'))).toBeTruthy();
    });

    it('should not display a cc license link', () => {
      expect(de.query(By.css('.license-link'))).toBeNull();
    });

    it('should have section status incomplete', () => {
      expect(component.getSectionStatus()).toBeObservable(cold('(a|)', { a: false }));
    });

    describe('when all options have a value selected', () => {

      beforeEach(() => {
        component.selectOption(ccLicence, ccLicence.fields[0], ccLicence.fields[0].enums[1]);
        component.selectOption(ccLicence, ccLicence.fields[1], ccLicence.fields[1].enums[0]);
        fixture.detectChanges();
      });

      it('should call the submission cc licenses data service getCcLicenseLink method', () => {
        expect(submissionCcLicenseUrlDataService.getCcLicenseLink).toHaveBeenCalledWith(
          ccLicence,
          new Map([
            [ccLicence.fields[0], ccLicence.fields[0].enums[1]],
            [ccLicence.fields[1], ccLicence.fields[1].enums[0]],
          ])
        );
      });

      it('should display a cc license link', () => {
        expect(de.query(By.css('.license-link'))).toBeTruthy();
      });

      it('should not be accepted', () => {
        expect(component.accepted).toBeFalse();
      });

      it('should have section status incomplete', () => {
        expect(component.getSectionStatus()).toBeObservable(cold('(a|)', { a: false }));
      });

      describe('when the cc license is accepted', () => {

        beforeEach(() => {
          component.setAccepted(true);
          fixture.detectChanges();
        });

        it('should have section status complete', () => {
          expect(component.getSectionStatus()).toBeObservable(cold('(a|)', { a: true }));
        });
      });
    });
  });
});
