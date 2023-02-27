import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { RouterStub } from '../../../../shared/testing/router.stub';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FormatFormComponent } from './format-form.component';
import { BitstreamFormat } from '../../../../core/shared/bitstream-format.model';
import { BitstreamFormatSupportLevel } from '../../../../core/shared/bitstream-format-support-level';
import { DynamicCheckboxModel, DynamicFormArrayModel, DynamicInputModel } from '@ng-dynamic-forms/core';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { isEmpty } from '../../../../shared/empty.util';

describe('FormatFormComponent', () => {
  let comp: FormatFormComponent;
  let fixture: ComponentFixture<FormatFormComponent>;

  const router = new RouterStub();

  const bitstreamFormat = new BitstreamFormat();
  bitstreamFormat.uuid = 'test-uuid-1';
  bitstreamFormat.id = 'test-uuid-1';
  bitstreamFormat.shortDescription = 'Unknown';
  bitstreamFormat.description = 'Unknown data format';
  bitstreamFormat.mimetype = 'application/octet-stream';
  bitstreamFormat.supportLevel = BitstreamFormatSupportLevel.Unknown;
  bitstreamFormat.internal = false;
  bitstreamFormat.extensions = [];

  const submittedBitstreamFormat = new BitstreamFormat();
  submittedBitstreamFormat.id = bitstreamFormat.id;
  submittedBitstreamFormat.shortDescription = bitstreamFormat.shortDescription;
  submittedBitstreamFormat.mimetype = bitstreamFormat.mimetype;
  submittedBitstreamFormat.description = bitstreamFormat.description;
  submittedBitstreamFormat.supportLevel = bitstreamFormat.supportLevel;
  submittedBitstreamFormat.internal = bitstreamFormat.internal;
  submittedBitstreamFormat.extensions = bitstreamFormat.extensions;

  const initAsync = () => {
    TestBed.configureTestingModule({
      imports: [CommonModule, RouterTestingModule.withRoutes([]), ReactiveFormsModule, FormsModule, TranslateModule.forRoot(), NgbModule],
      declarations: [FormatFormComponent],
      providers: [
        { provide: Router, useValue: router },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  };

  const initBeforeEach = () => {
    fixture = TestBed.createComponent(FormatFormComponent);
    comp = fixture.componentInstance;

    comp.bitstreamFormat = bitstreamFormat;
    fixture.detectChanges();
  };

  describe('initialise', () => {
    beforeEach(waitForAsync(initAsync));
    beforeEach(initBeforeEach);
    it('should initialises the values in the form', () => {

      expect((comp.formModel[0] as DynamicInputModel).value).toBe(bitstreamFormat.shortDescription);
      expect((comp.formModel[1] as DynamicInputModel).value).toBe(bitstreamFormat.mimetype);
      expect((comp.formModel[2] as DynamicInputModel).value).toBe(bitstreamFormat.description);
      expect((comp.formModel[3] as DynamicInputModel).value).toBe(bitstreamFormat.supportLevel);
      expect((comp.formModel[4] as DynamicCheckboxModel).value).toBe(bitstreamFormat.internal);

      const formArray = (comp.formModel[5] as DynamicFormArrayModel);
      const extensions = [];
      for (let i = 0; i < formArray.groups.length; i++) {
        const value = (formArray.get(i).get(0) as DynamicInputModel).value;
        if (!isEmpty(value)) {
          extensions.push((formArray.get(i).get(0) as DynamicInputModel).value);
        }
      }

      expect(extensions).toEqual(bitstreamFormat.extensions);

    });
  });
  describe('onSubmit', () => {
    beforeEach(waitForAsync(initAsync));
    beforeEach(initBeforeEach);

    it('should emit the bitstreamFormat currently present in the form', () => {
      spyOn(comp.updatedFormat, 'emit');
      comp.onSubmit();

      expect(comp.updatedFormat.emit).toHaveBeenCalledWith(submittedBitstreamFormat);
    });
  });
  describe('onCancel', () => {
    beforeEach(waitForAsync(initAsync));
    beforeEach(initBeforeEach);

    it('should navigate back to the bitstream overview', () => {
      comp.onCancel();
      expect(router.navigate).toHaveBeenCalledWith(['/admin/registries/bitstream-formats']);
    });
  });
});
