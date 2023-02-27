import { ChangeDetectionStrategy, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';

import { TranslateModule } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { DsoInputSuggestionsComponent } from './dso-input-suggestions.component';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';

describe('DsoInputSuggestionsComponent', () => {

  let comp: DsoInputSuggestionsComponent;
  let fixture: ComponentFixture<DsoInputSuggestionsComponent>;
  let de: DebugElement;
  let el: HTMLElement;

  const dso1 = {
    uuid: 'test-uuid-1',
    name: 'test-name-1'
  } as DSpaceObject;

  const dso2 = {
    uuid: 'test-uuid-2',
    name: 'test-name-2'
  } as DSpaceObject;

  const dso3 = {
    uuid: 'test-uuid-3',
    name: 'test-name-3'
  } as DSpaceObject;

  const suggestions = [dso1, dso2, dso3];

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([]), NoopAnimationsModule, FormsModule],
      declarations: [DsoInputSuggestionsComponent],
      providers: [],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(DsoInputSuggestionsComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DsoInputSuggestionsComponent);

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
      const clickedLink = de.query(By.css('.dropdown-list > div:nth-child(' + (clickedIndex + 1) + ') button'));
      clickedLink.triggerEventHandler('click', {});
      fixture.detectChanges();
    });
    it('should call onClickSuggestion() with the suggestion as a parameter', () => {
      expect(comp.onClickSuggestion).toHaveBeenCalledWith(suggestions[clickedIndex]);
    });
  });
});
