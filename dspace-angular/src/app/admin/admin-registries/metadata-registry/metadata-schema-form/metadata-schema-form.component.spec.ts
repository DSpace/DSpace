import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';

import { MetadataSchemaFormComponent } from './metadata-schema-form.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { EnumKeysPipe } from '../../../../shared/utils/enum-keys-pipe';
import { RegistryService } from '../../../../core/registry/registry.service';
import { FormBuilderService } from '../../../../shared/form/builder/form-builder.service';
import { of as observableOf } from 'rxjs';
import { MetadataSchema } from '../../../../core/metadata/metadata-schema.model';

describe('MetadataSchemaFormComponent', () => {
  let component: MetadataSchemaFormComponent;
  let fixture: ComponentFixture<MetadataSchemaFormComponent>;
  let registryService: RegistryService;

  /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
  const registryServiceStub = {
    getActiveMetadataSchema: () => observableOf(undefined),
    createOrUpdateMetadataSchema: (schema: MetadataSchema) => observableOf(schema),
    cancelEditMetadataSchema: () => {
    },
    clearMetadataSchemaRequests: () => observableOf(undefined)
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
      declarations: [MetadataSchemaFormComponent, EnumKeysPipe],
      providers: [
        { provide: RegistryService, useValue: registryServiceStub },
        { provide: FormBuilderService, useValue: formBuilderServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MetadataSchemaFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  beforeEach(inject([RegistryService], (s) => {
    registryService = s;
  }));

  describe('when submitting the form', () => {
    const namespace = 'fake namespace';
    const prefix = 'fake';

    const expected = Object.assign(new MetadataSchema(), {
      namespace: namespace,
      prefix: prefix
    });

    beforeEach(() => {
      spyOn(component.submitForm, 'emit');
      component.name.value = prefix;
      component.namespace.value = namespace;
    });

    describe('without an active schema', () => {
      beforeEach(() => {
        spyOn(registryService, 'getActiveMetadataSchema').and.returnValue(observableOf(undefined));
        component.onSubmit();
        fixture.detectChanges();
      });

      it('should emit a new schema using the correct values', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect(component.submitForm.emit).toHaveBeenCalledWith(expected);
        });
      }));
    });

    describe('with an active schema', () => {
      const expectedWithId = Object.assign(new MetadataSchema(), {
        id: 1,
        namespace: namespace,
        prefix: prefix
      });

      beforeEach(() => {
        spyOn(registryService, 'getActiveMetadataSchema').and.returnValue(observableOf(expectedWithId));
        component.onSubmit();
        fixture.detectChanges();
      });

      it('should edit the existing schema using the correct values', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect(component.submitForm.emit).toHaveBeenCalledWith(expectedWithId);
        });
      }));
    });
  });
});
