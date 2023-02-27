import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';

import { MetadataFieldFormComponent } from './metadata-field-form.component';
import { RegistryService } from '../../../../core/registry/registry.service';
import { of as observableOf } from 'rxjs';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { EnumKeysPipe } from '../../../../shared/utils/enum-keys-pipe';
import { FormBuilderService } from '../../../../shared/form/builder/form-builder.service';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { MetadataField } from '../../../../core/metadata/metadata-field.model';
import { MetadataSchema } from '../../../../core/metadata/metadata-schema.model';

describe('MetadataFieldFormComponent', () => {
  let component: MetadataFieldFormComponent;
  let fixture: ComponentFixture<MetadataFieldFormComponent>;
  let registryService: RegistryService;

  const metadataSchema = Object.assign(new MetadataSchema(), {
    id: 1,
    namespace: 'fake schema',
    prefix: 'fake'
  });

  /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
  const registryServiceStub = {
    getActiveMetadataField: () => observableOf(undefined),
    createMetadataField: (field: MetadataField) => observableOf(field),
    updateMetadataField: (field: MetadataField) => observableOf(field),
    cancelEditMetadataField: () => {
    },
    cancelEditMetadataSchema: () => {
    },
    clearMetadataFieldRequests: () => observableOf(undefined)
  };
  const formBuilderServiceStub = {
    createFormGroup: () => {
      return {
        patchValue: () => {
        }
      };
    }
  };
  /* eslint-enable no-empty, @typescript-eslint/no-empty-function */

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [CommonModule, RouterTestingModule.withRoutes([]), TranslateModule.forRoot(), NgbModule],
      declarations: [MetadataFieldFormComponent, EnumKeysPipe],
      providers: [
        { provide: RegistryService, useValue: registryServiceStub },
        { provide: FormBuilderService, useValue: formBuilderServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MetadataFieldFormComponent);
    component = fixture.componentInstance;
    component.metadataSchema = metadataSchema;
    fixture.detectChanges();
  });

  beforeEach(inject([RegistryService], (s) => {
    registryService = s;
  }));

  afterEach(() => {
    component = null;
    registryService = null;
  });

  describe('when submitting the form', () => {
    const element = 'fakeElement';
    const qualifier = 'fakeQualifier';
    const scopeNote = 'fakeScopeNote';

    const expected = Object.assign(new MetadataField(), {
      element: element,
      qualifier: qualifier,
      scopeNote: scopeNote
    });

    beforeEach(() => {
      spyOn(component.submitForm, 'emit');
      component.element.value = element;
      component.qualifier.value = qualifier;
      component.scopeNote.value = scopeNote;
    });

    describe('without an active field', () => {
      beforeEach(() => {
        spyOn(registryService, 'getActiveMetadataField').and.returnValue(observableOf(undefined));
        component.onSubmit();
        fixture.detectChanges();
      });

      it('should emit a new field using the correct values', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect(component.submitForm.emit).toHaveBeenCalledWith(expected);
        });
      }));
    });

    describe('with an active field', () => {
      const expectedWithId = Object.assign(new MetadataField(), {
        id: 1,
        schema: metadataSchema,
        element: element,
        qualifier: qualifier,
        scopeNote: scopeNote
      });

      beforeEach(() => {
        spyOn(registryService, 'getActiveMetadataField').and.returnValue(observableOf(expectedWithId));
        component.onSubmit();
        fixture.detectChanges();
      });

      it('should edit the existing field using the correct values', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect(component.submitForm.emit).toHaveBeenCalledWith(expectedWithId);
        });
      }));
    });
  });
});
