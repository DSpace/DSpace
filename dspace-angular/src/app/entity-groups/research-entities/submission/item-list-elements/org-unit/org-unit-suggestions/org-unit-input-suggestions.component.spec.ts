import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { OrgUnitInputSuggestionsComponent } from './org-unit-input-suggestions.component';
import { FormsModule } from '@angular/forms';

let component: OrgUnitInputSuggestionsComponent;
let fixture: ComponentFixture<OrgUnitInputSuggestionsComponent>;

let suggestions: string[];
let testValue;

function init() {
  suggestions = ['test', 'suggestion', 'example'];
  testValue = 'bla';
}

describe('OrgUnitInputSuggestionsComponent', () => {
  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      declarations: [OrgUnitInputSuggestionsComponent],
      imports: [
        FormsModule,
      ],
      providers: [
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(OrgUnitInputSuggestionsComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(OrgUnitInputSuggestionsComponent);
    component = fixture.componentInstance;
    component.suggestions = suggestions;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('When the component is initialized', () => {
    it('should set the value to the first value of the suggestions', () => {
      expect(component.value).toEqual('test');
    });
  });

  describe('When onSubmit is called', () => {
    it('should set the value to parameter of the method', () => {
      component.onSubmit(testValue);
      expect(component.value).toEqual(testValue);
    });
  });

  describe('When onClickSuggestion is called', () => {
    it('should set the value to parameter of the method', () => {
      component.onClickSuggestion(testValue);
      expect(component.value).toEqual(testValue);
    });
  });

});
