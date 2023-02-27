import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VarDirective } from '../../../shared/utils/var.directive';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { DsoEditMetadataFieldValuesComponent } from './dso-edit-metadata-field-values.component';
import { DsoEditMetadataForm } from '../dso-edit-metadata-form';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { MetadataValue } from '../../../core/shared/metadata.models';
import { of } from 'rxjs/internal/observable/of';
import { BehaviorSubject } from 'rxjs/internal/BehaviorSubject';
import { By } from '@angular/platform-browser';

describe('DsoEditMetadataFieldValuesComponent', () => {
  let component: DsoEditMetadataFieldValuesComponent;
  let fixture: ComponentFixture<DsoEditMetadataFieldValuesComponent>;

  let form: DsoEditMetadataForm;
  let dso: DSpaceObject;
  let mdField: string;
  let draggingMdField$: BehaviorSubject<string>;

  beforeEach(waitForAsync(() => {
    dso = Object.assign(new DSpaceObject(), {
      metadata: {
        'dc.title': [
          Object.assign(new MetadataValue(), {
            value: 'Test Title',
            language: 'en',
            place: 0,
          }),
        ],
        'dc.subject': [
          Object.assign(new MetadataValue(), {
            value: 'Subject One',
            language: 'en',
            place: 0,
          }),
          Object.assign(new MetadataValue(), {
            value: 'Subject Two',
            language: 'en',
            place: 1,
          }),
          Object.assign(new MetadataValue(), {
            value: 'Subject Three',
            language: 'en',
            place: 2,
          }),
        ],
      },
    });
    form = new DsoEditMetadataForm(dso.metadata);
    mdField = 'dc.subject';
    draggingMdField$ = new BehaviorSubject<string>(null);

    TestBed.configureTestingModule({
      declarations: [DsoEditMetadataFieldValuesComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DsoEditMetadataFieldValuesComponent);
    component = fixture.componentInstance;
    component.dso = dso;
    component.form = form;
    component.mdField = mdField;
    component.saving$ = of(false);
    component.draggingMdField$ = draggingMdField$;
    fixture.detectChanges();
  });

  describe('when draggingMdField$ emits a value equal to mdField', () => {
    beforeEach(() => {
      draggingMdField$.next(mdField);
      fixture.detectChanges();
    });

    it('should not disable the list', () => {
      expect(fixture.debugElement.query(By.css('.ds-drop-list.disabled'))).toBeNull();
    });
  });

  describe('when draggingMdField$ emits a value different to mdField', () => {
    beforeEach(() => {
      draggingMdField$.next(`${mdField}.fake`);
      fixture.detectChanges();
    });

    it('should disable the list', () => {
      expect(fixture.debugElement.query(By.css('.ds-drop-list.disabled'))).toBeTruthy();
    });
  });

  describe('when draggingMdField$ emits null', () => {
    beforeEach(() => {
      draggingMdField$.next(null);
      fixture.detectChanges();
    });

    it('should not disable the list', () => {
      expect(fixture.debugElement.query(By.css('.ds-drop-list.disabled'))).toBeNull();
    });
  });

  describe('dropping a value on a different index', () => {
    beforeEach(() => {
      component.drop(Object.assign({
        previousIndex: 0,
        currentIndex: 2,
      }));
    });

    it('should physically move the relevant metadata value within the form', () => {
      expect(form.fields[mdField][0].newValue.value).toEqual('Subject Two');
      expect(form.fields[mdField][1].newValue.value).toEqual('Subject Three');
      expect(form.fields[mdField][2].newValue.value).toEqual('Subject One');
    });

    it('should update the metadata values their new place to match the new physical order', () => {
      expect(form.fields[mdField][0].newValue.place).toEqual(0);
      expect(form.fields[mdField][1].newValue.place).toEqual(1);
      expect(form.fields[mdField][2].newValue.place).toEqual(2);
    });

    it('should maintain the metadata values their original place in their original value so it can be used later to determine the patch operations', () => {
      expect(form.fields[mdField][0].originalValue.place).toEqual(1);
      expect(form.fields[mdField][1].originalValue.place).toEqual(2);
      expect(form.fields[mdField][2].originalValue.place).toEqual(0);
    });
  });
});
