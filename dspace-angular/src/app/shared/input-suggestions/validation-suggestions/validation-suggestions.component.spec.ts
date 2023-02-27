import { ChangeDetectionStrategy, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';

import { TranslateModule } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { MetadataFieldDataService } from '../../../core/data/metadata-field-data.service';
import { ObjectUpdatesService } from '../../../core/data/object-updates/object-updates.service';
import { ValidationSuggestionsComponent } from './validation-suggestions.component';

describe('ValidationSuggestionsComponent', () => {

  let comp: ValidationSuggestionsComponent;
  let fixture: ComponentFixture<ValidationSuggestionsComponent>;
  let de: DebugElement;
  let el: HTMLElement;
  const suggestions = [{ displayValue: 'suggestion uno', value: 'suggestion uno' }, {
    displayValue: 'suggestion dos',
    value: 'suggestion dos'
  }, { displayValue: 'suggestion tres', value: 'suggestion tres' }];

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([]), NoopAnimationsModule, FormsModule, ReactiveFormsModule],
      declarations: [ValidationSuggestionsComponent],
      providers: [FormsModule,
        ReactiveFormsModule,
        { provide: MetadataFieldDataService, useValue: {} },
        { provide: ObjectUpdatesService, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ValidationSuggestionsComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ValidationSuggestionsComponent);

    comp = fixture.componentInstance; // LoadingComponent test instance
    comp.suggestions = suggestions;
    // query for the message <label> by CSS element selector
    de = fixture.debugElement;
    el = de.nativeElement;
    comp.show.next(true);
    fixture.detectChanges();
  });

  describe('when an element is clicked', () => {
    const clickedIndex = 0;
    beforeEach(() => {
      spyOn(comp, 'onClickSuggestion');
      const clickedLink = de.query(By.css('.dropdown-list > div:nth-child(' + (clickedIndex + 1) + ') a'));
      clickedLink.triggerEventHandler('click', {});
      fixture.detectChanges();
    });
    it('should call onClickSuggestion() with the suggestion as a parameter', () => {
      expect(comp.onClickSuggestion).toHaveBeenCalledWith(suggestions[clickedIndex].value);
    });
  });
  describe('can edit input', () => {
    describe('test input field readonly property when input disable is true', () => {
      beforeEach(() => {
        comp.disable = true;
        fixture.detectChanges();
      });
      it('it should be true', () => {
        fixture.detectChanges();
        const input = fixture.debugElement.query(By.css('input'));
        const element = input.nativeElement;
        expect(element.readOnly).toBe(true);
      });
    });
    describe('test input field readonly property when input disable is false', () => {
      beforeEach(() => {
        comp.disable = false;
        fixture.detectChanges();
      });
      it('it should be true', () => {
        fixture.detectChanges();
        const input = fixture.debugElement.query(By.css('input'));
        const element = input.nativeElement;
        expect(element.readOnly).toBe(false);
      });
    });
  });
});
