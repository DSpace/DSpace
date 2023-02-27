import { ChangeDetectionStrategy, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';

import { TranslateModule } from '@ngx-translate/core';
import { InputSuggestionsComponent } from './input-suggestions.component';
import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

describe('InputSuggestionsComponent', () => {

  let comp: InputSuggestionsComponent;
  let fixture: ComponentFixture<InputSuggestionsComponent>;
  let de: DebugElement;
  let el: HTMLElement;
  let suggestions;

  beforeEach(waitForAsync(() => {
    suggestions = [{ displayValue: 'suggestion uno', value: 'suggestion uno' }, {
      displayValue: 'suggestion dos',
      value: 'suggestion dos'
    }, { displayValue: 'suggestion tres', value: 'suggestion tres' }];

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([]), NoopAnimationsModule, FormsModule],
      declarations: [InputSuggestionsComponent],
      providers: [],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(InputSuggestionsComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(InputSuggestionsComponent);

    comp = fixture.componentInstance; // LoadingComponent test instance
    comp.suggestions = suggestions;
    // query for the message <label> by CSS element selector
    de = fixture.debugElement;
    el = de.nativeElement;
    comp.show.next(true);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(comp).toBeTruthy();
  });

  describe('when the input field is in focus', () => {

    beforeEach(() => {
      const inputElement = de.query(By.css('.suggestion_input'));
      inputElement.nativeElement.focus();
      fixture.detectChanges();

    });

    it('should not have any element in focus', () => {
      const activeElement = el.ownerDocument.activeElement;
      expect(activeElement.nodeName.toLowerCase()).not.toEqual('a');
    });

    describe('when key up is pressed', () => {
      beforeEach(() => {
        spyOn(comp, 'shiftFocusUp');
        const form = de.query(By.css('form'));
        form.triggerEventHandler('keydown.arrowup', {});
        fixture.detectChanges();
      });

      it('should call shiftFocusUp()', () => {
        expect(comp.shiftFocusUp).toHaveBeenCalled();
      });
    });

    describe('when shiftFocusUp() is triggered', () => {
      beforeEach(() => {
        comp.shiftFocusUp(new KeyboardEvent('keydown.arrowup'));
        fixture.detectChanges();
      });

      it('should put the focus on the last element ', () => {
        const lastLink = de.query(By.css('.dropdown-list > div:last-child a'));
        const activeElement = el.ownerDocument.activeElement;
        expect(activeElement).toEqual(lastLink.nativeElement);
      });
    });

    describe('when key down is pressed', () => {
      beforeEach(() => {
        spyOn(comp, 'shiftFocusDown');
        const form = de.query(By.css('form'));
        form.triggerEventHandler('keydown.arrowdown', {});
        fixture.detectChanges();
      });

      it('should call shiftFocusDown()', () => {
        expect(comp.shiftFocusDown).toHaveBeenCalled();
      });
    });

    describe('when shiftFocusDown() is triggered', () => {
      beforeEach(() => {
        comp.shiftFocusDown(new KeyboardEvent('keydown.arrowdown'));
        fixture.detectChanges();
      });

      it('should put the focus on the first element ', () => {
        const firstLink = de.query(By.css('.dropdown-list > div:first-child a'));
        const activeElement = el.ownerDocument.activeElement;
        expect(activeElement).toEqual(firstLink.nativeElement);
      });
    });

    describe('when changeFocus() is triggered when selectedIndex is 1', () => {
      beforeEach(() => {
        comp.selectedIndex = 1;
        comp.changeFocus();
        fixture.detectChanges();
      });

      it('should put the focus on the second element', () => {
        const secondLink = de.query(By.css('.dropdown-list > div:nth-child(2) a'));
        const activeElement = el.ownerDocument.activeElement;
        expect(activeElement).toEqual(secondLink.nativeElement);
      });
    });
  });

  describe('when the first element is in focus', () => {
    beforeEach(() => {
      const firstLink = de.query(By.css('.dropdown-list > div:first-child a'));
      firstLink.nativeElement.focus();
      comp.selectedIndex = 0;
      fixture.detectChanges();

    });

    describe('when shiftFocusUp() is triggered', () => {
      beforeEach(() => {
        comp.shiftFocusUp(new KeyboardEvent('keydown.arrowup'));
        fixture.detectChanges();
      });

      it('should put the focus on the last element ', () => {
        const lastLink = de.query(By.css('.dropdown-list > div:last-child a'));
        const activeElement = el.ownerDocument.activeElement;
        expect(activeElement).toEqual(lastLink.nativeElement);
      });
    });

    describe('when shiftFocusDown() is triggered', () => {
      beforeEach(() => {
        comp.shiftFocusDown(new KeyboardEvent('keydown.arrowdown'));
        fixture.detectChanges();
      });

      it('should put the focus on the second element ', () => {
        const secondLink = de.query(By.css('.dropdown-list > div:nth-child(2) a'));
        const activeElement = el.ownerDocument.activeElement;
        expect(activeElement).toEqual(secondLink.nativeElement);
      });
    });
  });

  describe('when the last element is in focus', () => {
    beforeEach(() => {
      const lastLink = de.query(By.css('.dropdown-list > div:last-child a'));
      lastLink.nativeElement.focus();
      comp.selectedIndex = suggestions.length - 1;
      fixture.detectChanges();

    });

    describe('when shiftFocusUp() is triggered', () => {
      beforeEach(() => {
        comp.shiftFocusUp(new KeyboardEvent('keydown.arrowup'));
        fixture.detectChanges();
      });

      it('should put the focus on the second last element ', () => {
        const secondLastLink = de.query(By.css('.dropdown-list > div:nth-last-child(2) a'));
        const activeElement = el.ownerDocument.activeElement;
        expect(activeElement).toEqual(secondLastLink.nativeElement);
      });
    });

    describe('when shiftFocusDown() is triggered', () => {
      beforeEach(() => {
        comp.shiftFocusDown(new KeyboardEvent('keydown.arrowdown'));
        fixture.detectChanges();
      });

      it('should put the focus on the first element ', () => {
        const firstLink = de.query(By.css('.dropdown-list > div:first-child a'));
        const activeElement = el.ownerDocument.activeElement;
        expect(activeElement).toEqual(firstLink.nativeElement);
      });
    });

    describe('when any key but is pressed in the form', () => {
      beforeEach(() => {
        spyOn(comp, 'onKeydown');
        const form = de.query(By.css('form'));
        form.triggerEventHandler('keydown', { key: 'Shift' });
        fixture.detectChanges();
      });

      it('should call onKeydown', () => {
        expect(comp.onKeydown).toHaveBeenCalled();
        fixture.detectChanges();
      });
    });
    describe('when onKeydown is triggered with the Enter key', () => {
      beforeEach(() => {
        spyOn(comp.queryInput.nativeElement, 'focus');
        comp.onKeydown(new KeyboardEvent('keydown', { key: 'Enter' }));
        fixture.detectChanges();
      });

      it('should not change the focus', () => {
        expect(comp.queryInput.nativeElement.focus).not.toHaveBeenCalled();
      });

    });

    describe('when onKeydown is triggered with the any other (not-Enter) key', () => {
      beforeEach(() => {
        spyOn(comp.queryInput.nativeElement, 'focus');
        comp.onKeydown(new KeyboardEvent('keydown', { key: 'Shift' }));
        fixture.detectChanges();
      });

      it('should change the focus', () => {
        expect(comp.queryInput.nativeElement.focus).toHaveBeenCalled();
      });

    });
  });

  describe('when the suggestions list is not empty and show is true', () => {
    beforeEach(() => {
      comp.show.next(true);
      fixture.detectChanges();
    });
    it('should contain an .autocomplete list with a \'show\' class', () => {
      const autocomplete = de.query(By.css('div.autocomplete'));
      expect(autocomplete.nativeElement.classList).toContain('show');
    });
  });

  describe('when the suggestions list is not empty and show is false', () => {
    beforeEach(() => {
      comp.show.next(false);
      fixture.detectChanges();
    });
    it('should contain an .autocomplete list without a \'show\' class', () => {
      const autocomplete = de.query(By.css('div.autocomplete'));
      expect(autocomplete.nativeElement.classList).not.toContain('show');
    });
  });

  describe('when the suggestions list is empty and show is false', () => {
    beforeEach(() => {
      comp.suggestions = [];
      comp.show.next(false);
      fixture.detectChanges();
    });
    it('should contain an .autocomplete list without a \'show\' class', () => {
      const autocomplete = de.query(By.css('div.autocomplete'));
      expect(autocomplete.nativeElement.classList).not.toContain('show');
    });
  });
  describe('when the suggestions list is empty and show is true', () => {
    beforeEach(() => {
      comp.suggestions = [];
      comp.show.next(true);
      fixture.detectChanges();
    });
    it('should contain an .autocomplete list without a \'show\' class', () => {
      const autocomplete = de.query(By.css('div.autocomplete'));
      expect(autocomplete.nativeElement.classList).not.toContain('show');
    });
  });

  describe('when the variable \'show\' is set to true and close() is called', () => {
    beforeEach(() => {
      comp.show.next(true);
      comp.close();
      fixture.detectChanges();
    });
    it('should set \'show\' to false', () => {
      expect(comp.show.getValue()).toBeFalsy();
    });
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
});
